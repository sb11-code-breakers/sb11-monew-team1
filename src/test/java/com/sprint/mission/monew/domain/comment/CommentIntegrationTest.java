package com.sprint.mission.monew.domain.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.mapper.CommentMapper;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.comment.service.CommentService;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class CommentIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CommentService commentService;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  private Article article;
  private User user;
  private String content;

  @BeforeEach
  void setUp() {
    article = articleRepository.save(
        Article.create(ArticleSource.NAVER, "https://test.com", "테스트 기사", Instant.now(), null));
    user = userRepository.save(new User());
    content = "댓글 내용";
  }

  @Nested
  @DisplayName("댓글 등록하기")
  class CreateComment {

    @Test
    @DisplayName("댓글 등록 실패 - 뉴스 기사가 존재하지 않음")
    void 댓글_등록_실패_뉴스기사_없음() {
      // given
      CommentCreateRequest request = new CommentCreateRequest(UUID.randomUUID(), user.getId(),
          content);

      // when & then
      assertThatThrownBy(
          () -> commentService.create(request)
      ).isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 등록 실패 - 사용자가 존재하지 않음")
    void 댓글_등록_실패_사용자_없음() {
      // given
      CommentCreateRequest request = new CommentCreateRequest(article.getId(), UUID.randomUUID(),
          content);

      // when & then
      assertThatThrownBy(
          () -> commentService.create(request)
      ).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 등록 성공")
    void 댓글_등록_성공() {
      // given
      CommentCreateRequest request = new CommentCreateRequest(article.getId(), user.getId(),
          content);

      // when
      CommentResponse response = commentService.create(request);
      Comment savedComment = commentRepository.findById(response.id()).orElseThrow();

      // then
      assertThat(response).isNotNull();
      assertThat(response.content()).isEqualTo(content);

      assertThat(savedComment).isNotNull();
      assertThat(savedComment.getContent()).isEqualTo(content);
    }
  }
}
