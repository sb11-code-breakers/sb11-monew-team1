package com.sprint.mission.monew.domain.article.controller;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.controller.api.ArticleApi;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.service.ArticleService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController implements ArticleApi {

  private final ArticleService articleService;

  @GetMapping
  @Override
  public ResponseEntity<CursorPageResponse<ArticleResponse>> search(
      @ParameterObject @ModelAttribute @Valid ArticleQueryCondition condition,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    return ResponseEntity.ok(articleService.search(condition, requestUserId));
  }
}
