package com.sprint.mission.monew.domain.useractivity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

@DataMongoTest
@Import(com.sprint.mission.monew.domain.useractivity.repository.UserActivityCustomMongoRepositoryImpl.class)
class UserActivityMongoRepositoryTest {

  @Autowired
  private UserActivityMongoRepository repository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @AfterEach
  void tearDown() {
    mongoTemplate.dropCollection(UserActivity.class);
  }

  @Test
  @DisplayName("유저 활동 도큐먼트를 생성하고 댓글을 push 하면 배열 맨 앞에 추가된다")
  void pushComment() {
    // given
    UUID userId = UUID.randomUUID();
    repository.createUserActivity(UserActivity.of(userId, Instant.now()));

    RecentComment comment = RecentComment.of(
        UUID.randomUUID(), UUID.randomUUID(), "새로운 기사", Instant.now()
    );

    // when
    repository.pushComment(userId, comment);

    // then
    UserActivity found = repository.findById(userId).orElseThrow();
    assertThat(found.getComments()).hasSize(1);
    assertThat(found.getComments().get(0).getArticleTitle()).isEqualTo("새로운 기사");
  }

  @Test
  @DisplayName("유저가 탈퇴(익명화)하면 활동 배열들이 모두 빈 배열로 초기화된다")
  void anonymize() {
    // given
    UUID userId = UUID.randomUUID();
    repository.createUserActivity(UserActivity.of(userId, Instant.now()));
    repository.pushComment(userId, RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기사", Instant.now()));

    // when
    repository.anonymize(userId);

    // then
    UserActivity found = repository.findById(userId).orElseThrow();
    assertThat(found.getComments()).isEmpty();
    assertThat(found.getCommentLikes()).isEmpty();
  }
}