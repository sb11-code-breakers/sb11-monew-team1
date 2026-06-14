package com.sprint.mission.monew.domain.interest.util;

public class LevenshteinUtils {

  private LevenshteinUtils() {}

  public static double similarity(String a, String b) {
    if (a == null || b == null) {
      throw new IllegalArgumentException("입력 문자열은 null일 수 없습니다");
    }
    int maxLen = Math.max(a.length(), b.length());
    if (maxLen == 0) {
      return 1.0;
    }
    return 1.0 - (double) distance(a, b) / maxLen;
  }

  public static int distance(String a, String b) {
    if (a == null || b == null) {
      throw new IllegalArgumentException("입력 문자열은 null일 수 없습니다");
    }
    int[] prev = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      prev[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      int[] curr = new int[b.length() + 1];
      curr[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
      }
      prev = curr;
    }
    return prev[b.length()];
  }
}