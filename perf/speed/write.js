// 쓰기 엔드포인트 W1~W3 (단건 latency).
// 입력을 매 반복 분산해 유니크 충돌·중복으로 흐름이 끊기지 않게 한다.
import http from 'k6/http';
import { BASE, headers, tags, randomUserId, pick, articleIds, comments, expect2xx } from './common.js';

// W1 · 댓글 작성 (INSERT + article.commentCount UPDATE). 바디에 userId 필수(CommentCreateRequest).
export function createComment() {
  const uid = randomUserId();
  const body = JSON.stringify({
    articleId: pick(articleIds),
    userId: uid,
    content: `load comment ${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  });
  const res = http.post(`${BASE}/api/comments`, body, {
    headers: headers(uid),
    tags: tags('W1 POST /api/comments', 'write'),
  });
  // 정상 생성은 201
  expect2xx(res, 'W1');
}

// W2 · 댓글 좋아요 (INSERT + likeCount UPDATE + 알림 이벤트). 바디 없음, 헤더로 userId.
// 같은 (user,comment)면 409 — 속도 측정에선 정상 응답시간으로 간주(common.js에서 409를 실패 제외).
export function like() {
  const uid = randomUserId();
  const c = pick(comments);
  http.post(`${BASE}/api/comments/${c.id}/comment-likes`, null, {
    headers: headers(uid),
    tags: tags('W2 POST /api/comments/{id}/comment-likes', 'write'),
  });
}

// W3 · 조회수 기록 (check-then-act → INSERT + viewCount UPDATE). 바디 없음.
export function articleView() {
  const uid = randomUserId();
  http.post(`${BASE}/api/articles/${pick(articleIds)}/article-views`, null, {
    headers: headers(uid),
    tags: tags('W3 POST /api/articles/{id}/article-views', 'write'),
  });
}