package com.sprint.mission.monew.domain.user.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserEmailDuplicateExceptionTest {

  @Nested
  @DisplayName("이메일 마스킹")
  class EmailMasking {

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
    @DisplayName("로컬파트가 없거나 골뱅이가 존재하지 않으면 도메인만 반환")
    void 로컬파트가_없거나_골뱅이가_존재하지_않으면_도메인만_반환() {
      // given & when
      UserEmailDuplicateException ex1 = UserEmailDuplicateException
          .withEmail("@example.com");
      UserEmailDuplicateException ex2 = UserEmailDuplicateException
          .withEmail("example.com");

      // then
      assertThat(ex1.getDetails()).containsEntry("email", "REDACTED");
      assertThat(ex2.getDetails()).containsEntry("email", "REDACTED");
    }
  }
}