package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
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
public class CommentCleanupJobIntegrationTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job commentCleanupJob;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    commentRepository.deleteAll();
    articleRepository.deleteAll();
    userRepository.deleteAll();

    Article article = articleRepository.save(
        Article.create(
            ArticleSource.NAVER,
            "https://example.com/news/1",
            "테스트 기사 제목",
            Instant.parse("2024-01-01T00:00:00Z"),
            "기사 요약 내용"
        ));
    User user = userRepository.save(
        User.create(
            "Test@naver.com", "test", "12345678"
        ));

    Comment comment1 = Comment.create(article, user, "첫 번째 댓글");
    Comment comment2 = Comment.create(article, user, "두 번째 댓글");

    ReflectionTestUtils.setField(comment1, "deletedAt", Instant.now().minusSeconds(86400));
    ReflectionTestUtils.setField(comment2, "deletedAt", Instant.now().plusSeconds(10));

    commentRepository.save(comment1);
    commentRepository.save(comment2);
  }

  @Nested
  @DisplayName("댓글 삭제 배치 통합 테스트하기")
  class CommentCleanupIntegrationTest {

    @Test
    @DisplayName("댓글 삭제 배치 통합 테스트")
    void 댓글_삭제_배치_통합테스트_성공() throws Exception {
      // given
      // BeforeEach에서 comment1, comment2 생성 후 논리삭제 세팅, 저장
      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();

      // when
      JobExecution execution = jobLauncher.run(commentCleanupJob, params);

      // then
      List<Comment> comments = commentRepository.findAll();

      // comment1만 삭제됨
      assertThat(comments).hasSize(1); // comment2
      assertThat(comments.get(0).getContent()).isEqualTo("두 번째 댓글");

      assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
  }

}
