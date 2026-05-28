package com.sprint.mission.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Nested
  @DisplayName("이메일 중복 확인")
  class 이메일_중복_확인 {

    @Test
    @DisplayName("존재하는 이메일이면 true 반환")
    void 존재하는_이메일이면_true_반환() {
      // given
      userRepository.save(User.create("test@test.com", "테스터", "encodedPassword"));

      // when
      boolean result = userRepository.existsByEmail("test@test.com");

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 false 반환")
    void 존재하지_않는_이메일이면_false_반환() {
      // given & when
      boolean result = userRepository.existsByEmail("none@test.com");

      // then
      assertThat(result).isFalse();
    }
  }
}