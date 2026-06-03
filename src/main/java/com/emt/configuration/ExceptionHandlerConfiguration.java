package com.emt.configuration;

import com.emt.model.exception.IdenticalPlayersException;
import com.emt.model.exception.MatchNotFoundException;
import com.emt.model.exception.PlayerAlreadyExistsException;
import com.emt.model.exception.PlayerNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class ExceptionHandlerConfiguration {

  private static final String ERROR_ATTRIBUTE = "error";

  @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
  public String handleValidationException(
      Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
    Map<String, String> errors = extractValidationErrors(ex);

    redirectAttributes.addFlashAttribute("errors", errors);
    log.warn("validation_error uri={} errors={}", request.getRequestURI(), errors);

    return redirectTargetFor(request);
  }

  @ExceptionHandler(PlayerAlreadyExistsException.class)
  public String handlePlayerAlreadyExistsException(
      PlayerAlreadyExistsException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Player already exists: " + ex.getMessage());
    log.warn("player_already_exists uri={} message={}", request.getRequestURI(), ex.getMessage());

    return redirectTargetFor(request);
  }

  @ExceptionHandler(IdenticalPlayersException.class)
  public String handleIdenticalPlayersException(
      IdenticalPlayersException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Match creation failed: " + ex.getMessage());
    log.warn("identical_players uri={} message={}", request.getRequestURI(), ex.getMessage());

    return redirectTargetFor(request);
  }

  @ExceptionHandler(MatchNotFoundException.class)
  public String handleMatchNotFoundException(
      MatchNotFoundException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Match not found: " + ex.getMessage());
    log.warn("match_not_found uri={} message={}", request.getRequestURI(), ex.getMessage());

    return redirectTargetFor(request);
  }

  @ExceptionHandler(PlayerNotFoundException.class)
  public String handlePlayerNotFoundException(
      PlayerNotFoundException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Player not found: " + ex.getMessage());
    log.warn("player_not_found uri={} message={}", request.getRequestURI(), ex.getMessage());

    return redirectTargetFor(request);
  }

  private Map<String, String> extractValidationErrors(Exception ex) {
    Map<String, String> errors = new HashMap<>();
    if (ex instanceof BindException bindException) {
      bindException
          .getBindingResult()
          .getAllErrors()
          .forEach(
              error -> {
                String fieldName = ((FieldError) error).getField();
                errors.put(fieldName, error.getDefaultMessage());
              });
    }
    return errors;
  }

  private String redirectTargetFor(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if ("/matches/report".equals(uri)) {
      return "redirect:/players";
    }
    if (uri != null && uri.startsWith("/matches")) {
      return "redirect:/matches";
    }
    return "redirect:/players";
  }
}
