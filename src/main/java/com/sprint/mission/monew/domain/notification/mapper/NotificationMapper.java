package com.sprint.mission.monew.domain.notification.mapper;

import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  NotificationResponse toResponse(Notification notification);

}