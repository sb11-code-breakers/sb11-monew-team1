package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
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

  public void pushComment(UUID userId, RecentComment comment) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("comments").atPosition(0).slice(10).each(comment);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  public void pullComment(UUID userId, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().pull("comments",
        Query.query(Criteria.where("id").is(commentId)));
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  public void updateNickname(UUID userId, String newNickname) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update()
        .set("nickname", newNickname)
        .set("comments.$[].userNickname", newNickname);
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  public void anonymize(UUID userId) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update()
        .set("nickname", "알 수 없음")
        .set("comments.$[].userNickname", "알 수 없음");
    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  public void pushArticleView(UUID userId, RecentArticleView articleView) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("articleViews").atPosition(0).slice(10).each(articleView);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  public void pullArticleViewsByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("articleViews",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  public void pullCommentsByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("comments",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  public void pushCommentLike(UUID userId, RecentCommentLike commentLike) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("commentLikes").atPosition(0).slice(10).each(commentLike);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  public void pullCommentLikesByArticleId(UUID articleId) {
    Query query = new Query();
    Update update = new Update().pull("commentLikes",
        Query.query(Criteria.where("articleId").is(articleId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  public void pushSubscription(UUID userId, RecentSubscription subscription) {
    Query query = Query.query(Criteria.where("_id").is(userId));
    Update update = new Update().push("subscriptions").atPosition(0).slice(10).each(subscription);
    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  public void pullSubscriptionsByInterestId(UUID interestId) {
    Query query = new Query();
    Update update = new Update().pull("subscriptions",
        Query.query(Criteria.where("interestId").is(interestId)));
    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }
}