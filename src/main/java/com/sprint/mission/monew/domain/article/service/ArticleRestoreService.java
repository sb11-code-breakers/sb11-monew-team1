package com.sprint.mission.monew.domain.article.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.batch.common.utils.BatchGzipUtils;
import com.sprint.mission.monew.domain.article.dto.ArticleBackupEntry;
import com.sprint.mission.monew.domain.article.dto.ArticleRestoreResultDto;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleInterest;
import com.sprint.mission.monew.domain.article.exception.ArticleRestoreFailedException;
import com.sprint.mission.monew.domain.article.repository.ArticleInterestRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRestoreService {

  private final ArticleRepository articleRepository;
  private final ArticleInterestRepository articleInterestRepository;
  private final InterestRepository interestRepository;
  private final S3Client s3Client;
  private final ObjectMapper objectMapper;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Transactional
  public List<ArticleRestoreResultDto> restore(Instant from, Instant to) {
    LocalDate fromDate = from.atZone(ZoneOffset.UTC).toLocalDate();
    LocalDate toDate = to.atZone(ZoneOffset.UTC).toLocalDate();

    List<ArticleRestoreResultDto> results = new ArrayList<>();
    for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
      restoreDate(date).ifPresent(results::add);
    }
    return results;
  }

  private Optional<ArticleRestoreResultDto> restoreDate(LocalDate date) {
    String s3Key = BatchGzipUtils.articleS3Key(date);

    List<ArticleBackupEntry> entries;
    try {
      byte[] compressed = s3Client.getObject(
          GetObjectRequest.builder().bucket(bucket).key(s3Key).build()).readAllBytes();
      entries = objectMapper.readValue(
          gunzip(compressed),
          objectMapper.getTypeFactory().constructCollectionType(List.class, ArticleBackupEntry.class));
    } catch (NoSuchKeyException e) {
      log.info("백업 파일 없음, skip: {}", s3Key);
      return Optional.empty();
    } catch (Exception e) {
      log.error("백업 파일 읽기 실패: {}", s3Key, e);
      throw ArticleRestoreFailedException.withKey(s3Key, e);
    }

    if (entries.isEmpty()) {
      return Optional.empty();
    }

    List<Interest> allInterests = interestRepository.findAllWithKeywords();
    if (allInterests.isEmpty()) {
      log.info("등록된 관심사 없음, 복구 건너뜀: {}", s3Key);
      return Optional.empty();
    }

    List<String> sourceUrls = entries.stream().map(ArticleBackupEntry::sourceUrl).toList();
    Set<String> existingUrls = articleRepository.findBySourceUrlIn(sourceUrls)
        .stream().map(Article::getSourceUrl).collect(Collectors.toSet());

    Map<String, List<Interest>> matchedByUrl = new LinkedHashMap<>();
    List<Article> toRestore = entries.stream()
        .filter(e -> !existingUrls.contains(e.sourceUrl()))
        .filter(e -> {
          List<Interest> matched = matchInterests(allInterests, e.title(), e.summary());
          if (!matched.isEmpty()) {
            matchedByUrl.put(e.sourceUrl(), matched);
            return true;
          }
          log.debug("관심사 미매칭, 복구 건너뜀 | sourceUrl={}", e.sourceUrl());
          return false;
        })
        .map(e -> Article.create(e.source(), e.sourceUrl(), e.title(), e.publishDate(), e.summary()))
        .toList();

    if (toRestore.isEmpty()) {
      return Optional.empty();
    }

    List<Article> saved = articleRepository.saveAll(toRestore);

    List<ArticleInterest> articleInterests = new ArrayList<>();
    for (Article article : saved) {
      List<Interest> matched = matchedByUrl.get(article.getSourceUrl());
      if (matched != null) {
        matched.stream()
            .map(i -> ArticleInterest.create(article, i))
            .forEach(articleInterests::add);
      }
    }
    articleInterestRepository.saveAll(articleInterests);

    List<UUID> restoredIds = saved.stream().map(Article::getId).toList();
    log.info("기사 복구 완료 | date={}, count={}", date, restoredIds.size());

    return Optional.of(new ArticleRestoreResultDto(
        date.atStartOfDay(ZoneOffset.UTC).toInstant(),
        restoredIds,
        restoredIds.size()));
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

  private byte[] gunzip(byte[] compressed) throws IOException {
    try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
      return gis.readAllBytes();
    }
  }
}
