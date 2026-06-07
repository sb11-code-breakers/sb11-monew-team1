package com.sprint.mission.monew.domain.article.mapper;

import com.sprint.mission.monew.domain.article.dto.ArticleViewResponse;
import com.sprint.mission.monew.domain.article.entity.ArticleView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleViewMapper {

  default ArticleViewResponse toResponse(ArticleView view, int viewCount) {
    return new ArticleViewResponse(
        view.getId(),
        view.getUserId(),
        view.getCreatedAt(),
        view.getArticle().getId(),
        view.getArticle().getSource(),
        view.getArticle().getSourceUrl(),
        view.getArticle().getTitle(),
        view.getArticle().getPublishDate(),
        view.getArticle().getSummary(),
        view.getArticle().getCommentCount(),
        viewCount);
  }
}
