package com.sprint.mission.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sprint.mission.monew.domain.article.dto.ArticleBackupEntry;
import com.sprint.mission.monew.domain.article.dto.ArticleRestoreResultDto;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.exception.ArticleRestoreFailedException;
import com.sprint.mission.monew.domain.article.repository.ArticleInterestRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class ArticleRestoreServiceTest {

  ArticleRestoreService articleRestoreService;
  @Mock ArticleRepository articleRepository;
  @Mock ArticleInterestRepository articleInterestRepository;
  @Mock InterestRepository interestRepository;
  @Mock S3Client s3Client;
  @Spy ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  Instant from;
  Instant to;

  @BeforeEach
  void setUp() {
    articleRestoreService = new ArticleRestoreService(
        articleRepository, articleInterestRepository, interestRepository, s3Client, objectMapper);
    ReflectionTestUtils.setField(articleRestoreService, "bucket", "test-bucket");
    from = LocalDate.now(ZoneOffset.UTC).minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    to = from;
  }

  private Interest 관심사_생성(String keyword) {
    return Interest.create("테스트 관심사", 14, List.of(keyword));
  }

  private Article 기사_생성(String sourceUrl) {
    return Article.create(ArticleSource.NAVER, sourceUrl, "제목", Instant.now(), "요약");
  }

  private ArticleBackupEntry 백업_항목(String sourceUrl) {
    return new ArticleBackupEntry(ArticleSource.NAVER, sourceUrl, "제목", Instant.now(), "요약");
  }

  private ResponseInputStream<GetObjectResponse> gzipStream(List<ArticleBackupEntry> entries)
      throws IOException {
    byte[] json = objectMapper.writeValueAsBytes(entries);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzos = new GZIPOutputStream(bos)) {
      gzos.write(json);
    }
    return new ResponseInputStream<>(
        GetObjectResponse.builder().build(),
        AbortableInputStream.create(new ByteArrayInputStream(bos.toByteArray())));
  }

  @Nested
  @DisplayName("유실 기사 복구")
  class Restore {

    @Test
    @DisplayName("S3에 백업 파일이 없는 날짜는 결과에 포함되지 않는다")
    void S3에_백업_파일이_없는_날짜는_결과에_포함되지_않는다() {
      // given
      given(s3Client.getObject(any(GetObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // then
      assertThat(result).isEmpty();
      verify(articleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("등록된 관심사가 없으면 복구를 건너뛴다")
    void 등록된_관심사가_없으면_복구를_건너뛴다() throws IOException {
      // given
      var stream = gzipStream(List.of(백업_항목("https://news.example.com/lost")));
      given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(stream);
      given(interestRepository.findAllWithKeywords()).willReturn(List.of());

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // then
      assertThat(result).isEmpty();
      verify(articleRepository, never()).findBySourceUrlIn(anyList());
      verify(articleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("관심사 키워드와 매칭되지 않는 기사는 복구하지 않는다")
    void 관심사_키워드와_매칭되지_않는_기사는_복구하지_않는다() throws IOException {
      // given
      var stream = gzipStream(List.of(백업_항목("https://news.example.com/no-match")));
      given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(stream);
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(관심사_생성("AI")));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of());

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // then — 기본 백업항목 제목 "제목"이 키워드 "AI"와 불일치
      assertThat(result).isEmpty();
      verify(articleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("DB에 이미 존재하는 기사는 복구하지 않는다")
    void DB에_이미_존재하는_기사는_복구하지_않는다() throws IOException {
      // given
      String sourceUrl = "https://news.example.com/exists";
      var stream = gzipStream(List.of(백업_항목(sourceUrl)));
      given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(stream);
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(관심사_생성("제목")));
      given(articleRepository.findBySourceUrlIn(anyList()))
          .willReturn(List.of(기사_생성(sourceUrl)));

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // then
      assertThat(result).isEmpty();
      verify(articleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("DB에 없고 관심사와 매칭되는 기사는 복구하고 ArticleInterest 매핑을 생성한다")
    void DB에_없는_기사는_복구하고_ArticleInterest_매핑을_생성한다() throws IOException {
      // given
      String sourceUrl = "https://news.example.com/lost";
      var stream = gzipStream(List.of(백업_항목(sourceUrl)));
      given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(stream);
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(관심사_생성("제목")));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of());
      given(articleRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
      given(articleInterestRepository.saveAll(anyList())).willReturn(List.of());

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).restoredArticleCount()).isEqualTo(1);
      assertThat(result.get(0).restoredArticleIds()).hasSize(1);
      verify(articleRepository).saveAll(anyList());
      verify(articleInterestRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("복구된 기사가 없는 날은 결과에 포함되지 않는다")
    void 복구된_기사가_없는_날은_결과에_포함되지_않는다() throws IOException {
      // given — 백업엔 있지만 DB에도 이미 존재
      String sourceUrl = "https://news.example.com/already";
      var stream = gzipStream(List.of(백업_항목(sourceUrl)));
      given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(stream);
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(관심사_생성("제목")));
      given(articleRepository.findBySourceUrlIn(anyList()))
          .willReturn(List.of(기사_생성(sourceUrl)));

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("여러 날에 걸쳐 복구 결과를 날짜별로 반환한다")
    void 여러_날에_걸쳐_복구_결과를_날짜별로_반환한다() throws IOException {
      // given — 이틀치 범위
      Instant twoDaysAgo = LocalDate.now(ZoneOffset.UTC).minusDays(2)
          .atStartOfDay(ZoneOffset.UTC).toInstant();
      var stream1 = gzipStream(List.of(백업_항목("https://news.example.com/a")));
      var stream2 = gzipStream(List.of(백업_항목("https://news.example.com/b")));
      given(s3Client.getObject(any(GetObjectRequest.class)))
          .willReturn(stream1).willReturn(stream2);
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(관심사_생성("제목")));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of());
      given(articleRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
      given(articleInterestRepository.saveAll(anyList())).willReturn(List.of());

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(twoDaysAgo, to);

      // then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("S3 읽기 중 예상치 못한 오류 발생 시 ArticleRestoreFailedException을 던진다")
    void S3_읽기_중_오류_발생_시_ArticleRestoreFailedException을_던진다() {
      // given
      given(s3Client.getObject(any(GetObjectRequest.class)))
          .willThrow(S3Exception.builder().statusCode(500).message("Internal Server Error").build());

      // when & then
      assertThatThrownBy(() -> articleRestoreService.restore(from, to))
          .isInstanceOf(ArticleRestoreFailedException.class);
    }

    @Test
    @DisplayName("백업 파일이 존재하지만 기사가 없으면 결과에 포함되지 않는다")
    void 백업_파일이_존재하지만_기사가_없으면_결과에_포함되지_않는다() throws IOException {
      // given — S3에 빈 배열 [] 가 담긴 파일 존재
      var stream = gzipStream(List.of());
      given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(stream);

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // then
      assertThat(result).isEmpty();
      verify(articleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("from이 to보다 늦으면 빈 결과를 반환한다")
    void from이_to보다_늦으면_빈_결과를_반환한다() {
      // given — from > to 이므로 루프 미실행
      Instant laterFrom = to.plusSeconds(86400);

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(laterFrom, to);

      // then
      assertThat(result).isEmpty();
      verify(s3Client, never()).getObject(any(GetObjectRequest.class));
    }
  }
}
