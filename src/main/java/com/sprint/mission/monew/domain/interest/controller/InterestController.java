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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController implements InterestApi {

  private final InterestService interestService;

  @Override
  @PostMapping
  public ResponseEntity<InterestResponse> create(
      @Valid @RequestBody InterestCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(interestService.create(request));
  }

  @Override
  @PatchMapping(path = "{id}")
  public ResponseEntity<InterestResponse> updateKeywords(
      @PathVariable UUID id,
      @Valid @RequestBody InterestUpdateRequest request) {
    return ResponseEntity.ok(interestService.updateKeywords(id, request));
  }

  @Override
  @DeleteMapping("{id}")
  public ResponseEntity<Void> hardDelete(
      @PathVariable UUID id) {
    interestService.hardDelete(id);
    return ResponseEntity.noContent().build();
  }
}