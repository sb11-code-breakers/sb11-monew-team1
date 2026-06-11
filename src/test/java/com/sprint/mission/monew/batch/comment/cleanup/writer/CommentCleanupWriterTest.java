package com.sprint.mission.monew.batch.comment.cleanup.writer;

import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.comment.cleanup.dto.CommentCleanupItem;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
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
public class CommentCleanupWriterTest {

  @InjectMocks
  CommentCleanupWriter writer;

  @Mock
  CommentRepository commentRepository;

  @Nested
  @DisplayName("댓글 삭제하기")
  class Writer {

    @Test
    @DisplayName("chunk의 id만 추출해서 batch delete한다")
    void chunk의_id만_추출하여_삭제() {
      // given
      CommentCleanupItem item1 = new CommentCleanupItem(UUID.randomUUID(), Instant.now());
      CommentCleanupItem item2 = new CommentCleanupItem(UUID.randomUUID(), Instant.now());

      Chunk<CommentCleanupItem> chunk = new Chunk<>(List.of(item1, item2));

      // when
      writer.write(chunk);

      // then
      verify(commentRepository).deleteAllByIdInBatch(List.of(item1.id(), item2.id()));
    }
  }
}
