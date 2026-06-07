package com.sprint.mission.monew.batch.writer;

import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.dto.UserCleanupItem;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
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
public class UserCleanupWriterTest {

  @InjectMocks
  UserCleanupWriter writer;

  @Mock
  UserRepository userRepository;

  @Nested
  @DisplayName("사용자 삭제하기")
  class Writer {

    @Test
    @DisplayName("chunk의 id만 추출해서 batch delete 한다")
    void write_batch_delete() {

      // given
      UserCleanupItem item1 = new UserCleanupItem(UUID.randomUUID(), Instant.now());
      UserCleanupItem item2 = new UserCleanupItem(UUID.randomUUID(), Instant.now());

      Chunk<UserCleanupItem> chunk =
          new Chunk<>(List.of(item1, item2));

      // when
      writer.write(chunk);

      // then
      verify(userRepository).deleteAllByIdInBatch(
          List.of(item1.id(), item2.id())
      );
    }
  }
}