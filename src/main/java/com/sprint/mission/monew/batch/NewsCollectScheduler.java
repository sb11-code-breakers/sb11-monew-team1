package com.sprint.mission.monew.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectScheduler {

  private final NewsCollectService newsCollectService;

  @Scheduled(cron = "0 0 * * * *")
  public void collect() {
    log.info("뉴스 수집 배치 시작");
    newsCollectService.collect();
    log.info("뉴스 수집 배치 완료");
  }
}
