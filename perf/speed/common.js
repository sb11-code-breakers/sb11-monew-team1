// k6 속도 시나리오 공통 모듈 — ID 풀 로드 · 헤더 · 헬퍼.
//
// 환경변수:
//   BASE_URL   대상(기본 http://localhost:8080 — perf/docker-compose.yml)
//   IDS_DIR    ID 풀 csv 위치(기본 '..' = perf/ — extract-ids.sh의 기본 OUT_DIR)
//   PAGE_LIMIT 목록 limit(기본 50)
import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';

export const BASE = __ENV.BASE_URL || 'http://localhost:8080';
export const PAGE_LIMIT = Number(__ENV.PAGE_LIMIT || 50);
const IDS_DIR = __ENV.IDS_DIR || '..'; // 스크립트(perf/speed) 기준 상대경로 → 기본은 perf/

// 좋아요 중복(409)은 "정상적으로 막은 것"이라 실패율에 넣지 않는다 (동시성 아닌 속도 측정).
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 299 }, 409));

// open()은 init 컨텍스트(SharedArray 콜백)에서만 호출 가능.
function loadLines(path) {
  let raw;
  try {
    raw = open(path);
  } catch (e) {
    throw new Error(`ID 풀 파일을 못 엶: ${path} — 먼저 'perf/extract-ids.sh' 실행 필요. (${e})`);
  }
  return raw.split('\n').map((l) => l.trim()).filter(Boolean);
}

// 빈 CSV가 통과하면 pick()이 undefined를 내고 URL/헤더에 섞여 가짜 측정이 된다 → 즉시 중단.
function assertNonEmpty(name, arr) {
  if (!arr.length) {
    throw new Error(`ID 풀 비어 있음: ${name} — 'perf/extract-ids.sh'로 다시 추출하세요.`);
  }
  return arr;
}

export const userIds = new SharedArray('userIds', () =>
  assertNonEmpty('user_ids.csv', loadLines(`${IDS_DIR}/user_ids.csv`)));
export const articleIds = new SharedArray('articleIds', () =>
  assertNonEmpty('article_ids.csv', loadLines(`${IDS_DIR}/article_ids.csv`)));
// comment_ids.csv: "commentId,articleId"
export const comments = new SharedArray('comments', () =>
  assertNonEmpty('comment_ids.csv', loadLines(`${IDS_DIR}/comment_ids.csv`)).map((line) => {
    const [id, articleId] = line.split(',');
    return { id, articleId };
  }),
);

export const pick = (arr) => arr[Math.floor(Math.random() * arr.length)];
export const randomUserId = () => pick(userIds);

// 모든 요청: Monew-Request-User-ID 헤더(JWT 없음).
export function headers(uid) {
  return { 'Monew-Request-User-ID': uid, 'Content-Type': 'application/json' };
}

// name: 엔드포인트 고정 라벨(URL의 UUID로 메트릭이 폭발하지 않게). category: 합격선 분류.
export function tags(name, category) {
  return { name, category };
}

export function expect2xx(res, label) {
  check(res, { [`${label} 2xx`]: (r) => r.status >= 200 && r.status < 300 });
  return res;
}

// 좋아요 전용: 409(중복)는 설계상 정상이므로 통과시키되 5xx 등 실패는 check에 드러낸다.
export function expect2xxOr409(res, label) {
  check(res, { [`${label} 2xx/409`]: (r) => (r.status >= 200 && r.status < 300) || r.status === 409 });
  return res;
}