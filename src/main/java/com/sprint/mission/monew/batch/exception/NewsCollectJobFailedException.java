package com.sprint.mission.monew.batch.exception;

public class NewsCollectJobFailedException extends BatchException {

  private NewsCollectJobFailedException(Throwable cause) {
    super(BatchErrorCode.NEWS_COLLECT_JOB_FAILED, null, cause);
  }

  public static NewsCollectJobFailedException wrap(Throwable cause) {
    return new NewsCollectJobFailedException(cause);
  }
}