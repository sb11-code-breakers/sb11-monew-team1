package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.document.UserSession;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserSessionRepository extends MongoRepository<UserSession, UUID> {

  void deleteByUserId(UUID userId);
}
