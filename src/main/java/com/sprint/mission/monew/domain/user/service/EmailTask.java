package com.sprint.mission.monew.domain.user.service;

public record EmailTask(String email, String token, int retryCount) {

  private static final int MAX_RETRY = 3;

  public EmailTask(String email, String token) {
    this(email, token, 0);
  }

  public EmailTask incrementRetry() {
    return new EmailTask(email, token, retryCount + 1);
  }

  public boolean hasReachedMaxRetry() {
    return retryCount >= MAX_RETRY;
  }
}