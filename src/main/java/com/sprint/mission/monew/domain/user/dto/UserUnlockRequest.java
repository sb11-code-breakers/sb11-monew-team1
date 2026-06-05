package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUnlockRequest(
    @NotBlank @Email String email
) {}