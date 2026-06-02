package com.sprint.mission.monew.domain.comment.repository.querydsl;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.comment.dto.CommentQueryCondition;
import com.sprint.mission.monew.domain.comment.dto.CommentResponse;
import java.util.UUID;

public interface CommentCustomRepository {

  CursorPageResponse<CommentResponse> getComments(CommentQueryCondition condition, UUID userId);

  long countByArticleId(UUID articleId);

}
