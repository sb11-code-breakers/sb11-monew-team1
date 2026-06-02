package com.sprint.mission.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.user.entity.EmailVerification;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class EmailVerificationRepositoryTest {

  @Autowired
  private EmailVerificationRepository emailVerificationRepository;

  @Nested
  @DisplayName("토큰으로 미사용 인증 조회")
  class FindByTokenAndUsedFalse {

    @Test
    @DisplayName("미사용 토큰으로 조회 성공")
    void 미사용_토큰으로_조회_성공() {
      // given
      EmailVerification verification = EmailVerification.create(UUID.randomUUID());
      emailVerificationRepository.save(verification);

      // when
      Optional<EmailVerification> result =
          emailVerificationRepository.findByTokenAndUsedFalse(verification.getToken());

      // then
      assertThat(result).isPresent();
    }

    @Test
    @DisplayName("사용된 토큰으로 조회 실패")
    void 사용된_토큰으로_조회_실패() {
      // given
      EmailVerification verification = EmailVerification.create(UUID.randomUUID());
      verification.use();
      emailVerificationRepository.save(verification);

      // when
      Optional<EmailVerification> result =
          emailVerificationRepository.findByTokenAndUsedFalse(verification.getToken());

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 조회 실패")
    void 존재하지_않는_토큰으로_조회_실패() {
      // when
      Optional<EmailVerification> result =
          emailVerificationRepository.findByTokenAndUsedFalse("invalid-token");

      // then
      assertThat(result).isEmpty();
    }
  }
}