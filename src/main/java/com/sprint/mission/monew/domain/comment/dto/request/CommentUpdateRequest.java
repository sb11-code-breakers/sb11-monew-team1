package com.sprint.mission.monew.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
    @NotBlank
    @Size(max = 500)
    String content
) {

}
