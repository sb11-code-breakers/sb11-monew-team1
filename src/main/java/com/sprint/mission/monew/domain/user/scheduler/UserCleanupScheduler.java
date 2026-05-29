package com.sprint.mission.monew.domain.user.scheduler;

import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserCleanupScheduler {

  private final UserRepository userRepository;

  @Transactional
  @Scheduled(cron = "${scheduler.user-cleanup.cron}")
  public void cleanUpDeletedUsers() {
    Instant threshold = Instant.now().minus(1, ChronoUnit.DAYS);
    log.info("물리 삭제 스케줄러 실행: threshold={}", threshold);

    userRepository.deleteAllByDeletedAtBefore(threshold);

    log.info("물리 삭제 완료");
  }
}