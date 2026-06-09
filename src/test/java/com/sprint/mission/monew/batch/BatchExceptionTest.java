package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.batch.exception.BatchErrorCode;
import com.sprint.mission.monew.batch.exception.BatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BatchExceptionTest {

  private BatchErrorCode errorCode;

  // 테스트용 구현체 (abstract라 직접 만들 수 없음)
  private static class TestBatchException extends BatchException {

    public TestBatchException(BatchErrorCode errorCode, String detail, Throwable cause) {
      super(errorCode, detail, cause);
    }
  }

  @BeforeEach
  void setUp() {
    errorCode = BatchErrorCode.USER_CLEANUP_JOB_FAILED;
  }

  @Test
  @DisplayName("detail이 null일 경우 기본 메시지만 생성")
  void detail_null이면_메시지만_생성() {
    // given
    // BeforeEach에서 errorCode 초기화
    String detail = null;

    // when
    BatchException exception = new TestBatchException(errorCode, detail, null);

    // then
    assertThat(exception.getMessage()).isEqualTo("사용자 삭제 배치 실행 실패");
  }

  @Test
  @DisplayName("detail이 빈 문자열이면 기본 메시지만 생성")
  void detail_빈_문자열이면_메시지만_생성() {
    // given
    // BeforeEach에서 errorCode 초기화
    String detail = "";

    // when
    BatchException exception = new TestBatchException(errorCode, detail, null);

    // then
    assertThat(exception.getMessage()).isEqualTo("사용자 삭제 배치 실행 실패");
  }

  @Test
  @DisplayName("detail이 존재하면 메시지에 detail을 추가")
  void detail_존재하면_메시지에_detail_추가() {
    // given
    // BeforeEach에서 errorCode 초기화
    String detail = "예외 발생";

    // when
    BatchException exception = new TestBatchException(errorCode, detail, null);

    // then
    assertThat(exception.getMessage()).isEqualTo("사용자 삭제 배치 실행 실패: 예외 발생");
  }
}
