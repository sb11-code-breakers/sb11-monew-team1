package com.sprint.mission.monew.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sprint.mission.monew.batch.exception.ArticleBackupFailedException;
import com.sprint.mission.monew.batch.metrics.ArticleBackupMetrics;
import com.sprint.mission.monew.batch.util.BatchGzipUtils;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class ArticleBackupServiceTest {

  @InjectMocks
  ArticleBackupService articleBackupService;
  @Mock ArticleRepository articleRepository;
  @Mock S3Client s3Client;
  @Spy ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  @Mock
  ArticleBackupMetrics metrics;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(articleBackupService, "bucket", "test-bucket");
  }

  private Article 기사_생성() {
    return Article.create(
        ArticleSource.NAVER,
        "https://news.example.com/1",
        "테스트 기사",
        Instant.now(),
        "요약 내용");
  }

  @Nested
  @DisplayName("기사 S3 백업")
  class Backup {

    @Test
    @DisplayName("전날 기사가 없으면 S3 업로드를 호출하지 않는다")
    void 전날_기사가_없으면_S3_업로드를_호출하지_않는다() {
      // given
      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of());

      // when
      articleBackupService.backup();

      // then
      verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3에 이미 동일 키가 존재하면 업로드 없이 skip 처리한다")
    void S3에_이미_동일_키가_존재하면_업로드_없이_skip_처리한다() {
      // given
      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of(기사_생성()));
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willReturn(HeadObjectResponse.builder().build());

      // when
      articleBackupService.backup();

      // then
      verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      verify(metrics).countSkipped();
    }

    @Test
    @DisplayName("기사가 존재하고 S3에 없으면 GZIP 압축 후 업로드한다")
    void 기사가_존재하고_S3에_없으면_GZIP_압축_후_업로드한다() {
      // given
      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of(기사_생성()));
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());

      // when
      articleBackupService.backup();

      // then
      verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3 키 경로가 articles/yyyy/MM/dd/articles-yyyyMMdd.json.gz 형식이다")
    void S3_키_경로가_올바른_형식이다() {
      // given
      LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
      String expectedKey = "articles/" + yesterday.format(BatchGzipUtils.PATH_FORMATTER)
          + "/articles-" + yesterday.format(BatchGzipUtils.FILE_FORMATTER) + ".json.gz";

      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of(기사_생성()));
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());

      // when
      articleBackupService.backup();

      // then — putObject가 호출된 PutObjectRequest의 key 검증
      ArgumentCaptor<PutObjectRequest> captor =
          ArgumentCaptor.forClass(PutObjectRequest.class);
      verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
      assertThat(captor.getValue().key()).isEqualTo(expectedKey);
    }

    @Test
    @DisplayName("업로드 성공 시 업로드 건수·바이트·소요 시간을 집계한다")
    void 업로드_성공_시_업로드_건수_바이트_소요_시간을_집계한다() {
      // given
      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of(기사_생성()));
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());

      // when
      articleBackupService.backup();

      // then
      verify(metrics).countUploaded();
      verify(metrics).recordBytes(anyLong());
      verify(metrics).recordDuration(any(Duration.class));
    }

    @Test
    @DisplayName("S3 업로드 중 예외 발생 시 ArticleBackupFailedException을 던진다")
    void S3_업로드_중_예외_발생_시_ArticleBackupFailedException을_던진다() {
      // given
      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of(기사_생성()));
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());
      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(new RuntimeException("S3 연결 오류"));

      // when & then
      assertThatThrownBy(() -> articleBackupService.backup())
          .isInstanceOf(ArticleBackupFailedException.class);
      verify(metrics).countFailed();
    }

    @Test
    @DisplayName("headObject에서 403 S3Exception 발생 시 ArticleBackupFailedException을 던진다")
    void headObject에서_403_S3Exception_발생_시_ArticleBackupFailedException을_던진다() {
      // given
      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of(기사_생성()));
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());

      // when & then
      assertThatThrownBy(() -> articleBackupService.backup())
          .isInstanceOf(ArticleBackupFailedException.class);
      verify(metrics).countFailed();
    }

    @Test
    @DisplayName("headObject에서 404 S3Exception 발생 시 업로드를 진행한다")
    void headObject에서_404_S3Exception_발생_시_업로드를_진행한다() {
      // given
      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of(기사_생성()));
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

      // when
      articleBackupService.backup();

      // then
      verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("전날 00:00:00 UTC ~ 오늘 00:00:00 UTC 범위로 기사를 조회한다")
    void 전날_날짜_범위로_기사를_조회한다() {
      // given
      LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
      Instant expectedFrom = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant();
      Instant expectedTo = yesterday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

      given(articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(any(), any()))
          .willReturn(List.of());

      // when
      articleBackupService.backup();

      // then
      ArgumentCaptor<Instant> fromCaptor =
          ArgumentCaptor.forClass(Instant.class);
      ArgumentCaptor<Instant> toCaptor =
          ArgumentCaptor.forClass(Instant.class);
      verify(articleRepository)
          .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(fromCaptor.capture(), toCaptor.capture());
      assertThat(fromCaptor.getValue()).isEqualTo(expectedFrom);
      assertThat(toCaptor.getValue()).isEqualTo(expectedTo);
    }
  }
}
