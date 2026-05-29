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
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ArticleMapper articleMapper;
  private final ArticleViewMapper articleViewMapper;

  public CursorPageResponse<ArticleResponse> search(ArticleQueryCondition condition, UUID requestUserId) {
    long totalElements = articleRepository.count(condition);
    List<Article> articles = articleRepository.findAll(condition);

    boolean hasNext = articles.size() > condition.limit();
    List<Article> page = hasNext ? articles.subList(0, condition.limit()) : articles;

    List<UUID> articleIds = page.stream().map(Article::getId).toList();
    Set<UUID> viewedIds =
        articleIds.isEmpty()
            ? Set.of()
            : articleViewRepository.findArticleIdsByArticleIdsAndUserId(articleIds, requestUserId);

    List<ArticleResponse> content =
        page.stream()
            .map(a -> articleMapper.toResponse(a, viewedIds.contains(a.getId())))
            .toList();

    String nextCursor = null;
    Instant nextAfter = null;
    if (hasNext && !page.isEmpty()) {
      Article last = page.get(page.size() - 1);
      nextCursor = articleRepository.buildCursor(last, condition.orderBy());
      nextAfter = last.getCreatedAt();
    }

    return CursorPageResponse.of(content, nextCursor, nextAfter, hasNext, content.size(), totalElements);
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
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> ArticleNotFoundException.withId(articleId));
    articleRepository.delete(article);
  }

  @Transactional
  public void softDelete(UUID articleId) {
    Article article = articleRepository.findById(articleId)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> ArticleNotFoundException.withId(articleId));
    article.softDelete();
  }

  @Transactional
  public ArticleViewResponse registerView(UUID articleId, UUID userId) {
    Article article = articleRepository.findById(articleId)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> ArticleNotFoundException.withId(articleId));

    return articleViewRepository.findByArticleIdAndUserId(articleId, userId)
        .map(articleViewMapper::toResponse)
        .orElseGet(() -> {
          article.incrementViewCount();
          ArticleView saved = articleViewRepository.save(ArticleView.create(userId, article));
          return articleViewMapper.toResponse(saved);
        });
  }
}
