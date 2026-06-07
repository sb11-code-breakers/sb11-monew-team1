package com.sprint.mission.monew.domain.comment.service;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.dto.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.CommentQueryCondition;
import com.sprint.mission.monew.domain.comment.dto.CommentUpdateRequest;
import com.sprint.mission.monew.domain.comment.dto.CommentResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.exception.CommentAccessDeniedException;
import com.sprint.mission.monew.domain.comment.exception.CommentNotFoundException;
import com.sprint.mission.monew.domain.comment.mapper.CommentMapper;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final CommentMapper commentMapper;

  @Transactional
  public CommentResponse create(CommentCreateRequest request) {

    log.debug("댓글 생성 시작 | articleId={}, userId={}", request.articleId(), request.userId());

    Article article = articleRepository.findById(request.articleId()).orElseThrow(
        () -> ArticleNotFoundException.withId(request.articleId())
    );
    User user = userRepository.findById(request.userId()).orElseThrow(
        () -> UserNotFoundException.withId(request.userId())
    );

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    articleRepository.increaseCommentCount(request.articleId());

    log.info("댓글 생성 완료 | commentId={}, articleId={}, userId={}",
        savedComment.getId(), request.articleId(), request.userId());

    return commentMapper.toResponse(savedComment, false);
  }

  @Transactional
  public CommentResponse update(UUID commentId, UUID userId, CommentUpdateRequest request) {

    log.debug("댓글 수정 시작 | commentId={}, userId={}", commentId, userId);

    Comment comment = commentRepository.findById(commentId).orElseThrow(
        () -> CommentNotFoundException.withId(commentId)
    );

    if (!comment.isOwner(userId)) {
      throw CommentAccessDeniedException.withId(commentId);
    }

    comment.updateContent(request.content());

    log.info("댓글 수정 완료 | commentId={}, userId={}", commentId, userId);

    return commentMapper.toResponse(comment, false);
  }

  @Transactional
  public void softDelete(UUID commentId, UUID userId) {
    log.debug("댓글 논리 삭제 시작 | commentId={}", commentId);

    Comment comment = commentRepository.findById(commentId).orElseThrow(
        () -> CommentNotFoundException.withId(commentId)
    );

    if (!comment.isOwner(userId)) {
      throw CommentAccessDeniedException.withId(commentId);
    }

    if (!comment.isDeleted()) {
      UUID articleId = comment.getArticle().getId();
      comment.softDelete();
      articleRepository.decreaseCommentCount(articleId);
    }

    log.info("댓글 논리 삭제 완료 | commentId={}, userId={}", commentId, userId);
  }

  @Transactional
  public void hardDelete(UUID commentId) {
    log.debug("댓글 물리 삭제 시작 | commentId={}", commentId);

    Comment comment = commentRepository.findById(commentId).orElseThrow(
        () -> CommentNotFoundException.withId(commentId)
    );

    boolean wasVisible = !comment.isDeleted();
    UUID articleId = comment.getArticle().getId();
    commentRepository.delete(comment);
    if (wasVisible) {
      articleRepository.decreaseCommentCount(articleId);
    }

    log.info("댓글 물리 삭제 완료 | commentId={}", commentId);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<CommentResponse> getComments(CommentQueryCondition condition,
      UUID requestId) {
    log.debug("댓글 목록 조회 시작 | articleId={}, orderBy={}, direction={}, limit={}, userId={}",
        condition.articleId(), condition.orderBy(), condition.direction(), condition.limit(), requestId);

    CursorPageResponse<CommentResponse> response = commentRepository.getComments(condition, requestId);

    log.info("댓글 목록 조회 완료 | count={}, hasNext={}, nextCursor={}",
        response.content().size(), response.hasNext(), response.nextCursor());

    return response;
  }

}
