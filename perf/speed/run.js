// 속도 측정 실행 — 엔드포인트별 격리 + 합격선 자동 판정.
//
// 실행:
//   cd perf/speed
//   k6 run -e BASE_URL=http://localhost:8080 --summary-export=summary.json run.js
//   k6 run -e ONLY=commentList -e DURATION_S=120 run.js     # 한 엔드포인트만 빠르게
//
// 환경변수(-e KEY=VALUE 로 전달):
//   VUS(기본 5) · DURATION_S(엔드포인트당 측정 초, 기본 180) · WARMUP_S(기본 60) · COOL_S(기본 30)
//   ONLY=<함수명>  지정 시 워밍업 + 그 엔드포인트만 실행
import {
  articleSearch, articleSearchKeyword, articleGet, commentList, commentListDeep,
  notifications, userActivity, interests, sources,
} from './read.js';
import { createComment, like, articleView } from './write.js';

// k6 scenario의 exec는 "테스트 스크립트(run.js)에서 export된 함수명"을 참조하므로 재export.
export {
  articleSearch, articleSearchKeyword, articleGet, commentList, commentListDeep,
  notifications, userActivity, interests, sources, createComment, like, articleView,
};

const VUS = Number(__ENV.VUS || 5);
const DURATION_S = Number(__ENV.DURATION_S || 180);
const WARMUP_S = Number(__ENV.WARMUP_S || 60);
const COOL_S = Number(__ENV.COOL_S || 30);
const step = DURATION_S + COOL_S; // 엔드포인트 사이 cooldown(GC·캐시 상태 분리)

// 실행 순서(읽기 → 쓰기). ONLY로 한 개만 돌릴 수도 있음.
const ALL = [
  'articleSearch', 'articleSearchKeyword', 'articleGet', 'commentList', 'commentListDeep',
  'notifications', 'userActivity', 'interests', 'sources',
  'createComment', 'like', 'articleView',
];
const targets = __ENV.ONLY ? [__ENV.ONLY] : ALL;

// 워밍업(측정 제외) → 엔드포인트를 겹치지 않게 startTime 어긋나 배치.
const scenarios = {
  warmup: {
    executor: 'constant-vus', vus: VUS, duration: `${WARMUP_S}s`,
    exec: 'articleSearch', startTime: '0s', tags: { phase: 'warmup' },
  },
};
targets.forEach((exec, i) => {
  scenarios[exec] = {
    executor: 'constant-vus', vus: VUS, duration: `${DURATION_S}s`,
    startTime: `${WARMUP_S + i * step}s`, exec, tags: { phase: 'measure' },
  };
});

export const options = {
  scenarios,
  // 합격선 — phase:measure만, 워밍업 제외. 분류별 p95/p99.
  thresholds: {
    'http_req_failed{phase:measure}': ['rate<0.01'],
    'http_req_duration{phase:measure,category:single}': ['p(95)<100', 'p(99)<250'],
    'http_req_duration{phase:measure,category:list}': ['p(95)<300', 'p(99)<800'],
    'http_req_duration{phase:measure,category:mongo}': ['p(95)<400', 'p(99)<1000'],
    'http_req_duration{phase:measure,category:write}': ['p(95)<300', 'p(99)<800'],
  },
};