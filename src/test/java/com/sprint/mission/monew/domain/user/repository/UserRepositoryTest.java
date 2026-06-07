package com.sprint.mission.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.batch.dto.UserCleanupItem;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

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

  @Nested
  @DisplayName("deletedAt이 지난 유저는 삭제하기")
  class DeleteByDeletedAt {
    @Test
    @DisplayName("deletedAt이 지난 유저 삭제")
    void deletedAt이_지난_유저_삭제() {
      // given
      Instant now = Instant.now();
      User user1 = User.create("test1@naver.com", "test1", "12345678");
      User user2 = User.create("test2@naver.com", "test2", "12345678");

      ReflectionTestUtils.setField(user1, "deletedAt", now.minusSeconds(10));
      ReflectionTestUtils.setField(user2, "deletedAt", now.plusSeconds(10));

      userRepository.save(user1);
      userRepository.save(user2);

      // when
      int deletedCount = userRepository.deleteAllByDeletedAtBefore(now);

      // then
      assertThat(deletedCount).isEqualTo(1); // user1 삭제
      assertThat(userRepository.findById(user2.getId())).isPresent(); // user2 유지
    }
  }

  @Nested
  @DisplayName("deletedAt + id 기준 cursor 조회가 정렬된 순서로 반환하기")
  class FindUsersForCleanup {
    @Test
    @DisplayName("deletedAt + id 기준 cursor 조회가 정렬된 순서로 반환된다")
    void findUsersForCleanup_ordering_test() {
      // given
      Instant base = Instant.now().minus(Duration.ofDays(2));

      User user1 = User.create("test1@naver.com", "test1", "12345678");
      User user2 = User.create("test2@naver.com", "test2", "12345678");
      User user3 = User.create("test3@naver.com", "test3", "12345678");

      ReflectionTestUtils.setField(user1, "deletedAt", base.minusSeconds(10));
      ReflectionTestUtils.setField(user2, "deletedAt", base.plusSeconds(10));
      ReflectionTestUtils.setField(user3, "deletedAt", base.plusSeconds(20));

      userRepository.save(user1);
      userRepository.save(user2);
      userRepository.save(user3);

      // when
      List<UserCleanupItem> result = userRepository.findUsersForCleanup(
          Instant.now(),
          Instant.EPOCH,
          UUID.randomUUID(),
          PageRequest.of(0, 10)
      );

      // then
      assertThat(result)
          .extracting(UserCleanupItem::id)
          .containsExactly(
              user1.getId(),
              user2.getId(),
              user3.getId()
          );
    }
  }
}