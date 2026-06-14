package com.sprint.mission.monew.batch.notification.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationCleanupJobIntegrationTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job notificationCleanupJob;

  @Autowired
  private NotificationRepository notificationRepository;

  private UUID userId;
  private ResourceType resourceType;
  private UUID resourceId;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();

    userId = UUID.randomUUID();
    resourceType = ResourceType.ARTICLE;
    resourceId = UUID.randomUUID();
    Notification notification1 = Notification.create(userId, "첫 번째 알림", resourceType, resourceId);
    Notification notification2 = Notification.create(userId, "두 번째 알림", resourceType, resourceId);
    Notification notification3 = Notification.create(userId, "세 번째 알림", resourceType, resourceId);

    ReflectionTestUtils.setField(
        notification1, "confirmedAt", Instant.now().minus(Duration.ofDays(8)));
    ReflectionTestUtils.setField(
        notification2, "confirmedAt", Instant.now().minus(Duration.ofDays(6)));
    ReflectionTestUtils.setField(
        notification3, "confirmedAt", Instant.now().minus(Duration.ofDays(1)));

    notificationRepository.save(notification1);
    notificationRepository.save(notification2);
    notificationRepository.save(notification3);
  }

  @Nested
  @DisplayName("알림 삭제 배치 통합 테스트하기")
  class NotificationDeleteBatchIntegrationTest {

    @Test
    @DisplayName("알림 삭제 배치 통합테스트")
    void 알림_삭제_배치_통합테스트_성공() throws Exception {
      // given
      // BeforeEach에서 notification1, 2, 3 생성 후 논리삭제 세팅, 저장
      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();

      // when
      JobExecution execution = jobLauncher.run(notificationCleanupJob, params);

      // then
      List<Notification> notifications = notificationRepository.findAll();

      // notification1만 삭제됨
      assertThat(notifications).hasSize(2); // notification2, 3
      assertThat(notifications.get(0).getContent()).isEqualTo("두 번째 알림");
      assertThat(notifications.get(1).getContent()).isEqualTo("세 번째 알림");

      assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
  }

}
