package com.sprint.mission.monew.domain.comment.repository.querydsl.impl;

import static com.sprint.mission.monew.domain.comment.entity.QComment.comment;
import static com.sprint.mission.monew.domain.comment.entity.QCommentLike.commentLike;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.comment.dto.CommentOrderBy;
import com.sprint.mission.monew.domain.comment.dto.CommentQueryCondition;
import com.sprint.mission.monew.domain.comment.dto.CommentResponse;
import com.sprint.mission.monew.domain.comment.repository.querydsl.CommentCustomRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponse<CommentResponse> getComments(CommentQueryCondition condition, UUID userId) {
    List<CommentResponse> raw = queryFactory.select(Projections.constructor(
            CommentResponse.class,
            comment.id,
            comment.article.id,
            comment.user.id,
            comment.user.nickname,
            comment.content,
            comment.likeCount,
            commentLike.id.isNotNull(),
            comment.createdAt
        ))
        .from(comment)
        .leftJoin(comment.user)
        .leftJoin(commentLike).on(
            commentLike.user.id.eq(userId).and(commentLike.comment.id.eq(comment.id))
        )
        .where(
            eqArticleId(condition.articleId()),
            isNullDeletedAt(),
            cursorCondition(condition)
        )
        .orderBy(
            buildOrderSpecifiers(condition.orderBy(), condition.direction())
        )
        .limit(condition.limit() + 1L)
        .fetch();

    boolean hasNext = raw.size() > condition.limit();
    List<CommentResponse> content = hasNext ? raw.subList(0, condition.limit()) : raw;

    String nextCursor = null;
    Instant nextAfter = null;
    if (hasNext && !content.isEmpty()) {
      CommentResponse last = content.get(content.size() - 1);
      nextCursor = extractCursor(last, condition.orderBy());
      nextAfter = last.createdAt();
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

  private OrderSpecifier<?>[] buildOrderSpecifiers(CommentOrderBy orderBy, SortDirection direction) {
    Order dir = direction == SortDirection.ASC ? Order.ASC : Order.DESC;
    return switch (orderBy) {
      case CREATED_AT -> new OrderSpecifier<?>[] {
          new OrderSpecifier<>(dir, comment.createdAt)
      };
      case LIKE_COUNT -> new OrderSpecifier<?>[] {
          new OrderSpecifier<>(dir, comment.likeCount),
          new OrderSpecifier<>(dir, comment.createdAt)
      };
    };
  }

  private BooleanExpression eqArticleId(UUID articleId) {
    return comment.article.id.eq(articleId);
  }

  private BooleanExpression isNullDeletedAt() {
    return comment.deletedAt.isNull();
  }

  private BooleanExpression cursorCondition(CommentQueryCondition condition) {
    String cursor = condition.cursor();
    Instant after = condition.after();
    boolean isAsc = condition.direction() == SortDirection.ASC;
    if (cursor == null) {
      return null;
    }
    return switch (condition.orderBy()) {
      case CREATED_AT -> buildCursorExpression(comment.createdAt, Instant.parse(cursor), isAsc);
      case LIKE_COUNT -> buildCursorExpression(comment.likeCount, Long.parseLong(cursor), after, isAsc);
    };
  }

  private BooleanExpression buildCursorExpression(
      ComparableExpression<Instant> field, Instant cursorValue, boolean isAsc) {
    return isAsc ? field.gt(cursorValue) : field.lt(cursorValue);
  }

  private BooleanExpression buildCursorExpression(
      NumberExpression<Long> field, long cursorValue, Instant after, boolean isAsc) {
    return isAsc
        ? field.gt(cursorValue).or(field.eq(cursorValue).and(comment.createdAt.gt(after)))
        : field.lt(cursorValue).or(field.eq(cursorValue).and(comment.createdAt.lt(after)));
  }

  private String extractCursor(CommentResponse last, CommentOrderBy orderBy) {
    return switch (orderBy) {
      case CREATED_AT -> last.createdAt().toString();
      case LIKE_COUNT -> String.valueOf(last.likeCount());
    };
  }
}