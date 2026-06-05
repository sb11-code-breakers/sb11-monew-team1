package com.sprint.mission.monew.batch;

import java.time.Instant;

public record ArticleCandidate(String sourceUrl, String title, Instant publishDate, String summary) {}
