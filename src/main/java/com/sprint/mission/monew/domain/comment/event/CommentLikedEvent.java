package com.sprint.mission.monew.domain.comment.event;

import java.util.UUID;

public record CommentLikedEvent(UUID commentId, UUID commentAuthorId, String likerNickname) {

}
