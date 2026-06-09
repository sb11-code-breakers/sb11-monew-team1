package com.sprint.mission.monew.domain.article.controller.api;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.dto.ArticleRestoreResultDto;
import com.sprint.mission.monew.domain.article.dto.ArticleViewResponse;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "뉴스 기사 관리", description = "뉴스 기사 API")
public interface ArticleApi {

  @Operation(summary = "뉴스 기사 목록 조회", description = "조건에 맞는 뉴스 기사 목록을 조회합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(schema = @Schema(implementation = CursorPageResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CursorPageResponse<ArticleResponse>> search(
      @ParameterObject @ModelAttribute @Valid ArticleQueryCondition condition,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID")
          UUID requestUserId);

  @Operation(summary = "출처 목록 조회", description = "뉴스 기사 출처 목록을 조회합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArticleSource.class)))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<List<ArticleSource>> getSources();

  @Operation(summary = "뉴스 기사 단건 조회", description = "뉴스 기사 ID로 뉴스 기사 단건을 조회합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(schema = @Schema(implementation = ArticleResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (잘못된 articleId 형식, 헤더 누락 등)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "기사를 찾을 수 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ArticleResponse> getArticle(
      @Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID")
          UUID requestUserId);

  @Operation(summary = "기사 조회수 등록", description = "뉴스 기사 조회수를 등록합니다. 중복 조회 시 기존 조회 정보를 반환합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회수 등록 성공",
        content = @Content(schema = @Schema(implementation = ArticleViewResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (잘못된 articleId 형식, 헤더 누락 등)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "기사를 찾을 수 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ArticleViewResponse> registerView(
      @Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID")
          UUID requestUserId);

  @Operation(
      summary = "뉴스 기사 물리 삭제",
      description = "뉴스 기사를 물리적으로 삭제합니다.",
      parameters = @Parameter(name = "Monew-Request-User-ID", in = ParameterIn.HEADER,
          description = "어드민 토큰 (ADR-11: 세션 토큰과 동일 헤더를 어드민 경로에서 재사용)", required = true))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "삭제 성공"),
    @ApiResponse(
        responseCode = "403",
        description = "관리자 권한 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "뉴스 기사 정보 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> hardDelete(@Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId);

  @Operation(summary = "뉴스 기사 논리 삭제", description = "뉴스 기사를 논리적으로 삭제합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "논리 삭제 성공"),
    @ApiResponse(
        responseCode = "404",
        description = "뉴스 기사 정보 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> softDelete(
      @Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId);

  @Operation(summary = "뉴스 복구", description = "유실된 뉴스 기사를 S3 백업 파일 기반으로 복구합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "복구 성공",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArticleRestoreResultDto.class)))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<List<ArticleRestoreResultDto>> restore(
      @Parameter(description = "복구 시작 날짜") @RequestParam Instant from,
      @Parameter(description = "복구 종료 날짜") @RequestParam Instant to);
}
