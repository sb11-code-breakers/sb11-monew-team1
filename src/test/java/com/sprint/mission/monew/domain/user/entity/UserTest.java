package com.sprint.mission.monew.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

  @Nested
  @DisplayName("User 생성")
  class Create {

    @Test
    @DisplayName("정상 입력으로 User 생성 성공")
    void 정상_입력으로_User_생성_성공() {
      // given
      String email = "test@test.com";
      String nickname = "테스터";
      String password = "encodedPassword";

      // when
      User user = User.create(email, nickname, password);

      // then
      assertThat(user.getEmail()).isEqualTo(email);
      assertThat(user.getNickname()).isEqualTo(nickname);
      assertThat(user.getPassword()).isEqualTo(password);
      assertThat(user.getId()).isNotNull();
      assertThat(user.isDeleted()).isFalse();
    }
  }
}