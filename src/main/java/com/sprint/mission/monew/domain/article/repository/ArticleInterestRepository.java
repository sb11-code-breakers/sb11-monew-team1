package com.sprint.mission.monew.domain.article.repository;

import com.sprint.mission.monew.domain.article.entity.ArticleInterest;
import com.sprint.mission.monew.domain.article.repository.dto.InterestArticleCount;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {

  @Query("""
      SELECT ai.interest.id   AS interestId,
             ai.interest.name AS interestName,
             COUNT(ai)        AS articleCount
      FROM ArticleInterest ai
      JOIN ai.article a
      WHERE a.createdAt > :since
        AND a.deletedAt IS NULL
      GROUP BY ai.interest.id, ai.interest.name
      """)
  List<InterestArticleCount> countByInterestSince(@Param("since") Instant since);
}
