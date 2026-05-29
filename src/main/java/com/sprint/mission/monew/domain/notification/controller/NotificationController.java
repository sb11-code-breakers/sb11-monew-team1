package com.sprint.mission.monew.domain.notification.controller;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.controller.api.NotificationApi;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

  private final NotificationService notificationService;

  @Override
  @GetMapping
  public ResponseEntity<CursorPageResponse<NotificationResponse>> findUnconfirmed(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @Valid @ModelAttribute NotificationQueryCondition request
  ) {
    return ResponseEntity.ok(notificationService.findUnconfirmed(userId, request));
  }

}
