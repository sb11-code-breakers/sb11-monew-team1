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

export default function () {
  const vuNumber = exec.vu.idInTest;
  const iterationNumber = exec.vu.iterationInInstance;

  const userIdx = String(vuNumber).padStart(12, '0');
  const myUserId = `bbbbbbbb-0000-0000-0000-${userIdx}`;

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Monew-Request-User-ID': myUserId,
    },
  };

  // 기사 순환 (100개)
  const articleIdx = String(((vuNumber + iterationNumber) % 100) + 1).padStart(12, '0');
  const targetArticleId = `aaaaaaaa-0000-0000-0000-${articleIdx}`;

  // 댓글 순환 (2500개)
  const commentIdx = String(((vuNumber + iterationNumber) % 2500) + 1).padStart(12, '0');
  const targetCommentId = `cccccccc-0000-0000-0000-${commentIdx}`;

  // 댓글 목록 조회
  const queryParams = `?articleId=${targetArticleId}&limit=10&orderBy=createdAt&direction=DESC`;
  const getCommentsRes = http.get(`http://localhost:8080/api/comments${queryParams}`, params);
  check(getCommentsRes, {
    'GET /api/comments status is 200': (r) => r.status === 200,
  });
  sleep(1);

  // 댓글 좋아요 등록
  const likeRes = http.post(
      `http://localhost:8080/api/comments/${targetCommentId}/comment-likes`,
      null,
      params
  );
  check(likeRes, {
    'POST /comment-likes status is 201': (r) => r.status === 201,
  });
  sleep(1);
}