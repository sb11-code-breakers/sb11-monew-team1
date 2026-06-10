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
  @Nested
  @DisplayName("로그인 실패 횟수")
  class LoginFailCount {

    @Test
    @DisplayName("생성 시 로그인 실패 횟수는 0")
    void 생성_시_로그인_실패_횟수는_0() {
      assertThat(user.getLoginFailCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("incrementLoginFailCount 호출 시 실패 횟수 1 증가")
    void incrementLoginFailCount_호출_시_실패_횟수_1_증가() {
      // when
      user.incrementLoginFailCount();

      // then
      assertThat(user.getLoginFailCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("실패 횟수 초기화")
    void 실패_횟수_초기화() {
      // given
      user.incrementLoginFailCount();
      user.incrementLoginFailCount();

      // when
      user.resetLoginFailCount();

      // then
      assertThat(user.getLoginFailCount()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("계정 잠금")
  class AccountLock {

    @Test
    @DisplayName("생성 시 잠금 상태 아님")
    void 생성_시_잠금_상태_아님() {
      assertThat(user.isLocked()).isFalse();
    }

    @Test
    @DisplayName("lock 호출 시 잠금 상태")
    void lock_호출_시_잠금_상태() {
      // when
      user.lock();

      // then
      assertThat(user.isLocked()).isTrue();
    }

    @Test
    @DisplayName("unlock 호출 시 잠금 해제")
    void unlock_호출_시_잠금_해제() {
      // given
      user.lock();

      // when
      user.unlock();

      // then
      assertThat(user.isLocked()).isFalse();
    }

    @Test
    @DisplayName("unlock 시 실패 횟수 초기화")
    void unlock_시_실패_횟수_초기화() {
      // given
      user.incrementLoginFailCount();
      user.lock();

      // when
      user.unlock();

      // then
      assertThat(user.getLoginFailCount()).isEqualTo(0);
    }
  }
  @Test
  @DisplayName("로그인 실패 횟수가 5회 이상이면 한도 초과")
  void 로그인_실패_횟수가_5회_이상이면_한도_초과() {
    // given
    for (int i = 0; i < 5; i++) {
      user.incrementLoginFailCount();
    }

    // then
    assertThat(user.hasExceededLoginFailLimit()).isTrue();
  }

  @Test
  @DisplayName("로그인 실패 횟수가 4회이면 한도 미초과")
  void 로그인_실패_횟수가_4회이면_한도_미초과() {
    // given
    for (int i = 0; i < 4; i++) {
      user.incrementLoginFailCount();
    }

    // then
    assertThat(user.hasExceededLoginFailLimit()).isFalse();
  }

  @Nested
  @DisplayName("낙관적락")
  class OptimisticLock {

    @Test
    @DisplayName("생성 시 version은 null이다")
    void 생성_시_version은_null이다() {
      // then
      assertThat(user.getVersion()).isNull();
    }
  }
}