package com.sprint.mission.monew.domain.article.repository;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.repository.querydsl.ArticleCustomRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ArticleRepository extends JpaRepository<Article, UUID>, ArticleCustomRepository {

  Optional<Article> findBySourceUrl(String sourceUrl);

  List<Article> findBySourceUrlIn(List<String> sourceUrls);

  List<Article> findByCreatedAtAfterAndDeletedAtIsNull(Instant since);

  List<Article> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
      Instant from, Instant to);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Article a set a.viewCount = a.viewCount + 1
            where a.id = :articleId and a.deletedAt is null
      """)
  void increaseViewCount(UUID articleId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Article a set a.commentCount = a.commentCount + 1
            where a.id = :articleId and a.deletedAt is null
      """)
  void increaseCommentCount(UUID articleId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Article a set a.commentCount = a.commentCount - 1
            where a.id = :articleId and a.commentCount > 0
      """)
  void decreaseCommentCount(UUID articleId);
}
