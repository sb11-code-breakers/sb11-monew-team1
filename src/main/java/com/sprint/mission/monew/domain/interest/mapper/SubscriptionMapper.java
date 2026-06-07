package com.sprint.mission.monew.domain.interest.mapper;

import com.sprint.mission.monew.domain.interest.dto.SubscriptionResponse;
import com.sprint.mission.monew.domain.interest.entity.InterestKeyword;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = InterestKeyword.class)
public interface SubscriptionMapper {

  @Mapping(target = "id", source = "subscription.id")
  @Mapping(target = "createdAt", source = "subscription.createdAt")
  @Mapping(target = "interestId", source = "subscription.interest.id")
  @Mapping(target = "interestName", source = "subscription.interest.name")
  @Mapping(target = "interestKeywords",
      expression = "java(subscription.getInterest().getKeywords().stream().map(InterestKeyword::getKeyword).toList())")
  @Mapping(target = "interestSubscriberCount", source = "subscriberCount")
  SubscriptionResponse toResponse(Subscription subscription, long subscriberCount);
}