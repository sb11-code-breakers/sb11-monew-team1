package com.sprint.mission.monew.domain.article.repository.querydsl.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.QArticle;
import com.sprint.mission.monew.domain.article.entity.QArticleInterest;
import com.sprint.mission.monew.domain.article.exception.ArticleInvalidCursorException;
import com.sprint.mission.monew.domain.article.repository.querydsl.ArticleCustomRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class ArticleCustomRepositoryImpl implements ArticleCustomRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Article> findAll(ArticleQueryCondition condition) {
    QArticle article = QArticle.article;

    JPAQuery<Article> query =
        queryFactory
            .selectFrom(article)
            .where(buildPredicate(article, condition))
            .orderBy(buildOrderSpecifiers(article, condition))
            .limit(condition.limit() + 1L);

    applyInterestJoin(query, article, condition);

    return query.fetch();
  }

  @Override
  public long count(ArticleQueryCondition condition) {
    QArticle article = QArticle.article;

    JPAQuery<Long> query =
        queryFactory.select(article.count()).from(article).where(buildPredicate(article, condition));

    applyInterestJoin(query, article, condition);

    Long result = query.fetchOne();
    return result != null ? result : 0L;
  }

  private <T> void applyInterestJoin(
      JPAQuery<T> query, QArticle article, ArticleQueryCondition condition) {
    if (condition.interestId() != null) {
      QArticleInterest articleInterest = QArticleInterest.articleInterest;
      query
          .join(articleInterest)
          .on(
              articleInterest
                  .article
                  .id
                  .eq(article.id)
                  .and(articleInterest.interest.id.eq(condition.interestId())));
    }
  }

  private BooleanBuilder buildPredicate(QArticle article, ArticleQueryCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();

    builder.and(article.deletedAt.isNull());

    if (StringUtils.hasText(condition.keyword())) {
      builder.and(
          article
              .title
              .containsIgnoreCase(condition.keyword())
              .or(article.summary.containsIgnoreCase(condition.keyword())));
    }

    if (condition.sourceIn() != null && !condition.sourceIn().isEmpty()) {
      builder.and(article.source.in(condition.sourceIn()));
    }

    if (condition.publishDateFrom() != null) {
      builder.and(article.publishDate.goe(condition.publishDateFrom()));
    }

    if (condition.publishDateTo() != null) {
      builder.and(article.publishDate.loe(condition.publishDateTo()));
    }

    if ((condition.cursor() == null) != (condition.after() == null)) {
      throw new IllegalArgumentException("cursor와 after는 함께 전달되어야 합니다.");
    }
    if (condition.cursor() != null) {
      builder.and(buildCursorCondition(article, condition));
    }

    return builder;
  }

  private BooleanExpression buildCursorCondition(QArticle article, ArticleQueryCondition condition) {
    boolean isDesc = condition.direction() == SortDirection.DESC;
    Instant after = condition.after();

    return switch (condition.orderBy()) {
      case PUBLISH_DATE -> {
        Instant cursorInstant;
        try {
          cursorInstant = Instant.parse(condition.cursor());
        } catch (DateTimeParseException ex) {
          throw ArticleInvalidCursorException.withCursor(condition.cursor());
        }
        yield isDesc
            ? article
                .publishDate
                .lt(cursorInstant)
                .or(article.publishDate.eq(cursorInstant).and(article.createdAt.lt(after)))
            : article
                .publishDate
                .gt(cursorInstant)
                .or(article.publishDate.eq(cursorInstant).and(article.createdAt.gt(after)));
      }
      case COMMENT_COUNT -> {
        int cursorVal;
        try {
          cursorVal = Integer.parseInt(condition.cursor());
        } catch (NumberFormatException ex) {
          throw ArticleInvalidCursorException.withCursor(condition.cursor());
        }
        yield isDesc
            ? article
                .commentCount
                .lt(cursorVal)
                .or(article.commentCount.eq(cursorVal).and(article.createdAt.lt(after)))
            : article
                .commentCount
                .gt(cursorVal)
                .or(article.commentCount.eq(cursorVal).and(article.createdAt.gt(after)));
      }
      case VIEW_COUNT -> {
        int cursorVal;
        try {
          cursorVal = Integer.parseInt(condition.cursor());
        } catch (NumberFormatException ex) {
          throw ArticleInvalidCursorException.withCursor(condition.cursor());
        }
        yield isDesc
            ? article
                .viewCount
                .lt(cursorVal)
                .or(article.viewCount.eq(cursorVal).and(article.createdAt.lt(after)))
            : article
                .viewCount
                .gt(cursorVal)
                .or(article.viewCount.eq(cursorVal).and(article.createdAt.gt(after)));
      }
    };
  }

  @Override
  public String buildCursor(Article article, ArticleOrderBy orderBy) {
    return switch (orderBy) {
      case PUBLISH_DATE -> article.getPublishDate().toString();
      case COMMENT_COUNT -> String.valueOf(article.getCommentCount());
      case VIEW_COUNT -> String.valueOf(article.getViewCount());
    };
  }

  private OrderSpecifier<?>[] buildOrderSpecifiers(
      QArticle article, ArticleQueryCondition condition) {
    Order dir = condition.direction() == SortDirection.DESC ? Order.DESC : Order.ASC;

    OrderSpecifier<?> primary =
        switch (condition.orderBy()) {
          case PUBLISH_DATE -> new OrderSpecifier<>(dir, article.publishDate);
          case COMMENT_COUNT -> new OrderSpecifier<>(dir, article.commentCount);
          case VIEW_COUNT -> new OrderSpecifier<>(dir, article.viewCount);
        };

    return new OrderSpecifier<?>[] {primary, new OrderSpecifier<>(dir, article.createdAt)};
  }
}
