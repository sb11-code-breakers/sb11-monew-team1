package com.sprint.mission.monew.domain.comment.entity;

import com.sprint.mission.monew.common.entity.BaseEntity;
import com.sprint.mission.monew.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "comment_likes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_comment_likes_user_comment", columnNames = {"user_id",
            "comment_id"})
    },
    indexes = {
        @Index(name = "idx_comment_likes_user_id", columnList = "user_id"),
        @Index(name = "idx_comment_likes_comment_id", columnList = "comment_id")
    }
)
public class CommentLike extends BaseEntity {
  // id, createdAt
  // 사용자, 댓글

  // User(UUID)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  // Comment(UUID), not null
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "comment_id", nullable = false)
  private Comment comment;

  // 생성자
  private CommentLike(User user, Comment comment) {
    this.user = user;
    this.comment = comment;
  }

  // 정적 팩토리 메서드
  public static CommentLike create(User user, Comment comment) {
    return new CommentLike(user, comment);
  }

}
