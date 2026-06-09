package com.sprint.mission.monew.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.monew.batch.dto.CommentCleanupItem;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
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
public class CommentCleanupReaderTest {

  @InjectMocks
  CommentCleanupReader reader;

  @Mock
  CommentRepository commentRepository;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reader, "chunkSize", 1000);
  }

  @Nested
  @DisplayName("댓글 삭제 목록 읽기")
  class Reader {

    @Test
    @DisplayName("cursor 기반 chunk 단위로 데이터를 순차 조회한다")
    void chunk_단위_cursor_기반으로_조회() {
      // given
      Instant now = Instant.now();

      CommentCleanupItem item1 = new CommentCleanupItem(UUID.randomUUID(), now.minusSeconds(10));
      CommentCleanupItem item2 = new CommentCleanupItem(UUID.randomUUID(), now.minusSeconds(5));

      given(commentRepository.findCommentsForCleanup(any(), any(), any(), any()))
          .willReturn(List.of(item1, item2), List.of());

      // when
      CommentCleanupItem r1 = reader.read();
      CommentCleanupItem r2 = reader.read();
      CommentCleanupItem r3 = reader.read();

      // then
      assertThat(r1).isNotNull();
      assertThat(r2).isNotNull();
      assertThat(r1.id()).isEqualTo(item1.id());
      assertThat(r2.id()).isEqualTo(item2.id());
      assertThat(r3).isNull();
    }
  }
}
