// k6 동시성(정합성) 보조 스크립트 — 같은 자원에 동시 버스트를 쏴 409/500 분포를 관측한다.
//
// ⚠️ 정합성 "단언"의 1차 도구는 JUnit + CountDownLatch(결정적). 이 스크립트는 **보조 폭격**이고,
//    데이터 불변식(행 수·카운터)은 실행 후 검증 SQL로 확인해야 한다(k6는 HTTP 상태 분포만 본다).
//
// 핵심: http.batch() 로 같은 (user, 자원)에 BURST개 요청을 한 VU에서 병렬 발사 → 서버측 동시성 재현.
//   → 버스트 1회는 반드시 단일 토큰(=단일 유저)이어야 (user,자원) 유니크 경합이 성립한다.
//
// 실행:
//   cd perf/concurrency
//   k6 run -e BASE_URL=http://localhost:8080 -e CASE=c2 concurrency.js   # 좋아요 중복(방어 검증)
//   k6 run -e CASE=c4 -e BURST=30 concurrency.js                        # 조회수 동시(500 재현 — 핵심)
//   k6 run -e CASE=c3 concurrency.js                                    # 구독 중복(관심사 시드 필요)
//
// CASE : c2(좋아요) | c3(구독) | c4(조회수)   ·  BURST(동시 요청 수, 기본 20)
// VUS(기본 20) · DURATION(기본 1m)
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE, headers, pick, randomAuth, loginPool, articleIds, comments } from '../speed/common.js';

const CASE = (__ENV.CASE || 'c2').toLowerCase();
const BURST = Number(__ENV.BURST || 20);
const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || '1m';

const ok2xx = new Counter('concurrency_2xx');     // 성공(보통 버스트당 1개여야 정상)
const conflict409 = new Counter('concurrency_409'); // 정상 거부(실패 아님)
const server5xx = new Counter('concurrency_5xx');   // 진짜 결함(미처리 예외/데드락) — C4 취약점 신호
const c23OverSuccess = new Counter('concurrency_c23_over_success'); // C2·C3 불변식 위반(성공>1) — threshold로 게이트

// 같은 (user, 자원)에 동일 POST를 BURST개 병렬 발사. token 하나로 고정해 단일 유저 경합을 만든다.
function fireBurst(url, token, name) {
  const params = { headers: headers(token), tags: { name } };
  const reqs = Array.from({ length: BURST }, () => ({ method: 'POST', url, params }));
  return http.batch(reqs);
}

// 응답 분포 집계 + 케이스별 불변식 체크.
function analyze(responses, label, expectIdempotent) {
  let okN = 0;
  let c409 = 0;
  let e5xx = 0;
  for (const r of responses) {
    if (r.status >= 200 && r.status < 300) okN++;
    else if (r.status === 409) c409++;
    else if (r.status >= 500) e5xx++;
  }
  ok2xx.add(okN);
  conflict409.add(c409);
  server5xx.add(e5xx);

  if (expectIdempotent) {
    // C4: 멱등이라면 5xx가 없어야 한다. registerView가 DataIntegrityViolationException을
    // 안 잡으면 진 쪽이 500 → 이 체크가 깨지는 것이 곧 취약점 재현 증거.
    check(null, { [`${label}: 5xx 없음(멱등 방어)`]: () => e5xx === 0 });
  } else {
    // C2·C3: 유니크 제약+catch로 방어 → 5xx 0건, 성공 1개 이하(나머지는 409).
    if (okN > 1) c23OverSuccess.add(1); // 불변식 위반 → threshold로 실패 처리(아래 options)
    check(null, {
      [`${label}: 5xx 없음(방어 정상)`]: () => e5xx === 0,
      [`${label}: 성공 1개 이하`]: () => okN <= 1,
    });
  }
}

// ── setup: 토큰 풀 발급(전 CASE 공통). C3는 관심사 id도 필요(medium 시드엔 없음) → 조회해 확보 ──
export function setup() {
  const pool = loginPool();
  if (CASE !== 'c3') return { pool };
  const { token } = randomAuth(pool);
  const res = http.get(`${BASE}/api/interests?orderBy=name&direction=ASC&limit=100`, { headers: headers(token) });
  // status 먼저 확인 — 401/500을 "관심사 없음"으로 오진하지 않도록 진짜 빈 데이터와 장애를 분리.
  if (res.status !== 200) {
    throw new Error(`C3: 관심사 조회 실패(status=${res.status}). 세션 토큰·서버 상태를 확인하세요.`);
  }
  const body = res.json();
  const interestIds = (body && body.content ? body.content : []).map((it) => it.id).filter(Boolean);
  if (interestIds.length === 0) {
    throw new Error('C3: 구독할 관심사가 없습니다. 관심사를 먼저 시드하거나 생성하세요(medium 시드엔 미포함).');
  }
  return { pool, interestIds };
}

export default function (data) {
  switch (CASE) {
    case 'c2': { // 댓글 좋아요 — 같은 (user, comment) 동시 중복
      const { token } = randomAuth(data.pool);
      const c = pick(comments);
      analyze(fireBurst(`${BASE}/api/comments/${c.id}/comment-likes`, token,
        'C2 POST comment-likes'), 'C2', false);
      break;
    }
    case 'c3': { // 관심사 구독 — 같은 (user, interest) 동시 중복
      const { token } = randomAuth(data.pool);
      const interestId = pick(data.interestIds);
      analyze(fireBurst(`${BASE}/api/interests/${interestId}/subscriptions`, token,
        'C3 POST subscriptions'), 'C3', false);
      break;
    }
    case 'c4': { // 기사 조회수 — 같은 (user, article) 동시 (500 재현 — 핵심)
      const { token } = randomAuth(data.pool);
      const articleId = pick(articleIds);
      analyze(fireBurst(`${BASE}/api/articles/${articleId}/article-views`, token,
        'C4 POST article-views'), 'C4', true);
      break;
    }
    default:
      throw new Error(`알 수 없는 CASE: ${CASE} (c2|c3|c4)`);
  }
}

// C2·C3는 방어가 깨지면(5xx 발생) 실패로 본다. C4는 5xx가 "예상된 관측"이라 게이트하지 않는다
// (5xx 카운트는 메트릭 concurrency_5xx 로 보고 → 후속 이슈). 데이터 불변식은 검증 SQL로 별도 확인.
export const options = {
  scenarios: {
    [CASE]: { executor: 'constant-vus', vus: VUS, duration: DURATION },
  },
  thresholds: CASE === 'c4'
    ? {} // C4: 500 재현이 목적이라 임계로 막지 않는다(분포는 요약 메트릭으로 본다)
    : { // C2·C3: 방어가 정상이면 5xx 0건 + 중복 성공(>1) 0건
        concurrency_5xx: ['count==0'],
        concurrency_c23_over_success: ['count==0'],
      },
};