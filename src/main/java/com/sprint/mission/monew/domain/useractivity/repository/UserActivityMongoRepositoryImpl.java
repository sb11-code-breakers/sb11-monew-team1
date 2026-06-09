package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
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

  public void pushComment(UUID id, RecentComment comment) {
    Query query = Query.query(Criteria.where("_id").is(id));

    Update update = new Update()
        .push("comments")
        .atPosition(0)
        .slice(10)
        .value(comment);

    mongoTemplate.upsert(query, update, UserActivity.class);
  }

  public void pullComment(UUID id, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(id));

    Update update = new Update().pull("comments", Query.query(Criteria.where("_id").is(commentId)));

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  public void updateNickname(UUID id, String newNickname) {
    Query query = Query.query(Criteria.where("_id").is(id));

    Update update = new Update()
        .set("nickname", newNickname)
        .set("comments.$[].userNickname", newNickname);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  public void anonymize(UUID id) {
    Query query = Query.query(Criteria.where("_id").is(id));

    Update update = new Update()
        .set("nickname", "알 수 없음")
        .set("comments.$[].userNickname", "알 수 없음");

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }
}