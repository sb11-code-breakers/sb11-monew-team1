package com.sprint.mission.monew.domain.article.service;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.mapper.ArticleMapper;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

  public CursorPageResponse<ArticleResponse> search(ArticleQueryCondition condition, UUID requestUserId) {
    long totalElements = articleRepository.count(condition);
    List<Article> articles = articleRepository.findAll(condition);

    boolean hasNext = articles.size() > condition.limit();
    List<Article> page = hasNext ? articles.subList(0, condition.limit()) : articles;

    List<UUID> articleIds = page.stream().map(Article::getId).collect(Collectors.toList());
    Set<UUID> viewedIds =
        articleIds.isEmpty()
            ? Set.of()
            : articleViewRepository.findArticleIdsByArticleIdsAndUserId(articleIds, requestUserId);

    List<ArticleResponse> content =
        page.stream()
            .map(a -> articleMapper.toDto(a, viewedIds.contains(a.getId())))
            .collect(Collectors.toList());

    String nextCursor = null;
    Instant nextAfter = null;
    if (hasNext && !page.isEmpty()) {
      Article last = page.get(page.size() - 1);
      nextCursor = buildCursor(last, condition.orderBy());
      nextAfter = last.getCreatedAt();
    }

    return CursorPageResponse.of(content, nextCursor, nextAfter, hasNext, content.size(), totalElements);
  }

  private String buildCursor(Article article, ArticleOrderBy orderBy) {
    return switch (orderBy) {
      case PUBLISH_DATE -> article.getPublishDate().toString();
      case COMMENT_COUNT -> String.valueOf(article.getCommentCount());
      case VIEW_COUNT -> String.valueOf(article.getViewCount());
    };
  }
}
