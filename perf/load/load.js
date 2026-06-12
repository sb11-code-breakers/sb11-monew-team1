// k6 부하 시나리오 — 한 파일에서 PATTERN(부하 모양) × TARGET(트래픽 대상)을 골라 실행.
// speed/ 의 read·write 함수를 그대로 재사용한다(태그·계약 동일). ID 풀은 perf/extract-ids.sh 산출 사용.
//
// 실행 예:
//   cd perf/load
//   k6 run -e BASE_URL=http://localhost:8080 -e PATTERN=smoke load.js                 # 기준선 확인
//   k6 run -e PATTERN=load  -e TARGET=mix -e VUS=100 --summary-export=load.json load.js   # 동시 100명 합격 판정
//   k6 run -e PATTERN=stress -e TARGET=articleSearch -e RATE=200 load.js              # 포화점 탐색(open)
//   k6 run -e PATTERN=spike -e TARGET=like load.js
//   k6 run -e PATTERN=soak  -e TARGET=mix -e SOAK=30m load.js
//
// PATTERN: smoke | load | stress | spike | soak   (기본 load)
// TARGET : mix | articleSearch | articleSearchKeyword | commentList | commentListDeep
//          | notifications | userActivity | like | createComment | articleView   (기본 mix)
//   ⚠️ commentListDeep는 1 이터레이션 = 순차 20요청이라 open 모델(stress/spike)의 RPS를 왜곡한다.
//      깊은 페이지는 speed 측정 전용으로 쓰고, 부하 TARGET으로는 단일 요청 엔드포인트를 권장.
// 숫자 env: VUS(closed 목표 동시성) · RATE(open 목표 RPS) · RAMP · STEADY · PRE_VUS · MAX_VUS · SOAK
import {
  articleSearch, articleSearchKeyword, commentList, commentListDeep,
  notifications, userActivity,
} from '../speed/read.js';
import { createComment, like, articleView } from '../speed/write.js';

// 복합(현실 트래픽 믹스) — 읽기 80% : 쓰기 20%
export function mix() {
  const r = Math.random();
  if (r < 0.40) articleSearch();        // 기사 목록/검색 40%
  else if (r < 0.60) commentList();     // 댓글 목록      20%
  else if (r < 0.80) notifications();   // 알림 폴링      20%
  else if (r < 0.90) like();            // 좋아요         10%
  else if (r < 0.95) createComment();   // 댓글 작성       5%
  else articleView();                   // 조회수          5%
}

// k6 scenario.exec 는 메인 스크립트에서 export된 함수명을 참조 → 전부 재export.
export {
  articleSearch, articleSearchKeyword, commentList, commentListDeep,
  notifications, userActivity, createComment, like, articleView,
};

const PATTERN = (__ENV.PATTERN || 'load').toLowerCase();
const TARGET = __ENV.TARGET || 'mix';

const VUS = Number(__ENV.VUS || 100);       // closed 목표 동시성
const RATE = Number(__ENV.RATE || 200);     // open 목표 RPS
const RAMP = __ENV.RAMP || '2m';
const STEADY = __ENV.STEADY || '5m';
const PRE_VUS = Number(__ENV.PRE_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || 500);
const SOAK = __ENV.SOAK || '30m';

// arrival-rate(stress/spike)는 maxVUs >= preAllocatedVUs여야 한다. 뒤집히면 RPS를 못 채워 측정이 왜곡됨.
if (MAX_VUS < PRE_VUS) {
  throw new Error(`MAX_VUS(${MAX_VUS})는 PRE_VUS(${PRE_VUS}) 이상이어야 합니다`);
}

function buildScenario() {
  switch (PATTERN) {
    // 기준선·동작 확인
    case 'smoke':
      return { executor: 'constant-vus', exec: TARGET, vus: 2, duration: '1m' };

    // closed 모델: "동시 N명"에서 합격 여부 (응답 느려지면 요청도 같이 줄어듦)
    case 'load':
      return {
        executor: 'ramping-vus', exec: TARGET, startVUs: 0,
        stages: [
          { duration: RAMP, target: VUS },     // 계단식 증가
          { duration: STEADY, target: VUS },   // 평탄 유지(합격 판정 구간)
          { duration: '30s', target: 0 },
        ],
      };

    // open 모델: 초당 요청수를 강제 주입 → 밀리는 게 그대로 드러남(포화점 탐색)
    case 'stress':
      return {
        executor: 'ramping-arrival-rate', exec: TARGET,
        startRate: Math.max(1, Math.floor(RATE / 8)), timeUnit: '1s',
        preAllocatedVUs: PRE_VUS, maxVUs: MAX_VUS,
        stages: [
          { duration: '2m', target: Math.floor(RATE * 0.25) },
          { duration: '2m', target: Math.floor(RATE * 0.5) },
          { duration: '2m', target: RATE },
          { duration: '2m', target: RATE * 2 },
          { duration: '2m', target: RATE * 4 }, // 무너질 때까지 계단 증가
        ],
      };

    // 순간 폭주 회복력
    case 'spike':
      return {
        executor: 'ramping-arrival-rate', exec: TARGET,
        startRate: 10, timeUnit: '1s',
        preAllocatedVUs: PRE_VUS, maxVUs: MAX_VUS,
        stages: [
          { duration: '30s', target: 10 },
          { duration: '15s', target: RATE * 3 }, // 급증
          { duration: '1m', target: RATE * 3 },
          { duration: '15s', target: 10 },       // 급감
          { duration: '1m', target: 10 },        // 회복 관찰
        ],
      };

    // 누수·서서히 악화(메모리/커넥션)
    case 'soak':
      return {
        executor: 'constant-vus', exec: TARGET,
        vus: Math.max(1, Math.floor(VUS / 2)), duration: SOAK,
      };

    default:
      throw new Error(`알 수 없는 PATTERN: ${PATTERN} (smoke|load|stress|spike|soak)`);
  }
}

export const options = {
  scenarios: { [PATTERN]: buildScenario() },
  // 합격선. abortOnFail 미설정(기본 false) → stress에서 포화로 FAIL이 떠도 끝까지 돌려
  // 단계별 출력에서 "어느 VU/RPS부터 무너지는지" 포화점을 읽는다(stress는 FAIL이 정상 관측).
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1500'],
  },
};