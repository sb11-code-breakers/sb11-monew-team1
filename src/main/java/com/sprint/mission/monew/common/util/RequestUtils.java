package com.sprint.mission.monew.common.util;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.util.DigestUtils;

public final class RequestUtils {

  private RequestUtils() {
  }

  public static String buildFingerprint(HttpServletRequest request) {
    String raw = request.getHeader("User-Agent") + "|"
        + request.getHeader("Accept-Language") + "|"
        + request.getHeader("Accept-Encoding");
    return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
  }

  public static boolean isSameSubnet24(String a, String b) {
    return subnet24(a).equals(subnet24(b));
  }

  private static String subnet24(String ip) {
    int last = ip.lastIndexOf('.');
    return last < 0 ? ip : ip.substring(0, last);
  }

  public static String resolveClientIp(HttpServletRequest request) {
    String cf = request.getHeader("CF-Connecting-IP");
    if (cf != null && !cf.isBlank()) {
      return cf;
    }
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}