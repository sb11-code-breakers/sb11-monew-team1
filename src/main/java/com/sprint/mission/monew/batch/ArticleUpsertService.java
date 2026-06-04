package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleUpsertService {

  private final ArticleRepository articleRepository;
  private final NewsCollectMetrics newsCollectMetrics;

  @Transactional
  public void upsert(ArticleSource source, String sourceUrl, String title,
      Instant publishDate, String summary) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      log.warn("sourceUrl 없어 기사 스킵 | title={}", title);
      return;
    }
    articleRepository.findBySourceUrl(sourceUrl)
        .ifPresentOrElse(
            existing -> {
              if (existing.isDeleted()) {
                log.debug("소프트 삭제된 기사 스킵 | sourceUrl={}", sourceUrl);
                return;
              }
              existing.update(title, summary);
              articleRepository.save(existing);
              newsCollectMetrics.countDuplicated();
              log.debug("기사 업데이트 완료 | sourceUrl={}", sourceUrl);
            },
            () -> {
              Article saved = articleRepository.save(
                  Article.create(source, sourceUrl, title, publishDate, summary));
              newsCollectMetrics.countCreated();
              log.info("기사 저장 완료 | articleId={}, sourceUrl={}", saved.getId(), sourceUrl);
            });
  }
}
