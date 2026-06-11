package com.sprint.mission.monew.domain.useractivity.projection;

import java.util.UUID;

public interface CommentLiveData {

  UUID getId();

  String getContent();

  long getLikeCount();
}
