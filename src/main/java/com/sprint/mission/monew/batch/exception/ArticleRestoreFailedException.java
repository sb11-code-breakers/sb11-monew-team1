package com.sprint.mission.monew.batch.exception;

public class ArticleRestoreFailedException extends BatchException {

  private ArticleRestoreFailedException(String s3Key, Throwable cause) {
    super(BatchErrorCode.ARTICLE_RESTORE_FAILED, s3Key, cause);
  }

  public static ArticleRestoreFailedException withKey(String s3Key, Throwable cause) {
    return new ArticleRestoreFailedException(s3Key, cause);
  }
}
