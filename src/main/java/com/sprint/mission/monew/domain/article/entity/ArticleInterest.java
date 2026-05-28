package com.sprint.mission.monew.domain.article.entity;

import com.sprint.mission.monew.common.entity.BaseEntity;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "article_interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleInterest extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  public static ArticleInterest create(Article article, Interest interest) {
    ArticleInterest ai = new ArticleInterest();
    ai.article = article;
    ai.interest = interest;
    return ai;
  }
}
