package com.sprint.mission.monew.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.request.CommentUpdateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.exception.CommentAccessDeniedException;
import com.sprint.mission.monew.domain.comment.exception.CommentNotFoundException;
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
  private UUID commentId;
  private Article article;
  private User user;
  private String content;
  private CommentCreateRequest createRequest;
  private CommentUpdateRequest updateRequest;

  @BeforeEach
  void setUp() {
    article = Article.create(
        ArticleSource.NAVER,
        "https://example.com/news/1",
        "테스트 기사 제목",
        Instant.parse("2024-01-01T00:00:00Z"),
        "기사 요약 내용"
    );
    user = User.create("Test@naver.com", "test", "12345678");
    content = "댓글 내용";
    articleId = article.getId();
    userId = user.getId();
    commentId = UUID.randomUUID();
    createRequest = new CommentCreateRequest(articleId, userId, content);
    updateRequest = new CommentUpdateRequest("수정한 댓글 내용");
  }

  @Nested
  @DisplayName("댓글 등록하기")
  class Service_Comment_Create {

    @Test
    @DisplayName("댓글 등록 실패 - 뉴스 기사가 존재하지 않음")
    void 댓글_등록_실패_뉴스기사_없음() {
      // given
      // user은 BeforeEach에서 초기화

      given(articleRepository.findById(articleId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.create(createRequest)).isInstanceOf(
          ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 등록 실패 - 사용자가 존재하지 않음")
    void 댓글_등록_실패_사용자_없음() {
      // given
      // article은 BeforeEach에서 초기화

      given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.create(createRequest)).isInstanceOf(
          UserNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 등록_성공")
    void 댓글_등록_성공() {
      // given
      // article, user은 BeforeEach에서 초기화

      CommentResponse expectedResponse = new CommentResponse(
          commentId,
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
      CommentResponse response = commentService.create(createRequest);

      // then
      assertThat(response).isEqualTo(expectedResponse);

      verify(articleRepository).findById(articleId);
      verify(userRepository).findById(userId);
      verify(commentRepository).save(any(Comment.class));
      verify(commentMapper).toResponse(any(Comment.class), eq(false));
    }
  }

  @Nested
  @DisplayName("댓글 수정하기")
  class Service_Comment_Update {

    @Test
    @DisplayName("댓글 수정 실패 - 댓글이 존재하지 않음")
    void 댓글_수정_실패_댓글_없음() {
      // given
      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> commentService.update(commentId, userId, updateRequest)).isInstanceOf(
          CommentNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 권한 없음")
    void 댓글_수정_실패_권한_없음() {
      // given
      User otherUser = User.create("test2@naver.com", "test2", "12345678");
      Comment comment = Comment.create(article, otherUser, content);

      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

      // when & then
      assertThatThrownBy(
          () -> commentService.update(commentId, userId, updateRequest)).isInstanceOf(
          CommentAccessDeniedException.class);
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void 댓글_수정_성공() {
      // given
      // article, user, content, commentId, updateRequest를 BeforeEach에서 초기화
      Comment comment = Comment.create(article, user, content);

      CommentResponse expectedResponse = new CommentResponse(
          commentId,
          articleId,
          userId,
          "닉네임",
          "수정한 댓글 내용",
          0,
          false,
          Instant.now()
      );

      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(commentMapper.toResponse(eq(comment), eq(false))).willReturn(expectedResponse);

      // when
      CommentResponse response = commentService.update(commentId, user.getId(), updateRequest);

      // then
      assertThat(response).isNotNull();
      assertThat(response.content()).isEqualTo("수정한 댓글 내용"); // Response DTO 검증
      assertThat(comment.getContent()).isEqualTo(updateRequest.content()); // Entity 검증
      verify(commentMapper).toResponse(eq(comment), eq(false)); // Mapper 검증
    }
  }

  @Nested
  @DisplayName("댓글 논리 삭제하기")
  class Service_Comment_SoftDelete {

    @Test
    @DisplayName("댓글 논리삭제 실패 - 댓글이 존재하지 않음")
    void 댓글_논리삭제_실패_댓글_없음() {
      // given
      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.softDelete(commentId, userId)).isInstanceOf(
          CommentNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 논리삭제 실패 - 권한 없음")
    void 댓글_논리삭제_실패_권한_없음() {
      // given
      User otherUser = User.create("test2@naver.com", "test2", "12345678");
      Comment comment = Comment.create(article, otherUser, content);

      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

      // when & then
      assertThatThrownBy(
          () -> commentService.softDelete(commentId, userId)).isInstanceOf(
          CommentAccessDeniedException.class);
    }


    @Test
    @DisplayName("댓글 논리삭제 성공")
    void 댓글_논리삭제_성공() {
      // given
      Comment comment = Comment.create(article, user, content);
      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

      // when
      commentService.softDelete(commentId, userId);

      // then
      assertThat(comment.isDeleted()).isTrue();
    }
  }
}
