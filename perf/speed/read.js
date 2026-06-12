// 읽기 엔드포인트 R1~R7 (+ 깊은 페이지).
// orderBy는 WebMvcConfig 커스텀 컨버터 통과값(publishDate/createdAt/name 등), direction은 ASC|DESC.
import http from 'k6/http';
import {
  BASE, PAGE_LIMIT, headers, tags, randomUserId, pick,
  articleIds, comments, expect2xx,
} from './common.js';

const DEEP_PAGES = Number(__ENV.DEEP_PAGES || 20);

// R1 · 기사 목록 (publishDate 정렬)
export function articleSearch() {
  const uid = randomUserId();
  const url = `${BASE}/api/articles?orderBy=publishDate&direction=DESC&limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(uid), tags: tags('R1 GET /api/articles', 'list') }), 'R1');
}

// R1' · 기사 검색 (키워드 + commentCount 정렬)
export function articleSearchKeyword() {
  const uid = randomUserId();
  const url = `${BASE}/api/articles?keyword=${encodeURIComponent('뉴스')}&orderBy=commentCount&direction=DESC&limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(uid), tags: tags('R1k GET /api/articles?keyword', 'list') }), 'R1k');
}

// R2 · 기사 단건 (PK 조회 baseline)
export function articleGet() {
  const uid = randomUserId();
  const url = `${BASE}/api/articles/${pick(articleIds)}`;
  expect2xx(http.get(url, { headers: headers(uid), tags: tags('R2 GET /api/articles/{id}', 'single') }), 'R2');
}

// R3 · 댓글 목록 (article_id + createdAt 정렬)
export function commentList() {
  const uid = randomUserId();
  const c = pick(comments);
  const url = `${BASE}/api/comments?articleId=${c.articleId}&orderBy=createdAt&direction=DESC&limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(uid), tags: tags('R3 GET /api/comments', 'list') }), 'R3');
}

// R3d · 댓글 목록 깊은 페이지 (복합 커서 인덱스 효과 — 첫 페이지 대비 평탄해야 정상)
export function commentListDeep() {
  const c = pick(comments);
  const base = `${BASE}/api/comments?articleId=${c.articleId}&orderBy=createdAt&direction=DESC&limit=${PAGE_LIMIT}`;
  let url = base;
  let res;
  const pages = Number.isInteger(DEEP_PAGES) && DEEP_PAGES > 0 ? DEEP_PAGES : 1; // 최소 1회 보장
  for (let p = 0; p < pages; p++) {
    res = http.get(url, { headers: headers(randomUserId()), tags: tags('R3d GET /api/comments(deep)', 'list') });
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
export function notifications() {
  const uid = randomUserId();
  const url = `${BASE}/api/notifications?limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(uid), tags: tags('R4 GET /api/notifications', 'list') }), 'R4');
}

// R5 · 활동 내역 (MongoDB 집계) — 헤더의 userId와 경로 userId 동일하게
export function userActivity() {
  const uid = randomUserId();
  const url = `${BASE}/api/user-activities/${uid}`;
  expect2xx(http.get(url, { headers: headers(uid), tags: tags('R5 GET /api/user-activities/{id}', 'mongo') }), 'R5');
}

// R6 · 관심사 목록 (name 정렬)
export function interests() {
  const uid = randomUserId();
  const url = `${BASE}/api/interests?orderBy=name&direction=ASC&limit=${PAGE_LIMIT}`;
  expect2xx(http.get(url, { headers: headers(uid), tags: tags('R6 GET /api/interests', 'list') }), 'R6');
}

// R7 · 출처 목록 (소량 고정 — 최속 기대 baseline)
export function sources() {
  const uid = randomUserId();
  const url = `${BASE}/api/articles/sources`;
  expect2xx(http.get(url, { headers: headers(uid), tags: tags('R7 GET /api/articles/sources', 'single') }), 'R7');
}