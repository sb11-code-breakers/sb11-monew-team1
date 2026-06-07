package com.sprint.mission.monew.batch.scheduler;

import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.batch.service.NewsCollectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsCollectSchedulerTest {

  @Mock
  private NewsCollectService newsCollectService;

  @InjectMocks
  private NewsCollectScheduler scheduler;

  @Nested
  @DisplayName("뉴스 수집 스케줄러")
  class Collect {

    @Test
    @DisplayName("스케줄러가 Batch Job을 호출한다")
    void 스케줄러가_Batch_Job의_collect_메서드를_호출한다() throws Exception {
      // given

      // when
      scheduler.collect();

      // then
      then(newsCollectService).should().executeCollect();
    }
  }
}
