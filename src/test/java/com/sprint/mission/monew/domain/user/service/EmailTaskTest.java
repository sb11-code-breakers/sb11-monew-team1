package com.sprint.mission.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailTaskTest {

  @Nested
  @DisplayName("EmailTask 생성")
  class Create {

    @Test
    @DisplayName("생성 시 retryCount는 0")
    void 생성_시_retryCount는_0() {
      // when
      EmailTask task = new EmailTask("test@test.com", "token123");

      // then
      assertThat(task.retryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("생성 시 MAX_RETRY 미달성")
    void 생성_시_MAX_RETRY_미달성() {
      // when
      EmailTask task = new EmailTask("test@test.com", "token123");

      // then
      assertThat(task.hasReachedMaxRetry()).isFalse();
    }
  }

  @Nested
  @DisplayName("재시도 횟수 증가")
  class IncrementRetry {

    @Test
    @DisplayName("incrementRetry 호출 시 retryCount 1 증가")
    void incrementRetry_호출_시_retryCount_1_증가() {
      // given
      EmailTask task = new EmailTask("test@test.com", "token123");

      // when
      EmailTask retried = task.incrementRetry();

      // then
      assertThat(retried.retryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("세번 재시도 시 MAX_RETRY 달성")
    void 세번_재시도_시_MAX_RETRY_달성() {
      // given
      EmailTask task = new EmailTask("test@test.com", "token123");

      // when
      EmailTask retried = task.incrementRetry().incrementRetry().incrementRetry();

      // then
      assertThat(retried.hasReachedMaxRetry()).isTrue();
    }

    @Test
    @DisplayName("두번 재시도는 MAX_RETRY 미달성")
    void 두번_재시도는_MAX_RETRY_미달성() {
      // given
      EmailTask task = new EmailTask("test@test.com", "token123");

      // when
      EmailTask retried = task.incrementRetry().incrementRetry();

      // then
      assertThat(retried.hasReachedMaxRetry()).isFalse();
    }
  }
  @Nested
  @DisplayName("EmailTask 타입")
  class TaskType {

    @Test
    @DisplayName("기본 생성 시 VERIFICATION 타입")
    void 기본_생성_시_VERIFICATION_타입() {
      // when
      EmailTask task = new EmailTask("test@test.com", "token123");

      // then
      assertThat(task.type()).isEqualTo(EmailTaskType.VERIFICATION);
    }

    @Test
    @DisplayName("PASSWORD_RESET 타입으로 생성")
    void PASSWORD_RESET_타입으로_생성() {
      // when
      EmailTask task = new EmailTask("test@test.com", "code123", EmailTaskType.PASSWORD_RESET);

      // then
      assertThat(task.type()).isEqualTo(EmailTaskType.PASSWORD_RESET);
    }

    @Test
    @DisplayName("incrementRetry 시 타입 유지")
    void incrementRetry_시_타입_유지() {
      // given
      EmailTask task = new EmailTask("test@test.com", "code123", EmailTaskType.PASSWORD_RESET);

      // when
      EmailTask retried = task.incrementRetry();

      // then
      assertThat(retried.type()).isEqualTo(EmailTaskType.PASSWORD_RESET);
    }
  }
}