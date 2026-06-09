package com.sprint.mission.monew.common.interceptor;

import com.sprint.mission.monew.common.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MdcLoggingInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    if (MDC.get("requestId") == null) {
      String requestId = UUID.randomUUID().toString().substring(0, 8);
      MDC.put("requestId", requestId);
      MDC.put("method", request.getMethod());
      MDC.put("url", request.getRequestURI());
      MDC.put("clientIp", RequestUtils.resolveClientIp(request));
    }
    response.setHeader("Monew-Request-ID", MDC.get("requestId"));
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    MDC.clear();
  }


}
