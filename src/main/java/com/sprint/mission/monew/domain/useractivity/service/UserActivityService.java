package com.sprint.mission.monew.domain.useractivity.service;

import com.sprint.mission.monew.domain.article.entity.ArticleView;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.interest.entity.InterestKeyword;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.dto.ArticleViewDto;
import com.sprint.mission.monew.domain.useractivity.dto.CommentDto;
import com.sprint.mission.monew.domain.useractivity.dto.CommentLikeDto;
import com.sprint.mission.monew.domain.useractivity.dto.SubscriptionDto;
import com.sprint.mission.monew.domain.useractivity.dto.UserActivityResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserActivityService {

  private final UserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleViewRepository articleViewRepository;

  public UserActivityResponse getUserActivity(UUID userId) {
    log.debug("활동 내역 조회 시도: userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
    List<SubscriptionDto> subscriptionDtos = subscriptions.stream()
        .map(s -> new SubscriptionDto(
            s.getId(),
            s.getInterest().getId(),
            s.getInterest().getName(),
            s.getInterest().getKeywords().stream()
                .map(InterestKeyword::getKeyword)
                .toList(),
            s.getInterest().getSubscriberCount(),
            s.getCreatedAt()
        ))
        .toList();

    List<Comment> comments = commentRepository.findTop10RecentCommentsByUserId(userId);
    List<CommentDto> commentDtos = comments.stream()
        .map(c -> new CommentDto(
            c.getId(),
            c.getArticle().getId(),
            c.getArticle().getTitle(),
            c.getUser() != null ? c.getUser().getId() : null,
            c.getUser() != null ? c.getUser().getNickname() : null,
            c.getContent(),
            c.getLikeCount(),
            c.getCreatedAt()
        ))
        .toList();

    List<CommentLike> commentLikes = commentLikeRepository.findTop10ByUserId(userId);
    List<CommentLikeDto> commentLikeDtos = commentLikes.stream()
        .map(cl -> new CommentLikeDto(
            cl.getId(),
            cl.getCreatedAt(),
            cl.getComment().getId(),
            cl.getComment().getArticle().getId(),
            cl.getComment().getArticle().getTitle(),
            cl.getComment().getUser() != null ? cl.getComment().getUser().getId() : null,
            cl.getComment().getUser() != null ? cl.getComment().getUser().getNickname() : null,
            cl.getComment().getContent(),
            cl.getComment().getLikeCount(),
            cl.getComment().getCreatedAt()
        ))
        .toList();

    List<ArticleView> articleViews = articleViewRepository
        .findTop10ByUserIdAndArticleNotDeleted(userId);
    List<ArticleViewDto> articleViewDtos = articleViews.stream()
        .map(av -> new ArticleViewDto(
            av.getId(),
            av.getUserId(),
            av.getCreatedAt(),
            av.getArticle().getId(),
            av.getArticle().getSource().name(),
            av.getArticle().getSourceUrl(),
            av.getArticle().getTitle(),
            av.getArticle().getPublishDate(),
            av.getArticle().getSummary(),
            av.getArticle().getCommentCount(),
            av.getArticle().getViewCount()
        ))
        .toList();

    log.info("활동 내역 조회 완료: userId={}", userId);

    return new UserActivityResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getCreatedAt(),
        subscriptionDtos,
        commentDtos,
        commentLikeDtos,
        articleViewDtos
    );
  }
}