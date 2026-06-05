package com.sprint.mission.monew.domain.user.entity;

import com.sprint.mission.monew.common.entity.BaseSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@Entity
public class User extends BaseSoftDeletableEntity {

  private static final int MAX_LOGIN_FAIL_COUNT = 5;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String nickname;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private boolean emailVerified = false;

  @Column(nullable = false)
  private int loginFailCount = 0;

  @Column
  private Instant lockedAt;

  public static User create(String email, String nickname, String password) {
    User user = new User();
    user.email = email;
    user.nickname = nickname;
    user.password = password;
    user.emailVerified = false;
    user.loginFailCount = 0;
    return user;
  }

  public void updateNickname(String nickname) {
    this.nickname = nickname;
  }

  public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
  }

  public void verifyEmail() {
    this.emailVerified = true;
  }

  public void incrementLoginFailCount() {
    this.loginFailCount++;
  }

  public void resetLoginFailCount() {
    this.loginFailCount = 0;
  }

  public void lock() {
    this.lockedAt = Instant.now();
  }

  public void unlock() {
    this.lockedAt = null;
    this.loginFailCount = 0;
  }

  public boolean isLocked() {
    return this.lockedAt != null;
  }

  public boolean hasExceededLoginFailLimit() {
    return this.loginFailCount >= MAX_LOGIN_FAIL_COUNT;
  }
}