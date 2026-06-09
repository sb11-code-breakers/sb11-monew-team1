package com.sprint.mission.monew.common.exception;

import com.sprint.mission.monew.common.dto.ErrorResponse;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
    CommonErrorCode code = CommonErrorCode.RESOURCE_NOT_FOUND;
    log.warn("[{}] {}", code.name(), e.getMessage());
    return errorResponse(HttpStatus.NOT_FOUND, code, null, e);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
    CommonErrorCode code = CommonErrorCode.METHOD_NOT_ALLOWED;
    log.warn("[{}] {}", code.name(), e.getMessage());
    return errorResponse(HttpStatus.METHOD_NOT_ALLOWED, code, null, e);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
    CommonErrorCode code = CommonErrorCode.MESSAGE_NOT_READABLE;
    log.warn("[{}] {}", code.name(), e.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, code, null, e);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException e) {
    CommonErrorCode code = CommonErrorCode.VALIDATION_ERROR;
    Map<String, Object> details = Map.of("header", e.getHeaderName());
    log.warn("[{}] {}", code.name(), details);
    return errorResponse(HttpStatus.BAD_REQUEST, code, details, e);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestParam(MissingServletRequestParameterException e) {
    CommonErrorCode code = CommonErrorCode.VALIDATION_ERROR;
    Map<String, Object> details = Map.of(e.getParameterName(), "필수 파라미터입니다");
    log.warn("[{}] {}", code.name(), details);
    return errorResponse(HttpStatus.BAD_REQUEST, code, details, e);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    CommonErrorCode code = CommonErrorCode.TYPE_MISMATCH;
    Map<String, Object> details = Map.of(
        e.getName(),
        e.getRequiredType() != null ? e.getRequiredType().getSimpleName() + " 타입이어야 합니다" : "invalid type"
    );
    log.warn("[{}] {}", code.name(), details);
    return errorResponse(HttpStatus.BAD_REQUEST, code, details, e);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
    CommonErrorCode code = CommonErrorCode.VALIDATION_ERROR;
    Map<String, Object> details = e.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            this::resolveFieldErrorMessage,
            (existing, replacement) -> existing
        ));
    log.warn("[{}] {}", code.name(), details);
    return errorResponse(HttpStatus.BAD_REQUEST, code, details, e);
  }

  // GenericConversionService가 컨버터 예외를 ConversionFailedException으로 래핑하므로 root cause까지 탐색한다.
  private String resolveFieldErrorMessage(FieldError fe) {
    if (fe.contains(TypeMismatchException.class)) {
      Throwable root = NestedExceptionUtils.getMostSpecificCause(
          fe.unwrap(TypeMismatchException.class));
      if (root instanceof InvalidOrderByException ioe) {
        return ioe.getMessage();
      }
    }
    return fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid";
  }

  @ExceptionHandler(MonewException.class)
  public ResponseEntity<ErrorResponse> handleMonewException(MonewException e) {
    ErrorCode code = e.getErrorCode();
    log.warn("[{}] {}", code.getCode(), e.getMessage());
    return errorResponse(e.getStatus(), code, e.getDetails(), e);
  }

  @ExceptionHandler(ClientAbortException.class)
  public void handleClientAbort(ClientAbortException e) {
    log.warn("[ClientAbort] message: {}", e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    CommonErrorCode code = CommonErrorCode.INTERNAL_ERROR;
    log.error("[{}] cause: {}, message: {}", code.name(), e.getClass().getSimpleName(),
        e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
    return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, code, null, e);
  }

  private ResponseEntity<ErrorResponse> errorResponse(
      HttpStatus status,
      ErrorCode code,
      Map<String,Object> details,
      Exception e
  ) {
    return ResponseEntity
        .status(status)
        .body(new ErrorResponse(
            Instant.now(),
            code.getCode(),
            code.getMessage(),
            details,
            e.getClass().getSimpleName(),
            status.value()
        ));
  }
}
