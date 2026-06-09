package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityMongoRepositoryImpl {

  private final MongoTemplate mongoTemplate;

  // 1. 댓글 작성 기록 누적
  public void pushComment(UUID id, RecentComment comment) {
    Query query = Query.query(Criteria.where("_id").is(id));
    Update update = new Update().push("comments").atPosition(0).slice(10).value(comment);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  // 2. 단건 댓글 삭제 (자바 클래스 명세인 'id' 필드를 기준으로 정확히 청소)
  public void pullComment(UUID id, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(id));
    Update update = new Update().pull("comments", Query.query(Criteria.where("id").is(commentId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 3. 사용자 닉네임 변경 시 댓글 내 닉네임 동기화
  public void updateNickname(UUID id, String newNickname) {
    Query query = Query.query(Criteria.where("_id").is(id));
    Update update = new Update().set("nickname", newNickname)
        .set("comments.$[].userNickname", newNickname);
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 4. 회원 탈퇴 시 익명화 처리
  public void anonymize(UUID id) {
    Query query = Query.query(Criteria.where("_id").is(id));
    Update update = new Update().set("nickname", "알 수 없음")
        .set("comments.$[].userNickname", "알 수 없음");
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 5. 기사 조회 기록 누적 (RecentArticleView.of 명세 완벽 일치)
  public void pushArticleView(UUID id, UUID articleId) {
    Query query = Query.query(Criteria.where("_id").is(id));
    RecentArticleView articleView = RecentArticleView.of(
        UUID.randomUUID(), id, Instant.now(), articleId,
        "테스트 출처", "https://test.url", "테스트 기사 제목",
        Instant.now(), "테스트 요약", 0L, 0L
    );
    Update update = new Update().push("articleViews", articleView);
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 6. [교정] 기사 삭제 시 모든 유저의 조회 기록에서 일괄 제거 (객체 내 'articleId' 필드 저격)
  public void pullArticleViewsByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("articleViews",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  // 7. 기사 삭제 시 모든 유저의 댓글 일괄 제거
  public void pullCommentsByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("comments",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  // 8.댓글 좋아요 기록 누적 (RecentCommentLike.of 명세 완벽 일치)
  public void pushCommentLike(UUID id, UUID articleId, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(id));
    RecentCommentLike commentLike = RecentCommentLike.of(
        UUID.randomUUID(), Instant.now(), commentId, articleId,
        "테스트 기사 제목", UUID.randomUUID(), "테스트 닉네임",
        "테스트 댓글 내용", 0L, Instant.now()
    );
    Update update = new Update().push("commentLikes", commentLike);
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 9. 기사 삭제 시 모든 유저의 댓글 좋아요 기록 일괄 제거
  public void pullCommentLikesByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("commentLikes",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }
}