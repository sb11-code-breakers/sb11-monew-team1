package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordResetCodeRequest(
    @NotBlank String code,
    @NotBlank String newPassword
) {}