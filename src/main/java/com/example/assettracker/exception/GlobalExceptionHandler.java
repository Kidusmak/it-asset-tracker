package com.example.assettracker.exception;

import com.example.assettracker.dto.ErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converts every error into a clean JSON body: timestamp, status, error, message, path.
 * Extends ResponseEntityExceptionHandler so Spring MVC's own exceptions (unknown URL -> 404,
 * 405, 415, missing params, type mismatches, ...) use the same format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- application exceptions ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data (duplicate or referenced record)",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to access this resource",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                request.getRequestURI(), null);
    }

    // ---------- Spring MVC exceptions ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> fieldErrors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));

        String message = "Validation failed: " + fieldErrors.entrySet().stream()
                .map(e -> e.getKey() + " - " + e.getValue())
                .collect(Collectors.joining("; "));

        return asObject(build(HttpStatus.BAD_REQUEST, message, pathOf(request), fieldErrors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String message = "Malformed or missing JSON request body";
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String field = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                message = "Invalid value '" + ife.getValue() + "' for field '" + field
                        + "'. Allowed values: " + Arrays.toString(ife.getTargetType().getEnumConstants());
            } else {
                message = "Invalid value '" + ife.getValue() + "' for field '" + field + "'";
            }
        }
        return asObject(build(HttpStatus.BAD_REQUEST, message, pathOf(request), null));
    }

    /**
     * Fallback for all other Spring MVC exceptions (NoResourceFoundException -> 404,
     * HttpRequestMethodNotSupportedException -> 405, TypeMismatchException -> 400, ...).
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message;
        if (httpStatus == HttpStatus.BAD_REQUEST && ex instanceof org.springframework.beans.TypeMismatchException tme) {
            message = "Invalid value '" + tme.getValue() + "' for parameter '" + tme.getPropertyName() + "'";
            if (tme.getRequiredType() != null && tme.getRequiredType().isEnum()) {
                message += ". Allowed values: " + Arrays.toString(tme.getRequiredType().getEnumConstants());
            }
        } else if (body instanceof ProblemDetail pd && pd.getDetail() != null) {
            message = pd.getDetail();
        } else if (httpStatus == HttpStatus.NOT_FOUND) {
            message = "Resource not found";
        } else {
            message = httpStatus.getReasonPhrase();
        }
        return asObject(build(httpStatus, message, pathOf(request), null));
    }

    // ---------- helpers ----------

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path,
                                                       Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message, path, errors));
    }

    private static ResponseEntity<Object> asObject(ResponseEntity<ErrorResponse> response) {
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private static String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }
}
