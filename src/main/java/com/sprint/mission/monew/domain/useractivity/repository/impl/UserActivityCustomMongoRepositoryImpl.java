package com.sprint.mission.monew.domain.useractivity.repository.impl;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityCustomMongoRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityCustomMongoRepositoryImpl implements UserActivityCustomMongoRepository {

  private final MongoTemplate mongoTemplate;

  @Override
  public void pushComment(UUID userId, RecentComment comment) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("comments").atPosition(0).slice(10).each(comment);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  @Override
  public void pullComment(UUID userId, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("comments",
        Query.query(Criteria.where("id").is(commentId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  @Override
  public void updateNickname(UUID userId, String newNickname) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update()
        .set("nickname", newNickname)
        .set("comments.$[].userNickname", newNickname);
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  @Override
  public void anonymize(UUID userId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update()
        .set("nickname", "알 수 없음")
        .set("comments.$[].userNickname", "알 수 없음");
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  @Override
  public void pushArticleView(UUID userId, RecentArticleView articleView) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("articleViews").atPosition(0).slice(10).each(articleView);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  @Override
  public void pullArticleViewsByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("articleViews",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  @Override
  public void pullCommentsByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("comments",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  @Override
  public void pushCommentLike(UUID userId, RecentCommentLike commentLike) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("commentLikes").atPosition(0).slice(10).each(commentLike);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  @Override
  public void pullCommentLikesByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("commentLikes",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  @Override
  public void pushSubscription(UUID userId, RecentSubscription subscription) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("subscriptions").atPosition(0).slice(10).each(subscription);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  @Override
  public void pullSubscriptionsByInterestId(UUID interestId) {
    Query query = new Query();
    Update update = new Update().pull("subscriptions",
        Query.query(Criteria.where("interestId").is(interestId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  @Override
  public void createUserActivity(UserActivity userActivity) {
    mongoTemplate.save(userActivity);
  }

  // 💡 1. 회원 탈퇴 시 해당 유저가 단 댓글들의 좋아요 정보를 익명화(알 수 없음) 처리
  @Override
  public void anonymizeCommentLikesByCommentUserId(UUID userId) {
    Query query = new Query();
    Update update = new Update().set("commentLikes.$[elem].userNickname", "알 수 없음");
    update.filterArray(Criteria.where("elem.userId").is(userId));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  // 💡 2. 구독 취소 시 해당 관심사(interestId) 구독 건을 배열에서 삭제 (★우리가 찾아낸 정답 반영)
  @Override
  public void pullSubscription(UUID userId, UUID interestId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("subscriptions",
        Query.query(Criteria.where("interestId").is(interestId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 💡 3. 댓글 수정 시 몽고DB 내부의 배열 데이터 내용도 동기화 수정
  @Override
  public void updateCommentContent(UUID userId, UUID commentId, String content) {
    Query query = Query.query(Criteria.where("_id").is(userId).and("comments.id").is(commentId));
    Update update = new Update().set("comments.$.content", content);
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 💡 4. 댓글 좋아요 취소 시 해당 좋아요 건을 배열에서 제거
  @Override
  public void pullCommentLike(UUID userId, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("commentLikes",
        Query.query(Criteria.where("commentId").is(commentId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 💡 5. 게시글 삭제 시 유저 활동 문서에서 해당 게시글(articleId) 삭제
  @Override
  public void pullArticle(UUID userId, UUID articleId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("articles", Query.query(Criteria.where("id").is(articleId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 💡 6. 게시글 삭제 시 유저 활동 문서에서 해당 게시글에 눌렀던 좋아요 내역도 삭제
  @Override
  public void pullArticleLike(UUID userId, UUID articleId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("articleLikes", Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 💡 7. 게시글 삭제 시 유저 활동 문서에서 해당 최근 본 게시글 내역도 함께 삭제
  @Override
  public void pullArticleView(UUID userId, UUID articleId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("articleViews", Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }
}