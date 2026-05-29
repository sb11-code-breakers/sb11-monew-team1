package com.sprint.mission.monew.domain.notification.mapper;

import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(target = "updatedAt", source = "confirmedAt")
  NotificationResponse toResponse(Notification notification);

}