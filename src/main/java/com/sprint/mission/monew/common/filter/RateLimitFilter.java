package com.sprint.mission.monew.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.common.exception.CommonErrorCode;
import com.sprint.mission.monew.common.util.RequestUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PreDestroy;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

  @Value("${monew.rate-limit.enabled}")
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
      new RateLimitRule(HttpMethod.POST, "/api/users/password-reset", "pwreset", 3, Duration.ofHours(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.POST, "/api/users/password/reset", "pwreset", 3, Duration.ofHours(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.POST, "/api/users/unlock", "unlock", 3, Duration.ofHours(1), KeyStrategy.USER_ID_OR_IP)
  );

  private static final List<RateLimitRule> USER_RULES = List.of(
      new RateLimitRule(HttpMethod.GET, "/api/articles", "articles", 120, Duration.ofMinutes(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.GET, "/api/notifications", "notifications", 60, Duration.ofMinutes(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.POST, "/api/comments", "comments", 10, Duration.ofMinutes(1), KeyStrategy.USER_ID_OR_IP),
      new RateLimitRule(HttpMethod.POST, "/api/comments/*/likes", "comment-likes", 30, Duration.ofMinutes(1), KeyStrategy.USER_ID_OR_IP)
  );

  private final Map<String, Bucket> ipBuckets = Collections.synchronizedMap(
      new LinkedHashMap<>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
          return size() > 10_000;
        }
      });

  private final Map<String, Bucket> userBuckets = Collections.synchronizedMap(
      new LinkedHashMap<>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
          return size() > 50_000;
        }
      });

  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor();

  public RateLimitFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    scheduler.scheduleAtFixedRate(
        () -> {
          ipBuckets.clear();
          userBuckets.clear();
          log.debug("Rate limit 버킷 초기화 완료");
        },
        1, 1, TimeUnit.HOURS
    );
  }

  public void clearBuckets() {
    ipBuckets.clear();
    userBuckets.clear();
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void runScheduledCleanup() {
    ipBuckets.clear();
    userBuckets.clear();
    log.debug("Rate limit 버킷 초기화 완료");
  }

  @PreDestroy
  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
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
        return ipBuckets.computeIfAbsent(rule.prefix() + ":" + key,
            k -> Bucket.builder()
                .addLimit(Bandwidth.classic(rule.limit(),
                    Refill.greedy(rule.limit(), rule.duration())))
                .build());
      }
    }
    for (RateLimitRule rule : USER_RULES) {
      if (rule.method().matches(method) && pathMatcher.match(rule.path(), path)) {
        if (userId == null) return null;
        return userBuckets.computeIfAbsent(rule.prefix() + ":" + userId,
            k -> Bucket.builder()
                .addLimit(Bandwidth.classic(rule.limit(),
                    Refill.greedy(rule.limit(), rule.duration())))
                .build());
      }
    }
    return null;
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