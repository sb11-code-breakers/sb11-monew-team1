package com.sprint.mission.monew.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = User.create("test@test.com", "테스터", "encodedPassword");
  }

  @Nested
  @DisplayName("비밀번호 변경")
  class UpdatePassword {

    @Test
    @DisplayName("새 비밀번호로 변경")
    void 새_비밀번호로_변경() {
      // when
      user.updatePassword("newEncodedPassword");

      // then
      assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
    }
  }

  @Nested
  @DisplayName("닉네임 변경")
  class UpdateNickname {

    @Test
    @DisplayName("새 닉네임으로 변경")
    void 새_닉네임으로_변경() {
      // when
      user.updateNickname("새닉네임");

      // then
      assertThat(user.getNickname()).isEqualTo("새닉네임");
    }
  }

  @Nested
  @DisplayName("이메일 인증")
  class VerifyEmail {

    @Test
    @DisplayName("회원가입 시 이메일 미인증 상태")
    void 회원가입_시_이메일_미인증_상태() {
      // then
      assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("이메일 인증 후 인증 상태로 변경")
    void 이메일_인증_후_인증_상태로_변경() {
      // when
      user.verifyEmail();

      // then
      assertThat(user.isEmailVerified()).isTrue();
    }
  }
}