package com.sprint.mission.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.entity.InterestKeyword;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
class InterestRepositoryTest {

  @Autowired
  InterestRepository interestRepository;

  @Autowired
  TestEntityManager em;

  @BeforeEach
  void setUp() {
    interestRepository.deleteAll();
  }

  @Nested
  @DisplayName("관심사 저장")
  class Save {

    @Test
    @DisplayName("저장 후 ID로 조회하면 name과 keywords가 일치한다")
    void 저장_후_ID로_조회하면_name과_keywords가_일치한다() {
      // given
      List<String> keywords = List.of("AI", "머신러닝", "딥러닝");
      Interest interest = Interest.create("인공지능", keywords);

      // when
      Interest saved = interestRepository.save(interest);
      Optional<Interest> found = interestRepository.findById(saved.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getName()).isEqualTo("인공지능");
      assertThat(found.get().getKeywords())
          .extracting(InterestKeyword::getKeyword)
          .containsExactlyInAnyOrderElementsOf(keywords);
    }
  }

  @Nested
  @DisplayName("관심사 삭제")
  class Delete {

    @Test
    @DisplayName("삭제 후 findById로 조회하면 empty를 반환한다")
    void 삭제_후_findById로_조회하면_empty를_반환한다() {
      // given
      Interest interest = Interest.create("블록체인", List.of("비트코인", "이더리움"));
      Interest saved = interestRepository.save(interest);
      UUID savedId = saved.getId();

      // when
      interestRepository.deleteById(savedId);

      // then
      assertThat(interestRepository.findById(savedId)).isEmpty();
    }

    @Test
    @DisplayName("관심사 삭제 시 cascade로 키워드도 함께 삭제된다")
    void 관심사_삭제_시_cascade로_키워드도_함께_삭제된다() {
      // given
      Interest interest = Interest.create("메타버스", List.of("VR", "AR"));
      Interest saved = interestRepository.save(interest);
      List<UUID> keywordIds = saved.getKeywords().stream()
          .map(InterestKeyword::getId)
          .toList();

      // when — save 시 PC에 올라온 keyword 엔티티들에 cascade REMOVE가 전파된다
      interestRepository.deleteById(saved.getId());
      em.flush();
      em.clear();

      // then — REMOVED 상태의 엔티티는 em.find()에서 null 반환
      keywordIds.forEach(id ->
          assertThat(em.find(InterestKeyword.class, id)).isNull()
      );
    }
  }
}
