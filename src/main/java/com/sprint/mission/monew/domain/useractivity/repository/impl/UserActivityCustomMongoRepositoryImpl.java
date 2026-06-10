package com.sprint.mission.monew.domain.useractivity.repository.impl;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityCustomMongoRepository;
import java.util.ArrayList;
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

  // ==========================================
  // 1. 라이프사이클 & 개인정보 처리
  // ==========================================

  @Override
  public void createUserActivity(UserActivity userActivity) {
    mongoTemplate.save(userActivity);
  }

  @Override
  public void anonymize(UUID userId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    // 텍스트 변경 대신 활동 내역 자체를 완전히 파기(초기화)
    Update update = new Update()
        .set("subscriptions", new ArrayList<>())
        .set("comments", new ArrayList<>())
        .set("commentLikes", new ArrayList<>())
        .set("articleViews", new ArrayList<>());
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  @Override
  public void anonymizeCommentLikesByCommentUserId(UUID userId) {
    // 상대방의 도큐먼트에 남은 내 흔적(userId)을 찾아 좋아요 기록 자체를 삭제
    Query query = new Query(Criteria.where("commentLikes.commentUserId").is(userId));
    Update update = new Update().pull("commentLikes", Query.query(Criteria.where("commentUserId").is(userId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  // ==========================================
  // 2. 활동 추가 (맨 앞에 넣고 10개 유지 - Genius Move!)
  // ==========================================

  @Override
  public void pushComment(UUID userId, RecentComment comment) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("comments").atPosition(0).slice(10).each(comment);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  @Override
  public void pushSubscription(UUID userId, RecentSubscription subscription) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    // 구독은 개수 제한을 두지 않거나 별도로 관리할 수 있으므로 slice 생략 (필요시 추가)
    Update update = new Update().push("subscriptions").atPosition(0).each(subscription);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  @Override
  public void pushCommentLike(UUID userId, RecentCommentLike commentLike) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("commentLikes").atPosition(0).slice(10).each(commentLike);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  @Override
  public void pushArticleView(UUID userId, RecentArticleView articleView) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("articleViews").atPosition(0).slice(10).each(articleView);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  // ==========================================
  // 3. 개별 단건 삭제 (Pull)
  // ==========================================

  @Override
  public void pullComment(UUID userId, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("comments", Query.query(Criteria.where("commentId").is(commentId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  @Override
  public void pullSubscription(UUID userId, UUID interestId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("subscriptions", Query.query(Criteria.where("interestId").is(interestId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  @Override
  public void pullCommentLike(UUID userId, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("commentLikes", Query.query(Criteria.where("commentId").is(commentId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  @Override
  public void pullArticleView(UUID userId, UUID articleId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("articleViews", Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // ==========================================
  // 4. 연쇄 삭제 (Cascade Delete)
  // ==========================================

  @Override
  public void pullArticleViewsByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("articleViews", Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  @Override
  public void pullCommentsByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("comments", Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  @Override
  public void pullCommentLikesByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("commentLikes", Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  @Override
  public void pullSubscriptionsByInterestId(UUID interestId) {
    Query query = new Query();
    Update update = new Update().pull("subscriptions", Query.query(Criteria.where("interestId").is(interestId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }
}