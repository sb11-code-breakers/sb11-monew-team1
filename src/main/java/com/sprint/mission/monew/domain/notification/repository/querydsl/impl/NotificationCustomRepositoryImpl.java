package com.sprint.mission.monew.domain.notification.repository.querydsl.impl;

import static com.sprint.mission.monew.domain.notification.entity.QNotification.notification;

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
    int pageSize = condition.limit();

    List<NotificationResponse> fetched = queryFactory
        .select(Projections.constructor(NotificationResponse.class,
            notification.id,
            notification.createdAt,
            notification.updatedAt,
            notification.confirmedAt.isNotNull(),
            notification.userId,
            notification.content,
            notification.resourceType,
            notification.resourceId))
        .from(notification)
        .where(
            eqUserId(userId),
            unconfirmedOnly(),
            cursorCondition(condition.cursor(), condition.after())
        )
        .orderBy(notification.createdAt.desc(), notification.id.desc())
        .limit(pageSize + 1)
        .fetch();

    boolean hasNext = fetched.size() > pageSize;
    List<NotificationResponse> page = hasNext ? fetched.subList(0, pageSize) : fetched;

    String nextCursor = null;
    Instant nextAfter = null;
    if (hasNext && !page.isEmpty()) {
      NotificationResponse last = page.get(page.size() - 1);
      nextCursor = last.id().toString();
      nextAfter = last.createdAt();
    }

    Long totalElements = queryFactory
        .select(notification.count())
        .from(notification)
        .where(eqUserId(userId), unconfirmedOnly())
        .fetchOne();

    return CursorPageResponse.of(page, nextCursor, nextAfter, hasNext, page.size(), totalElements);
  }

  private BooleanExpression eqUserId(UUID userId) {
    return notification.userId.eq(userId);
  }

  private BooleanExpression unconfirmedOnly() {
    return notification.confirmedAt.isNull();
  }

  private BooleanExpression cursorCondition(UUID cursorId, Instant cursorCreatedAt) {
    if (cursorId == null || cursorCreatedAt == null) {
      return null;
    }
    return notification.createdAt.lt(cursorCreatedAt)
        .or(notification.createdAt.eq(cursorCreatedAt).and(notification.id.lt(cursorId)));
  }
}