package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {

}