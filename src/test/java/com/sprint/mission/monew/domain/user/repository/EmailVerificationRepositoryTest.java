package com.sprint.mission.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.domain.user.entity.EmailVerification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmailVerificationRepositoryTest {

  @Autowired
  private EmailVerificationRepository emailVerificationRepository;

  @Nested
  @DisplayName("토큰으로 유효한 인증 조회")
  class FindByTokenAndExpiredAtAfter {

    @Test
    @DisplayName("유효한 토큰으로 조회 성공")
    void 유효한_토큰으로_조회_성공() {
      // given
      EmailVerification verification = EmailVerification.create(UUID.randomUUID());
      emailVerificationRepository.save(verification);

      // when
      Optional<EmailVerification> result =
          emailVerificationRepository.findByTokenAndExpiredAtAfter(
              verification.getToken(), Instant.now());

      // then
      assertThat(result).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 조회 실패")
    void 존재하지_않는_토큰으로_조회_실패() {
      // when
      Optional<EmailVerification> result =
          emailVerificationRepository.findByTokenAndExpiredAtAfter(
              "invalid-token", Instant.now());

      // then
      assertThat(result).isEmpty();
    }
  }
}