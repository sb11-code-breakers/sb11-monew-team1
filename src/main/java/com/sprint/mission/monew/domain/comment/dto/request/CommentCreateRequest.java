package com.sprint.mission.monew.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

// 기사ID(생성시 필수), 사용자ID(생성 시 필수), 댓글 내용(공백 생성 제한, 500자 제한)
public record CommentCreateRequest(
    @NotNull
    UUID articleId,

    @NotNull
    UUID userId,

    @NotBlank
    @Size(max = 500)
    String content
) {

}
