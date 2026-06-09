package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityMongoRepository extends MongoRepository<UserActivity, UUID> {

  // 🟢 핵심: 자바 컴파일러와 Mockito가 메서드의 규격을 인식할 수 있도록 껍데기 명세만 선언
  void createUserActivity(UserActivity userActivity);
  Optional<UserActivity> findByIdAndNicknameIsNotNull(UUID id);
  void updateNickname(UUID userId, String nickname);
  void anonymize(UUID userId);
  void anonymizeCommentLikesByCommentUserId(UUID userId);
  void pushSubscription(UUID userId, UUID targetId);
  void pullSubscription(UUID userId, UUID targetId);
  void pushComment(UUID userId, UUID articleId, UUID commentId);
}