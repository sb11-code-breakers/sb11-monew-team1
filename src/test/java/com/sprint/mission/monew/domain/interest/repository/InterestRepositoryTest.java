package com.sprint.mission.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig.class)
class InterestRepositoryTest {

  @Autowired InterestRepository interestRepository;

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
      assertThat(found.get().getKeywords()).containsExactlyInAnyOrderElementsOf(keywords);
    }
  }
}
