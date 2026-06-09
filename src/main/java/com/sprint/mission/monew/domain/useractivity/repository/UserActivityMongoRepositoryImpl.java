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

  /**
   * 댓글을 사용자의 활동 문서 내 comments 배열 맨 앞(0번 인덱스)에 추가하고, 전체 댓글 수가 10개를 초과하면 최신 10개만 남기고 나머지는 잘라냅니다.
   */
  public void pushComment(UUID id, RecentComment comment) {
    Query query = Query.query(Criteria.where("_id").is(id));
    // .slice(10)을 추가하여 배열이 항상 최대 10개만 유지되도록 원자적 제어
    Update update = new Update()
        .push("comments")
        .atPosition(0)
        .slice(10)
        .value(comment);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  public void pullComment(UUID id, UUID commentId) {
    Query query = Query.query(Criteria.where("_id").is(id));

    // comments 배열 엘리먼트 중 _id가 지우려는 commentId와 일치하는 것을 지정하여 $pull
    Update update = new Update().pull("comments", Query.query(Criteria.where("_id").is(commentId)));

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }
}