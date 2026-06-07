package com.sprint.mission.monew.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InternalErrorCode {

  USER_CLEANUP_JOB_FAILED("사용자 삭제 배치 실행 실패"),
  NOTIFICATION_CLEANUP_JOB_FAILED("알림 삭제 배치 실행 실패"),
  NEWS_COLLECT_JOB_FAILED("뉴스 수집 배치 실행 실패"),
  LOG_BACKUP_JOB_FAILED("로그 백업 배치 실행 실패"),
  LOG_BACKUP_FAILED("로그 파일 S3 업로드 실패"),
  LOG_BACKUP_DELETE_FAILED("로컬 로그 파일 삭제 실패"),
  ARTICLE_BACKUP_FAILED("기사 S3 백업 실패");

  private final String message;
}