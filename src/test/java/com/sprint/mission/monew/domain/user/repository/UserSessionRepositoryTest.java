package com.sprint.mission.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.user.document.UserSession;
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
class UserSessionRepositoryTest {

  @Autowired
  private UserSessionRepository userSessionRepository;

  @BeforeEach
  void setUp() {
    userSessionRepository.deleteAll();
  }

  @Nested
  @DisplayName("ID로 세션 조회")
  class FindById {

    @Test
    @DisplayName("저장된 세션을 ID로 조회 성공")
    void 저장된_세션을_ID로_조회_성공() {
      // given
      UserSession session = UserSession.create(UUID.randomUUID(), "1.2.3.4", "fp-abc", 30);
      userSessionRepository.save(session);

      // when
      Optional<UserSession> result = userSessionRepository.findById(session.getId());

      // then
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(session.getId());
    }
    
    @Test
    @DisplayName("존재하지 않는 ID 조회 시 빈 Optional 반환")
    void 존재하지_않는_ID_조회_시_빈_Optional_반환() {
      // when
      Optional<UserSession> result = userSessionRepository.findById(UUID.randomUUID());

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("세션 삭제")
  class DeleteById {

    @Test
    @DisplayName("삭제 후 조회 시 빈 Optional 반환")
    void 삭제_후_조회_시_빈_Optional_반환() {
      // given
      UserSession session = UserSession.create(UUID.randomUUID(), "1.2.3.4", "fp-abc", 30);
      userSessionRepository.save(session);

      // when
      userSessionRepository.deleteById(session.getId());

      // then
      assertThat(userSessionRepository.findById(session.getId())).isEmpty();
    }
  }
}