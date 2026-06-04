package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
    @NotBlank String currentPassword,
    @NotBlank
    @Size(min = 8, max = 50)
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).*$", message = "비밀번호는 영문자와 숫자를 포함해야 합니다")
    String newPassword
) {}