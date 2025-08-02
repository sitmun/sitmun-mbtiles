package org.sitmun.mbtiles.controllers;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.sitmun.mbtiles.service.MBTilesFileNotFoundException;
import org.sitmun.mbtiles.service.MBTilesUnexpectedInternalException;
import org.sitmun.mbtiles.service.MBTilesUnexpectedRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for MBTiles application. Uses Spring Boot's standard ProblemDetail
 * format (RFC 7807).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // Common URN prefix for sitmun-mbtiles problems
  private static final String URN_PREFIX = "urn:sitmun-mbtiles:problem:";

  // Problem type URIs following RFC 7807
  private static final URI VALIDATION_ERROR_TYPE = URI.create(URN_PREFIX + "validation-error");
  private static final URI INVALID_REQUEST_TYPE = URI.create(URN_PREFIX + "invalid-request");
  private static final URI RESOURCE_NOT_FOUND_TYPE = URI.create(URN_PREFIX + "resource-not-found");
  private static final URI INTERNAL_ERROR_TYPE = URI.create(URN_PREFIX + "internal-error");
  private static final URI UNEXPECTED_ERROR_TYPE = URI.create(URN_PREFIX + "unexpected-error");

  /**
   * Handles validation errors from @Valid annotations. Returns clean error messages without stack
   * traces.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(
      MethodArgumentNotValidException e) {
    logger.warn("Validation error: {}", e.getMessage());

    List<String> errors =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problemDetail.setType(VALIDATION_ERROR_TYPE);
    problemDetail.setTitle("Validation Error");
    problemDetail.setProperty("errors", errors);

    return ResponseEntity.badRequest().body(problemDetail);
  }

  /** Handles unexpected request exceptions. */
  @ExceptionHandler(MBTilesUnexpectedRequestException.class)
  public ResponseEntity<ProblemDetail> handleUnexpectedRequestException(
      MBTilesUnexpectedRequestException e) {
    logger.warn("Invalid request error: {}", e.getMessage());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getMessage() != null ? e.getMessage() : "Invalid request parameters");
    problemDetail.setType(INVALID_REQUEST_TYPE);
    problemDetail.setTitle("Invalid Request");

    return ResponseEntity.badRequest().body(problemDetail);
  }

  /** Handles file not found exceptions. */
  @ExceptionHandler(MBTilesFileNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleFileNotFoundException(MBTilesFileNotFoundException e) {
    logger.warn("Resource not found error: {}", e.getMessage());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            e.getMessage() != null ? e.getMessage() : "Requested resource not found");
    problemDetail.setType(RESOURCE_NOT_FOUND_TYPE);
    problemDetail.setTitle("Resource Not Found");

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  /** Handles internal server errors. */
  @ExceptionHandler(MBTilesUnexpectedInternalException.class)
  public ResponseEntity<ProblemDetail> handleInternalException(
      MBTilesUnexpectedInternalException e) {
    logger.error("Internal server error: {}", e.getMessage(), e);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred");
    problemDetail.setType(INTERNAL_ERROR_TYPE);
    problemDetail.setTitle("Internal Server Error");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
  }

  /** Handles all other unexpected exceptions. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(Exception e) {
    logger.error("Unexpected error: {}", e.getMessage(), e);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problemDetail.setType(UNEXPECTED_ERROR_TYPE);
    problemDetail.setTitle("Unexpected Error");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
  }
}
