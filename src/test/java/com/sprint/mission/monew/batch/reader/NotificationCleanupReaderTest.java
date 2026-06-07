package com.sprint.mission.monew.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.monew.batch.dto.NotificationCleanupItem;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class NotificationCleanupReaderTest {

  @InjectMocks
  NotificationCleanupReader reader;

  @Mock
  NotificationRepository repository;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reader, "chunkSize", 1000);
  }

  @Nested
  @DisplayName("알림 삭제 목록 읽기")
  class Reader {

    @Test
    @DisplayName("cursor 기반으로 chunk 단위로 데이터를 순차 조회한다")
    void read_cursor_chunk_flow() {

      NotificationCleanupItem item1 =
          new NotificationCleanupItem(UUID.randomUUID(), Instant.now());

      NotificationCleanupItem item2 =
          new NotificationCleanupItem(UUID.randomUUID(), Instant.now());

      given(repository.findNotificationsForCleanup(any(), any(), any(), any()))
          .willReturn(List.of(item1, item2), List.of());

      NotificationCleanupItem r1 = reader.read();
      NotificationCleanupItem r2 = reader.read();
      NotificationCleanupItem r3 = reader.read();

      assertThat(r1.id()).isEqualTo(item1.id());
      assertThat(r2.id()).isEqualTo(item2.id());
      assertThat(r3).isNull();
    }
  }
}