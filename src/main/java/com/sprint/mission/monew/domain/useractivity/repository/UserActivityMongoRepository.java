package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

// 몽고디비 기본 기능과 우리가 만든 커스텀 명세를 상속받는 메인 인터페이스
public interface UserActivityMongoRepository extends MongoRepository<UserActivity, UUID>, UserActivityCustomMongoRepository {

  // 기존에 선언되어 있던 쿼리 메서드가 있다면 여기에 둡니다.
  Optional<UserActivity> findByIdAndNicknameIsNotNull(UUID id);
}