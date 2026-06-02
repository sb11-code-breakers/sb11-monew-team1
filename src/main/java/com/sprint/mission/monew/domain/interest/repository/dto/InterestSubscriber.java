package com.sprint.mission.monew.domain.interest.repository.dto;

import java.util.UUID;

/**
 * 관심사 ID와 그 관심사를 구독한 사용자 ID 쌍을 담는 조회 전용 projection.
 */
public interface InterestSubscriber {

  UUID getInterestId();

  UUID getUserId();
}
