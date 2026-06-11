package com.sprint.mission.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.user.document.UserUnlockToken;
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

@DataMongoTest
@Import(MongoContainerConfig.class)
class UserUnlockTokenRepositoryTest {

  @Autowired
  private UserUnlockTokenRepository userUnlockTokenRepository;

  @BeforeEach
  void setUp() {
    userUnlockTokenRepository.deleteAll();
  }

  @Nested
  @DisplayName("토큰으로 유효한 잠금 해제 토큰 조회")
  class FindByTokenAndExpiredAtAfter {

    @Test
    @DisplayName("유효한 토큰으로 조회 성공")
    void 유효한_토큰으로_조회_성공() {
      // given
      UserUnlockToken token = UserUnlockToken.create(UUID.randomUUID());
      userUnlockTokenRepository.save(token);

      // when
      Optional<UserUnlockToken> result =
          userUnlockTokenRepository.findByTokenAndExpiredAtAfter(
              token.getToken(), Instant.now());

      // then
      assertThat(result).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 조회 실패")
    void 존재하지_않는_토큰으로_조회_실패() {
      // when
      Optional<UserUnlockToken> result =
          userUnlockTokenRepository.findByTokenAndExpiredAtAfter(
              "invalid-token", Instant.now());

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
      UserUnlockToken token = UserUnlockToken.create(userId);
      userUnlockTokenRepository.save(token);

      // when
      userUnlockTokenRepository.deleteByUserId(userId);

      // then
      assertThat(userUnlockTokenRepository.findAll()).isEmpty();
    }
  }
}