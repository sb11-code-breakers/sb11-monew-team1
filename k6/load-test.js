import http from 'k6/http';
import { sleep, check } from 'k6';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 테스트 옵션 (1단계 소규모)
export const options = {
  stages: [
    { duration: '10s', target: 5 },   // 5명까지 증가
    { duration: '30s', target: 5 },   // 5명 유지
    { duration: '10s', target: 0 },   // 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95%가 2초 이내
    http_req_failed: ['rate<0.1'],     // 실패율 10% 미만
  },
};

const BASE_URL = 'http://localhost:8080';

// DB에서 가져온 유저 UUID
const USER_IDS = [
  'f449d35e-667f-4016-b369-a59c495f26dd',
  'bdcddb24-fccd-4c77-b03f-d75025ad2698',
  '7f5d7623-bf75-4e2b-9206-77d49032313d',
  'f710c442-7c5a-4460-a186-228b5e75ca94',
  'be72eb40-303b-43b6-bcd2-01caeffd0eba',
];

export default function () {
  const userId = randomItem(USER_IDS);
  const headers = { 'Monew-Request-User-ID': userId };

  // API 골고루 랜덤하게 호출
  const apis = [
    () => {
      // 유저 활동 내역 조회
      const res = http.get(`${BASE_URL}/api/user-activities/${userId}`, { headers });
      check(res, { 'user-activity 200': (r) => r.status === 200 });
    },
    () => {
      // 기사 목록 조회
      const res = http.get(`${BASE_URL}/api/articles?limit=10&orderBy=publishDate&direction=DESC`, { headers });
      check(res, { 'articles 200': (r) => r.status === 200 });
    },
    () => {
      // 관심사 목록 조회
      const res = http.get(`${BASE_URL}/api/interests?limit=10&orderBy=name&direction=ASC`, { headers });
      check(res, { 'interests 200': (r) => r.status === 200 });
    },
    () => {
      // 알림 목록 조회
      const res = http.get(`${BASE_URL}/api/notifications?limit=10`, { headers });
      check(res, { 'notifications 200': (r) => r.status === 200 });
    },
  ];

  // 랜덤으로 API 선택해서 호출
  randomItem(apis)();

  sleep(1);
}