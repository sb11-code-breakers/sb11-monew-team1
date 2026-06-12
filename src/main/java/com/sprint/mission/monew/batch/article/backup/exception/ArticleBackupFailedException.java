package com.sprint.mission.monew.batch.article.backup.exception;

import com.sprint.mission.monew.batch.common.exception.BatchErrorCode;
import com.sprint.mission.monew.batch.common.exception.BatchException;

public class ArticleBackupFailedException extends BatchException {

  private ArticleBackupFailedException(String s3Key, Throwable cause) {
    super(BatchErrorCode.ARTICLE_BACKUP_FAILED, s3Key, cause);
  }

  public static ArticleBackupFailedException withKey(String s3Key, Throwable cause) {
    return new ArticleBackupFailedException(s3Key, cause);
  }
}
