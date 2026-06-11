package com.sprint.mission.monew.batch.notification.cleanup.writer;

import com.sprint.mission.monew.batch.notification.cleanup.dto.NotificationCleanupItem;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupWriter implements ItemWriter<NotificationCleanupItem> {

  private final NotificationRepository notificationRepository;

  @Override
  public void write(Chunk<? extends NotificationCleanupItem> chunk) {

    List<UUID> ids = chunk.getItems()
        .stream()
        .map(NotificationCleanupItem::id)
        .toList();

    log.info("Notification Cleanup Writer 실행: delete size={}", ids.size());

    notificationRepository.deleteAllByIdInBatch(ids);

    log.info("Notification Cleanup Writer 완료");
  }

}
