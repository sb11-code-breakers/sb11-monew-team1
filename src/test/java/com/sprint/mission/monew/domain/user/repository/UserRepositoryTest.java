package com.sprint.mission.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import java.util.Optional;
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
  class EmailDuplicateCheck {

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

  @Nested
  @DisplayName("이메일로 활성 사용자 조회")
  class FindByEmailAndDeletedAtIsNull {

    @Test
    @DisplayName("활성 사용자는 조회 성공")
    void 활성_사용자는_조회_성공() {
      // given
      userRepository.save(User.create("test@test.com", "테스터", "encodedPassword"));

      // when
      Optional<User> result = userRepository.findByEmailAndDeletedAtIsNull("test@test.com");

      // then
      assertThat(result).isPresent();
      assertThat(result.get().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("논리 삭제된 사용자는 조회 실패")
    void 논리_삭제된_사용자는_조회_실패() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      user.softDelete();
      userRepository.save(user);

      // when
      Optional<User> result = userRepository.findByEmailAndDeletedAtIsNull("test@test.com");

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 빈 Optional 반환")
    void 존재하지_않는_이메일이면_빈_Optional_반환() {
      // given & when
      Optional<User> result = userRepository.findByEmailAndDeletedAtIsNull("none@test.com");

      // then
      assertThat(result).isEmpty();
    }
  }
}