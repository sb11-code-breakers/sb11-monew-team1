package com.sprint.mission.monew.domain.comment.controller;

import com.sprint.mission.monew.domain.comment.controller.api.CommentApi;
import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
import com.sprint.mission.monew.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
