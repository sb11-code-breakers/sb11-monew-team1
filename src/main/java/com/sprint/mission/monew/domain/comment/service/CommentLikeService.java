package com.sprint.mission.monew.domain.comment.service;

import com.sprint.mission.monew.domain.comment.dto.CommentLikeResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import com.sprint.mission.monew.domain.comment.event.CommentLikedNotificationEvent;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
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

import com.sprint.mission.monew.domain.useractivity.listener.CommentLikedEvent;
import com.sprint.mission.monew.domain.useractivity.listener.CommentLikeRemovedEvent;
import java.time.Instant;

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
    log.debug("댓글 좋아요 등록 시작 | commentId={}, userId={}", commentId, userId);

    if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
      throw CommentLikeAlreadyExistsException.withId(userId, commentId);
    }

    User user =
        userRepository.findById(userId).orElseThrow(() -> UserNotFoundException.withId(userId));
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> CommentNotFoundException.withId(commentId));

    UUID articleId = comment.getArticle().getId();
    String articleTitle = comment.getArticle().getTitle();
    Instant commentCreatedAt = comment.getCreatedAt();

    UUID commentUserId = comment.getUser() != null ? comment.getUser().getId() : null;
    String commentUserNickname = comment.getUser() != null ? comment.getUser().getNickname() : null;
    String commentContent = comment.getContent();

    long nextLikeCount = comment.getLikeCount() + 1;

    CommentLike commentLike = CommentLike.create(user, comment);

    CommentLike savedCommentLike;
    CommentLikeResponse response;
    try {
      savedCommentLike = commentLikeRepository.saveAndFlush(commentLike);
      response = commentLikeMapper.toResponse(savedCommentLike, nextLikeCount);
      commentRepository.increaseLikeCount(commentId);
    } catch (DataIntegrityViolationException e) {
      throw CommentLikeAlreadyExistsException.withId(userId, commentId);
    }

    log.info("댓글 좋아요 등록 완료 | commentLikeId={}, commentId={}, userId={}",
        savedCommentLike.getId(), commentId, userId);

    log.debug("CommentLikedEvent 발행 | commentId={}, userId={}", commentId, userId);
    eventPublisher.publishEvent(new CommentLikedEvent(
        userId,
        savedCommentLike.getId(),
        savedCommentLike.getCreatedAt(),
        commentId,
        articleId,
        articleTitle,
        commentUserId,
        commentUserNickname,
        commentContent,
        nextLikeCount,
        commentCreatedAt
    ));

    UUID authorId = comment.getUser() != null ? comment.getUser().getId() : null;
    if (authorId != null && !authorId.equals(userId)) {
      String message = "[" + user.getNickname() + "]님이 나의 댓글을 좋아합니다.";
      eventPublisher.publishEvent(
          new CommentLikedNotificationEvent(authorId, message, ResourceType.COMMENT, commentId));
    }

    return response;
  }

  @Transactional
  public void cancel(UUID commentId, UUID userId) {
    log.debug("댓글 좋아요 취소 시작 | commentId={}, userId={}", commentId, userId);

    int deleted = commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);

    if (deleted == 0) {
      throw CommentLikeNotFoundException.withId(userId, commentId);
    }

    commentRepository.decreaseLikeCount(commentId);

    log.debug("CommentLikeRemovedEvent 발행 | commentId={}, userId={}", commentId, userId);
    eventPublisher.publishEvent(new CommentLikeRemovedEvent(userId, commentId));

    log.info("댓글 좋아요 취소 완료 | commentId={}, userId={}", commentId, userId);
  }
}