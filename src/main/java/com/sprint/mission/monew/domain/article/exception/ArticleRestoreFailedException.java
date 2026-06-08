package com.sprint.mission.monew.domain.article.exception;

import com.sprint.mission.monew.common.exception.InternalErrorCode;
import com.sprint.mission.monew.common.exception.MonewInternalException;

public class ArticleRestoreFailedException extends MonewInternalException {

  private ArticleRestoreFailedException(String s3Key, Throwable cause) {
    super(InternalErrorCode.ARTICLE_RESTORE_FAILED, s3Key, cause);
  }

  public static ArticleRestoreFailedException withKey(String s3Key, Throwable cause) {
    return new ArticleRestoreFailedException(s3Key, cause);
  }
}
