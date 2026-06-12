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
import com.sprint.mission.monew.domain.interest.util.JamoNormalizer;
import com.sprint.mission.monew.domain.interest.util.LevenshteinUtils;
import com.sprint.mission.monew.domain.interest.util.SynonymUtils;
import java.util.Arrays;
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
  private final SynonymUtils synonymUtils;
  private final ApplicationEventPublisher eventPublisher;

  public CursorPageResponse<InterestResponse> findAll(InterestQueryCondition condition,
      UUID userId) {
    return interestRepository.findInterests(condition, userId);
  }

  @Transactional
  public InterestResponse create(InterestCreateRequest request) {
    String name = request.name();
    log.debug("관심사 생성 시작 | name={}", name);

    if (interestRepository.existsByName(name)) {
      throw InterestAlreadyExistsException.withName(name);
    }

    String normalized = JamoNormalizer.normalize(name);
    int jamoLen = normalized.length();
    int minJamo = (int) Math.ceil(jamoLen * 0.8);
    int maxJamo = (int) Math.floor(jamoLen / 0.8);

    boolean typoMatch = interestRepository.findTypoCandidates(minJamo, maxJamo)
        .stream()
        .anyMatch(existing ->
            LevenshteinUtils.similarity(normalized, JamoNormalizer.normalize(existing)) >= 0.8);

    List<String> rawTokens = splitRaw(name);
    List<String> normTokens = rawTokens.stream().map(JamoNormalizer::normalize).toList();
    List<String> searchTokens = synonymUtils.expandSearchTokens(rawTokens);
    boolean synonymMatch = !rawTokens.isEmpty() &&
        interestRepository.findNamesByTokens(searchTokens)
            .stream()
            .anyMatch(existing -> {
              List<String> existingTokens = splitRaw(existing).stream()
                  .map(JamoNormalizer::normalize).toList();
              return synonymUtils.jaccardSimilarity(normTokens, existingTokens) >= 0.8;
            });

    if (typoMatch || synonymMatch) {
      throw InterestAlreadyExistsException.withName(name);
    }

    Interest saved = interestRepository.save(Interest.create(name, jamoLen, request.keywords()));
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

  private List<String> splitRaw(String s) {
    return Arrays.stream(s.trim().split("\\s+"))
        .filter(t -> !t.isBlank())
        .toList();
  }
}