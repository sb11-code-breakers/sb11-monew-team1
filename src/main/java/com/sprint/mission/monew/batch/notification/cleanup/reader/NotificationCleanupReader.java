package com.sprint.mission.monew.batch.notification.cleanup.reader;

import com.sprint.mission.monew.batch.notification.cleanup.dto.NotificationCleanupItem;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class NotificationCleanupReader implements ItemReader<NotificationCleanupItem> {

  private final NotificationRepository notificationRepository;

  @Value("${batch.notification-cleanup.chunk-size}")
  private int chunkSize;

  private Instant cutoff;
  private Instant lastConfirmedAt;
  private UUID lastId;

  private Iterator<NotificationCleanupItem> iterator;

  @Override
  public NotificationCleanupItem read() {

    if (cutoff == null) {
      cutoff = Instant.now().minus(Duration.ofDays(7));
      lastConfirmedAt = Instant.EPOCH;
      lastId = new UUID(0L, 0L);

      log.info("Notification Cleanup Reader 시작: threshold={}, chunkSize={}", cutoff, chunkSize);
    }

    while (iterator == null || !iterator.hasNext()) {

      List<NotificationCleanupItem> items =
          notificationRepository.findNotificationsForCleanup(
              cutoff,
              lastConfirmedAt,
              lastId,
              PageRequest.of(0, chunkSize)
          );

      if (items.isEmpty()) {
        return null;
      }

      iterator = items.iterator();
      log.info("Notification Cleanup Reader chunk load 완료: size={}", items.size());
    }

    NotificationCleanupItem item = iterator.next();

    lastConfirmedAt = item.confirmedAt();
    lastId = item.id();

    return item;
  }

}
