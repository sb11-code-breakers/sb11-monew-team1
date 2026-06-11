package com.sprint.mission.monew.common.filter;

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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter implements Filter {

  @Value("${monew.rate-limit.enabled:true}")
  private boolean enabled;

  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
  private final Map<UUID, Bucket> userBuckets = new ConcurrentHashMap<>();

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
    String ip = getClientIp(request);
    String userIdHeader = request.getHeader("Monew-Request-User-ID");

    Bucket bucket = resolveBucket(path, method, ip, userIdHeader);

    if (bucket != null && !bucket.tryConsume(1)) {
      log.warn("Rate limit 초과 | path={}, method={}, ip={}", path, method, ip);
      response.setStatus(429);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write(
          "{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"요청 한도를 초과했습니다.\"}");
      return;
    }

    chain.doFilter(req, res);
  }

  private Bucket resolveBucket(String path, String method, String ip, String userIdHeader) {
    if ("POST".equals(method) && isLoginPath(path)) {
      return ipBuckets.computeIfAbsent("login:" + ip,
          k -> Bucket.builder()
              .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(1))))
              .build());
    }
    if ("POST".equals(method) && isSignupPath(path)) {
      return ipBuckets.computeIfAbsent("signup:" + ip,
          k -> Bucket.builder()
              .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofHours(1))))
              .build());
    }
    if ("POST".equals(method) && isPasswordResetPath(path)) {
      String key = userIdHeader != null ? userIdHeader : ip;
      return ipBuckets.computeIfAbsent("pwreset:" + key,
          k -> Bucket.builder()
              .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofHours(1))))
              .build());
    }
    if ("POST".equals(method) && isUnlockPath(path)) {
      String key = userIdHeader != null ? userIdHeader : ip;
      return ipBuckets.computeIfAbsent("unlock:" + key,
          k -> Bucket.builder()
              .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofHours(1))))
              .build());
    }
    if ("GET".equals(method) && isArticlesPath(path)) {
      if (userIdHeader == null) return null;
      UUID userId = UUID.fromString(userIdHeader);
      return userBuckets.computeIfAbsent(userId,
          k -> Bucket.builder()
              .addLimit(Bandwidth.classic(120, Refill.greedy(120, Duration.ofMinutes(1))))
              .build());
    }
    if ("GET".equals(method) && pathMatcher.match("/api/notifications", path)) {
      if (userIdHeader == null) return null;
      UUID userId = UUID.fromString(userIdHeader);
      return userBuckets.computeIfAbsent(userId,
          k -> Bucket.builder()
              .addLimit(Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1))))
              .build());
    }
    if ("POST".equals(method) && pathMatcher.match("/api/comments", path)) {
      if (userIdHeader == null) return null;
      UUID userId = UUID.fromString(userIdHeader);
      return userBuckets.computeIfAbsent(userId,
          k -> Bucket.builder()
              .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
              .build());
    }
    if ("POST".equals(method) && pathMatcher.match("/api/comments/*/likes", path)) {
      if (userIdHeader == null) return null;
      UUID userId = UUID.fromString(userIdHeader);
      return userBuckets.computeIfAbsent(userId,
          k -> Bucket.builder()
              .addLimit(Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1))))
              .build());
    }
    return null;
  }

  private boolean isLoginPath(String path) {
    return pathMatcher.match("/api/users/login", path)
        || pathMatcher.match("/test/rate/login", path);
  }

  private boolean isSignupPath(String path) {
    return pathMatcher.match("/api/users", path)
        || pathMatcher.match("/test/rate/signup", path);
  }

  private boolean isPasswordResetPath(String path) {
    return pathMatcher.match("/api/users/password-reset", path)
        || pathMatcher.match("/test/rate/password-reset", path);
  }

  private boolean isUnlockPath(String path) {
    return pathMatcher.match("/api/users/unlock", path)
        || pathMatcher.match("/test/rate/unlock", path);
  }

  private boolean isArticlesPath(String path) {
    return pathMatcher.match("/api/articles", path)
        || pathMatcher.match("/test/rate/articles", path);
  }

  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isEmpty()) {
      return ip.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}