package com.emt.configuration;

import com.emt.controller.api.MatchRestController;
import com.emt.controller.api.PlayerRestController;
import com.emt.controller.api.TournamentRestController;
import com.emt.model.api.ApiErrorResponse;
import com.emt.model.exception.IdenticalPlayersException;
import com.emt.model.exception.MatchNotFoundException;
import com.emt.model.exception.PlayerAlreadyExistsException;
import com.emt.model.exception.PlayerNotFoundException;
import com.emt.model.exception.TournamentCreationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice(
    assignableTypes = {
      PlayerRestController.class,
      MatchRestController.class,
      TournamentRestController.class
    })
public class RestApiExceptionHandler {

  private static final String GLOBAL_ERROR_ATTRIBUTE = "global";

  private final Clock clock;

  @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      Exception ex, HttpServletRequest request) {
    Map<String, String> errors = validationErrors(ex);
    log.warn("api_validation_error path={} errors={}", request.getRequestURI(), errors);
    return errorResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
  }

  @ExceptionHandler({
    IdenticalPlayersException.class,
    PlayerAlreadyExistsException.class,
    TournamentCreationException.class
  })
  public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
    log.warn("api_bad_request path={} message={}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
  }

  @ExceptionHandler({MatchNotFoundException.class, PlayerNotFoundException.class})
  public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
    log.warn("api_not_found path={} message={}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    PropertyReferenceException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
      Exception ex, HttpServletRequest request) {
    log.warn("api_invalid_request path={} message={}", request.getRequestURI(), ex.getMessage());
    return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
      Exception ex, HttpServletRequest request) {
    log.error("api_unexpected_error path={}", request.getRequestURI(), ex);
    return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request, Map.of());
  }

  private Map<String, String> validationErrors(Exception ex) {
    Map<String, String> errors = new HashMap<>();
    if (ex instanceof BindException bindException) {
      bindException.getBindingResult().getAllErrors().forEach(error -> addError(errors, error));
    }
    if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
      methodArgumentNotValidException
          .getBindingResult()
          .getAllErrors()
          .forEach(error -> addError(errors, error));
    }
    return errors;
  }

  private void addError(Map<String, String> errors, ObjectError error) {
    if (error instanceof FieldError fieldError) {
      errors.put(fieldError.getField(), error.getDefaultMessage());
      return;
    }
    errors.merge(GLOBAL_ERROR_ATTRIBUTE, error.getDefaultMessage(), (left, right) -> left + "; " + right);
  }

  private ResponseEntity<ApiErrorResponse> errorResponse(
      HttpStatus status, String message, HttpServletRequest request, Map<String, String> errors) {
    ApiErrorResponse response =
        ApiErrorResponse.builder()
            .timestamp(Instant.now(clock))
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(request.getRequestURI())
            .validationErrors(errors)
            .build();
    return ResponseEntity.status(status).body(response);
  }
}
