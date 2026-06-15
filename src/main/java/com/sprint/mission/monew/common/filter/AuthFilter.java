package com.sprint.mission.monew.common.filter;

import com.sprint.mission.monew.common.exception.AuthException;
import com.sprint.mission.monew.common.exception.ForbiddenAdminException;
import com.sprint.mission.monew.common.exception.UnauthorizedException;
import com.sprint.mission.monew.common.util.RequestUtils;
import com.sprint.mission.monew.domain.user.document.UserSession;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthFilter implements Filter {

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private record MethodPath(HttpMethod method, String path) {}

  private static final List<MethodPath> EXCLUDED = List.of(
      new MethodPath(HttpMethod.GET, "/"),
      new MethodPath(HttpMethod.GET, "/#/**"),
      new MethodPath(HttpMethod.GET, "/index.html"),
      new MethodPath(HttpMethod.GET, "/favicon.ico"),
      new MethodPath(HttpMethod.GET, "/assets/**"),
      new MethodPath(HttpMethod.GET, "/fonts/**"),
      new MethodPath(HttpMethod.GET, "/.well-known/**"),
      new MethodPath(HttpMethod.GET, "/swagger-ui/**"),
      new MethodPath(HttpMethod.GET, "/v3/api-docs/**"),
      new MethodPath(HttpMethod.GET, "/actuator/**"),
      new MethodPath(HttpMethod.POST, "/api/users"),
      new MethodPath(HttpMethod.POST, "/api/users/login"),
      new MethodPath(HttpMethod.GET, "/api/users/verify"),
      new MethodPath(HttpMethod.POST, "/api/users/password/reset"),
      new MethodPath(HttpMethod.PATCH, "/api/users/password/reset"),
      new MethodPath(HttpMethod.POST, "/api/users/unlock"),
      new MethodPath(HttpMethod.GET, "/api/users/unlock")
  );

  private static final List<MethodPath> ADMIN_ONLY = List.of(
      new MethodPath(HttpMethod.DELETE, "/api/users/*/hard"),
      new MethodPath(HttpMethod.DELETE, "/api/articles/*/hard"),
      new MethodPath(HttpMethod.DELETE, "/api/comments/*/hard")
  );

  private final UserSessionRepository userSessionRepository;
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Value("${monew.session.timeout-minutes}")
  private int sessionTimeoutMinutes;

  @Value("${monew.admin-token}")
  private String adminToken;

  @Autowired
  public AuthFilter(UserSessionRepository userSessionRepository,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
    this.userSessionRepository = userSessionRepository;
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    String requestId = UUID.randomUUID().toString();
    MDC.put("requestId", requestId);
    MDC.put("method", request.getMethod());
    MDC.put("url", request.getRequestURI());
    MDC.put("clientIp", RequestUtils.resolveClientIp(request));
    response.setHeader("Monew-Request-ID", requestId);

    try {
      if (isExcluded(request.getMethod(), request.getRequestURI())) {
        chain.doFilter(request, response);
        return;
      }

      if (isAdminOnly(request.getMethod(), request.getRequestURI())) {
        String token = request.getHeader("Monew-Request-User-ID");
        if (token == null || !token.equals(adminToken)) {
          throw ForbiddenAdminException.of();
        }
        chain.doFilter(request, response);
        return;
      }

      String token = request.getHeader("Monew-Request-User-ID");
      if (token == null || token.isBlank()) {
        throw UnauthorizedException.of();
      }
      UUID sessionToken;
      try {
        sessionToken = UUID.fromString(token);
      } catch (IllegalArgumentException e) {
        throw UnauthorizedException.of();
      }
      UserSession session = userSessionRepository.findById(sessionToken)
          .orElseThrow(UnauthorizedException::of);
      if (session.getExpiresAt().isBefore(java.time.Instant.now())) {
        userSessionRepository.deleteById(session.getId());
        throw UnauthorizedException.of();
      }
      String currentIp = RequestUtils.resolveClientIp(request);
      if (!RequestUtils.isSameSubnet24(session.getIp(), currentIp)) {
        userSessionRepository.deleteById(session.getId());
        throw UnauthorizedException.of();
      }
      String currentFingerprint = RequestUtils.buildFingerprint(request);
      if (!session.getDeviceFingerprint().equals(currentFingerprint)) {
        userSessionRepository.deleteById(session.getId());
        throw UnauthorizedException.of();
      }
      session.refreshExpiry(sessionTimeoutMinutes);
      userSessionRepository.save(session);
      chain.doFilter(new UserIdHeaderWrapper(request, session.getUserId()), response);
    } catch (AuthException e) {
      handlerExceptionResolver.resolveException(request, response, null, e);
    } finally {
      MDC.clear();
    }
  }

  private boolean isExcluded(String method, String uri) {
    HttpMethod httpMethod = HttpMethod.valueOf(method);
    return EXCLUDED.stream()
        .anyMatch(e -> e.method() == httpMethod && PATH_MATCHER.match(e.path(), uri));
  }

  private boolean isAdminOnly(String method, String uri) {
    HttpMethod httpMethod = HttpMethod.valueOf(method);
    return ADMIN_ONLY.stream()
        .anyMatch(e -> e.method() == httpMethod && PATH_MATCHER.match(e.path(), uri));
  }

  private static class UserIdHeaderWrapper extends HttpServletRequestWrapper {

    private final String userId;

    UserIdHeaderWrapper(HttpServletRequest request, UUID userId) {
      super(request);
      this.userId = userId.toString();
    }

    @Override
    public String getHeader(String name) {
      if ("Monew-Request-User-ID".equals(name)) {
        return userId;
      }
      return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      if ("Monew-Request-User-ID".equals(name)) {
        return Collections.enumeration(List.of(userId));
      }
      return super.getHeaders(name);
    }
  }
}