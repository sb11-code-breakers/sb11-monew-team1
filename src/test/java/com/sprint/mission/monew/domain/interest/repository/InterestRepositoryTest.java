package com.sprint.mission.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.interest.dto.InterestOrderBy;
import com.sprint.mission.monew.domain.interest.dto.InterestQueryCondition;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.entity.InterestKeyword;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import com.sprint.mission.monew.domain.user.entity.User;
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

  @Autowired InterestRepository interestRepository;

  @Autowired TestEntityManager em;

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
  @DisplayName("관심사 목록 조회")
  class FindInterests {

    @Test
    @DisplayName("검색어 없으면 전체 관심사를 name DESC로 반환한다")
    void 검색어_없으면_전체_관심사를_name_DESC로_반환한다() {
      // given
      interestRepository.saveAll(
          List.of(
              Interest.create("Celebrity", List.of("연예인")),
              Interest.create("AI", List.of("인공지능")),
              Interest.create("Baseball", List.of("야구"))));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition("", InterestOrderBy.NAME, SortDirection.DESC, null, null, 10);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then
      assertThat(result.content()).hasSize(3);
      assertThat(result.content())
          .extracting(InterestResponse::name)
          .containsExactly("Celebrity", "Baseball", "AI");
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("검색어가 관심사 이름에 부분일치하면 필터링된다")
    void 검색어가_관심사_이름에_부분일치하면_필터링된다() {
      // given
      interestRepository.saveAll(
          List.of(
              Interest.create("Baseball", List.of("bat", "pitcher")),
              Interest.create("Basketball", List.of("court", "dunk")),
              Interest.create("AI", List.of("machine learning"))));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition(
              "Base", InterestOrderBy.NAME, SortDirection.DESC, null, null, 10);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).name()).isEqualTo("Baseball");
    }

    @Test
    @DisplayName("검색어가 keywords에 부분일치하면 필터링된다")
    void 검색어가_keywords에_부분일치하면_필터링된다() {
      // given
      interestRepository.saveAll(
          List.of(
              Interest.create("Soccer", List.of("football", "goal")),
              Interest.create("Basketball", List.of("court", "dunk")),
              Interest.create("Tennis", List.of("racket", "serve"))));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition(
              "court", InterestOrderBy.NAME, SortDirection.DESC, null, null, 10);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).name()).isEqualTo("Basketball");
    }

    @Test
    @DisplayName("이름·키워드 둘 다 불일치하면 결과가 없다")
    void 이름_키워드_둘_다_불일치하면_결과가_없다() {
      // given
      interestRepository.saveAll(
          List.of(
              Interest.create("Soccer", List.of("football")),
              Interest.create("Tennis", List.of("racket"))));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition(
              "baseball", InterestOrderBy.NAME, SortDirection.DESC, null, null, 10);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then
      assertThat(result.content()).isEmpty();
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("orderBy=name, direction=ASC로 정렬된다")
    void orderBy_name_direction_ASC로_정렬된다() {
      // given
      interestRepository.saveAll(
          List.of(
              Interest.create("Celebrity", List.of("연예인")),
              Interest.create("AI", List.of("인공지능")),
              Interest.create("Baseball", List.of("야구"))));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition("", InterestOrderBy.NAME, SortDirection.ASC, null, null, 10);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then
      assertThat(result.content())
          .extracting(InterestResponse::name)
          .containsExactly("AI", "Baseball", "Celebrity");
    }

    @Test
    @DisplayName("orderBy=subscriberCount, direction=DESC로 정렬된다")
    void orderBy_subscriberCount_direction_DESC로_정렬된다() {
      // given
      Interest soccer = Interest.create("Soccer", List.of("football"));
      Interest tennis = Interest.create("Tennis", List.of("racket"));
      Interest ai = Interest.create("AI", List.of("인공지능"));
      soccer.increaseSubscriberCount();
      soccer.increaseSubscriberCount(); // 2
      tennis.increaseSubscriberCount(); // 1
      interestRepository.saveAll(List.of(soccer, tennis, ai));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition(
              "", InterestOrderBy.SUBSCRIBER_COUNT, SortDirection.DESC, null, null, 10);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then
      assertThat(result.content())
          .extracting(InterestResponse::name)
          .containsExactly("Soccer", "Tennis", "AI");
    }

    @Test
    @DisplayName("orderBy=subscriberCount, direction=ASC로 정렬된다")
    void orderBy_subscriberCount_direction_ASC로_정렬된다() {
      // given
      Interest soccer = Interest.create("Soccer", List.of("football"));
      Interest tennis = Interest.create("Tennis", List.of("racket"));
      Interest ai = Interest.create("AI", List.of("인공지능"));
      soccer.increaseSubscriberCount();
      soccer.increaseSubscriberCount(); // 2
      tennis.increaseSubscriberCount(); // 1
      interestRepository.saveAll(List.of(soccer, tennis, ai));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition(
              "", InterestOrderBy.SUBSCRIBER_COUNT, SortDirection.ASC, null, null, 10);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then
      assertThat(result.content())
          .extracting(InterestResponse::name)
          .containsExactly("AI", "Tennis", "Soccer");
    }

    @Test
    @DisplayName("limit 초과 시 hasNext=true + nextCursor/nextAfter 반환된다")
    void limit_초과_시_hasNext_true_nextCursor_nextAfter_반환된다() {
      // given
      interestRepository.saveAll(
          List.of(
              Interest.create("Celebrity", List.of("연예인")),
              Interest.create("Baseball", List.of("야구")),
              Interest.create("AI", List.of("인공지능"))));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition("", InterestOrderBy.NAME, SortDirection.DESC, null, null, 2);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then — DESC: Celebrity, Baseball, AI → 첫 페이지 마지막은 Baseball
      assertThat(result.hasNext()).isTrue();
      assertThat(result.content()).hasSize(2);
      assertThat(result.nextCursor()).isEqualTo("Baseball");
      assertThat(result.nextAfter()).isNotNull();
    }

    @Test
    @DisplayName("SUBSCRIBER_COUNT 정렬 hasNext=true 시 nextCursor는 정수 문자열이다")
    void SUBSCRIBER_COUNT_hasNext_true_시_nextCursor는_정수다() {
      // given — DESC: Soccer(2), Tennis(1), AI(0)
      Interest soccer = Interest.create("Soccer", List.of("football"));
      Interest tennis = Interest.create("Tennis", List.of("racket"));
      Interest ai = Interest.create("AI", List.of("인공지능"));
      soccer.increaseSubscriberCount();
      soccer.increaseSubscriberCount();
      tennis.increaseSubscriberCount();
      interestRepository.saveAll(List.of(soccer, tennis, ai));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition(
              "", InterestOrderBy.SUBSCRIBER_COUNT, SortDirection.DESC, null, null, 2);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then — 첫 페이지 마지막은 Tennis(subscriberCount=1)
      assertThat(result.hasNext()).isTrue();
      assertThat(result.content()).hasSize(2);
      assertThat(result.nextCursor()).isEqualTo("1");
      assertThat(result.nextAfter()).isNotNull();
    }

    @Test
    @DisplayName("SUBSCRIBER_COUNT ASC 정렬 hasNext=true 시 nextCursor는 정수 문자열이다")
    void SUBSCRIBER_COUNT_ASC_hasNext_true_시_nextCursor는_정수다() {
      // given — ASC: AI(0), Tennis(1), Soccer(2)
      Interest soccer = Interest.create("Soccer", List.of("football"));
      Interest tennis = Interest.create("Tennis", List.of("racket"));
      Interest ai = Interest.create("AI", List.of("인공지능"));
      soccer.increaseSubscriberCount();
      soccer.increaseSubscriberCount();
      tennis.increaseSubscriberCount();
      interestRepository.saveAll(List.of(soccer, tennis, ai));
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition =
          new InterestQueryCondition(
              "", InterestOrderBy.SUBSCRIBER_COUNT, SortDirection.ASC, null, null, 2);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, userId);

      // then — 첫 페이지 마지막은 Tennis(subscriberCount=1)
      assertThat(result.hasNext()).isTrue();
      assertThat(result.content()).hasSize(2);
      assertThat(result.nextCursor()).isEqualTo("1");
      assertThat(result.nextAfter()).isNotNull();
    }

    @Test
    @DisplayName("cursor 기반으로 다음 페이지를 조회한다")
    void cursor_기반으로_다음_페이지를_조회한다() {
      // given
      interestRepository.saveAll(
          List.of(
              Interest.create("Celebrity", List.of("연예인")),
              Interest.create("Baseball", List.of("야구")),
              Interest.create("AI", List.of("인공지능"))));
      UUID userId = UUID.randomUUID();
      CursorPageResponse<InterestResponse> firstPage =
          interestRepository.findInterests(
              new InterestQueryCondition(
                  "", InterestOrderBy.NAME, SortDirection.DESC, null, null, 2),
              userId);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(
              new InterestQueryCondition(
                  "",
                  InterestOrderBy.NAME,
                  SortDirection.DESC,
                  firstPage.nextCursor(),
                  firstPage.nextAfter(),
                  2),
              userId);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).name()).isEqualTo("AI");
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("cursor 기반으로 NAME ASC 다음 페이지를 조회한다")
    void cursor_기반으로_NAME_ASC_다음_페이지를_조회한다() {
      // given — ASC: AI, Baseball, Celebrity
      interestRepository.saveAll(
          List.of(
              Interest.create("Celebrity", List.of("연예인")),
              Interest.create("Baseball", List.of("야구")),
              Interest.create("AI", List.of("인공지능"))));
      UUID userId = UUID.randomUUID();
      CursorPageResponse<InterestResponse> firstPage =
          interestRepository.findInterests(
              new InterestQueryCondition(
                  "", InterestOrderBy.NAME, SortDirection.ASC, null, null, 2),
              userId);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(
              new InterestQueryCondition(
                  "",
                  InterestOrderBy.NAME,
                  SortDirection.ASC,
                  firstPage.nextCursor(),
                  firstPage.nextAfter(),
                  2),
              userId);

      // then — 마지막 페이지: Celebrity
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).name()).isEqualTo("Celebrity");
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("cursor 기반으로 SUBSCRIBER_COUNT DESC 다음 페이지를 조회한다")
    void cursor_기반으로_SUBSCRIBER_COUNT_DESC_다음_페이지를_조회한다() {
      // given — DESC: Soccer(2), Tennis(1), AI(0)
      Interest soccer = Interest.create("Soccer", List.of("football"));
      Interest tennis = Interest.create("Tennis", List.of("racket"));
      Interest ai = Interest.create("AI", List.of("인공지능"));
      soccer.increaseSubscriberCount();
      soccer.increaseSubscriberCount();
      tennis.increaseSubscriberCount();
      interestRepository.saveAll(List.of(soccer, tennis, ai));
      UUID userId = UUID.randomUUID();
      CursorPageResponse<InterestResponse> firstPage =
          interestRepository.findInterests(
              new InterestQueryCondition(
                  "", InterestOrderBy.SUBSCRIBER_COUNT, SortDirection.DESC, null, null, 2),
              userId);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(
              new InterestQueryCondition(
                  "",
                  InterestOrderBy.SUBSCRIBER_COUNT,
                  SortDirection.DESC,
                  firstPage.nextCursor(),
                  firstPage.nextAfter(),
                  2),
              userId);

      // then — 마지막 페이지: AI
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).name()).isEqualTo("AI");
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("cursor 기반으로 SUBSCRIBER_COUNT ASC 다음 페이지를 조회한다")
    void cursor_기반으로_SUBSCRIBER_COUNT_ASC_다음_페이지를_조회한다() {
      // given — ASC: AI(0), Tennis(1), Soccer(2)
      Interest soccer = Interest.create("Soccer", List.of("football"));
      Interest tennis = Interest.create("Tennis", List.of("racket"));
      Interest ai = Interest.create("AI", List.of("인공지능"));
      soccer.increaseSubscriberCount();
      soccer.increaseSubscriberCount();
      tennis.increaseSubscriberCount();
      interestRepository.saveAll(List.of(soccer, tennis, ai));
      UUID userId = UUID.randomUUID();
      CursorPageResponse<InterestResponse> firstPage =
          interestRepository.findInterests(
              new InterestQueryCondition(
                  "", InterestOrderBy.SUBSCRIBER_COUNT, SortDirection.ASC, null, null, 2),
              userId);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(
              new InterestQueryCondition(
                  "",
                  InterestOrderBy.SUBSCRIBER_COUNT,
                  SortDirection.ASC,
                  firstPage.nextCursor(),
                  firstPage.nextAfter(),
                  2),
              userId);

      // then — 마지막 페이지: Soccer
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).name()).isEqualTo("Soccer");
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("구독한 관심사는 subscribedByMe=true, 미구독은 false로 반환된다")
    void 구독한_관심사는_subscribedByMe가_true_미구독은_false로_반환된다() {
      // given
      Interest soccer = interestRepository.save(Interest.create("Soccer", List.of("football")));
      Interest tennis = interestRepository.save(Interest.create("Tennis", List.of("racket")));

      User user = em.persistAndFlush(User.create("test@test.com", "tester", "pass"));
      em.persistAndFlush(Subscription.create(soccer, user));
      em.clear();

      InterestQueryCondition condition =
          new InterestQueryCondition("", InterestOrderBy.NAME, SortDirection.DESC, null, null, 10);

      // when
      CursorPageResponse<InterestResponse> result =
          interestRepository.findInterests(condition, user.getId());

      // then — DESC: Tennis, Soccer
      assertThat(result.content())
          .extracting(InterestResponse::name)
          .containsExactly("Tennis", "Soccer");
      assertThat(result.content().get(0).subscribedByMe()).isFalse(); // Tennis
      assertThat(result.content().get(1).subscribedByMe()).isTrue(); // Soccer
    }
  }

  @Nested
  @DisplayName("기사 제목·요약 기준 키워드 매칭 관심사 조회")
  class FindMatchingInterests {

    @Test
    @DisplayName("기사 제목 또는 요약에 키워드가 포함된 관심사를 반환한다")
    void 기사_제목_또는_요약에_키워드가_포함된_관심사를_반환한다() {
      // given
      interestRepository.save(Interest.create("인공지능", List.of("AI", "머신러닝")));
      interestRepository.save(Interest.create("스포츠", List.of("축구", "야구")));

      // when
      List<Interest> result = interestRepository.findMatchingInterests("AI 반도체 전망", "머신러닝 동향");

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("인공지능");
    }

    @Test
    @DisplayName("일치하는 키워드가 없으면 빈 목록을 반환한다")
    void 일치하는_키워드가_없으면_빈_목록을_반환한다() {
      // given
      interestRepository.save(Interest.create("인공지능", List.of("AI", "머신러닝")));

      // when
      List<Interest> result = interestRepository.findMatchingInterests("오늘의 날씨", "맑고 쾌청한 하루");

      // then
      assertThat(result).isEmpty();
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
      List<UUID> keywordIds = saved.getKeywords().stream().map(InterestKeyword::getId).toList();

      // when — save 시 PC에 올라온 keyword 엔티티들에 cascade REMOVE가 전파된다
      interestRepository.deleteById(saved.getId());
      em.flush();
      em.clear();

      // then — REMOVED 상태의 엔티티는 em.find()에서 null 반환
      keywordIds.forEach(id -> assertThat(em.find(InterestKeyword.class, id)).isNull());
    }
  }
}
