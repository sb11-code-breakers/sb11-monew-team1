package com.sprint.mission.monew.domain.interest.repository.querydsl.impl;

import static com.sprint.mission.monew.domain.interest.entity.QInterest.interest;
import static com.sprint.mission.monew.domain.interest.entity.QInterestKeyword.interestKeyword;
import static com.sprint.mission.monew.domain.interest.entity.QSubscription.subscription;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.interest.dto.InterestOrderBy;
import com.sprint.mission.monew.domain.interest.dto.InterestQueryCondition;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.querydsl.InterestCustomRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class InterestCustomRepositoryImpl implements InterestCustomRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponse<InterestResponse> findInterests(InterestQueryCondition condition,
      UUID userId) {
    List<Tuple> raw = queryFactory
        .selectDistinct(interest, subscription.id)
        .from(interest)
        .leftJoin(subscription).on(
            subscription.interest.eq(interest).and(subscription.user.id.eq(userId)))
        .where(
            likeNameOrKeyword(condition.keyword()),
            cursorCondition(condition)
        )
        .orderBy(
            buildOrderSpecifier(condition.orderBy(), condition.direction()),
            buildCreatedAtOrderSpecifier(condition.direction()),
            buildIdOrderSpecifier(condition.direction())
        )
        .limit(condition.limit() + 1L)
        .fetch();

    boolean hasNext = raw.size() > condition.limit();
    List<Tuple> content = hasNext ? raw.subList(0, condition.limit()) : raw;

    Map<UUID, List<String>> keywordMap = fetchKeywordMap(content);
    List<InterestResponse> responses = content.stream()
        .map(t -> toResponse(t.get(interest), t.get(subscription.id) != null, keywordMap))
        .toList();

    String nextCursor = null;
    Instant nextAfter = null;
    UUID nextIdAfter = null;
    if (hasNext && !content.isEmpty()) {
      Interest last = content.get(content.size() - 1).get(interest);
      nextCursor = extractCursor(last, condition.orderBy());
      nextAfter = last.getCreatedAt();
      nextIdAfter = last.getId();
    }

    return CursorPageResponse.of(
        responses,
        nextCursor,
        nextAfter,
        nextIdAfter,
        hasNext,
        content.size(),
        null
    );
  }

  private BooleanExpression likeNameOrKeyword(String keyword) {
    return !StringUtils.hasText(keyword) ? null
        : likeName(keyword).or(likeKeyword(keyword));
  }

  private BooleanExpression likeName(String keyword) {
    return interest.name.containsIgnoreCase(keyword);
  }

  private BooleanExpression likeKeyword(String keyword) {
    return JPAExpressions.selectOne()
        .from(interestKeyword)
        .where(
            interestKeyword.interest.eq(interest),
            interestKeyword.keyword.containsIgnoreCase(keyword)
        )
        .exists();
  }

  private BooleanExpression cursorCondition(InterestQueryCondition condition) {
    String cursor = condition.cursor();
    Instant after = condition.after();
    UUID idAfter = condition.idAfter();
    boolean isAsc = condition.direction() == SortDirection.ASC;
    if (cursor == null) {
      return null;
    }
    return switch (condition.orderBy()) {
      case NAME -> buildCursorExpression(interest.name, cursor, after, idAfter, isAsc);
      case SUBSCRIBER_COUNT ->
          buildCursorExpression(interest.subscriberCount, Long.parseLong(cursor), after, idAfter, isAsc);
    };
  }

  private BooleanExpression buildCursorExpression(
      ComparableExpression<String> field, String cursorValue, Instant after, UUID idAfter,
      boolean isAsc) {
    return isAsc
        ? field.gt(cursorValue)
            .or(field.eq(cursorValue).and(interest.createdAt.gt(after)))
            .or(field.eq(cursorValue).and(interest.createdAt.eq(after)).and(interest.id.gt(idAfter)))
        : field.lt(cursorValue)
            .or(field.eq(cursorValue).and(interest.createdAt.lt(after)))
            .or(field.eq(cursorValue).and(interest.createdAt.eq(after)).and(interest.id.lt(idAfter)));
  }

  private BooleanExpression buildCursorExpression(
      NumberExpression<Long> field, Long cursorValue, Instant after, UUID idAfter, boolean isAsc) {
    return isAsc
        ? field.gt(cursorValue)
            .or(field.eq(cursorValue).and(interest.createdAt.gt(after)))
            .or(field.eq(cursorValue).and(interest.createdAt.eq(after)).and(interest.id.gt(idAfter)))
        : field.lt(cursorValue)
            .or(field.eq(cursorValue).and(interest.createdAt.lt(after)))
            .or(field.eq(cursorValue).and(interest.createdAt.eq(after)).and(interest.id.lt(idAfter)));
  }

  private OrderSpecifier<?> buildOrderSpecifier(InterestOrderBy orderBy, SortDirection direction) {
    Order dir = direction == SortDirection.ASC ? Order.ASC : Order.DESC;
    return switch (orderBy) {
      case NAME -> new OrderSpecifier<>(dir, interest.name);
      case SUBSCRIBER_COUNT -> new OrderSpecifier<>(dir, interest.subscriberCount);
    };
  }

  private OrderSpecifier<?> buildCreatedAtOrderSpecifier(SortDirection direction) {
    Order dir = direction == SortDirection.ASC ? Order.ASC : Order.DESC;
    return new OrderSpecifier<>(dir, interest.createdAt);
  }

  private OrderSpecifier<?> buildIdOrderSpecifier(SortDirection direction) {
    Order dir = direction == SortDirection.ASC ? Order.ASC : Order.DESC;
    return new OrderSpecifier<>(dir, interest.id);
  }

  private String extractCursor(Interest i, InterestOrderBy orderBy) {
    return switch (orderBy) {
      case NAME -> i.getName();
      case SUBSCRIBER_COUNT -> String.valueOf(i.getSubscriberCount());
    };
  }

  private Map<UUID, List<String>> fetchKeywordMap(List<Tuple> content) {
    List<UUID> ids = content.stream()
        .map(t -> t.get(interest).getId())
        .toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return queryFactory
        .select(interestKeyword.interest.id, interestKeyword.keyword)
        .from(interestKeyword)
        .where(interestKeyword.interest.id.in(ids))
        .fetch()
        .stream()
        .collect(Collectors.groupingBy(
            t -> t.get(interestKeyword.interest.id),
            Collectors.mapping(t -> t.get(interestKeyword.keyword), Collectors.toList())
        ));
  }

  private InterestResponse toResponse(Interest i, boolean subscribedByMe,
      Map<UUID, List<String>> keywordMap) {
    return new InterestResponse(
        i.getId(),
        i.getName(),
        keywordMap.getOrDefault(i.getId(), List.of()),
        i.getSubscriberCount(),
        subscribedByMe
    );
  }
}