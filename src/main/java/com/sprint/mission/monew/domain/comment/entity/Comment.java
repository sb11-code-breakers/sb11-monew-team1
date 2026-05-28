package com.sprint.mission.monew.domain.comment.entity;

import com.sprint.mission.monew.common.entity.BaseSoftDeletableEntity;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_article_id", columnList = "article_id"),
    @Index(name = "idx_comments_user_id", columnList = "user_id"),
    @Index(name = "idx_comments_like_count", columnList = "like_count")}
)
public class Comment extends BaseSoftDeletableEntity {
  // id, createdAt, updatedAt, deletedAt
  // 기사, 사용자, 댓글 내용, 좋아요, 좋아요 수

  // Article(UUID), not null
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  // User(UUID)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  // varchar(500), not null
  @Column(nullable = false, length = 500)
  private String content;

  // int, not null(default 0)
  @Column(name = "like_count", nullable = false)
  private int likeCount = 0;

  // 생성자
  private Comment(Article article, User user, String content) {
    this.article = article;
    this.user = user;
    this.content = content;
  }

  // 정적 팩토리 메서드
  public static Comment create(Article article, User user, String content) {
    return new Comment(article, user, content);
  }

  public void updateContent(String newContent) {
    this.content = newContent;
  }

  // 댓글의 userId와 받아온 userId가 같은지 여부를 체크
  // 탈퇴하지 않은 사용자의 댓글에만 체크, 사용자 ID까지 Objects.equals를 사용하여 null 체크
  public boolean isOwner(UUID userId) {
    return this.user != null && Objects.equals(this.user.getId(), userId);
  }
}
