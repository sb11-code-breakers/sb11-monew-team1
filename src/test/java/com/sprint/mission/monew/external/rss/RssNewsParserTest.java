package com.sprint.mission.monew.external.rss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RssNewsParserTest {

  RssNewsParser rssNewsParser;
  String testRssUrl;

  @BeforeEach
  void setUp() {
    rssNewsParser = new RssNewsParser();
    testRssUrl = getClass().getClassLoader().getResource("test-rss.xml").toString();
    ReflectionTestUtils.setField(rssNewsParser, "hankyungUrl", testRssUrl);
    ReflectionTestUtils.setField(rssNewsParser, "chosunUrl", testRssUrl);
    ReflectionTestUtils.setField(rssNewsParser, "yonhapUrl", testRssUrl);
  }

  @Nested
  @DisplayName("parse")
  class Parse {

    @Test
    @DisplayName("link 없는 기사와 pubDate 없는 기사는 제외하고 반환한다")
    void link_없는_기사와_pubDate_없는_기사는_제외하고_반환한다() {
      // when
      List<RssArticleDto> result = rssNewsParser.parse(ArticleSource.HANKYUNG);

      // then — link 없는 항목 1건 + pubDate 없는 항목 1건 제외 → 2건
      assertThat(result).hasSize(2);
      assertThat(result.get(0).sourceUrl()).isEqualTo("https://test.com/news/1");
      assertThat(result.get(0).source()).isEqualTo(ArticleSource.HANKYUNG);
    }

    @Test
    @DisplayName("기사 제목의 HTML 태그가 제거된다")
    void 기사_제목의_HTML_태그가_제거된다() {
      // when
      List<RssArticleDto> result = rssNewsParser.parse(ArticleSource.CHOSUN);

      // then
      assertThat(result.get(0).title()).isEqualTo("테스트 기사 제목");
    }

    @Test
    @DisplayName("description 없는 기사는 summary가 빈 문자열이다")
    void description_없는_기사는_summary가_빈_문자열이다() {
      // when
      List<RssArticleDto> result = rssNewsParser.parse(ArticleSource.YONHAP);

      // then — 두 번째 기사(요약 없는 기사, pubDate 없는 기사는 이미 제외됨)
      assertThat(result.get(1).summary()).isEmpty();
    }

    @Test
    @DisplayName("pubDate 없는 기사는 결과에서 제외된다")
    void pubDate_없는_기사는_결과에서_제외된다() {
      // when
      List<RssArticleDto> result = rssNewsParser.parse(ArticleSource.HANKYUNG);

      // then — 날짜 없는 기사(news/3)는 포함되지 않음
      assertThat(result).noneMatch(dto -> dto.sourceUrl().equals("https://test.com/news/3"));
    }

    @Test
    @DisplayName("존재하지 않는 URL이면 빈 리스트를 반환한다")
    void 존재하지_않는_URL이면_빈_리스트를_반환한다() {
      // given
      ReflectionTestUtils.setField(rssNewsParser, "hankyungUrl", "file:///nonexistent.xml");

      // when
      List<RssArticleDto> result = rssNewsParser.parse(ArticleSource.HANKYUNG);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("RSS 미지원 출처이면 IllegalArgumentException을 던진다")
    void RSS_미지원_출처이면_IllegalArgumentException을_던진다() {
      // when & then
      assertThatThrownBy(() -> rssNewsParser.parse(ArticleSource.NAVER))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
