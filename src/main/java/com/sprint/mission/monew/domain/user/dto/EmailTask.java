package com.sprint.mission.monew.domain.user.dto;

public record EmailTask(String email, String token, int retryCount, EmailTaskType type) {

  private static final int MAX_RETRY = 3;

  public EmailTask(String email, String token) {
    this(email, token, 0, EmailTaskType.VERIFICATION);
  }

  public EmailTask(String email, String token, EmailTaskType type) {
    this(email, token, 0, type);
  }

  public EmailTask incrementRetry() {
    return new EmailTask(email, token, retryCount + 1, type);
  }

  public boolean hasReachedMaxRetry() {
    return retryCount >= MAX_RETRY;
  }
}