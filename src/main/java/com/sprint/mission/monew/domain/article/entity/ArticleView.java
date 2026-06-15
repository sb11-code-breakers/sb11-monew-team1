package com.sprint.mission.monew.domain.article.entity;

import com.sprint.mission.monew.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "article_views",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "article_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleView extends BaseEntity {

  @Column(name = "user_id")
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  public static ArticleView create(UUID userId, Article article) {
    ArticleView view = new ArticleView();
    view.userId = userId;
    view.article = article;
    return view;
  }
}
