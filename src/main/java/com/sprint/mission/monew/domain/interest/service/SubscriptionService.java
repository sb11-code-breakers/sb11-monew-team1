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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> InterestNotFoundException.withId(interestId));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    if (subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)) {
      throw SubscriptionAlreadyExistsException.withIds(interestId, userId);
    }
    try {
      Subscription saved = subscriptionRepository.saveAndFlush(Subscription.create(interest, user));
      interest.increaseSubscriberCount();
      return subscriptionMapper.toResponse(saved);
    } catch (DataIntegrityViolationException e) {
      throw SubscriptionAlreadyExistsException.withIds(interestId, userId);
    }
  }

  @Transactional
  public void unsubscribe(UUID interestId, UUID userId) {
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> InterestNotFoundException.withId(interestId));

    Subscription subscription = subscriptionRepository.findByInterestIdAndUserId(interestId, userId)
        .orElseThrow(() -> SubscriptionNotFoundException.withIds(interestId, userId));

    interest.decreaseSubscriberCount();
    subscriptionRepository.delete(subscription);
  }
}