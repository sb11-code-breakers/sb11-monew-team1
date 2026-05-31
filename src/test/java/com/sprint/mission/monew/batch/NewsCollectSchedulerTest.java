package com.sprint.mission.monew.batch;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsCollectSchedulerTest {

  @InjectMocks NewsCollectScheduler newsCollectScheduler;
  @Mock NewsCollectService newsCollectService;

  @Nested
  @DisplayName("뉴스 수집 스케줄러")
  class Collect {

    @Test
    @DisplayName("collect 호출 시 NewsCollectService에 위임한다")
    void collect_호출_시_서비스에_위임한다() {
      // when
      newsCollectScheduler.collect();

      // then
      verify(newsCollectService).collect();
    }
  }
}
