package com.sprint.mission.monew.external.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.sprint.mission.monew.common.util.HtmlUtils;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RssNewsParser {

  @Value("${monew.rss.hankyung-url}")
  private String hankyungUrl;

  @Value("${monew.rss.chosun-url}")
  private String chosunUrl;

  @Value("${monew.rss.yonhap-url}")
  private String yonhapUrl;

  public List<RssArticleDto> parse(ArticleSource source) {
    String url = switch (source) {
      case HANKYUNG -> hankyungUrl;
      case CHOSUN -> chosunUrl;
      case YONHAP -> yonhapUrl;
      default -> throw new IllegalArgumentException("RSS 미지원 출처: " + source);
    };

    try (XmlReader reader = new XmlReader(new URL(url))) {
      SyndFeedInput input = new SyndFeedInput();
      SyndFeed feed = input.build(reader);
      return feed.getEntries().stream()
          .filter(entry -> entry.getLink() != null && !entry.getLink().isBlank())
          .filter(entry -> entry.getPublishedDate() != null)
          .map(entry -> toDto(source, entry))
          .toList();
    } catch (Exception e) {
      log.error("RSS 파싱 실패: source={}, url={}", source, url, e);
      return List.of();
    }
  }

  private RssArticleDto toDto(ArticleSource source, SyndEntry entry) {
    String title = HtmlUtils.strip(entry.getTitle());
    String sourceUrl = entry.getLink();
    Instant publishDate = entry.getPublishedDate().toInstant();
    String summary = entry.getDescription() != null
        ? HtmlUtils.strip(entry.getDescription().getValue())
        : "";
    return new RssArticleDto(source, sourceUrl, title, publishDate, summary);
  }
}
