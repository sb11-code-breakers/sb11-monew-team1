package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleUpsertService {

  private final ArticleRepository articleRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final NewsCollectMetrics newsCollectMetrics;

  // save() + publishEvent()를 같은 트랜잭션으로 묶어 @TransactionalEventListener(AFTER_COMMIT) 안전 보장
  @Transactional
  public void upsert(ArticleSource source, String sourceUrl, String title,
      Instant publishDate, String summary) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      log.warn("sourceUrl이 없어 기사를 건너뜁니다: title={}", title);
      return;
    }
    articleRepository.findBySourceUrl(sourceUrl)
        .ifPresentOrElse(
            existing -> {
              if (existing.isDeleted()) {
                log.debug("소프트 삭제된 기사 건너뜁니다: sourceUrl={}", sourceUrl);
                return;
              }
              existing.update(title, summary);
              articleRepository.save(existing);
              newsCollectMetrics.countDuplicated();
            },
            () -> {
              Article saved = articleRepository.save(
                  Article.create(source, sourceUrl, title, publishDate, summary));
              eventPublisher.publishEvent(new ArticleCreatedEvent(saved));
              newsCollectMetrics.countCreated();
            });
  }
}
