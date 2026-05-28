package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);
}