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
}