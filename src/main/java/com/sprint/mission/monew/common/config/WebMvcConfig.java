package com.sprint.mission.monew.common.config;

import com.sprint.mission.monew.common.exception.InvalidOrderByException;
import com.sprint.mission.monew.common.interceptor.MdcLoggingInterceptor;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.interest.dto.InterestOrderBy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final MdcLoggingInterceptor mdcLoggingInterceptor;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(
            "https://monew.dev"
        )
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("Content-Type", "Monew-Request-User-ID")
        .allowCredentials(false)
        .maxAge(3600);
  }

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
      default ->
          throw new InvalidOrderByException(value, ArticleOrderBy.class, "지원하는 정렬 기준이 아닙니다.");
    });

    registry.addConverter(String.class, InterestOrderBy.class, value -> switch (value) {
      case "name" -> InterestOrderBy.NAME;
      case "subscriberCount" -> InterestOrderBy.SUBSCRIBER_COUNT;
      default ->
          throw new InvalidOrderByException(value, InterestOrderBy.class, "지원하는 정렬 기준이 아닙니다.");
    });
  }
}