package com.sprint.mission.monew.batch.service;

import com.sprint.mission.monew.batch.dto.ArticleCandidate;
import com.sprint.mission.monew.batch.metrics.NewsCollectMetrics;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleInterest;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleInterestRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final ArticleInterestRepository articleInterestRepository;
  private final InterestRepository interestRepository;
  private final NewsCollectMetrics newsCollectMetrics;

  @Transactional
  public void upsertAll(ArticleSource source, List<ArticleCandidate> candidates) {
    Map<String, ArticleCandidate> deduped = candidates.stream()
        .filter(c -> c.sourceUrl() != null && !c.sourceUrl().isBlank())
        .collect(Collectors.toMap(
            ArticleCandidate::sourceUrl,
            Function.identity(),
            (a, b) -> a,
            LinkedHashMap::new));

    if (deduped.isEmpty()) {
      return;
    }

    List<Interest> allInterests = interestRepository.findAllWithKeywords();
    if (allInterests.isEmpty()) {
      log.info("등록된 관심사 없음, 기사 저장 건너뜀");
      return;
    }

    Map<String, Article> existing = articleRepository.findBySourceUrlIn(new ArrayList<>(deduped.keySet()))
        .stream()
        .collect(Collectors.toMap(Article::getSourceUrl, Function.identity(), (a, b) -> a));

    List<Article> toCreate = new ArrayList<>();
    Map<String, List<Interest>> matchedByUrl = new LinkedHashMap<>();

    for (ArticleCandidate c : deduped.values()) {
      Article article = existing.get(c.sourceUrl());
      if (article == null) {
        List<Interest> matched = matchInterests(allInterests, c.title(), c.summary());
        if (!matched.isEmpty()) {
          toCreate.add(Article.create(source, c.sourceUrl(), c.title(), c.publishDate(), c.summary()));
          matchedByUrl.put(c.sourceUrl(), matched);
        } else {
          log.debug("관심사 미매칭, 저장 건너뜀 | sourceUrl={}", c.sourceUrl());
        }
      } else if (!article.isDeleted()) {
        article.update(c.title(), c.summary());
        newsCollectMetrics.countDuplicated(source);
        log.debug("기사 업데이트 완료 | sourceUrl={}", c.sourceUrl());
      }
    }

    if (toCreate.isEmpty()) {
      return;
    }

    List<Article> saved = articleRepository.saveAll(toCreate);
    List<ArticleInterest> articleInterests = new ArrayList<>();
    for (Article article : saved) {
      List<Interest> matched = matchedByUrl.get(article.getSourceUrl());
      if (matched != null) {
        matched.stream()
            .map(i -> ArticleInterest.create(article, i))
            .forEach(articleInterests::add);
      }
      newsCollectMetrics.countCreated(source);
      log.info("기사 저장 완료 | articleId={}, sourceUrl={}", article.getId(), article.getSourceUrl());
    }
    articleInterestRepository.saveAll(articleInterests);
  }

  private List<Interest> matchInterests(List<Interest> interests, String title, String summary) {
    String lowerTitle = title != null ? title.toLowerCase() : "";
    String lowerSummary = summary != null ? summary.toLowerCase() : "";
    return interests.stream()
        .filter(i -> i.getKeywords().stream().anyMatch(k -> {
          String kw = k.getKeyword().toLowerCase();
          return lowerTitle.contains(kw) || lowerSummary.contains(kw);
        }))
        .toList();
  }
}
