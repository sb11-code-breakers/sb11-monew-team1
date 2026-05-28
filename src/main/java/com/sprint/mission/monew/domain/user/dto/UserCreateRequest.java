package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 2, max = 20) String nickname,
    @NotBlank String password
) {

}