package com.sprint.mission.monew.domain.interest.service;

import com.sprint.mission.monew.domain.interest.dto.SubscriptionResponse;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import com.sprint.mission.monew.domain.interest.exception.InterestNotFoundException;
import com.sprint.mission.monew.domain.interest.exception.SubscriptionAlreadyExistsException;
import com.sprint.mission.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.sprint.mission.monew.domain.interest.mapper.SubscriptionMapper;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionService {

  private final InterestRepository interestRepository;
  private final UserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionMapper subscriptionMapper;

  @Transactional
  public SubscriptionResponse subscribe(UUID interestId, UUID userId) {
    log.debug("관심사 구독 시작 | interestId={}, userId={}", interestId, userId);
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> InterestNotFoundException.withId(interestId));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    if (subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)) {
      throw SubscriptionAlreadyExistsException.withIds(interestId, userId);
    }
    try {
      Subscription saved = subscriptionRepository.saveAndFlush(Subscription.create(interest, user));
      SubscriptionResponse response = subscriptionMapper.toResponse(saved, interest.getSubscriberCount() + 1);
      interestRepository.increaseSubscriberCount(interestId);
      log.info("관심사 구독 완료 | interestId={}, userId={}", interestId, userId);
      return response;
    } catch (DataIntegrityViolationException e) {
      throw SubscriptionAlreadyExistsException.withIds(interestId, userId);
    }
  }

  @Transactional
  public void unsubscribe(UUID interestId, UUID userId) {
    log.debug("관심사 구독 취소 시작 | interestId={}, userId={}", interestId, userId);
    if (!interestRepository.existsById(interestId)) {
      throw InterestNotFoundException.withId(interestId);
    }

    int deleted = subscriptionRepository.deleteByInterestIdAndUserId(interestId, userId);
    if (deleted == 0) {
      throw SubscriptionNotFoundException.withIds(interestId, userId);
    }

    int decreased = interestRepository.decreaseSubscriberCount(interestId);
    if (decreased == 0) {
      log.warn("구독은 삭제됐으나 subscriberCount가 이미 0이라 감소되지 않음 | interestId={}", interestId);
    }
    log.info("관심사 구독 취소 완료 | interestId={}, userId={}", interestId, userId);
  }
}