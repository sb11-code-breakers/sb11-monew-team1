package com.sprint.mission.monew.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("prod")
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectScheduler {

  private final NewsCollectService newsCollectService;

  @Scheduled(cron = "${scheduler.news-collect.cron}")
  public void collect() {
    log.info("뉴스 수집 배치 시작");
    newsCollectService.collect();
    log.info("뉴스 수집 배치 완료");
  }
}
