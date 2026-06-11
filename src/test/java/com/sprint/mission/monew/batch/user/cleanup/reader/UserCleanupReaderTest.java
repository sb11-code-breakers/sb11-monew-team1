package com.sprint.mission.monew.batch.user.cleanup.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.monew.batch.user.cleanup.dto.UserCleanupItem;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
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
public class UserCleanupReaderTest {

  @InjectMocks
  UserCleanupReader reader;

  @Mock
  UserRepository userRepository;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reader, "chunkSize", 1000);
  }

  @Nested
  @DisplayName("사용자 삭제 목록 읽기")
  class Reader {

    @Test
    @DisplayName("cursor 기반으로 chunk 단위로 데이터를 순차 조회한다")
    void chunk_단위_cursor_기반으로_조회() {
      // given
      Instant now = Instant.now();

      UserCleanupItem item1 = new UserCleanupItem(UUID.randomUUID(), now.minusSeconds(10));
      UserCleanupItem item2 = new UserCleanupItem(UUID.randomUUID(), now.minusSeconds(5));

      given(userRepository.findUsersForCleanup(any(), any(), any(), any()))
          .willReturn(List.of(item1, item2), List.of());

      // when
      UserCleanupItem r1 = reader.read();
      UserCleanupItem r2 = reader.read();
      UserCleanupItem r3 = reader.read();

      // then
      assertThat(r1).isNotNull();
      assertThat(r2).isNotNull();
      assertThat(r1.id()).isEqualTo(item1.id());
      assertThat(r2.id()).isEqualTo(item2.id());
      assertThat(r3).isNull();
    }
  }
}