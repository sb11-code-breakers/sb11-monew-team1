package com.sprint.mission.monew.domain.interest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestUpdateRequest(
    @NotNull
    @Size(min = 1, max = 10)
    List<@NotBlank @Size(max = 50) String> keywords
) {

}
