package com.sprint.mission.monew.domain.useractivity.service;

import com.sprint.mission.monew.domain.article.entity.ArticleView;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
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
  private final ArticleViewRepository articleViewRepository;

  public UserActivityResponse getUserActivity(UUID userId) {
    log.debug("활동 내역 조회 시도: userId={}", userId);

    // TODO: 구현 예정

    log.info("활동 내역 조회 완료: userId={}", userId);
    return null;
  }
}