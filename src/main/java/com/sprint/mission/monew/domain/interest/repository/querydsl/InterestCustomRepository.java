package com.sprint.mission.monew.domain.interest.repository.querydsl;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestQueryCondition;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import java.util.List;
import java.util.UUID;

public interface InterestCustomRepository {

  CursorPageResponse<InterestResponse> findInterests(InterestQueryCondition condition, UUID userId);

  List<String> findNamesByTokens(List<String> tokens);
}