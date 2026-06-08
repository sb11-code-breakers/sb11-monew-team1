package com.sprint.mission.monew.domain.user.dto;

import java.util.UUID;

public record LoginResult(
    UserResponse response,
    UUID sessionToken
) {

}