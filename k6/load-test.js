import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

export const options = {
  stages: [
    { duration: '20s', target: 50 },
    { duration: '50s', target: 50 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.1'],
  },
};

const USER_IDS = Array.from({ length: 50 }, (_, i) => {
  const userIdx = String(i + 1).padStart(12, '0');
  return `bbbbbbbb-0000-0000-0000-${userIdx}`;
});

export default function () {
  const vuNumber = exec.vu.idInTest;

  const myUserId = USER_IDS[(vuNumber - 1) % USER_IDS.length];

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Monew-Request-User-ID': myUserId,
    },
  };

  // 이슈 #168 목표 API — 유저 활동 이력 조회 (다중 JOIN 성능 측정)
  const res = http.get(
      `http://localhost:8080/api/user-activities/${myUserId}`,
      params
  );

  check(res, {
    'GET /api/user-activities/{userId} status is 200': (r) => r.status === 200,
  });

  sleep(1);
}