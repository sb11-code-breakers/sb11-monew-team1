package com.sprint.mission.monew.domain.useractivity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.user.document.UserSession;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(MongoContainerConfig.class)
public class UserActivityIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private UserSessionRepository userSessionRepository;
  @Autowired private UserActivityMongoRepository userActivityMongoRepository;

  private User user;
  private Article article;
  private UUID sessionToken;
  private UUID ghostSessionToken;
  private UUID ghostUserId;

  @BeforeEach
  void setUp() {
    user = userRepository.save(User.create("test@test.com", "테스터", "password123!"));
    article = articleRepository.save(
        Article.create(ArticleSource.NAVER, "https://test.com/news/1", "테스트 기사",
            Instant.now(), "테스트 요약")
    );

    userActivityMongoRepository.save(
        UserActivity.of(user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt())
    );

    UserSession session = UserSession.create(user.getId(), "127.0.0.1",
        "1acaf8f7bdf7054e8279b8a17955fc66", 30);
    userSessionRepository.save(session);
    sessionToken = session.getId();

    ghostUserId = UUID.randomUUID();
    UserSession ghostSession = UserSession.create(ghostUserId, "127.0.0.1",
        "1acaf8f7bdf7054e8279b8a17955fc66", 30);
    userSessionRepository.save(ghostSession);
    ghostSessionToken = ghostSession.getId();
  }

  @AfterEach
  void tearDown() {
    userActivityMongoRepository.deleteById(user.getId());
  }

  @Nested
  @DisplayName("GET /api/user-activities/{userId} — 활동 내역 조회")
  class GetUserActivity {

    @Test
    @DisplayName("존재하지 않는 userId면 404와 에러 응답을 반환한다")
    void 존재하지_않는_userId면_404와_에러_응답을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", ghostUserId)
              .header("Monew-Request-User-ID", ghostSessionToken))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("성공 시 200과 활동 내역을 반환한다")
    void 성공_시_200과_활동_내역을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", user.getId())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(user.getId().toString()))
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.nickname").value("테스터"))
          .andExpect(jsonPath("$.subscriptions").isArray())
          .andExpect(jsonPath("$.comments").isArray())
          .andExpect(jsonPath("$.commentLikes").isArray())
          .andExpect(jsonPath("$.articleViews").isArray());
    }

    @Test
    @DisplayName("구독 관심사가 있으면 응답에 포함된다")
    void 구독_관심사가_있으면_응답에_포함된다() throws Exception {
      // given
      userActivityMongoRepository.pushSubscription(user.getId(),
          RecentSubscription.of(UUID.randomUUID(), UUID.randomUUID(), "인공지능",
              List.of("AI"), 1L, Instant.now())
      );

      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", user.getId())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.subscriptions.length()").value(1))
          .andExpect(jsonPath("$.subscriptions[0].interestName").value("인공지능"));
    }

    @Test
    @DisplayName("최근 작성한 댓글이 있으면 응답에 포함된다")
    void 최근_작성한_댓글이_있으면_응답에_포함된다() throws Exception {
      // given
      userActivityMongoRepository.pushComment(user.getId(),
          RecentComment.of(UUID.randomUUID(), article.getId(), article.getTitle(),
              user.getId(), user.getNickname(), "테스트 댓글", 0L, Instant.now())
      );

      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", user.getId())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.comments.length()").value(1))
          .andExpect(jsonPath("$.comments[0].content").value("테스트 댓글"));
    }

    @Test
    @DisplayName("최근 좋아요한 댓글이 있으면 응답에 포함된다")
    void 최근_좋아요한_댓글이_있으면_응답에_포함된다() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      userActivityMongoRepository.pushCommentLike(user.getId(),
          RecentCommentLike.of(UUID.randomUUID(), Instant.now(),
              commentId, article.getId(), article.getTitle(),
              user.getId(), user.getNickname(), "테스트 댓글",
              1L, Instant.now().minusSeconds(60))
      );

      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", user.getId())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.commentLikes.length()").value(1))
          .andExpect(jsonPath("$.commentLikes[0].commentId").value(commentId.toString()));
    }

    @Test
    @DisplayName("최근 본 기사가 있으면 응답에 포함된다")
    void 최근_본_기사가_있으면_응답에_포함된다() throws Exception {
      // given
      userActivityMongoRepository.pushArticleView(user.getId(),
          RecentArticleView.of(UUID.randomUUID(), user.getId(), Instant.now(),
              article.getId(), article.getSource().name(), article.getSourceUrl(),
              article.getTitle(), article.getPublishDate(), article.getSummary(),
              0L, 1L)
      );

      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", user.getId())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.articleViews.length()").value(1))
          .andExpect(jsonPath("$.articleViews[0].articleTitle").value("테스트 기사"));
    }
  }
}
