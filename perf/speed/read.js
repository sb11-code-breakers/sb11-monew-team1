// 읽기 엔드포인트 R1~R7 (+ 깊은 페이지).
// orderBy는 WebMvcConfig 커스텀 컨버터 통과값(publishDate/createdAt/name 등), direction은 ASC|DESC.
// 각 함수는 setup()이 넘긴 data.pool([{token,userId}])에서 인증을 무작위로 골라 멀티유저로 분산한다.
import http from 'k6/http';
import {
  BASE, PAGE_LIMIT, headers, tags, randomAuth, pick,
  articleIds, comments, expect2xx,
} from './common.js';

const DEEP_PAGES = Number(__ENV.DEEP_PAGES || 20);

// R1 · 기사 목록 (publishDate 정렬)
export function articleSearch(data) {
  const { token } = randomAuth(data.pool);
  const url = `${BASE}/api/articles?orderBy=publishDate&direction=DESC&limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(token), tags: tags('R1 GET /api/articles', 'list') }), 'R1');
}

// R1' · 기사 검색 (키워드 + commentCount 정렬)
export function articleSearchKeyword(data) {
  const { token } = randomAuth(data.pool);
  const url = `${BASE}/api/articles?keyword=${encodeURIComponent('뉴스')}&orderBy=commentCount&direction=DESC&limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(token), tags: tags('R1k GET /api/articles?keyword', 'list') }), 'R1k');
}

// R2 · 기사 단건 (PK 조회 baseline)
export function articleGet(data) {
  const { token } = randomAuth(data.pool);
  const url = `${BASE}/api/articles/${pick(articleIds)}`;
  expect2xx(http.get(url, { headers: headers(token), tags: tags('R2 GET /api/articles/{id}', 'single') }), 'R2');
}

// R3 · 댓글 목록 (article_id + createdAt 정렬)
export function commentList(data) {
  const { token } = randomAuth(data.pool);
  const c = pick(comments);
  const url = `${BASE}/api/comments?articleId=${c.articleId}&orderBy=createdAt&direction=DESC&limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(token), tags: tags('R3 GET /api/comments', 'list') }), 'R3');
}

// R3d · 댓글 목록 깊은 페이지 (복합 커서 인덱스 효과 — 첫 페이지 대비 평탄해야 정상)
export function commentListDeep(data) {
  const { token } = randomAuth(data.pool);
  const c = pick(comments);
  const base = `${BASE}/api/comments?articleId=${c.articleId}&orderBy=createdAt&direction=DESC&limit=${PAGE_LIMIT}`;
  let url = base;
  let res;
  const pages = Number.isInteger(DEEP_PAGES) && DEEP_PAGES > 0 ? DEEP_PAGES : 1; // 최소 1회 보장
  for (let p = 0; p < pages; p++) {
    res = http.get(url, { headers: headers(token), tags: tags('R3d GET /api/comments(deep)', 'list') });
    if (res.status < 200 || res.status >= 300) break;
    const body = res.json();
    if (!body || !body.hasNext) break;
    const q = `cursor=${encodeURIComponent(body.nextCursor)}`
      + `&after=${encodeURIComponent(body.nextAfter)}`
      + `&idAfter=${body.nextIdAfter}`;
    url = `${base}&${q}`;
  }
  expect2xx(res, 'R3d');
}

// R4 · 알림 목록 (유저별 미확인)
export function notifications(data) {
  const { token } = randomAuth(data.pool);
  const url = `${BASE}/api/notifications?limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(token), tags: tags('R4 GET /api/notifications', 'list') }), 'R4');
}

// R5 · 활동 내역 (MongoDB 집계) — 경로 userId는 인증유저와 같아야 하므로 토큰의 주인 userId를 쓴다.
export function userActivity(data) {
  const { token, userId } = randomAuth(data.pool);
  const url = `${BASE}/api/user-activities/${userId}`;
  expect2xx(http.get(url, { headers: headers(token), tags: tags('R5 GET /api/user-activities/{id}', 'mongo') }), 'R5');
}

// R6 · 관심사 목록 (name 정렬)
export function interests(data) {
  const { token } = randomAuth(data.pool);
  const url = `${BASE}/api/interests?orderBy=name&direction=ASC&limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(token), tags: tags('R6 GET /api/interests', 'list') }), 'R6');
}

// R7 · 출처 목록 (소량 고정 — 최속 기대 baseline)
export function sources(data) {
  const { token } = randomAuth(data.pool);
  const url = `${BASE}/api/articles/sources`;
  expect2xx(http.get(url, { headers: headers(token), tags: tags('R7 GET /api/articles/sources', 'single') }), 'R7');
}