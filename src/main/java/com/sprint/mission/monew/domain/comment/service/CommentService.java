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
  private final CommentMetrics commentMetrics;

  @Transactional
  public CommentResponse create(CommentCreateRequest request) {

    log.debug("[COMMENT_CREATE_START] 댓글 생성 시작 - 뉴스 기사 ID={}, 댓글 작성자 ID={}",
        request.articleId(), request.userId());

    Article article = articleRepository.findById(request.articleId()).orElseThrow(
        () -> ArticleNotFoundException.withId(request.articleId())
    );
    User user = userRepository.findById(request.userId()).orElseThrow(
        () -> UserNotFoundException.withId(request.userId())
    );

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    commentMetrics.countCreated();

    log.info("[COMMENT_CREATE_SUCCESS] 댓글 생성 성공 - 댓글 ID={}, 뉴스 기사 ID={}, 댓글 작성자 ID={}",
        savedComment.getId(), request.articleId(), request.userId());

    return commentMapper.toResponse(savedComment, false);
  }

  @Transactional
  public CommentResponse update(UUID commentId, UUID userId, CommentUpdateRequest request) {

    log.debug("[COMMENT_UPDATE_START] 댓글 수정 시작 - 댓글 ID={}, 요청자 ID={}",
        commentId, userId);

    Comment comment = commentRepository.findById(commentId).orElseThrow(
        () -> CommentNotFoundException.withId(commentId)
    );

    if (!comment.isOwner(userId)) {
      throw CommentAccessDeniedException.withId(commentId);
    }

    comment.updateContent(request.content());

    log.info("[COMMENT_UPDATE_SUCCESS] 댓글 수정 성공 - 댓글 ID={}, 요청자 ID={}",
        commentId, userId);

    return commentMapper.toResponse(comment, false);
  }

  @Transactional
  public void softDelete(UUID commentId, UUID userId) {
    log.debug("[COMMENT_SOFT_DELETE_START] 댓글 논리삭제 시작 - 댓글 ID={}",
        commentId);

    Comment comment = commentRepository.findById(commentId).orElseThrow(
        () -> CommentNotFoundException.withId(commentId)
    );

    if (!comment.isOwner(userId)) {
      throw CommentAccessDeniedException.withId(commentId);
    }

    comment.softDelete();

    log.info("[COMMENT_SOFT_DELETE_SUCCESS] 댓글 논리삭제 성공 - 댓글 ID={}, 요청자 ID={}",
        commentId, userId);
  }

  @Transactional
  public void hardDelete(UUID commentId) {
    log.debug("[COMMENT_HARD_DELETE_START] 댓글 물리삭제 시작 - 댓글 ID={}", commentId);

    Comment comment = commentRepository.findById(commentId).orElseThrow(
        () -> CommentNotFoundException.withId(commentId)
    );

    commentRepository.delete(comment);

    log.info("[COMMENT_HARD_DELETE_SUCCESS] 댓글 물리삭제 성공 - 댓글 ID={}", commentId);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<CommentResponse> getComments(CommentQueryCondition condition,
      UUID requestId) {
    log.debug("[COMMENT_READ_START] 댓글 목록 조회 시작 - 뉴스 기사 ID={}, 정렬={}, 방향={}, 페이지 크기={}, 요청자 ID={}",
        condition.articleId(), condition.orderBy(), condition.direction(), condition.limit(),
        requestId);

    CursorPageResponse<CommentResponse> response = commentRepository.getComments(condition, requestId);

    log.info("[COMMENT_READ_SUCCESS] 댓글 목록 조회 성공 - 조회된 댓글 수={}, 다음 페이지 여부={}, 다음 커서={}",
        response.content().size(), response.hasNext(), response.nextCursor());

    return response;
  }

}
