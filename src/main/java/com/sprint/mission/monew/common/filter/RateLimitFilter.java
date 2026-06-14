package com.sprint.mission.monew.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.common.exception.CommonErrorCode;
import com.sprint.mission.monew.common.util.RequestUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter implements Filter {

  // NOTE: 인메모리 버킷은 인스턴스별로 분리됩니다.
  // 다중 인스턴스(LB) 환경에서는 실효 한도가 (한도 × N)이 됩니다.
  // 분산 Rate Limiting이 필요한 경우 Bucket4j + Redis 백엔드 전환을 고려하세요.

  @Value("${monew.rate-limit.enabled:true}")
  private volatile boolean enabled;

  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final ObjectMapper objectMapper;

  private enum KeyStrategy {
    IP_ONLY,
    USER_ID_OR_IP
  }

  private static final List<RateLimitRule> IP_RULES = List.of(
      new RateLimitRule(HttpMethod.POST, "/api/users/login", "login", 3, Duration.ofMinutes(1), KeyStrategy.IP_ONLY),
      new RateLimitRule(HttpMethod.POST, "/api/users", "signup", 5, Duration.ofHours(1), KeyStrategy.IP_ONLY),
      new RateLimitRule(HttpMethod.POST, "/api/users/password/reset", "pwreset", 3, Duration.ofHours(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.POST, "/api/users/unlock", "unlock", 3, Duration.ofHours(1), KeyStrategy.USER_ID_OR_IP)
  );

  private static final List<RateLimitRule> USER_RULES = List.of(
      new RateLimitRule(HttpMethod.GET, "/api/articles", "articles", 120, Duration.ofMinutes(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.GET, "/api/notifications", "notifications", 60, Duration.ofMinutes(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.POST, "/api/comments", "comments", 10, Duration.ofMinutes(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.POST, "/api/comments/*/likes", "comment-likes", 30, Duration.ofMinutes(1), KeyStrategy.USER_ID_OR_IP)
  );

  // NOTE: CF-Connecting-IP → X-Forwarded-For 순으로 신뢰합니다.
  // Cloudflare 등 신뢰 프록시가 앞단에 없는 환경에서는 X-Forwarded-For 위조로
  // IP 기반 한도가 우회될 수 있습니다. 배포 환경의 프록시 설정을 확인하세요.
  private final Cache<String, Bucket> ipBuckets = Caffeine.newBuilder()
      .maximumSize(10_000)
      .expireAfterAccess(Duration.ofHours(1))
      .build();

  private final Cache<String, Bucket> userBuckets = Caffeine.newBuilder()
      .maximumSize(50_000)
      .expireAfterAccess(Duration.ofHours(1))
      .build();

  public RateLimitFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void clearBuckets() {
    ipBuckets.invalidateAll();
    userBuckets.invalidateAll();
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    if (!enabled) {
      chain.doFilter(req, res);
      return;
    }

    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String path = request.getRequestURI();
    String method = request.getMethod();
    String ip = RequestUtils.resolveClientIp(request);
    String userIdHeader = request.getHeader("Monew-Request-User-ID");

    UUID userId = parseUserId(userIdHeader);
    Bucket bucket = resolveBucket(path, method, ip, userId);

    if (bucket != null && !bucket.tryConsume(1)) {
      log.warn("Rate limit 초과 | path={}, method={}, ip={}", path, method, ip);
      // NOTE: AuthFilter는 handlerExceptionResolver로 예외를 위임하지만,
      // RateLimitFilter는 필터 체인 초기에 실행되어 DispatcherServlet 컨텍스트 밖에서
      // 동작할 수 있으므로 ObjectMapper로 직접 직렬화합니다.
      ErrorResponse errorResponse = new ErrorResponse(
          Instant.now(),
          CommonErrorCode.TOO_MANY_REQUESTS.getCode(),
          CommonErrorCode.TOO_MANY_REQUESTS.getMessage(),
          null,
          "RateLimitFilter",
          429
      );
      response.setStatus(429);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
      return;
    }

    chain.doFilter(req, res);
  }

  private UUID parseUserId(String userIdHeader) {
    if (userIdHeader == null) return null;
    try {
      return UUID.fromString(userIdHeader);
    } catch (IllegalArgumentException e) {
      log.warn("비정상 userId 헤더 감지 | value={}", userIdHeader);
      return null;
    }
  }

  private Bucket resolveBucket(String path, String method, String ip, UUID userId) {
    for (RateLimitRule rule : IP_RULES) {
      if (rule.method().matches(method) && pathMatcher.match(rule.path(), path)) {
        String key = resolveKey(rule.keyStrategy(), ip, userId);
        return ipBuckets.get(rule.prefix() + ":" + key, k -> createBucket(rule));
      }
    }
    for (RateLimitRule rule : USER_RULES) {
      if (rule.method().matches(method) && pathMatcher.match(rule.path(), path)) {
        if (userId == null) {
          return userBuckets.get(rule.prefix() + ":ip:" + ip, k -> createBucket(rule));
        }
        return userBuckets.get(rule.prefix() + ":" + userId, k -> createBucket(rule));
      }
    }
    return null;
  }

  private Bucket createBucket(RateLimitRule rule) {
    return Bucket.builder()
        .addLimit(Bandwidth.classic(rule.limit(), Refill.greedy(rule.limit(), rule.duration())))
        .build();
  }

  private String resolveKey(KeyStrategy strategy, String ip, UUID userId) {
    return switch (strategy) {
      case IP_ONLY -> ip;
      case USER_ID_OR_IP -> userId != null ? userId.toString() : ip;
    };
  }

  private record RateLimitRule(
      HttpMethod method,
      String path,
      String prefix,
      int limit,
      Duration duration,
      KeyStrategy keyStrategy
  ) {}
}