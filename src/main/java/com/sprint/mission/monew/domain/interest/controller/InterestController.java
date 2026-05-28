package com.sprint.mission.monew.domain.interest.controller;

import com.sprint.mission.monew.domain.interest.controller.api.InterestApi;
import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import com.sprint.mission.monew.domain.interest.service.InterestService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController implements InterestApi {

  private final InterestService interestService;

  @PostMapping
  @Override
  public ResponseEntity<InterestResponse> create(
      @Valid @RequestBody InterestCreateRequest request,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(interestService.create(request, requestUserId));
  }

  @Override
  @PatchMapping(path = "{id}")
  public ResponseEntity<InterestResponse> updateKeywords(
      @PathVariable UUID id,
      @Valid @RequestBody InterestUpdateRequest request,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    return ResponseEntity.ok(interestService.updateKeywords(id, request, requestUserId));
  }
}
