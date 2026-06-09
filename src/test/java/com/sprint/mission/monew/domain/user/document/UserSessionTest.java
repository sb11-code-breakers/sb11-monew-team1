package com.sprint.mission.monew.domain.user.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserSessionTest {

  @Nested
  @DisplayName("정적 팩토리 메서드")
  class Create {

    @Test
    @DisplayName("sessionToken은 호출마다 다른 랜덤 UUID로 생성")
    void sessionToken은_호출마다_다른_랜덤_UUID로_생성() {
      // given
      UUID userId = UUID.randomUUID();

      // when
      UserSession s1 = UserSession.create(userId, "1.2.3.4", "fp-abc", 30);
      UserSession s2 = UserSession.create(userId, "1.2.3.4", "fp-abc", 30);

      // then
      assertThat(s1.getId()).isNotNull();
      assertThat(s1.getId()).isNotEqualTo(s2.getId());
    }

    @Test
    @DisplayName("userId, ip, deviceFingerprint가 그대로 저장")
    void userId_ip_deviceFingerprint가_그대로_저장() {
      // given
      UUID userId = UUID.randomUUID();

      // when
      UserSession session = UserSession.create(userId, "1.2.3.4", "fp-abc", 30);

      // then
      assertThat(session.getUserId()).isEqualTo(userId);
      assertThat(session.getIp()).isEqualTo("1.2.3.4");
      assertThat(session.getDeviceFingerprint()).isEqualTo("fp-abc");
    }

    @Test
    @DisplayName("expiresAt은 생성 시각 + timeoutMinutes")
    void expiresAt은_생성_시각_plus_timeoutMinutes() {
      // given
      Instant before = Instant.now();

      // when
      UserSession session = UserSession.create(UUID.randomUUID(), "1.2.3.4", "fp", 30);

      // then
      Instant after = Instant.now();
      assertThat(session.getExpiresAt())
          .isAfterOrEqualTo(before.plus(30, ChronoUnit.MINUTES))
          .isBeforeOrEqualTo(after.plus(30, ChronoUnit.MINUTES));
    }
  }

  @Nested
  @DisplayName("만료 갱신")
  class RefreshExpiry {

    @Test
    @DisplayName("refreshExpiry 호출 시 expiresAt이 연장됨")
    void refreshExpiry_호출_시_expiresAt이_연장됨() {
      // given
      UserSession session = UserSession.create(UUID.randomUUID(), "1.2.3.4", "fp", 30);
      Instant before = Instant.now();

      // when
      session.refreshExpiry(30);

      // then
      Instant after = Instant.now();
      assertThat(session.getExpiresAt())
          .isAfterOrEqualTo(before.plus(30, ChronoUnit.MINUTES))
          .isBeforeOrEqualTo(after.plus(30, ChronoUnit.MINUTES));
    }
  }
}