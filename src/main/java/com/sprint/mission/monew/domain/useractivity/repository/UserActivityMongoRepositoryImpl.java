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


  //댓글을 사용자의 활동 문서 내 comments 배열 맨 앞(0번 인덱스)에 추가

  public void pushComment(UUID id, RecentComment comment) {
    Query query = Query.query(Criteria.where("_id").is(id));

    // $push 연산과 position(0)을 사용하여 최신 댓글이 항상 배열 첫 번째에 오도록 처리
    Update update = new Update().push("comments").atPosition(0).value(comment);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }
}