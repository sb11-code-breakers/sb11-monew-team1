package com.sprint.mission.monew.common.util;

public final class HtmlUtils {

  private HtmlUtils() {}

  public static String strip(String html) {
    if (html == null) return "";
    return html.replaceAll("<[^>]*>", "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim();
  }
}
