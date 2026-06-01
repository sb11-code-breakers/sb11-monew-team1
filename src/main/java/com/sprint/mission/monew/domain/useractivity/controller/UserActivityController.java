package com.sprint.mission.monew.domain.useractivity.controller;

import com.sprint.mission.monew.domain.useractivity.controller.api.UserActivityApi;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.service.UserActivityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-activities")
public class UserActivityController implements UserActivityApi {

  private final UserActivityService userActivityService;

  @Override
  @GetMapping("/{userId}")
  public ResponseEntity<UserActivityResponse> getUserActivity(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {

    log.info("[USER_ACTIVITY_GET_REQUEST] 활동 내역 조회 요청 - 사용자 ID={}", userId);

    UserActivityResponse response = userActivityService.getUserActivity(userId, requestUserId);

    log.debug("[USER_ACTIVITY_GET_RESPONSE] 활동 내역 조회 응답 - 사용자 ID={}", userId);

    return ResponseEntity.ok(response);
  }
}