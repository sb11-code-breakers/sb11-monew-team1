package com.sprint.mission.monew.domain.article.repository;

import com.sprint.mission.monew.domain.article.entity.ArticleView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  @Query(
      "SELECT DISTINCT av.article.id FROM ArticleView av"
          + " WHERE av.article.id IN :articleIds AND av.userId = :userId")
  Set<UUID> findArticleIdsByArticleIdsAndUserId(
      @Param("articleIds") List<UUID> articleIds, @Param("userId") UUID userId);

  @Query("SELECT av FROM ArticleView av " +
      "JOIN FETCH av.article a " +
      "WHERE av.userId = :userId " +
      "AND a.deletedAt IS NULL " +
      "ORDER BY av.createdAt DESC "
  )
  List<ArticleView> findTop10ByUserIdAndArticleNotDeleted(
      @Param("userId") UUID userId, Pageable pageable);

  boolean existsByArticleIdAndUserId(UUID articleId, UUID userId);

  Optional<ArticleView> findByArticleIdAndUserId(UUID articleId, UUID userId);
}
