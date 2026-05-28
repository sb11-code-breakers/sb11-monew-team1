package com.sprint.mission.monew.domain.interest.mapper;

import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = Collectors.class)
public interface InterestMapper {

  @Mapping(target = "subscribedByMe", constant = "false")
  @Mapping(target = "keywords", expression = "java(interest.getKeywords().stream().map(k -> k.getKeyword()).collect(Collectors.toList()))")
  InterestResponse toResponse(Interest interest);
}
