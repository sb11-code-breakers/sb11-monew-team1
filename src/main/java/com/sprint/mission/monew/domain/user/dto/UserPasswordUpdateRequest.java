package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordUpdateRequest(
    @NotBlank String currentPassword,
    @NotBlank String newPassword
) {}