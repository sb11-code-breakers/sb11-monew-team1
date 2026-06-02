package com.sprint.mission.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import com.sprint.mission.monew.domain.interest.repository.dto.InterestSubscriber;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
class SubscriptionRepositoryTest {

  @Autowired
  SubscriptionRepository subscriptionRepository;

  @Autowired
  InterestRepository interestRepository;

  @Autowired
  UserRepository userRepository;

  Interest interest;
  User user;

  @BeforeEach
  void setUp() {
    subscriptionRepository.deleteAll();
    interestRepository.deleteAll();
    userRepository.deleteAll();

    interest = interestRepository.save(Interest.create("인공지능", List.of("AI")));
    user = userRepository.save(User.create("test@test.com", "테스터", "password123!"));
  }

  @Nested
  @DisplayName("구독 저장")
  class Save {

    @Test
    @DisplayName("저장 후 ID로 조회하면 Interest·User가 일치한다")
    void 저장_후_ID로_조회하면_Interest와_User가_일치한다() {
      // given
      Subscription subscription = Subscription.create(interest, user);

      // when
      Subscription saved = subscriptionRepository.save(subscription);
      Optional<Subscription> found = subscriptionRepository.findById(saved.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getInterest().getId()).isEqualTo(interest.getId());
      assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }
  }

  @Nested
  @DisplayName("중복 구독 여부 확인")
  class ExistsByInterestIdAndUserId {

    @Test
    @DisplayName("구독 중이면 true를 반환한다")
    void 구독_중이면_true를_반환한다() {
      // given
      subscriptionRepository.save(Subscription.create(interest, user));

      // when
      boolean exists = subscriptionRepository.existsByInterestIdAndUserId(
          interest.getId(), user.getId());

      // then
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("구독하지 않으면 false를 반환한다")
    void 구독하지_않으면_false를_반환한다() {
      // when
      boolean exists = subscriptionRepository.existsByInterestIdAndUserId(
          interest.getId(), user.getId());

      // then
      assertThat(exists).isFalse();
    }
  }

  @Nested
  @DisplayName("구독 삭제")
  class Delete {

    @Test
    @DisplayName("구독 삭제 후 existsByInterestIdAndUserId가 false를 반환한다")
    void 구독_삭제_후_existsByInterestIdAndUserId가_false를_반환한다() {
      // given
      Subscription subscription = subscriptionRepository.save(Subscription.create(interest, user));

      // when
      subscriptionRepository.delete(subscription);

      // then
      assertThat(subscriptionRepository.existsByInterestIdAndUserId(
          interest.getId(), user.getId())).isFalse();
    }
  }

  @Nested
  @DisplayName("구독 정보 조회")
  class FindByInterestIdAndUserId {

    @Test
    @DisplayName("구독 중이면 구독 정보를 반환한다")
    void 구독_중이면_구독_정보를_반환한다() {
      // given
      subscriptionRepository.save(Subscription.create(interest, user));

      // when
      Optional<Subscription> found = subscriptionRepository.findByInterestIdAndUserId(
          interest.getId(), user.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getInterest().getId()).isEqualTo(interest.getId());
      assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("구독하지 않으면 empty를 반환한다")
    void 구독하지_않으면_empty를_반환한다() {
      // when
      Optional<Subscription> found = subscriptionRepository.findByInterestIdAndUserId(
          interest.getId(), user.getId());

      // then
      assertThat(found).isEmpty();
    }
  }



  @Nested
  @DisplayName("userId로 구독 목록 조회")
  class FindByUserId {

    @Test
    @DisplayName("구독한 관심사가 없으면 빈 리스트를 반환한다")
    void 구독한_관심사가_없으면_빈_리스트를_반환한다() {
      // given
      // 구독 없이 user만 있음

      // when
      List<Subscription> result = subscriptionRepository.findAllByUserId(user.getId(), PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("관심사 목록 구독자 일괄 조회")
  class FindSubscribersByInterestIds {

    @Test
    @DisplayName("조회 대상 관심사의 (관심사ID, 구독자ID) 쌍만 반환한다")
    void 조회_대상_관심사의_구독자_쌍만_반환한다() {
      // given — interest는 조회 대상(구독자 2명), otherInterest는 제외 대상
      Interest otherInterest = interestRepository.save(Interest.create("스포츠", List.of("축구")));
      User user2 = userRepository.save(User.create("user2@test.com", "유저2", "password123!"));
      subscriptionRepository.save(Subscription.create(interest, user));
      subscriptionRepository.save(Subscription.create(interest, user2));
      subscriptionRepository.save(Subscription.create(otherInterest, user)); // 제외돼야 함

      // when
      List<InterestSubscriber> result =
          subscriptionRepository.findSubscribersByInterestIds(List.of(interest.getId()));

      // then
      assertThat(result)
          .extracting(InterestSubscriber::getInterestId, InterestSubscriber::getUserId)
          .containsExactlyInAnyOrder(
              tuple(interest.getId(), user.getId()),
              tuple(interest.getId(), user2.getId()));
    }
  }
}
