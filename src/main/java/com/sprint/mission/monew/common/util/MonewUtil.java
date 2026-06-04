package com.sprint.mission.monew.common.util;

public class MonewUtil {

  private MonewUtil() {}

  public static String maskEmail(String email) {
    if (email == null || email.isBlank()) return "***";
    int atIndex = email.indexOf('@');
    if (atIndex <= 1) return "***";
    return email.charAt(0) + "***" + email.substring(atIndex);
  }
}