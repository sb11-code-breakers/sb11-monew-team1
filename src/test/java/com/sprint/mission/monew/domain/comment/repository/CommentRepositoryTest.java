package com.sprint.mission.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.batch.dto.CommentCleanupItem;
import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.dto.CommentOrderBy;
import com.sprint.mission.monew.domain.comment.dto.CommentQueryCondition;
import com.sprint.mission.monew.domain.comment.dto.CommentResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
public class CommentRepositoryTest {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager testEntityManager;

  private Article article;
  private User user;
  private Comment comment;
  private Instant baseTime;

  @BeforeEach
  void setUp() {
    article = articleRepository.save(
        Article.create(
            ArticleSource.NAVER,
            "https://example.com/news/1",
            "테스트 기사 제목",
            Instant.parse("2024-01-01T00:00:00Z"),
            "기사 요약 내용"
        ));
    user = userRepository.save(User.create(
        "Test@naver.com", "test", "12345678"
    ));
    comment = Comment.create(article, user, "댓글 내용");

    baseTime = Instant.parse("2024-01-01T00:00:00Z");
  }

  @Nested
  @DisplayName("save() 테스트")
  class Save {

    @Test
    @DisplayName("댓글 저장 성공")
    void 댓글_저장_성공() {
      // given
      // comment는 BeforeEach에서 초기화

      // when
      Comment savedComment = commentRepository.save(comment);

      // then
      assertThat(savedComment.getId()).isNotNull();
      assertThat(savedComment.getArticle().getId()).isEqualTo(article.getId());
      assertThat(savedComment.getUser().getId()).isEqualTo(user.getId());
      assertThat(savedComment.getContent()).isEqualTo("댓글 내용");
    }
  }

  @Nested
  @DisplayName("findById() 테스트")
  class FindById {

    @Test
    @DisplayName("존재하지 않는 댓글 조회")
    void 존재하지_않는_댓글_조회() {
      // given
      UUID notSavedCommentId = UUID.randomUUID();

      // when
      Optional<Comment> foundComment = commentRepository.findById(notSavedCommentId);

      // then
      assertThat(foundComment).isEmpty();
    }

    @Test
    @DisplayName("댓글 조회 성공")
    void 댓글_조회_성공() {
      // given
      Comment savedComment = commentRepository.save(comment);

      // when
      Comment foundComment = commentRepository.findById(savedComment.getId()).orElseThrow();

      // then
      assertThat(foundComment.getId()).isEqualTo(savedComment.getId());
      assertThat(foundComment.getArticle().getId()).isEqualTo(savedComment.getArticle().getId());
      assertThat(foundComment.getUser().getId()).isEqualTo(savedComment.getUser().getId());
      assertThat(foundComment.getContent()).isEqualTo(savedComment.getContent());
    }

  }

  @Nested
  @DisplayName("delete() 테스트")
  class Delete {

    @Test
    @DisplayName("댓글 삭제 성공")
    void 댓글_삭제_성공() {
      // given
      Comment savedComment = commentRepository.save(comment);
      commentRepository.delete(savedComment);

      // when
      Optional<Comment> result = commentRepository.findById(comment.getId());

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("findTop10RecentCommentsByUserId() 테스트")
  class FindTop10RecentCommentsByUserId {

    @Test
    @DisplayName("존재하지 않는 userId로 조회하면 빈 리스트를 반환한다")
    void 존재하지_않는_userId로_조회하면_빈_리스트를_반환한다() {
      // given
      UUID nonExistentUserId = UUID.randomUUID();

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(nonExistentUserId, PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("댓글이 없는 userId로 조회하면 빈 리스트를 반환한다")
    void 댓글이_없는_userId로_조회하면_빈_리스트를_반환한다() {
      // given
      // 댓글 없이 user만 있음

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(user.getId(), PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 댓글은 조회되지 않는다")
    void 삭제된_댓글은_조회되지_않는다() {
      // given
      Comment savedComment = commentRepository.save(comment);
      savedComment.softDelete();
      commentRepository.save(savedComment);

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(user.getId(), PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 기사의 댓글은 조회되지 않는다")
    void 삭제된_기사의_댓글은_조회되지_않는다() {
      // given
      commentRepository.save(comment);
      article.softDelete();
      articleRepository.save(article);

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(user.getId(), PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("댓글이 있으면 최근 10건을 반환한다")
    void 댓글이_있으면_최근_10건을_반환한다() {
      // given
      for (int i = 0; i < 15; i++) {
        commentRepository.save(Comment.create(article, user, "댓글 " + i));
      }

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(user.getId(), PageRequest.of(0, 10));

      // then
      assertThat(result).hasSize(10);
      List<Instant> createdAts = result.stream()
          .map(av -> av.getCreatedAt())
          .toList();
      assertThat(createdAts).isSortedAccordingTo((a, b) -> b.compareTo(a));;
    }
  }


  @Nested
  @DisplayName("increaseLikeCount() 테스트")
  class IncreaseLikeCount {

    @Test
    @DisplayName("댓글 좋아요 +1 증가 성공")
    void 댓글_좋아요_1_증가_성공() {
      // given
      Comment savedComment = commentRepository.save(comment);
      Comment before = commentRepository.findById(savedComment.getId()).orElseThrow();
      assertThat(before.getLikeCount()).isEqualTo(0);

      // when
      commentRepository.increaseLikeCount(comment.getId());

      testEntityManager.flush();
      testEntityManager.clear();

      // then
      Comment after = commentRepository.findById(savedComment.getId()).orElseThrow();
      assertThat(after.getLikeCount()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("decreaseLikeCount() 테스트")
  class DecreaseLikeCount {

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void 댓글_좋아요_취소_성공() {
      // given
      Comment savedComment = commentRepository.save(comment);

      commentRepository.findById(savedComment.getId()).orElseThrow();
      commentRepository.increaseLikeCount(savedComment.getId());
      testEntityManager.flush();
      testEntityManager.clear();
      Comment before = commentRepository.findById(savedComment.getId()).orElseThrow();
      assertThat(before.getLikeCount()).isEqualTo(1);

      // when
      commentRepository.decreaseLikeCount(savedComment.getId());

      testEntityManager.flush();
      testEntityManager.clear();

      // then
      Comment after = commentRepository.findById(savedComment.getId()).orElseThrow();
      assertThat(after.getLikeCount()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회하기")
  class Find {

    @Test
    @DisplayName("등록순(createdAt DESC) 조회")
    void 등록순_조회() {
      // given
      UUID articleId = article.getId();

      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));

      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));

      testEntityManager.flush();
      testEntityManager.clear();

      firstComment = commentRepository.findById(firstComment.getId()).orElseThrow();
      secondComment = commentRepository.findById(secondComment.getId()).orElseThrow();

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          null,
          null, null,
          5
      );

      // when
      CursorPageResponse<CommentResponse> response = commentRepository.getComments(condition,
          user.getId());
      List<CommentResponse> comments = response.content();

      // then
      assertThat(comments).hasSize(2);
      assertThat(comments).extracting(CommentResponse::createdAt)
          .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("좋아요순(likeCount DESC), 2순위 등록순(createdAt DESC) 조회")
    void 좋아요순_등록순_조회() {
      // given
      UUID articleId = article.getId();

      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));

      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));

      Comment thirdComment = commentRepository.save(Comment.create(article, user, "세 번째 댓글"));

      // 첫 번째 댓글 : 좋아요 2개
      commentRepository.increaseLikeCount(firstComment.getId());
      commentRepository.increaseLikeCount(firstComment.getId());

      // 두 번째 댓글 : 좋아요 2개
      commentRepository.increaseLikeCount(secondComment.getId());
      commentRepository.increaseLikeCount(secondComment.getId());

      // 세 번째 댓글 : 좋아요 1개
      commentRepository.increaseLikeCount(thirdComment.getId());

      testEntityManager.flush();
      testEntityManager.clear();

      firstComment = commentRepository.findById(firstComment.getId()).orElseThrow();
      secondComment = commentRepository.findById(secondComment.getId()).orElseThrow();
      thirdComment = commentRepository.findById(thirdComment.getId()).orElseThrow();

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.LIKE_COUNT,
          SortDirection.DESC,
          null,
          null, null,
          5
      );

      // when
      CursorPageResponse<CommentResponse> response = commentRepository.getComments(condition,
          user.getId());
      List<CommentResponse> comments = response.content();

      // then
      assertThat(comments).hasSize(3);

      // 2번째(좋아요2개, 등록순 2번째), 1번째(좋아요 2개, 등록순 1번째), 3번째(좋아요 1개) 순으로 정렬되어야 함
      assertThat(comments.get(0).id()).isEqualTo(secondComment.getId());
      assertThat(comments.get(1).id()).isEqualTo(firstComment.getId());
      assertThat(comments.get(2).id()).isEqualTo(thirdComment.getId());
    }

    @Test
    @DisplayName("등록순 첫 페이지 조회")
    void 등록순_조회_cursor_null() {
      // given
      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));

      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));

      testEntityManager.flush();
      testEntityManager.clear();

      CommentQueryCondition condition = new CommentQueryCondition(
          article.getId(),
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          null,
          null,
          null,
          5
      );

      // when
      CursorPageResponse<CommentResponse> response = commentRepository.getComments(condition,
          user.getId());
      List<CommentResponse> comments = response.content();

      // then
      assertThat(comments).hasSize(2);
    }

    @Test
    @DisplayName("등록순 다음 페이지 조회")
    void 등록순_조회_cursor() {
      // given
      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));

      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));

      Comment thirdComment = commentRepository.save(Comment.create(article, user, "세 번째 댓글"));

      testEntityManager.flush();
      testEntityManager.clear();

      firstComment = commentRepository.findById(firstComment.getId()).orElseThrow();
      secondComment = commentRepository.findById(secondComment.getId()).orElseThrow();
      thirdComment = commentRepository.findById(thirdComment.getId()).orElseThrow();

      CommentQueryCondition condition = new CommentQueryCondition(
          article.getId(),
          CommentOrderBy.CREATED_AT,
          SortDirection.DESC,
          secondComment.getCreatedAt().toString(), // 두번째 시간 이전의 댓글(firstComment)만 조회됨
          null,
          secondComment.getId(),
          5
      );

      // when
      CursorPageResponse<CommentResponse> response = commentRepository.getComments(condition,
          user.getId());
      List<CommentResponse> comments = response.content();

      // then
      assertThat(comments).hasSize(1); // 그래서 size는 3이 아닌 1이 나옴
      assertThat(comments.get(0).id()).isEqualTo(firstComment.getId());
    }

    @Test
    @DisplayName("좋아요순(2순위 등록순) 커서 조회")
    void 좋아요순_등록순_조회_cursor() {
      // given
      UUID articleId = article.getId();

      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));

      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));

      Comment thirdComment = commentRepository.save(Comment.create(article, user, "세 번째 댓글"));

      testEntityManager.flush();
      testEntityManager.clear();

      firstComment = commentRepository.findById(firstComment.getId()).orElseThrow();
      secondComment = commentRepository.findById(secondComment.getId()).orElseThrow();
      thirdComment = commentRepository.findById(thirdComment.getId()).orElseThrow();

      // 첫 번째 댓글 : 좋아요 2개
      commentRepository.increaseLikeCount(firstComment.getId());
      commentRepository.increaseLikeCount(firstComment.getId());

      // 두 번째 댓글 : 좋아요 2개
      commentRepository.increaseLikeCount(secondComment.getId());
      commentRepository.increaseLikeCount(secondComment.getId());

      // 세 번째 댓글 : 좋아요 1개
      commentRepository.increaseLikeCount(thirdComment.getId());

      CommentQueryCondition condition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.LIKE_COUNT,
          SortDirection.DESC,
          "2",
          secondComment.getCreatedAt(),
          secondComment.getId(),
          5
      );

      // when
      CursorPageResponse<CommentResponse> response = commentRepository.getComments(condition,
          user.getId());
      List<CommentResponse> comments = response.content();

      // then
      // 첫번째 페이지(2번째 댓글) 이후 다음 페이지에 1번째, 3번째 댓글이 나와야 함
      assertThat(comments).hasSize(2);
      assertThat(comments.get(0).id()).isEqualTo(firstComment.getId());
      assertThat(comments.get(1).id()).isEqualTo(thirdComment.getId());
    }

    @Test
    @DisplayName("등록순 오름차순 커서 조회")
    void 등록순_오름차순_커서_조회() throws InterruptedException {
      // given
      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));
      Thread.sleep(50);
      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));
      Thread.sleep(50);
      Comment thirdComment = commentRepository.save(Comment.create(article, user, "세 번째 댓글"));

      testEntityManager.flush();
      testEntityManager.clear();

      firstComment = commentRepository.findById(firstComment.getId()).orElseThrow();
      secondComment = commentRepository.findById(secondComment.getId()).orElseThrow();

      // secondComment를 cursor로 → thirdComment만 반환되어야 함
      CommentQueryCondition condition = new CommentQueryCondition(
          article.getId(),
          CommentOrderBy.CREATED_AT,
          SortDirection.ASC,
          secondComment.getCreatedAt().toString(),
          null,
          secondComment.getId(),
          5
      );

      // when
      CursorPageResponse<CommentResponse> response = commentRepository.getComments(condition, user.getId());
      List<CommentResponse> comments = response.content();

      // then
      assertThat(comments).hasSize(1);
      assertThat(comments.get(0).id()).isEqualTo(thirdComment.getId());
    }

    @Test
    @DisplayName("좋아요 수가 같을 때 2순위 등록순 정렬(좋아요순 ASC)")
    void 좋아요순_오름차순_동일_2순위_등록순_조회() throws InterruptedException {
      // given
      UUID articleId = article.getId();

      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));
      Thread.sleep(100);
      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));
      Thread.sleep(100);
      Comment thirdComment = commentRepository.save(Comment.create(article, user, "세 번째 댓글"));
      Thread.sleep(100);
      Comment fourthComment = commentRepository.save(Comment.create(article, user, "네 번째 댓글"));

      // 좋아요 수 세팅(첫 번째 : 1, 두 번째 : 2, 세 번째 : 2, 네 번째 : 3)
      commentRepository.increaseLikeCount(firstComment.getId());

      commentRepository.increaseLikeCount(secondComment.getId());
      commentRepository.increaseLikeCount(secondComment.getId());

      commentRepository.increaseLikeCount(thirdComment.getId());
      commentRepository.increaseLikeCount(thirdComment.getId());

      commentRepository.increaseLikeCount(fourthComment.getId());
      commentRepository.increaseLikeCount(fourthComment.getId());
      commentRepository.increaseLikeCount(fourthComment.getId());

      testEntityManager.flush();
      testEntityManager.clear();

      firstComment = commentRepository.findById(firstComment.getId()).orElseThrow();
      secondComment = commentRepository.findById(secondComment.getId()).orElseThrow();
      thirdComment = commentRepository.findById(thirdComment.getId()).orElseThrow();
      fourthComment = commentRepository.findById(fourthComment.getId()).orElseThrow();

      CommentQueryCondition firstCondition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.LIKE_COUNT,
          SortDirection.ASC,
          null,
          null, null,
          2
      );

      // when
      CursorPageResponse<CommentResponse> response1 = commentRepository.getComments(firstCondition,
          user.getId());
      List<CommentResponse> comments1 = response1.content();

      // then
      // 첫 페이지
      assertThat(comments1)
          .extracting(CommentResponse::likeCount)
          .contains(1L, 2L);

      CommentResponse lastOfTwoGroup = comments1.stream()
          .filter(c -> c.likeCount() == 2L)
          .max(Comparator.comparing(CommentResponse::createdAt))
          .orElseThrow();

      CommentQueryCondition secondCondition = new CommentQueryCondition(
          articleId,
          CommentOrderBy.LIKE_COUNT,
          SortDirection.ASC,
          String.valueOf(lastOfTwoGroup.likeCount()),
          lastOfTwoGroup.createdAt(),
          lastOfTwoGroup.id(),
          5
      );

      CursorPageResponse<CommentResponse> response2 = commentRepository.getComments(secondCondition,
          user.getId());
      List<CommentResponse> comments2 = response2.content();

      // 2 페이지
      assertThat(comments2)
          .extracting(CommentResponse::likeCount)
          .contains(2L, 3L);

      // tie-break 검증
      List<CommentResponse> tieGroup = Stream.concat(comments1.stream(), comments2.stream())
          .filter(c -> c.likeCount() == 2L)
          .toList();

      assertThat(tieGroup)
          .extracting(CommentResponse::createdAt)
          .isSortedAccordingTo(Comparator.naturalOrder()); // ASC 기준
    }
  }

  @Nested
  @DisplayName("deletedAt + id 기준 cursor 조회가 정렬된 순서로 반환하기")
  class FindCommentsForCleanup {
    @Test
    @DisplayName("deletedAt + id 기준 cursor 조회가 정렬된 순서로 반환된다")
    void findCommentsForCleanup_ordering_test() {
      // given
      Instant base = Instant.now().minus(Duration.ofDays(2));

      Comment comment1 = Comment.create(article, user, "첫 번째 댓글");
      Comment comment2 = Comment.create(article, user, "두 번째 댓글");
      Comment comment3 = Comment.create(article, user, "세 번째 댓글");

      ReflectionTestUtils.setField(comment1, "deletedAt", base.minusSeconds(10));
      ReflectionTestUtils.setField(comment2, "deletedAt", base.plusSeconds(10));
      ReflectionTestUtils.setField(comment3, "deletedAt", base.plusSeconds(20));

      commentRepository.save(comment1);
      commentRepository.save(comment2);
      commentRepository.save(comment3);

      // when
      List<CommentCleanupItem> result = commentRepository.findCommentsForCleanup(
          Instant.now(),
          Instant.EPOCH,
          UUID.randomUUID(),
          PageRequest.of(0, 10)
      );

      // then
      assertThat(result)
          .extracting(CommentCleanupItem::id)
          .containsExactly(
              comment1.getId(),
              comment2.getId(),
              comment3.getId()
          );
    }
  }
}
