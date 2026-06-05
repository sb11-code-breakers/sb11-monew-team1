package com.sprint.mission.monew.domain.article.repository.querydsl.impl;

import static com.sprint.mission.monew.domain.article.entity.QArticle.article;
import static com.sprint.mission.monew.domain.article.entity.QArticleInterest.articleInterest;
import static com.sprint.mission.monew.domain.article.entity.QArticleView.articleView;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.querydsl.ArticleCustomRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class ArticleCustomRepositoryImpl implements ArticleCustomRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponse<ArticleResponse> search(ArticleQueryCondition condition,
      UUID requestUserId) {
    BooleanExpression viewedByMeExpr = articleView.id.isNotNull();
    JPAQuery<Tuple> query = queryFactory
        .select(
            article.id,
            article.source,
            article.sourceUrl,
            article.title,
            article.publishDate,
            article.summary,
            article.commentCount,
            article.viewCount,
            viewedByMeExpr,
            article.createdAt
        )
        .from(article)
        .leftJoin(articleView).on(
            articleView.article.id.eq(article.id).and(articleView.userId.eq(requestUserId))
        )
        .where(
            isNullDeletedAt(),
            likeKeyword(condition.keyword()),
            eqSourceIn(condition.sourceIn()),
            goePublishDateFrom(condition.publishDateFrom()),
            loePublishDateTo(condition.publishDateTo()),
            cursorCondition(condition)
        )
        .orderBy(
            buildOrderSpecifier(condition.orderBy(), condition.direction()),
            buildCreatedAtOrderSpecifier(condition.direction())
        )
        .limit(condition.limit() + 1L);
    if (condition.interestId() != null) {
      query.join(articleInterest).on(
          articleInterest.article.id.eq(article.id)
              .and(articleInterest.interest.id.eq(condition.interestId())));
    }

    List<Tuple> raw = query.fetch();
    boolean hasNext = raw.size() > condition.limit();
    List<Tuple> rawContent = hasNext ? raw.subList(0, condition.limit()) : raw;

    List<ArticleResponse> content = rawContent.stream()
        .map(t -> new ArticleResponse(
            t.get(article.id),
            t.get(article.source),
            t.get(article.sourceUrl),
            t.get(article.title),
            t.get(article.publishDate),
            t.get(article.summary),
            t.get(article.commentCount),
            t.get(article.viewCount),
            Boolean.TRUE.equals(t.get(viewedByMeExpr))))
        .toList();

    String nextCursor = null;
    Instant nextAfter = null;
    if (hasNext && !rawContent.isEmpty()) {
      Tuple last = rawContent.get(rawContent.size() - 1);
      nextCursor = extractCursor(last, condition.orderBy());
      nextAfter = last.get(article.createdAt);
    }

    return CursorPageResponse.of(
        content,
        nextCursor,
        nextAfter,
        hasNext,
        content.size(),
        null
    );
  }

  private BooleanExpression isNullDeletedAt() {
    return article.deletedAt.isNull();
  }

  private BooleanExpression likeKeyword(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return null;
    }
    return article.title.containsIgnoreCase(keyword)
        .or(article.summary.containsIgnoreCase(keyword));
  }

  private BooleanExpression eqSourceIn(List<ArticleSource> sources) {
    if (sources == null || sources.isEmpty()) {
      return null;
    }
    return article.source.in(sources);
  }

  private BooleanExpression goePublishDateFrom(Instant from) {
    return from != null ? article.publishDate.goe(from) : null;
  }

  private BooleanExpression loePublishDateTo(Instant to) {
    return to != null ? article.publishDate.loe(to) : null;
  }

  private BooleanExpression cursorCondition(ArticleQueryCondition condition) {
    String cursor = condition.cursor();
    Instant after = condition.after();
    boolean isAsc = condition.direction() == SortDirection.ASC;
    if (cursor == null) {
      return null;
    }
    return switch (condition.orderBy()) {
      case PUBLISH_DATE ->
          buildCursorExpression(article.publishDate, Instant.parse(cursor), after, isAsc);
      case COMMENT_COUNT ->
          buildCursorExpression(article.commentCount, Integer.parseInt(cursor), after, isAsc);
      case VIEW_COUNT ->
          buildCursorExpression(article.viewCount, Integer.parseInt(cursor), after, isAsc);
    };
  }

  private BooleanExpression buildCursorExpression(
      ComparableExpression<Instant> field, Instant cursorValue, Instant after, boolean isAsc) {
    return isAsc
        ? field.gt(cursorValue).or(field.eq(cursorValue).and(article.createdAt.gt(after)))
        : field.lt(cursorValue).or(field.eq(cursorValue).and(article.createdAt.lt(after)));
  }

  private BooleanExpression buildCursorExpression(
      NumberExpression<Integer> field, int cursorValue, Instant after, boolean isAsc) {
    return isAsc
        ? field.gt(cursorValue).or(field.eq(cursorValue).and(article.createdAt.gt(after)))
        : field.lt(cursorValue).or(field.eq(cursorValue).and(article.createdAt.lt(after)));
  }

  private String extractCursor(Tuple last, ArticleOrderBy orderBy) {
    return switch (orderBy) {
      case PUBLISH_DATE -> last.get(article.publishDate).toString();
      case COMMENT_COUNT -> String.valueOf(last.get(article.commentCount));
      case VIEW_COUNT -> String.valueOf(last.get(article.viewCount));
    };
  }

  private OrderSpecifier<?> buildOrderSpecifier(ArticleOrderBy orderBy, SortDirection direction) {
    Order dir = direction == SortDirection.DESC ? Order.DESC : Order.ASC;
    return switch (orderBy) {
      case PUBLISH_DATE -> new OrderSpecifier<>(dir, article.publishDate);
      case COMMENT_COUNT -> new OrderSpecifier<>(dir, article.commentCount);
      case VIEW_COUNT -> new OrderSpecifier<>(dir, article.viewCount);
    };
  }

  private OrderSpecifier<?> buildCreatedAtOrderSpecifier(SortDirection direction) {
    Order dir = direction == SortDirection.DESC ? Order.DESC : Order.ASC;
    return new OrderSpecifier<>(dir, article.createdAt);
  }
}