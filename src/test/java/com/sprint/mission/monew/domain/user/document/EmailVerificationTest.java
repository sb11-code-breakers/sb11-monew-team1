package com.sprint.mission.monew.domain.user.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailVerificationTest {

  @Nested
  @DisplayName("이메일 인증 토큰 생성")
  class Create {

    @Test
    @DisplayName("생성 시 토큰은 null이 아님")
    void 생성_시_토큰은_null이_아님() {
      // when
      EmailVerification verification = EmailVerification.create(UUID.randomUUID());

      // then
      assertThat(verification.getToken()).isNotNull();
    }

    @Test
    @DisplayName("생성 시 만료되지 않음")
    void 생성_시_만료되지_않음() {
      // when
      EmailVerification verification = EmailVerification.create(UUID.randomUUID());

      // then
      assertThat(verification.isExpired()).isFalse();
    }
  }
}