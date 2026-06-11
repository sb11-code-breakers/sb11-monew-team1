package com.sprint.mission.monew.domain.article.service;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.dto.ArticleViewResponse;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleView;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.mapper.ArticleMapper;
import com.sprint.mission.monew.domain.article.mapper.ArticleViewMapper;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 💡 [수정] 스프링 이벤트 발행 및 기사 조회/삭제 이벤트 import 추가
import org.springframework.context.ApplicationEventPublisher;
import com.sprint.mission.monew.domain.article.event.ArticleViewedEvent;
import com.sprint.mission.monew.domain.article.event.ArticleDeletedEvent;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ArticleMapper articleMapper;
  private final ArticleViewMapper articleViewMapper;
  private final ApplicationEventPublisher eventPublisher; // 💡 [수정] 이벤트 퍼블리셔 주입 추가

  public CursorPageResponse<ArticleResponse> search(ArticleQueryCondition condition, UUID requestUserId) {
    return articleRepository.search(condition, requestUserId);
  }

  public ArticleResponse getArticle(UUID articleId, UUID requestUserId) {
    Article article = articleRepository.findById(articleId)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> ArticleNotFoundException.withId(articleId));
    boolean viewedByMe = articleViewRepository.existsByArticleIdAndUserId(articleId, requestUserId);
    return articleMapper.toResponse(article, viewedByMe);
  }

  @Transactional
  public void hardDelete(UUID articleId) {
    log.debug("기사 물리 삭제 시작 | articleId={}", articleId);
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> ArticleNotFoundException.withId(articleId));
    articleRepository.delete(article);
    log.info("기사 물리 삭제 완료 | articleId={}", articleId);
  }

  @Transactional
  public void softDelete(UUID articleId) {
    log.debug("기사 논리 삭제 시작 | articleId={}", articleId);
    Article article = articleRepository.findById(articleId)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> ArticleNotFoundException.withId(articleId));
    article.softDelete();

    // 💡 [수정] 기사 논리 삭제 이벤트 발행 (MongoDB 활동 로그 전파용)
    eventPublisher.publishEvent(new ArticleDeletedEvent(articleId));

    log.info("기사 논리 삭제 완료 | articleId={}", articleId);
  }

  @Transactional
  public ArticleViewResponse registerView(UUID articleId, UUID userId) {
    log.debug("기사 조회 등록 시작 | articleId={}, userId={}", articleId, userId);
    Article article = articleRepository.findById(articleId)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> ArticleNotFoundException.withId(articleId));

    return articleViewRepository.findByArticleIdAndUserId(articleId, userId)
        .map(view -> articleViewMapper.toResponse(view, view.getArticle().getViewCount()))
        .orElseGet(() -> {
          ArticleView saved = articleViewRepository.save(ArticleView.create(userId, article));
          articleRepository.increaseViewCount(articleId);

          // 💡 [수정] 기사 최초 조회 성공 시 이벤트 발행 (MongoDB articleViews 배열에 최대 10건 적재 트리거)
          // 몽고DB 일기장에 기록될 유저ID, 기사ID와 불변 텍스트인 기사 제목을 실어 보냅니다.
          eventPublisher.publishEvent(new ArticleViewedEvent(userId, articleId, article.getTitle()));

          log.info("기사 조회 등록 완료 | articleId={}, userId={}", articleId, userId);
          return articleViewMapper.toResponse(saved, article.getViewCount() + 1);
        });
  }
}