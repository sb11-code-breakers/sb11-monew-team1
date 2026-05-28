package com.sprint.mission.monew.domain.article.repository;

import com.sprint.mission.monew.domain.article.entity.ArticleView;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  @Query(
      "SELECT DISTINCT av.article.id FROM ArticleView av"
          + " WHERE av.article.id IN :articleIds AND av.userId = :userId")
  Set<UUID> findArticleIdsByArticleIdsAndUserId(
      @Param("articleIds") List<UUID> articleIds, @Param("userId") UUID userId);
}
