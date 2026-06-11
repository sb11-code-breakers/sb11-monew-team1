package com.sprint.mission.monew.domain.interest.service;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestQueryCondition;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.exception.InterestAlreadyExistsException;
import com.sprint.mission.monew.domain.interest.exception.InterestNotFoundException;
import com.sprint.mission.monew.domain.interest.mapper.InterestMapper;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.useractivity.listener.InterestDeletedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InterestService {

  private final InterestRepository interestRepository;
  private final InterestMapper interestMapper;
  private final ApplicationEventPublisher eventPublisher;

  public CursorPageResponse<InterestResponse> findAll(InterestQueryCondition condition, UUID userId) {
    return interestRepository.findInterests(condition, userId);
  }

  @Transactional
  public InterestResponse create(InterestCreateRequest request) {
    log.debug("관심사 생성 시작 | name={}", request.name());
    List<Interest> existingInterests = interestRepository.findAll();
    boolean hasSimilar =
        existingInterests.stream()
            .anyMatch(existing -> similarity(request.name(), existing.getName()) >= 0.8);
    if (hasSimilar) {
      throw InterestAlreadyExistsException.withName(request.name());
    }
    Interest saved = interestRepository.save(Interest.create(request.name(), request.keywords()));
    log.info("관심사 생성 완료 | interestId={}, name={}", saved.getId(), saved.getName());
    return interestMapper.toResponse(saved);
  }

  @Transactional
  public InterestResponse updateKeywords(UUID id, InterestUpdateRequest request) {
    log.debug("관심사 키워드 수정 시작 | interestId={}", id);
    Interest interest = interestRepository.findById(id)
        .orElseThrow(() -> InterestNotFoundException.withId(id));
    interest.updateKeywords(request.keywords());
    log.info("관심사 키워드 수정 완료 | interestId={}", id);
    return interestMapper.toResponse(interest);
  }

  @Transactional
  public void hardDelete(UUID id) {
    log.debug("관심사 물리 삭제 시작 | interestId={}", id);
    Interest interest = interestRepository.findById(id)
        .orElseThrow(() -> InterestNotFoundException.withId(id));
    interestRepository.delete(interest);
    log.debug("InterestDeletedEvent 발행 | interestId={}", id);
    eventPublisher.publishEvent(new InterestDeletedEvent(id));
    log.info("관심사 물리 삭제 완료 | interestId={}", id);
  }

  private double similarity(String a, String b) {
    int maxLen = Math.max(a.length(), b.length());
    if (maxLen == 0) {
      return 1.0;
    }
    return 1.0 - (double) levenshteinDistance(a, b) / maxLen;
  }

  private int levenshteinDistance(String a, String b) {
    int[] prev = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      prev[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      int[] curr = new int[b.length() + 1];
      curr[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
      }
      prev = curr;
    }
    return prev[b.length()];
  }
}
