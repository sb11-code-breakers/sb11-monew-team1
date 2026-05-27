package com.sprint.mission.monew.domain.user.entity;

import com.sprint.mission.monew.common.entity.BaseSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@Entity
public class User extends BaseSoftDeletableEntity {

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String nickname;

  @Column(nullable = false)
  private String password;

  public static User create(String email, String nickname, String password) {
    User user = new User();
    user.email = email;
    user.nickname = nickname;
    user.password = password;
    return user;
  }
}