package com.sprint.mission.monew.domain.useractivity.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityMongoCommandRepository {

  private final MongoTemplate mongoTemplate;
}