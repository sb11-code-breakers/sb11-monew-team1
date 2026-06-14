package com.sprint.mission.monew.domain.interest.config;

import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monew.synonyms")
public record SynonymProperties(List<Set<String>> suffixGroups, List<Set<String>> prefixGroups) {}