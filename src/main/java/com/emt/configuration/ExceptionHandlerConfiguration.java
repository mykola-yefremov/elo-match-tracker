package com.emt.configuration;

import com.emt.controller.MatchController;
import com.emt.controller.PlayerController;
import com.emt.controller.TournamentController;
import com.emt.model.exception.IdenticalPlayersException;
import com.emt.model.exception.InvalidMatchScoreException;
import com.emt.model.exception.MatchNotFoundException;
import com.emt.model.exception.PlayerAlreadyExistsException;
import com.emt.model.exception.PlayerNotFoundException;
import com.emt.model.exception.TournamentCreationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(assignableTypes = {PlayerController.class, MatchController.class, TournamentController.class})
public class ExceptionHandlerConfiguration {

  private static final String ERROR_ATTRIBUTE = "error";
  private static final String GLOBAL_ERROR_ATTRIBUTE = "global";

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

  @ExceptionHandler({IdenticalPlayersException.class, InvalidMatchScoreException.class})
  public String handleIdenticalPlayersException(
      RuntimeException ex,
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

  @ExceptionHandler(TournamentCreationException.class)
  public String handleTournamentCreationException(
      TournamentCreationException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute(
        ERROR_ATTRIBUTE, "Tournament creation failed: " + ex.getMessage());
    log.warn("tournament_creation_failed uri={} message={}", request.getRequestURI(), ex.getMessage());

    return redirectTargetFor(request);
  }

  private Map<String, String> extractValidationErrors(Exception ex) {
    Map<String, String> errors = new HashMap<>();
    extractBindingResult(ex)
        .ifPresent(
            bindingResult ->
                bindingResult.getAllErrors().forEach(error -> addValidationError(errors, error)));
    return errors;
  }

  private Optional<BindingResult> extractBindingResult(Exception ex) {
    if (ex instanceof BindException bindException) {
      return Optional.of(bindException.getBindingResult());
    }
    if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
      return Optional.of(methodArgumentNotValidException.getBindingResult());
    }
    return Optional.empty();
  }

  private void addValidationError(Map<String, String> errors, ObjectError error) {
    if (error instanceof FieldError fieldError) {
      errors.put(fieldError.getField(), error.getDefaultMessage());
      return;
    }

    errors.merge(GLOBAL_ERROR_ATTRIBUTE, error.getDefaultMessage(), (left, right) -> left + "; " + right);
  }

  private String redirectTargetFor(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if ("/matches/report".equals(uri)) {
      return "redirect:/players";
    }
    if (uri != null && uri.startsWith("/matches")) {
      return "redirect:/matches";
    }
    if (uri != null && uri.startsWith("/tournaments")) {
      return "redirect:/tournaments";
    }
    return "redirect:/players";
  }
}
