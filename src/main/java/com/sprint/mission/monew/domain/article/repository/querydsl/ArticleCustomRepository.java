package com.sprint.mission.monew.domain.article.repository.querydsl;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import java.util.UUID;

public interface ArticleCustomRepository {

  CursorPageResponse<ArticleResponse> search(ArticleQueryCondition condition, UUID requestUserId);
}
