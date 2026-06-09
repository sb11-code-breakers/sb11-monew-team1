package com.sprint.mission.monew.batch.exception;

public class ArticleBackupFailedException extends BatchException {

  private ArticleBackupFailedException(String s3Key, Throwable cause) {
    super(BatchErrorCode.ARTICLE_BACKUP_FAILED, s3Key, cause);
  }

  public static ArticleBackupFailedException withKey(String s3Key, Throwable cause) {
    return new ArticleBackupFailedException(s3Key, cause);
  }
}
