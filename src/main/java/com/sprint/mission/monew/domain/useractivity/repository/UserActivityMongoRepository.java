package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityMongoRepository
    extends MongoRepository<UserActivity, UUID> {

  Optional<UserActivity> findByIdAndNicknameIsNotNull(UUID id);
}