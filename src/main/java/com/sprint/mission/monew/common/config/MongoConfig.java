package com.sprint.mission.monew.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@Profile("!test & !prod")
@EnableMongoAuditing
public class MongoConfig {

}
