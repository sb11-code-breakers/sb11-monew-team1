package com.sprint.mission.monew.batch.article.backup.exception;

import com.sprint.mission.monew.batch.common.exception.BatchErrorCode;
import com.sprint.mission.monew.batch.common.exception.BatchException;

public class ArticleBackupJobFailedException extends BatchException {

  private ArticleBackupJobFailedException(Throwable cause) {
    super(BatchErrorCode.ARTICLE_BACKUP_JOB_FAILED, null, cause);
  }

  public static ArticleBackupJobFailedException wrap(Throwable cause) {
    return new ArticleBackupJobFailedException(cause);
  }
}
