package com.sprint.mission.monew.domain.comment.controller;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.comment.controller.api.CommentApi;
import com.sprint.mission.monew.domain.comment.dto.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.CommentQueryCondition;
import com.sprint.mission.monew.domain.comment.dto.CommentUpdateRequest;
import com.sprint.mission.monew.domain.comment.dto.CommentResponse;
import com.sprint.mission.monew.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController implements CommentApi {

  private final CommentService commentService;

  @Override
  @PostMapping
  public ResponseEntity<CommentResponse> createComment(
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @RequestBody @Valid CommentCreateRequest request) {
    CommentResponse response = commentService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentResponse> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @RequestBody @Valid CommentUpdateRequest request
  ) {
    CommentResponse response = commentService.update(commentId, userId, request);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> softDeleteComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    commentService.softDelete(commentId, userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDeleteComment(@PathVariable UUID commentId) {
    commentService.hardDelete(commentId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  @GetMapping
  public ResponseEntity<CursorPageResponse<CommentResponse>> getComments(
      @ModelAttribute @Valid CommentQueryCondition condition,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    CursorPageResponse<CommentResponse> response = commentService.getComments(condition, userId);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
