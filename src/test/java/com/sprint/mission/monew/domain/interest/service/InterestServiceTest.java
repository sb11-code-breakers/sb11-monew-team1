package com.sprint.mission.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.exception.InterestAlreadyExistsException;
import com.sprint.mission.monew.domain.interest.exception.InterestNotFoundException;
import com.sprint.mission.monew.domain.interest.mapper.InterestMapper;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @InjectMocks
  InterestService interestService;

  @Mock
  InterestRepository interestRepository;

  @Mock
  InterestMapper interestMapper;

  @Nested
  @DisplayName("관심사 등록")
  class Register {

    @Test
    @DisplayName("80% 이상 유사한 이름이 존재하면 InterestAlreadyExistsException이 발생한다")
    void 유사한_이름이_존재하면_예외가_발생한다() {
      // given
      InterestCreateRequest request = new InterestCreateRequest("인공지능", List.of("AI"));
      Interest existing = Interest.create("인공지능X", List.of("머신러닝")); // 유사도 80% (거리 1, maxLen 5)

      given(interestRepository.findAll()).willReturn(List.of(existing));

      // when & then
      assertThatThrownBy(() -> interestService.create(request))
          .isInstanceOf(InterestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("유사한 관심사가 없으면 저장 후 InterestDto를 반환한다")
    void 유사한_관심사가_없으면_저장_후_InterestDto를_반환한다() {
      // given
      InterestCreateRequest request = new InterestCreateRequest("인공지능", List.of("AI", "머신러닝"));
      Interest saved = Interest.create("인공지능", List.of("AI", "머신러닝"));
      InterestResponse expectedDto =
          new InterestResponse(saved.getId(), "인공지능", List.of("AI", "머신러닝"), 0L, false);

      given(interestRepository.findAll()).willReturn(List.of());
      given(interestRepository.save(any(Interest.class))).willReturn(saved);
      given(interestMapper.toResponse(any(Interest.class))).willReturn(expectedDto);

      // when
      InterestResponse result = interestService.create(request);

      // then
      assertThat(result.name()).isEqualTo("인공지능");
      assertThat(result.keywords()).containsExactlyInAnyOrderElementsOf(request.keywords());
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
      Interest interest = Interest.create("인공지능", List.of("AI"));
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
      Interest interest = Interest.create("인공지능", List.of("AI"));
      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

      // when
      interestService.hardDelete(interestId);

      // then
      then(interestRepository).should().delete(interest);
    }
  }
}