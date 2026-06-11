package com.sprint.mission.monew.batch.news.collect.exception;

import com.sprint.mission.monew.batch.common.exception.BatchErrorCode;
import com.sprint.mission.monew.batch.common.exception.BatchException;

public class NewsCollectJobFailedException extends BatchException {

  private NewsCollectJobFailedException(Throwable cause) {
    super(BatchErrorCode.NEWS_COLLECT_JOB_FAILED, null, cause);
  }

  public static NewsCollectJobFailedException wrap(Throwable cause) {
    return new NewsCollectJobFailedException(cause);
  }
}