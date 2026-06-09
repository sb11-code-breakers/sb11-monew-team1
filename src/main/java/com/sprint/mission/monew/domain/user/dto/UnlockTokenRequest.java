package com.sprint.mission.monew.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UnlockTokenRequest(
    @NotNull UUID token
) {}