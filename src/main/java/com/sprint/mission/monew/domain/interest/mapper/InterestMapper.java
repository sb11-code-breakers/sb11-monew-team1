package com.sprint.mission.monew.domain.interest.mapper;

import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.entity.InterestKeyword;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = InterestKeyword.class)
public interface InterestMapper {

  @Mapping(target = "subscribedByMe", constant = "false")
  @Mapping(target = "keywords", expression = "java(interest.getKeywords().stream().map(InterestKeyword::getKeyword).toList())")
  InterestResponse toResponse(Interest interest);
}
