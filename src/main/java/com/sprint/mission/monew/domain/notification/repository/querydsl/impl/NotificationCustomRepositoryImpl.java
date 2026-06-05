package com.sprint.mission.monew.domain.notification.repository.querydsl.impl;

import static com.sprint.mission.monew.domain.notification.entity.QNotification.notification;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.repository.querydsl.NotificationCustomRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationCustomRepositoryImpl implements NotificationCustomRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponse<NotificationResponse> findUnconfirmed(UUID userId,
      NotificationQueryCondition condition) {
    List<NotificationResponse> raw = queryFactory
        .select(Projections.constructor(NotificationResponse.class,
            notification.id,
            notification.createdAt,
            notification.confirmedAt,
            notification.confirmedAt.isNotNull(),
            notification.userId,
            notification.content,
            notification.resourceType,
            notification.resourceId))
        .from(notification)
        .where(
            eqUserId(userId),
            isNullConfirmedAt(),
            cursorCondition(condition)
        )
        .orderBy(
            buildCreatedAtOrderSpecifier(),
            buildIdOrderSpecifier()
        )
        .limit(condition.limit() + 1L)
        .fetch();

    boolean hasNext = raw.size() > condition.limit();
    List<NotificationResponse> content = hasNext ? raw.subList(0, condition.limit()) : raw;

    String nextCursor = null;
    Instant nextAfter = null;
    UUID nextIdAfter = null;
    if (hasNext && !content.isEmpty()) {
      NotificationResponse last = content.get(content.size() - 1);
      nextCursor = last.createdAt().toString();
      nextAfter = last.createdAt();
      nextIdAfter = last.id();
    }

    Long totalElements = queryFactory
        .select(notification.count())
        .from(notification)
        .where(
            eqUserId(userId),
            isNullConfirmedAt()
        )
        .fetchOne();

    return CursorPageResponse.of(
        content,
        nextCursor,
        nextAfter,
        nextIdAfter,
        hasNext,
        content.size(),
        totalElements
    );
  }

  private BooleanExpression eqUserId(UUID userId) {
    return notification.userId.eq(userId);
  }

  private BooleanExpression isNullConfirmedAt() {
    return notification.confirmedAt.isNull();
  }

  private BooleanExpression cursorCondition(NotificationQueryCondition condition) {
    Instant cursor = condition.cursor();
    UUID idAfter = condition.idAfter();
    if (cursor == null) {
      return null;
    }
    return notification.createdAt.gt(cursor)
        .or(notification.createdAt.eq(cursor).and(notification.id.gt(idAfter)));
  }

  private OrderSpecifier<?> buildCreatedAtOrderSpecifier() {
    return new OrderSpecifier<>(Order.ASC, notification.createdAt);
  }

  private OrderSpecifier<?> buildIdOrderSpecifier() {
    return new OrderSpecifier<>(Order.ASC, notification.id);
  }
}