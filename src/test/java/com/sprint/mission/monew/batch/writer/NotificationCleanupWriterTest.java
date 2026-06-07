package com.sprint.mission.monew.batch.writer;

import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.dto.NotificationCleanupItem;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
public class NotificationCleanupWriterTest {

  @Mock
  NotificationRepository repository;

  @InjectMocks
  NotificationCleanupWriter writer;

  @Nested
  @DisplayName("알림 삭제하기")
  class Writer {

    @Test
    @DisplayName("chunk의 id만 추출해서 batch delete 한다")
    void write_batch_delete() {
      // given
      NotificationCleanupItem i1 =
          new NotificationCleanupItem(UUID.randomUUID(), Instant.now());

      NotificationCleanupItem i2 =
          new NotificationCleanupItem(UUID.randomUUID(), Instant.now());

      Chunk<NotificationCleanupItem> chunk = new Chunk<>(List.of(i1, i2));

      // when
      writer.write(chunk);

      // then
      verify(repository).deleteAllByIdInBatch(List.of(i1.id(), i2.id()));
    }
  }
}