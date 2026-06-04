# k6 부하테스트 결과 — 1단계 소규모

## 테스트 환경

- 유저: 10명
- 가상 사용자(VU): 5명
- 테스트 시간: 50초
- DB 방식: PostgreSQL JOIN

## 테스트 데이터

| 테이블               | 건수  |
|-------------------|-----|
| users             | 10  |
| interests         | 5   |
| interest_keywords | 10  |
| articles          | 20  |
| subscriptions     | 20  |
| comments          | 50  |
| comment_likes     | 100 |
| article_views     | 100 |

## 결과

| 지표            | 값        |
|---------------|----------|
| 평균 응답시간 (avg) | 12.48ms  |
| 중간값 (med)     | 11.04ms  |
| p(90)         | 16.98ms  |
| p(95)         | 18.91ms  |
| 최대 응답시간 (max) | 100.09ms |
| 실패율           | 0.00%    |
| TPS           | 4.03/s   |
| 총 요청 수        | 205      |

## 임계값

- p(95) < 2000ms → 18.91ms ✅
- 실패율 < 10% → 0.00% ✅

## 원본 출력

```
     execution: local
        script: k6/load-test.js

  █ THRESHOLDS 
    http_req_duration
    ✓ 'p(95)<2000' p(95)=18.91ms
    http_req_failed
    ✓ 'rate<0.1' rate=0.00%

  █ TOTAL RESULTS 
    checks_total.......: 205     4.034722/s
    checks_succeeded...: 100.00% 205 out of 205
    checks_failed......: 0.00%   0 out of 205
    ✓ user-activity 200
    ✓ articles 200
    ✓ interests 200
    ✓ notifications 200

    http_req_duration..: avg=12.48ms min=4.55ms med=11.04ms max=100.09ms p(90)=16.98ms p(95)=18.91ms
    http_req_failed....: 0.00%  0 out of 205
    http_reqs..........: 205    4.034722/s
    vus................: 1      min=1 max=5
    vus_max............: 5      min=5 max=5

running (0m50.8s), 0/5 VUs, 205 complete and 0 interrupted iterations
default ✓ [======================================] 0/5 VUs  50s
```

## 주의사항

- 결과값은 하드웨어 성능에 따라 다를 수 있음

## 비고

- 소규모에서는 PostgreSQL JOIN 방식도 매우 빠름 (p95=18.91ms)
- 다음 단계(50명 유저, VU 20명)에서 성능 변화 확인 예정