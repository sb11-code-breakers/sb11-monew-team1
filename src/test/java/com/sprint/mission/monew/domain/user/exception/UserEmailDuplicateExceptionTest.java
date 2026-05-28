package com.sprint.mission.monew.domain.user.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserEmailDuplicateExceptionTest {

  @Nested
  @DisplayName("이메일 마스킹")
  class 이메일_마스킹 {

    @Test
    @DisplayName("일반 이메일은 첫 글자만 남기고 마스킹")
    void 일반_이메일은_첫_글자만_남기고_마스킹() {
      // given & when
      UserEmailDuplicateException ex = UserEmailDuplicateException
          .withEmail("test@example.com");

      // then
      assertThat(ex.getDetails()).containsEntry("email", "t***@example.com");
    }

    @Test
    @DisplayName("로컬파트가 1자 이하면 도메인만 반환")
    void 로컬파트가_1자_이하면_도메인만_반환() {
      // given & when
      UserEmailDuplicateException ex1 = UserEmailDuplicateException
          .withEmail("a@example.com");
      UserEmailDuplicateException ex2 = UserEmailDuplicateException
          .withEmail("@example.com");

      // then
      assertThat(ex1.getDetails()).containsEntry("email", "@example.com");
      assertThat(ex2.getDetails()).containsEntry("email", "@example.com");
    }
  }
}