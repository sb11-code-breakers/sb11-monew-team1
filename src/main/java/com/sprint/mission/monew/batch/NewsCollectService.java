package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NewsCollectService {

  private final ArticleRepository articleRepository;
  private final NaverNewsClient naverNewsClient;
  private final RssNewsParser rssNewsParser;
  private final ApplicationEventPublisher eventPublisher;

  // 네트워크 호출이 포함되므로 트랜잭션 없이 실행, 각 upsert는 Spring Data 개별 트랜잭션으로 처리
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void collect() {
    collectNaver();
    collectRss(ArticleSource.HANKYUNG);
    collectRss(ArticleSource.CHOSUN);
    collectRss(ArticleSource.YONHAP);
  }

  private void collectNaver() {
    try {
      List<NaverNewsItem> items = naverNewsClient.fetchNews();
      for (NaverNewsItem item : items) {
        try {
          String sourceUrl = item.originallink() != null && !item.originallink().isBlank()
              ? item.originallink() : item.link();
          String title = NaverNewsClient.stripHtml(item.title());
          String summary = NaverNewsClient.stripHtml(item.description());
          upsert(ArticleSource.NAVER, sourceUrl, title,
              NaverNewsClient.parseNaverDate(item.pubDate()), summary);
        } catch (Exception e) {
          log.warn("Naver 기사 단건 처리 실패: link={}", item.link(), e);
        }
      }
      log.info("Naver 뉴스 수집 완료: {}건", items.size());
    } catch (Exception e) {
      log.error("Naver 뉴스 수집 실패", e);
    }
  }

  private void collectRss(ArticleSource source) {
    try {
      List<RssArticleDto> items = rssNewsParser.parse(source);
      for (RssArticleDto item : items) {
        try {
          upsert(item.source(), item.sourceUrl(), item.title(), item.publishDate(), item.summary());
        } catch (Exception e) {
          log.warn("{} 기사 단건 처리 실패: url={}", source, item.sourceUrl(), e);
        }
      }
      log.info("{} RSS 수집 완료: {}건", source, items.size());
    } catch (Exception e) {
      log.error("{} RSS 수집 실패", source, e);
    }
  }

  private void upsert(ArticleSource source, String sourceUrl, String title,
      Instant publishDate, String summary) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      log.warn("sourceUrl이 없어 기사를 건너뜁니다: title={}", title);
      return;
    }
    articleRepository.findBySourceUrl(sourceUrl)
        .ifPresentOrElse(
            existing -> {
              existing.update(title, summary);
              articleRepository.save(existing);
            },
            () -> {
              Article saved = articleRepository.save(
                  Article.create(source, sourceUrl, title, publishDate, summary));
              eventPublisher.publishEvent(new ArticleCreatedEvent(saved));
            });
  }
}
