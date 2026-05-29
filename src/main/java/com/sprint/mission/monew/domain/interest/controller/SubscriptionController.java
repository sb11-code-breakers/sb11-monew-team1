package com.sprint.mission.monew.domain.interest.controller;

import com.sprint.mission.monew.domain.interest.controller.api.SubscriptionApi;
import com.sprint.mission.monew.domain.interest.dto.SubscriptionResponse;
import com.sprint.mission.monew.domain.interest.service.SubscriptionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionApi {

  private final SubscriptionService subscriptionService;

  @Override
  @PostMapping("{interestId}/subscriptions")
  public ResponseEntity<SubscriptionResponse> subscribe(
      @PathVariable UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.subscribe(interestId, userId));
  }

  @Override
  @DeleteMapping("{interestId}/subscriptions")
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    subscriptionService.unsubscribe(interestId, userId);
    return ResponseEntity.noContent().build();
  }
}