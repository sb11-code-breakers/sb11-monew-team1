package com.sprint.mission.monew.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@Profile("!test") // mongodb 전환 domain 작업 시 테스트 코드 작성 후 해제
@EnableMongoAuditing
public class MongoConfig {

}
