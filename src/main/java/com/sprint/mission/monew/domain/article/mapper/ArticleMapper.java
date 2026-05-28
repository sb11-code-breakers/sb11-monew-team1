package com.sprint.mission.monew.domain.article.mapper;

import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.entity.Article;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleMapper {

  default ArticleResponse toDto(Article article, boolean viewedByMe) {
    return new ArticleResponse(
        article.getId(),
        article.getSource(),
        article.getSourceUrl(),
        article.getTitle(),
        article.getPublishDate(),
        article.getSummary(),
        article.getCommentCount(),
        article.getViewCount(),
        viewedByMe);
  }
}
