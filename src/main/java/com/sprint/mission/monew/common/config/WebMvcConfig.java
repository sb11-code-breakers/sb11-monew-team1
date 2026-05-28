package com.sprint.mission.monew.common.config;

import com.sprint.mission.monew.common.interceptor.MdcLoggingInterceptor;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final MdcLoggingInterceptor mdcLoggingInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(mdcLoggingInterceptor)
        .addPathPatterns("/**");
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(String.class, ArticleOrderBy.class, value -> switch (value) {
      case "publishDate" -> ArticleOrderBy.PUBLISH_DATE;
      case "commentCount" -> ArticleOrderBy.COMMENT_COUNT;
      case "viewCount" -> ArticleOrderBy.VIEW_COUNT;
      default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준: " + value);
    });
  }
}