package com.sprint.mission.monew.domain.interest.service;

import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.exception.InterestNotFoundException;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 💡 [수정] 스프링 이벤트 발행 및 관심사 삭제 이벤트 import 추가
import org.springframework.context.ApplicationEventPublisher;
import com.sprint.mission.monew.domain.interest.event.InterestDeletedEvent;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InterestService {

  private final InterestRepository interestRepository;
  private final ApplicationEventPublisher eventPublisher; // 💡 [수정] 이벤트 퍼블리셔 주입 추가

  @Transactional
  public void hardDelete(UUID interestId) {
    log.debug("관심사 물리 삭제 시작 | interestId={}", interestId);

    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> InterestNotFoundException.withId(interestId));

    interestRepository.delete(interest);

    // 💡 [수정] 관심사 마스터 데이터 삭제 완료 이벤트 발행
    // 몽고DB 진영(Listener)에서 이 이벤트를 구독하여 유저들의 subscriptions 배열 내에
    // 잔존하는 해당 interestId 관련 데이터를 동기화(Cascade Delete 개념)할 수 있도록 전파합니다.
    eventPublisher.publishEvent(new InterestDeletedEvent(interestId));

    log.info("관심사 물리 삭제 완료 | interestId={}", interestId);
  }
}