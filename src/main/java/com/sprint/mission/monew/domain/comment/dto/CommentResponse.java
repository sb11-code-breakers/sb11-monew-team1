package com.sprint.mission.monew.domain.comment.dto;

import java.time.Instant;
import java.util.UUID;

// 댓글ID, 뉴스 기사ID, 사용자(댓글 작성자)ID, 사용자(댓글 작성자) 닉네임, 댓글 내용, 좋아요 수, 현재 로그인 사용자의 좋아요 여부, 생성 일자
public record CommentResponse(
    UUID id,
    UUID articleId,
    UUID userId,
    String userNickname,
    String content,
    long likeCount,
    boolean likedByMe,
    Instant createdAt
) {

}
