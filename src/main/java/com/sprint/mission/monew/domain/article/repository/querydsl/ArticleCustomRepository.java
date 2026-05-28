package com.sprint.mission.monew.domain.article.repository.querydsl;

import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.entity.Article;
import java.util.List;

public interface ArticleCustomRepository {

  List<Article> findAll(ArticleQueryCondition condition);

  long count(ArticleQueryCondition condition);

  String buildCursor(Article article, ArticleOrderBy orderBy);
}
