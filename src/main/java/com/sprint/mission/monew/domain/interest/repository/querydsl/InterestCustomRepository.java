package com.sprint.mission.monew.domain.interest.repository.querydsl;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestQueryCondition;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import java.util.UUID;

public interface InterestCustomRepository {

  CursorPageResponse<InterestResponse> findInterests(InterestQueryCondition condition, UUID userId);
}