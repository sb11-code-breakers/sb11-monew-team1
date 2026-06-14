package com.sprint.mission.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.sprint.mission.monew.domain.interest.config.SynonymProperties;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestOrderBy;
import com.sprint.mission.monew.domain.interest.dto.InterestQueryCondition;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.exception.InterestAlreadyExistsException;
import com.sprint.mission.monew.domain.interest.exception.InterestNotFoundException;
import com.sprint.mission.monew.domain.interest.mapper.InterestMapper;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.interest.util.SynonymUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @InjectMocks
  InterestService interestService;

  @Mock
  InterestRepository interestRepository;

  @Mock
  InterestMapper interestMapper;

  @Spy
  SynonymUtils synonymUtils = new SynonymUtils(new SynonymProperties(
      List.of(
          Set.of("뉴스", "소식", "속보", "단신"),
          Set.of("정보", "동향", "현황", "상황"),
          Set.of("분석", "전망", "예측"),
          Set.of("트렌드", "흐름"),
          Set.of("이슈", "리뷰", "점검", "검토"),
          Set.of("이야기", "얘기", "스토리"),
          Set.of("기술", "테크"),
          Set.of("산업", "업계", "섹터"),
          Set.of("시장", "마켓"),
          Set.of("정책", "규제", "법안", "제도"),
          Set.of("사태", "사건", "사고", "논란", "갈등"),
          Set.of("건강", "보건", "헬스케어")
      ),
      List.of(
          Set.of("최신", "최근", "요즘"),
          Set.of("주요", "핵심", "관련", "중요"),
          Set.of("첨단", "차세대", "미래")
      )
  ));

  @Nested
  @DisplayName("관심사 목록 조회")
  class FindAll {

    @Test
    @DisplayName("조건에 맞는 관심사 목록을 조회한다")
    void 조건에_맞는_관심사_목록을_조회한다() {
      // given
      UUID userId = UUID.randomUUID();
      InterestQueryCondition condition = new InterestQueryCondition(
          "", InterestOrderBy.NAME, SortDirection.DESC, null, null, null, 10);
      CursorPageResponse<InterestResponse> expected =
          CursorPageResponse.of(List.of(), null, null, null, false, 0, 0L);

      given(interestRepository.findInterests(condition, userId)).willReturn(expected);

      // when
      CursorPageResponse<InterestResponse> result = interestService.findAll(condition, userId);

      // then
      assertThat(result).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("관심사 등록")
  class Create {

    @Test
    @DisplayName("이미 존재하는 이름으로 등록 시 유사도 검사 없이 즉시 예외를 던진다")
    void 이미_존재하는_이름으로_등록_시_즉시_예외를_던진다() {
      // given
      given(interestRepository.existsByName("인공지능")).willReturn(true);

      // when & then
      assertThatThrownBy(
          () -> interestService.create(new InterestCreateRequest("인공지능", List.of())))
          .isInstanceOf(InterestAlreadyExistsException.class);
      then(interestRepository).should(never()).findTypoCandidates(anyInt(), anyInt());
    }

    @Test
    @DisplayName("공백만으로 이루어진 이름은 동의어 검사를 건너뛴다")
    void 공백만으로_이루어진_이름은_동의어_검사를_건너뛴다() {
      // given — "   " → splitRaw 결과 empty → synonymMatch short-circuit → findNamesByTokens 미호출
      given(interestRepository.existsByName("   ")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt())).willReturn(List.of());
      given(interestRepository.save(any())).willReturn(mock(Interest.class));
      given(interestMapper.toResponse(any())).willReturn(mock(InterestResponse.class));

      // when & then
      assertThatCode(
          () -> interestService.create(new InterestCreateRequest("   ", List.of())))
          .doesNotThrowAnyException();
      then(interestRepository).should(never()).findNamesByTokens(anyList());
    }

    @Test
    @DisplayName("대소문자·공백 차이 있어도 정규화 후 동일 이름이면 차단한다")
    void 대소문자_공백_차이_있어도_정규화_후_동일_이름이면_차단한다() {
      // given
      given(interestRepository.existsByName("AI 뉴스")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt()))
          .willReturn(List.of("ai뉴스"));

      // when & then
      assertThatThrownBy(
          () -> interestService.create(new InterestCreateRequest("AI 뉴스", List.of())))
          .isInstanceOf(InterestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("자모 1개 차이 유사도 0.8 이상이면 차단한다")
    void 자모_1개_차이_유사도_0_8이상이면_차단() {
      // given — "반도쳬" 등록 시도, DB에 "반도체" 존재
      given(interestRepository.existsByName("반도쳬")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt()))
          .willReturn(List.of("반도체"));

      // when & then
      assertThatThrownBy(
          () -> interestService.create(new InterestCreateRequest("반도쳬", List.of())))
          .isInstanceOf(InterestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("유사도 0.8 미만이면 통과한다")
    void 유사도_0_8미만이면_통과() {
      // given — "환경" 등록 시도, DB에 "금융" 존재
      given(interestRepository.existsByName("환경")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt()))
          .willReturn(List.of("금융"));
      given(interestRepository.save(any())).willReturn(mock(Interest.class));
      given(interestMapper.toResponse(any())).willReturn(mock(InterestResponse.class));

      // when & then
      assertThatCode(() -> interestService.create(new InterestCreateRequest("환경", List.of())))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("같은 suffix 그룹이면 차단한다")
    void 같은_suffix_그룹이면_차단() {
      // given — "AI소식" 등록 시도, DB에 "AI뉴스" 존재 (둘 다 보도 그룹 → ai_S0 동일)
      given(interestRepository.existsByName("AI소식")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt())).willReturn(List.of());
      given(interestRepository.findNamesByTokens(anyList())).willReturn(List.of("AI뉴스"));

      // when & then
      assertThatThrownBy(
          () -> interestService.create(new InterestCreateRequest("AI소식", List.of())))
          .isInstanceOf(InterestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("같은 prefix 그룹이면 차단한다")
    void 같은_prefix_그룹이면_차단() {
      // given — "관련AI" 등록 시도, DB에 "핵심AI" 존재 (둘 다 중요도 그룹 → P1_ai 동일)
      given(interestRepository.existsByName("관련AI")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt())).willReturn(List.of());
      given(interestRepository.findNamesByTokens(anyList())).willReturn(List.of("핵심AI"));

      // when & then
      assertThatThrownBy(
          () -> interestService.create(new InterestCreateRequest("관련AI", List.of())))
          .isInstanceOf(InterestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("다른 suffix 그룹이면 통과한다")
    void 다른_suffix_그룹이면_통과() {
      // given — "AI분석" 등록 시도, DB에 "AI뉴스" 존재 (분석=S2, 보도=S0 → 다른 그룹)
      given(interestRepository.existsByName("AI분석")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt())).willReturn(List.of());
      given(interestRepository.findNamesByTokens(anyList())).willReturn(List.of("AI뉴스"));
      given(interestRepository.save(any())).willReturn(mock(Interest.class));
      given(interestMapper.toResponse(any())).willReturn(mock(InterestResponse.class));

      // when & then
      assertThatCode(
          () -> interestService.create(new InterestCreateRequest("AI분석", List.of())))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("prefix+suffix 동시 매칭 시 suffix가 우선 적용되어 동일 canonical로 차단한다")
    void prefix_suffix_동시_매칭_시_suffix_우선_적용() {
      // given — "핵심AI뉴스" 등록 시도, DB에 "최신AI뉴스" 존재
      // 두 토큰 모두 suffix=뉴스(S0)가 우선 매칭 → core에서 prefix(핵심/최신) 제거 → canonical "ai_S0"으로 동일
      // suffix 우선이 아니라면 prefix 그룹이 달라 서로 다른 canonical이 되어 차단되지 않음
      given(interestRepository.existsByName("핵심AI뉴스")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt())).willReturn(List.of());
      given(interestRepository.findNamesByTokens(anyList())).willReturn(List.of("최신AI뉴스"));

      // when & then
      assertThatThrownBy(
          () -> interestService.create(new InterestCreateRequest("핵심AI뉴스", List.of())))
          .isInstanceOf(InterestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("pure 키워드는 decorated 이름과 공존할 수 있다")
    void pure_키워드는_decorated와_공존한다() {
      // given — "AI" 등록 시도, DB에 "AI뉴스" 존재 (AI는 pure → canonical 불일치)
      given(interestRepository.existsByName("AI")).willReturn(false);
      given(interestRepository.findTypoCandidates(anyInt(), anyInt())).willReturn(List.of());
      given(interestRepository.findNamesByTokens(anyList())).willReturn(List.of("AI뉴스"));
      given(interestRepository.save(any())).willReturn(mock(Interest.class));
      given(interestMapper.toResponse(any())).willReturn(mock(InterestResponse.class));

      // when & then
      assertThatCode(() -> interestService.create(new InterestCreateRequest("AI", List.of())))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("관심사 키워드 수정")
  class UpdateKeywords {

    private UUID interestId;
    private InterestUpdateRequest request;

    @BeforeEach
    void setUp() {
      interestId = UUID.randomUUID();
      request = new InterestUpdateRequest(List.of("자연어처리", "GPT"));
    }

    @Test
    @DisplayName("존재하지 않는 관심사 수정 시 InterestNotFoundException이 발생한다")
    void 존재하지_않는_관심사_수정_시_예외가_발생한다() {
      // given
      given(interestRepository.findById(interestId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> interestService.updateKeywords(interestId, request))
          .isInstanceOf(InterestNotFoundException.class);
    }

    @Test
    @DisplayName("유효한 관심사 키워드 수정 시 InterestResponse를 반환한다")
    void 유효한_관심사_키워드_수정_시_InterestResponse를_반환한다() {
      // given
      Interest interest = Interest.create("인공지능", 11, List.of("AI"));
      InterestResponse expected =
          new InterestResponse(interest.getId(), "인공지능", List.of("자연어처리", "GPT"), 0L, false);

      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(interestMapper.toResponse(interest)).willReturn(expected);

      // when
      InterestResponse result = interestService.updateKeywords(interestId, request);

      // then
      assertThat(result.keywords()).containsExactlyElementsOf(request.keywords());
    }
  }

  @Nested
  @DisplayName("관심사 물리 삭제")
  class HardDelete {

    private UUID interestId;

    @BeforeEach
    void setUp() {
      interestId = UUID.randomUUID();
    }

    @Test
    @DisplayName("존재하지 않는 관심사 삭제 시 InterestNotFoundException이 발생한다")
    void 존재하지_않는_관심사_삭제_시_InterestNotFoundException이_발생한다() {
      // given
      given(interestRepository.findById(interestId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> interestService.hardDelete(interestId))
          .isInstanceOf(InterestNotFoundException.class);
    }

    @Test
    @DisplayName("존재하는 관심사 삭제 시 interestRepository.delete()가 호출된다")
    void 존재하는_관심사_삭제_시_repository_delete가_호출된다() {
      // given
      Interest interest = Interest.create("인공지능", 11, List.of("AI"));
      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

      // when
      interestService.hardDelete(interestId);

      // then
      then(interestRepository).should().delete(interest);
    }
  }
}