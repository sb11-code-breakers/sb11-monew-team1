package com.sprint.mission.monew.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.dto.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.CommentOrderBy;
import com.sprint.mission.monew.domain.comment.dto.CommentQueryCondition;
import com.sprint.mission.monew.domain.comment.dto.CommentUpdateRequest;
import com.sprint.mission.monew.domain.comment.dto.CommentResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.exception.CommentAccessDeniedException;
import com.sprint.mission.monew.domain.comment.exception.CommentNotFoundException;
import com.sprint.mission.monew.domain.comment.mapper.CommentMapper;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

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
  private CommentLikeRepository commentLikeRepository;

  @Mock
  private CommentMapper commentMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

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
      verify(articleRepository).increaseCommentCount(articleId);
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
          comment.getId(),
          articleId,
          userId,
          "닉네임",
          "수정한 댓글 내용",
          0,
          false,
          Instant.now()
      );

      given(commentRepository.findById(comment.getId())).willReturn(Optional.of(comment));
      given(commentMapper.toResponse(eq(comment), eq(false))).willReturn(expectedResponse);

      // when
      CommentResponse response = commentService.update(comment.getId(), user.getId(),
          updateRequest);

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
      given(commentRepository.findById(comment.getId())).willReturn(Optional.of(comment));

      // when
      commentService.softDelete(comment.getId(), userId);

      // then
      assertThat(comment.isDeleted()).isTrue();
      verify(articleRepository).decreaseCommentCount(article.getId());
    }

    @Test
    @DisplayName("이미 논리삭제되어 카운트에서 빠진 댓글은 다시 논리삭제해도 중복 차감하지 않는다")
    void 이미_논리삭제된_댓글은_다시_논리삭제해도_중복_차감하지_않는다() {
      // given
      Comment comment = Comment.create(article, user, content);
      comment.softDelete();
      given(commentRepository.findById(comment.getId())).willReturn(Optional.of(comment));

      // when
      commentService.softDelete(comment.getId(), userId);

      // then
      verify(articleRepository, never()).decreaseCommentCount(any());
    }
  }

  @Nested
  @DisplayName("댓글 물리 삭제하기")
  class Service_Comment_HardDelete {

    @Test
    @DisplayName("댓글 물리삭제 실패 - 댓글이 존재하지 않음")
    void 댓글_물리삭제_실패() {
      // given
      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.hardDelete(commentId)).isInstanceOf(
          CommentNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 물리삭제 성공")
    void 댓글_물리삭제_성공() {
      // given
      Comment comment = Comment.create(article, user, content);
      given(commentRepository.findById(comment.getId())).willReturn(Optional.of(comment));

      // when
      commentService.hardDelete(comment.getId());

      // then
      verify(commentRepository).delete(comment);
      verify(articleRepository).decreaseCommentCount(article.getId());
    }

    @Test
    @DisplayName("이미 논리삭제되어 카운트에서 빠진 댓글은 물리삭제해도 중복 차감하지 않는다")
    void 이미_논리삭제되어_카운트에서_빠진_댓글은_물리삭제해도_중복_차감하지_않는다() {
      // given
      Comment comment = Comment.create(article, user, content);
      comment.softDelete();
      given(commentRepository.findById(comment.getId())).willReturn(Optional.of(comment));

      // when
      commentService.hardDelete(comment.getId());

      // then
      verify(commentRepository).delete(comment);
      verify(articleRepository, never()).decreaseCommentCount(any());
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회하기")
  class Service_Comment_Find {

    @Test
    @DisplayName("hasNext True 테스트(임시 limit 2로 고정)")
    void hasNext_True() {
      // given
      Comment firstComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(firstComment, "createdAt", Instant.now());

      Comment secondComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(secondComment, "createdAt", Instant.now().plusSeconds(1));

      Comment thirdComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(thirdComment, "createdAt", Instant.now().plusSeconds(2));

      CommentResponse firstResponse = new CommentResponse(
          firstComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "첫 번째 댓글",
          0L,
          false,
          firstComment.getCreatedAt()
      );
      CommentResponse secondResponse = new CommentResponse(
          secondComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "두 번째 댓글",
          0L,
          false,
          secondComment.getCreatedAt()
      );
      CommentResponse thirdResponse = new CommentResponse(
          thirdComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "세 번째 댓글",
          0L,
          false,
          thirdComment.getCreatedAt()
      );

      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(firstResponse, secondResponse),
          "cursor",
          secondResponse.createdAt(),
          secondResponse.id(),
          true,
          2,
          3L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          null,
          null, null,
          2
      );

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition, userId);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.content().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("다음 페이지가 없을 때 hasNext false")
    void hasNext_False() {
      // given
      Comment firstComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(firstComment, "createdAt", Instant.now());

      Comment secondComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(secondComment, "createdAt", Instant.now().plusSeconds(1));

      Comment thirdComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(thirdComment, "createdAt", Instant.now().plusSeconds(2));

      CommentResponse firstResponse = new CommentResponse(
          firstComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "첫 번째 댓글",
          0L,
          false,
          firstComment.getCreatedAt()
      );
      CommentResponse secondResponse = new CommentResponse(
          secondComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "두 번째 댓글",
          0L,
          false,
          secondComment.getCreatedAt()
      );
      CommentResponse thirdResponse = new CommentResponse(
          thirdComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "세 번째 댓글",
          0L,
          false,
          thirdComment.getCreatedAt()
      );

      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(firstResponse, secondResponse),
          "cursor",
          secondResponse.createdAt(),
          null,
          false,
          2,
          3L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          null,
          null, null,
          5
      );

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition, userId);

      // then
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("등록순 조회 시 nextCursor 반환")
    void 등록순_nextCursor() {
      // given
      Comment firstComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(firstComment, "createdAt", Instant.now());

      Comment secondComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(secondComment, "createdAt", Instant.now().plusSeconds(1));

      Comment thirdComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(thirdComment, "createdAt", Instant.now().plusSeconds(2));

      CommentResponse firstResponse = new CommentResponse(
          firstComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "첫 번째 댓글",
          0L,
          false,
          firstComment.getCreatedAt()
      );
      CommentResponse secondResponse = new CommentResponse(
          secondComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "두 번째 댓글",
          0L,
          false,
          secondComment.getCreatedAt()
      );
      CommentResponse thirdResponse = new CommentResponse(
          thirdComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "세 번째 댓글",
          0L,
          false,
          thirdComment.getCreatedAt()
      );

      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(firstResponse, secondResponse),
          secondResponse.createdAt().toString(),
          secondResponse.createdAt(),
          secondResponse.id(),
          true,
          2,
          3L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      CommentQueryCondition condition =
          new CommentQueryCondition(
              articleId,
              CommentOrderBy.CREATED_AT,
              SortDirection.DESC,
              null,
              null, null,
              2
          );

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition, userId);

      // then
      assertThat(result.nextCursor()).isEqualTo(secondComment.getCreatedAt().toString());
    }

    @Test
    @DisplayName("등록순 조회 시 nextAfter 반환")
    void 등록순_nextAfter() {
      // given
      Comment firstComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(firstComment, "createdAt", Instant.now());
      Comment secondComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(secondComment, "createdAt", Instant.now().plusSeconds(1));
      Comment thirdComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(thirdComment, "createdAt", Instant.now().plusSeconds(2));

      CommentResponse firstResponse = new CommentResponse(
          firstComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "첫 번째 댓글",
          0L,
          false,
          firstComment.getCreatedAt()
      );
      CommentResponse secondResponse = new CommentResponse(
          secondComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "두 번째 댓글",
          0L,
          false,
          secondComment.getCreatedAt()
      );
      CommentResponse thirdResponse = new CommentResponse(
          thirdComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "세 번째 댓글",
          0L,
          false,
          thirdComment.getCreatedAt()
      );

      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(firstResponse, secondResponse),
          "cursor",
          secondResponse.createdAt(),
          secondResponse.id(),
          true,
          2,
          3L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          null,
          null, null,
          2
      );

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition, userId);

      // then
      assertThat(result.nextAfter()).isEqualTo(secondComment.getCreatedAt().toString());
    }

    @Test
    @DisplayName("좋아요 순 nextCursor 반환")
    void 좋아요순_nextCursor() {
      // given
      Comment firstComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(firstComment, "createdAt", Instant.now());
      ReflectionTestUtils.setField(firstComment, "likeCount", 2);
      Comment secondComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(secondComment, "createdAt", Instant.now().plusSeconds(1));
      ReflectionTestUtils.setField(secondComment, "likeCount", 2);
      Comment thirdComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(thirdComment, "createdAt", Instant.now().plusSeconds(2));
      ReflectionTestUtils.setField(thirdComment, "likeCount", 1);

      CommentResponse firstResponse = new CommentResponse(
          firstComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "첫 번째 댓글",
          2L,
          false,
          firstComment.getCreatedAt()
      );
      CommentResponse secondResponse = new CommentResponse(
          secondComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "두 번째 댓글",
          2L,
          false,
          secondComment.getCreatedAt()
      );
      CommentResponse thirdResponse = new CommentResponse(
          thirdComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "세 번째 댓글",
          1L,
          false,
          thirdComment.getCreatedAt()
      );

      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(firstResponse, secondResponse),
          String.valueOf(secondResponse.likeCount()),
          secondResponse.createdAt(),
          secondResponse.id(),
          true,
          2,
          3L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.LIKE_COUNT,
          SortDirection.DESC,
          null,
          null, null,
          2
      );

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition, userId);

      // then
      assertThat(result.nextCursor()).isEqualTo("2");
    }

    @Test
    @DisplayName("좋아요 순 nextAfter 반환")
    void 좋아요순_nextAfter() {
      // given
      Comment firstComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(firstComment, "createdAt", Instant.now());
      ReflectionTestUtils.setField(firstComment, "likeCount", 2);
      Comment secondComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(secondComment, "createdAt", Instant.now().plusSeconds(1));
      ReflectionTestUtils.setField(secondComment, "likeCount", 2);
      Comment thirdComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(thirdComment, "createdAt", Instant.now().plusSeconds(2));
      ReflectionTestUtils.setField(thirdComment, "likeCount", 1);

      CommentResponse firstResponse = new CommentResponse(
          firstComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "첫 번째 댓글",
          0L,
          false,
          firstComment.getCreatedAt()
      );
      CommentResponse secondResponse = new CommentResponse(
          secondComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "두 번째 댓글",
          0L,
          false,
          secondComment.getCreatedAt()
      );
      CommentResponse thirdResponse = new CommentResponse(
          thirdComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "세 번째 댓글",
          0L,
          false,
          thirdComment.getCreatedAt()
      );

      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(firstResponse, secondResponse),
          "cursor",
          secondResponse.createdAt(),
          secondResponse.id(),
          true,
          2,
          3L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.LIKE_COUNT,
          SortDirection.DESC,
          null,
          null, null,
          2
      );

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition, userId);

      // then
      assertThat(result.nextAfter()).isEqualTo(secondComment.getCreatedAt().toString());
    }

    @Test
    @DisplayName("likedByMe false 테스트")
    void likedByMe_false() {
      // given
      UUID requestId = UUID.randomUUID();
      Comment comment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(comment, "createdAt", Instant.now());

      CommentResponse commentResponse = new CommentResponse(
          comment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "댓글 내용",
          0L,
          false,
          comment.getCreatedAt()
      );
      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(commentResponse),
          "cursor",
          commentResponse.createdAt(),
          commentResponse.id(),
          true,
          2,
          1L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          null,
          null, null,
          5
      );

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition, requestId);

      // then
      assertThat(result.content().get(0).likedByMe()).isFalse();
    }

    @Test
    @DisplayName("likedByMe true 테스트")
    void likedByMe_true() {
      // given
      UUID requestId = UUID.randomUUID();
      Comment comment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(comment, "createdAt", Instant.now());

      CommentResponse commentResponse = new CommentResponse(
          comment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "댓글 내용",
          0L,
          true,
          comment.getCreatedAt()
      );
      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(commentResponse),
          "cursor",
          commentResponse.createdAt(),
          commentResponse.id(),
          true,
          2,
          1L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          null,
          null, null,
          5
      );

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition, requestId);

      // then
      assertThat(result.content().get(0).likedByMe()).isTrue();
    }

    @Test
    @DisplayName("댓글 목록 조회 성공")
    void 댓글_목록_조회_성공() {
      // given
      Comment firstComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(firstComment, "createdAt", Instant.now());
      Comment secondComment = Comment.create(article, user, content);
      ReflectionTestUtils.setField(secondComment, "createdAt", Instant.now().plusSeconds(1));

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          null,
          null, null,
          5
      );

      CommentResponse firstResponse = new CommentResponse(
          firstComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "첫 번째 댓글",
          0L,
          false,
          firstComment.getCreatedAt()
      );
      CommentResponse secondResponse = new CommentResponse(
          secondComment.getId(),
          article.getId(),
          user.getId(),
          user.getNickname(),
          "첫 번째 댓글",
          0L,
          false,
          secondComment.getCreatedAt()
      );

      CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
          List.of(firstResponse, secondResponse),
          "cursor",
          secondResponse.createdAt(),
          secondResponse.id(),
          true,
          2,
          2L
      );

      given(commentRepository.getComments(any(), any())).willReturn(response);

      // when
      CursorPageResponse<CommentResponse> result = commentService.getComments(condition,
          user.getId());

      // then
      assertThat(result.content()).hasSize(2);
    }
  }
}
