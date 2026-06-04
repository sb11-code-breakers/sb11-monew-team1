import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

export const options = {
  stages: [
    { duration: '20s', target: 150 }, // 20초 동안 VU 150명까지 가파르게 점증
    { duration: '50s', target: 150 }, // 50초 동안 VU 150명 풀 가동 (최대 임계 타격)
    { duration: '20s', target: 0 },   // 20초 동안 부하 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // RDB가 지옥의 다중조인 속에서 2초를 버티는지 검증
    http_req_failed: ['rate<0.1'],
  },
};

// ==========================================
// 😎 [대규모 확장] 새 스펙에 맞게 150명 UUID 배열 자동 생성
// ==========================================
const USER_IDS = Array.from({ length: 150 }, (_, i) => {
  const userIdx = String(i + 1).padStart(12, '0');
  return `bbbbbbbb-0000-0000-0000-${userIdx}`;
});

export default function () {
  const vuNumber = exec.vu.idInTest;

  // 150명의 고유 유저 ID 풀에서 가상 유저 번호 매핑
  const myUserId = USER_IDS[(vuNumber - 1) % USER_IDS.length];

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Monew-Request-User-ID': myUserId,
    },
  };

  // 이슈 #168 목표 API — 유저 활동 이력 조회 (한 요청당 90건 복합 JOIN 처리 구간)
  const res = http.get(
      `http://localhost:8080/api/user-activities/${myUserId}`,
      params
  );

  check(res, {
    'GET /api/user-activities/{userId} status is 200': (r) => r.status === 200,
  });

  // ⚡ [0.5초 대기] 요청 주기를 절반으로 줄여 부하 밀도를 극대화
  sleep(0.5);
}