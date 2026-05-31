package com.sprint.mission.monew.domain.comment.service;

import com.sprint.mission.monew.domain.comment.dto.response.CommentLikeResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import com.sprint.mission.monew.domain.comment.event.CommentLikedEvent;
import com.sprint.mission.monew.domain.comment.exception.CommentLikeAlreadyExistsException;
import com.sprint.mission.monew.domain.comment.exception.CommentLikeNotFoundException;
import com.sprint.mission.monew.domain.comment.exception.CommentNotFoundException;
import com.sprint.mission.monew.domain.comment.mapper.CommentLikeMapper;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

  private final CommentLikeRepository commentLikeRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeMapper commentLikeMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public CommentLikeResponse create(UUID commentId, UUID userId) {
    log.debug("[COMMENT_LIKE_CREATE_START] 댓글 좋아요 등록 시작 - 요청자 ID={}, 댓글 ID={}", userId, commentId);

    if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
      throw CommentLikeAlreadyExistsException.withId(userId, commentId);
    }

    User user =
        userRepository.findById(userId).orElseThrow(() -> UserNotFoundException.withId(userId));
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> CommentNotFoundException.withId(commentId));

    CommentLike commentLike = CommentLike.create(user, comment);

    commentRepository.increaseLikeCount(commentId);
    CommentLike savedCommentLike;
    try {
      savedCommentLike = commentLikeRepository.saveAndFlush(commentLike);
    } catch (DataIntegrityViolationException e) {
      throw CommentLikeAlreadyExistsException.withId(userId, commentId);
    }

    log.info(
        "[COMMENT_LIKE_CREATE_SUCCESS] 댓글 좋아요 등록 성공 - 좋아요 ID={}, 요청자 ID={}, 댓글 ID={}",
        savedCommentLike.getId(),
        userId,
        commentId);

    UUID authorId = comment.getUser() != null ? comment.getUser().getId() : null;
    if (authorId != null && !authorId.equals(userId)) {
      eventPublisher.publishEvent(
          new CommentLikedEvent(commentId, authorId, user.getNickname()));
    }

    return commentLikeMapper.toResponse(savedCommentLike);
  }

  @Transactional
  public void cancel(UUID commentId, UUID userId) {
    log.debug("[COMMENT_LIKE_CANCEL_START] 댓글 좋아요 취소 시작 - 요청자 ID={}, 댓글 ID={}", userId, commentId);

    int deleted = commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);

    if (deleted == 0) {
      throw CommentLikeNotFoundException.withId(userId, commentId);
    }

    commentRepository.decreaseLikeCount(commentId);

    log.info("[COMMENT_LIKE_CANCEL_SUCCESS] 댓글 좋아요 취소 성공 - 요청자 ID={}, 댓글 ID={}", userId, commentId);
  }
}
