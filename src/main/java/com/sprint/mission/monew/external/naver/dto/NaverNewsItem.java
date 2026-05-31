package com.sprint.mission.monew.external.naver.dto;

public record NaverNewsItem(
    String title,
    String originallink,
    String link,
    String description,
    String pubDate
) {}
