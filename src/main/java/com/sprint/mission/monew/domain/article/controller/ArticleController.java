package com.sprint.mission.monew.domain.article.controller;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.controller.api.ArticleApi;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.dto.ArticleRestoreResultDto;
import com.sprint.mission.monew.domain.article.dto.ArticleViewResponse;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.service.ArticleRestoreService;
import com.sprint.mission.monew.domain.article.service.ArticleService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController implements ArticleApi {

  private final ArticleService articleService;
  private final ArticleRestoreService articleRestoreService;

  @GetMapping
  @Override
  public ResponseEntity<CursorPageResponse<ArticleResponse>> search(
      @ParameterObject @ModelAttribute @Valid ArticleQueryCondition condition,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    return ResponseEntity.ok(articleService.search(condition, requestUserId));
  }

  @Cacheable("article-sources")
  @GetMapping("/sources")
  @Override
  public ResponseEntity<List<ArticleSource>> getSources() {
    return ResponseEntity.ok(Arrays.stream(ArticleSource.values()).toList());
  }

  @GetMapping("/{articleId}")
  @Override
  public ResponseEntity<ArticleResponse> getArticle(
      @PathVariable UUID articleId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    return ResponseEntity.ok(articleService.getArticle(articleId, requestUserId));
  }

  @PostMapping("/{articleId}/article-views")
  @Override
  public ResponseEntity<ArticleViewResponse> registerView(
      @PathVariable UUID articleId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    return ResponseEntity.ok(articleService.registerView(articleId, requestUserId));
  }

  @DeleteMapping("/{articleId}/hard")
  @Override
  public ResponseEntity<Void> hardDelete(@PathVariable UUID articleId) {
    articleService.hardDelete(articleId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{articleId}")
  @Override
  public ResponseEntity<Void> softDelete(@PathVariable UUID articleId) {
    articleService.softDelete(articleId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/restore")
  @Override
  public ResponseEntity<List<ArticleRestoreResultDto>> restore(
      @RequestParam Instant from,
      @RequestParam Instant to) {
    return ResponseEntity.ok(articleRestoreService.restore(from, to));
  }
}
