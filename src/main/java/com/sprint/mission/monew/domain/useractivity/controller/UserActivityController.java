package com.sprint.mission.monew.domain.useractivity.controller;

import com.sprint.mission.monew.domain.useractivity.controller.api.UserActivityApi;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.service.UserActivityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-activities")
public class UserActivityController implements UserActivityApi {

  private final UserActivityService userActivityService;

  @Override
  @GetMapping("/{userId}")
  public ResponseEntity<UserActivity> getUserActivity(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    return ResponseEntity.ok(userActivityService.getUserActivity(userId, requestUserId));
  }
}
