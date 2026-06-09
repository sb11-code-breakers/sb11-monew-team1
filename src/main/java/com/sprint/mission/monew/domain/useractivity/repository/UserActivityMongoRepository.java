package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

//extends 뒤에 몽고DB 기본 기능과 DirectRepository 명세서를 같이 적어서 상속받게 함
public interface UserActivityMongoRepository extends MongoRepository<UserActivity, UUID>, UserActivityMongoDirectRepository {

  //쿼리 메소드만 남기기
  Optional<UserActivity> findByIdAndNicknameIsNotNull(UUID id);

}