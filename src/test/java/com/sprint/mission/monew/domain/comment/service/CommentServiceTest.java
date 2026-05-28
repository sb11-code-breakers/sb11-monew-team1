package com.sprint.mission.monew.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.mapper.CommentMapper;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

  @InjectMocks
  private CommentService commentService;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CommentMapper commentMapper;

  private UUID articleId;
  private UUID userId;
  private CommentCreateRequest request;

  @BeforeEach
  void setUp() {
    articleId = UUID.randomUUID();
    userId = UUID.randomUUID();
    String content = "댓글 내용";
    request = new CommentCreateRequest(articleId, userId, content);
  }

  @Nested
  @DisplayName("댓글 등록하기")
  class 댓글_등록하기 {

    @Test
    @DisplayName("댓글 등록 실패 - 뉴스 기사가 존재하지 않음")
    void 댓글_등록_실패_뉴스기사_없음() {
      // given
      given(articleRepository.findById(articleId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.create(request)).isInstanceOf(
          ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 등록 실패 - 사용자가 존재하지 않음")
    void 댓글_등록_실패_사용자_없음() {
      // given
      Article article = Article.create(
          ArticleSource.NAVER,
          "https://example.com",
          "제목",
          Instant.now(),
          null
      );

      given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.create(request)).isInstanceOf(
          UserNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 등록_성공")
    void 댓글_등록_성공() {
      // given
      Article article = Article.create(
          ArticleSource.NAVER,
          "https://example.com/news/1",
          "테스트 기사 제목",
          Instant.parse("2024-01-01T00:00:00Z"),
          "기사 요약 내용"
      );

      User user = User.create(
          "Test@naver.com", "test", "12345678"
      );

      CommentResponse expectedResponse = new CommentResponse(
          UUID.randomUUID(),
          articleId,
          userId,
          "닉네임",
          "댓글 내용",
          0,
          false,
          Instant.now()
      );

      given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(commentRepository.save(any(Comment.class)))
          .willAnswer(invocation -> invocation.getArgument(0));
      given(commentMapper.toResponse(any(Comment.class), eq(false))).willReturn(expectedResponse);

      // when
      CommentResponse response = commentService.create(request);

      // then
      assertThat(response).isEqualTo(expectedResponse);

      verify(articleRepository).findById(articleId);
      verify(userRepository).findById(userId);
      verify(commentRepository).save(any(Comment.class));
      verify(commentMapper).toResponse(any(Comment.class), eq(false));
    }
  }
}
