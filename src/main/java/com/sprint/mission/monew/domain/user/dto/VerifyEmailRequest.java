package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
    @NotBlank String token
) {}