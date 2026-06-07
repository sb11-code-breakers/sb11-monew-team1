package com.sprint.mission.monew.batch.exception;

import com.sprint.mission.monew.common.exception.InternalErrorCode;
import com.sprint.mission.monew.common.exception.MonewInternalException;

public class NewsCollectJobFailedException extends MonewInternalException {

  private NewsCollectJobFailedException(Throwable cause) {
    super(InternalErrorCode.NEWS_COLLECT_JOB_FAILED, null, cause);
  }

  public static NewsCollectJobFailedException wrap(Throwable cause) {
    return new NewsCollectJobFailedException(cause);
  }
}