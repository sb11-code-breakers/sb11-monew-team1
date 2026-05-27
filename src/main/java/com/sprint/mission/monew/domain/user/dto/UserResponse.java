package com.sprint.mission.monew.domain.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String nickname,
    Instant createdAt
) {

}