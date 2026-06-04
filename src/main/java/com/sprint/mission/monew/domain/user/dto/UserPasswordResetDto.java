package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordResetDto(
    @NotBlank String code,
    @NotBlank String newPassword
) {}