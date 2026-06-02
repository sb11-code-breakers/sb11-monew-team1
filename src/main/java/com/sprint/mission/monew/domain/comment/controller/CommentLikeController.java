package com.sprint.mission.monew.domain.comment.controller;

import com.sprint.mission.monew.domain.comment.controller.api.CommentLikeApi;
import com.sprint.mission.monew.domain.comment.dto.CommentLikeResponse;
import com.sprint.mission.monew.domain.comment.service.CommentLikeService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentLikeController implements CommentLikeApi {

  private final CommentLikeService commentLikeService;

  @Override
  @PostMapping("/{commentId}/comment-likes")
  public ResponseEntity<CommentLikeResponse> createCommentLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    log.info("[COMMENT_LIKE_CREATE_REQUEST] 댓글 좋아요 등록 요청 - 댓글 ID={}",
        commentId);
    log.debug("[COMMENT_LIKE_CREATE_REQUEST] 댓글 좋아요 등록 요청 - 요청자 ID={}",
        userId);

    CommentLikeResponse response = commentLikeService.create(commentId, userId);

    log.debug("[COMMENT_LIKE_CREATE_RESPONSE] 댓글 좋아요 등록 응답 - 댓글 좋아요 ID={}",
        response.id());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @DeleteMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> cancelCommentLike(
      @PathVariable @Parameter(description = "댓글 ID") UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    log.info("[COMMENT_LIKE_CANCEL_REQUEST] 댓글 좋아요 취소 요청 - 댓글 ID={}, 요청자 ID={}",
        commentId, userId);

    commentLikeService.cancel(commentId, userId);

    log.debug("[COMMENT_LIKE_CANCEL_RESPONSE] 댓글 좋아요 취소 응답 - 댓글 ID={}, 요청자 ID={}",
        commentId, userId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
