package com.sprint.mission.monew.domain.useractivity.projection;

import java.util.UUID;

public interface CommentLikeLiveData {

  UUID getId();

  UUID getCommentId();

  UUID getCommentUserId();

  String getCommentUserNickname();

  String getCommentContent();

  long getCommentLikeCount();
}
