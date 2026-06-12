package com.sprint.mission.monew.batch.user.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
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
public class UserCleanupJobIntegrationTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job userCleanupJob;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    User user1 = User.create("test@naver.com", "test", "12345678");
    User user2 = User.create("test2@naver.com", "test2", "12345678");

    ReflectionTestUtils.setField(user1, "deletedAt", Instant.now().minusSeconds(86400));
    ReflectionTestUtils.setField(user2, "deletedAt", Instant.now().plusSeconds(10));

    userRepository.save(user1);
    userRepository.save(user2);
  }

  @Nested
  @DisplayName("사용자 삭제 배치 통합 테스트하기")
  class UserDeleteBatchIntegrationTest {

    @Test
    @DisplayName("사용자 삭제 배치 통합테스트")
    void 사용자_삭제_배치_통합테스트_성공() throws Exception {
      // given
      // BeforeEach에서 user1, user2 생성 후 논리삭제 세팅, 저장
      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();

      // when
      JobExecution execution = jobLauncher.run(userCleanupJob, params);

      // then
      List<User> users = userRepository.findAll();

      // user1만 삭제됨
      assertThat(users).hasSize(1); // user2
      assertThat(users.get(0).getNickname()).isEqualTo("test2");

      assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
  }

}
