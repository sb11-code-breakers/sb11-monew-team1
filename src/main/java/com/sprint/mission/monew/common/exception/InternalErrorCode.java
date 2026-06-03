package com.sprint.mission.monew.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InternalErrorCode {

  LOG_BACKUP_FAILED("로그 파일 S3 업로드 실패"),
  LOG_BACKUP_DELETE_FAILED("로컬 로그 파일 삭제 실패");

  private final String message;
}