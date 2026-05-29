package com.sprint.mission.monew.domain.comment.controller;

import com.sprint.mission.monew.domain.comment.controller.api.CommentApi;
import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.request.CommentUpdateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
import com.sprint.mission.monew.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController implements CommentApi {

  private final CommentService commentService;

  @Override
  @PostMapping
  public ResponseEntity<CommentResponse> createComment(
      @RequestBody @Valid CommentCreateRequest request) {
    log.info("[COMMENT_CREATE_REQUEST] 댓글 생성 요청 - 뉴스 기사 ID={}", request.articleId());
    log.debug("[COMMENT_CREATE_REQUEST] 댓글 생성 요청 - 댓글 작성자 ID={}", request.userId());

    CommentResponse response = commentService.create(request);

    log.debug("[COMMENT_CREATE_RESPONSE] 댓글 생성 응답 - 댓글 ID={}", response.id());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentResponse> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @RequestBody @Valid CommentUpdateRequest request
  ) {
    log.info("[COMMENT_UPDATE_REQUEST] 댓글 수정 요청 - 댓글 ID={}", commentId);
    log.debug("[COMMENT_UPDATE_REQUEST] 댓글 수정 요청 - 요청자 ID={}", userId);

    CommentResponse response = commentService.update(commentId, userId, request);

    log.debug("[COMMENT_UPDATE_RESPONSE] 댓글 수정 응답 - 댓글 ID={}", response.id());

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> softDeleteComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    log.info("[COMMENT_SOFT_DELETE_REQUEST] 댓글 논리 삭제 요청 - 댓글 ID={}", commentId);
    log.debug("[COMMENT_SOFT_DELETE_REQUEST] 댓글 논리 삭제 요청 - 요청자 ID={}", userId);

    commentService.softDelete(commentId, userId);

    log.debug("[COMMENT_SOFT_DELETE_RESPONSE] 댓글 논리 삭제 응답 - 댓글 ID={}", commentId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDeleteComment(@PathVariable UUID commentId) {
    log.info("[COMMENT_HARD_DELETE_REQUEST] 댓글 물리 삭제 요청 - 댓글 ID={}", commentId);

    commentService.hardDelete(commentId);

    log.debug("[COMMENT_HARD_DELETE_RESPONSE] 댓글 물리 삭제 응답 - 댓글 ID={}", commentId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
