package com.sprint.mission.monew.domain.article.repository;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.repository.querydsl.ArticleCustomRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, UUID>, ArticleCustomRepository {

  Optional<Article> findBySourceUrl(String sourceUrl);

  List<Article> findBySourceUrlIn(List<String> sourceUrls);

  List<Article> findByCreatedAtAfterAndDeletedAtIsNull(Instant since);
}
