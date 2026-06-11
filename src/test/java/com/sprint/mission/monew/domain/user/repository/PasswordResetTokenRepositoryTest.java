package com.sprint.mission.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.user.document.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataMongoTest
@ActiveProfiles("test")
@Import(MongoContainerConfig.class)
class PasswordResetTokenRepositoryTest {

  @Autowired
  private PasswordResetTokenRepository passwordResetTokenRepository;

  @BeforeEach
  void setUp() {
    passwordResetTokenRepository.deleteAll();
  }

  @Nested
  @DisplayName("코드로 유효한 토큰 조회")
  class FindByCodeAndExpiredAtAfter {

    @Test
    @DisplayName("유효한 코드로 조회 성공")
    void 유효한_코드로_조회_성공() {
      // given
      PasswordResetToken token = PasswordResetToken.create(UUID.randomUUID());
      passwordResetTokenRepository.save(token);

      // when
      Optional<PasswordResetToken> result =
          passwordResetTokenRepository.findByCodeAndExpiredAtAfter(
              token.getCode(), Instant.now());

      // then
      assertThat(result).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 코드로 조회 실패")
    void 존재하지_않는_코드로_조회_실패() {
      // when
      Optional<PasswordResetToken> result =
          passwordResetTokenRepository.findByCodeAndExpiredAtAfter(
              "invalid-code", Instant.now());

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("만료된 토큰은 조회되지 않음")
    void 만료된_토큰은_조회되지_않음() {
      // given
      PasswordResetToken token = PasswordResetToken.create(UUID.randomUUID());
      ReflectionTestUtils.setField(token, "expiredAt", Instant.now().minusSeconds(10));
      passwordResetTokenRepository.save(token);

      // when
      Optional<PasswordResetToken> result =
          passwordResetTokenRepository.findByCodeAndExpiredAtAfter(
              token.getCode(), Instant.now());

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("userId로 토큰 삭제")
  class DeleteByUserId {

    @Test
    @DisplayName("userId로 토큰 삭제 성공")
    void userId로_토큰_삭제_성공() {
      // given
      UUID userId = UUID.randomUUID();
      PasswordResetToken token = PasswordResetToken.create(userId);
      passwordResetTokenRepository.save(token);

      // when
      passwordResetTokenRepository.deleteByUserId(userId);

      // then
      assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    }
  }
}