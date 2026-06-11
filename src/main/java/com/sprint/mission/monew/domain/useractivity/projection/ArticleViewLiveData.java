package com.sprint.mission.monew.domain.useractivity.projection;

import java.util.UUID;

public interface ArticleViewLiveData {

  UUID getId();

  UUID getArticleId();

  long getArticleCommentCount();

  long getArticleViewCount();
}
