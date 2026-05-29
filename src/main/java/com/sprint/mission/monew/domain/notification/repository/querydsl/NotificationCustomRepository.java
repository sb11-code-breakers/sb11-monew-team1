package com.sprint.mission.monew.domain.notification.repository.querydsl;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import java.util.UUID;

public interface NotificationCustomRepository {

  CursorPageResponse<NotificationResponse> findUnconfirmed(UUID userId,
      NotificationQueryCondition condition);

}