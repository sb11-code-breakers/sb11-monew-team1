package com.sprint.mission.monew.external.naver.dto;

import java.util.List;

public record NaverNewsResponse(
    int total,
    int start,
    int display,
    List<NaverNewsItem> items
) {}
