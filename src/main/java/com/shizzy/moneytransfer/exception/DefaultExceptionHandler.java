package com.shizzy.moneytransfer.exception;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.shizzy.moneytransfer.exception.BusinessErrorCodes.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestControllerAdvice
public class DefaultExceptionHandler {

        // @ExceptionHandler(ResourceNotFoundException.class)
        // @ResponseBody
        // public ResponseEntity<ApiError> handleException (ResourceNotFoundException e,
        // HttpServletRequest request){
        // final ApiError apiError = ApiError.builder()
        // .message(e.getMessage())
        // .statusCode(HttpStatus.NOT_FOUND.value())
        // .localDateTime(LocalDateTime.now())
        // .build();
        //
        // return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
        // }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> handleException(ResourceNotFoundException ex, HttpServletRequest request) {
                // Log the Accept header for debugging
                String acceptHeader = request.getHeader("Accept");
                System.out.println("Client Accept Header: " + acceptHeader);

                ApiError error = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(ex.getMessage())
                                .statusCode(BAD_REQUEST.value())
                                .description("Resource not found")
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(error);
        }

        @ExceptionHandler(InvalidOtpException.class)
        public ResponseEntity<ApiError> handleInvalidOtpException(InvalidOtpException ex, HttpServletRequest request) {
                ApiError errorResponse = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(ex.getMessage())
                                .statusCode(HttpStatus.UNAUTHORIZED.value())
                                .localDateTime(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                Set<String> errors = new HashSet<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String errorMessage = error.getDefaultMessage();
                        errors.add(errorMessage);
                });
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .validationErrors(errors)
                                .localDateTime(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(InvalidFileFormatException.class)
        public ResponseEntity<ApiError> handleException(InvalidFileFormatException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(FraudulentTransactionException.class)
        public ResponseEntity<ApiError> handleException(FraudulentTransactionException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(FORBIDDEN.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, FORBIDDEN);
        }

        @ExceptionHandler(LockedException.class)
        public ResponseEntity<ApiError> handleException(LockedException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(ACCOUNT_LOCKED.getCode())
                                .description(ACCOUNT_LOCKED.getMessage())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, ACCOUNT_LOCKED.getHttpStatus());
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<ApiError> handleException(DisabledException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(ACCOUNT_DISABLED.getCode())
                                .description(ACCOUNT_DISABLED.getMessage())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, ACCOUNT_DISABLED.getHttpStatus());
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiError> handleException(BadCredentialsException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(BAD_CREDENTIALS.getCode())
                                .description(BAD_CREDENTIALS.getMessage())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, BAD_CREDENTIALS.getHttpStatus());
        }

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiError> handleException(UsernameNotFoundException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(BAD_CREDENTIALS.getCode())
                                .description(BAD_CREDENTIALS.getMessage())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, BAD_CREDENTIALS.getHttpStatus());
        }

        @ExceptionHandler(OptimisticLockingFailureException.class)
        public ResponseEntity<ApiError> handleException(OptimisticLockingFailureException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(DUPLICATE_TRANSACTION.getCode())
                                .description(DUPLICATE_TRANSACTION.getMessage())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, DUPLICATE_TRANSACTION.getHttpStatus());
        }

        @ExceptionHandler(MessagingException.class)
        public ResponseEntity<ApiError> handleException(MessagingException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ApiError> handleException(DuplicateResourceException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(HttpStatus.CONFLICT.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleException(IllegalArgumentException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(InvalidRequestException.class)
        public ResponseEntity<ApiError> handleException(InvalidRequestException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(TransactionLimitException.class)
        public ResponseEntity<ApiError> handleException(TransactionLimitException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(FORBIDDEN.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, FORBIDDEN);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiError> handleException(AuthenticationException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(HttpStatus.UNAUTHORIZED.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(InsufficientBalanceException.class)
        public ResponseEntity<ApiError> handleException(InsufficientBalanceException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(FORBIDDEN.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(UnauthorizedAccessException.class)
        public ResponseEntity<ApiError> handleException(UnauthorizedAccessException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(HttpStatus.UNAUTHORIZED.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(InvalidSecurityAnswerException.class)
        public ResponseEntity<ApiError> handleException(InvalidSecurityAnswerException e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleException(Exception e,
                        HttpServletRequest request) {
                final ApiError apiError = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(e.getMessage())
                                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .localDateTime(LocalDateTime.now())
                                .build();

                return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(LimitExceededException.class)
        public ResponseEntity<ApiError> handleLimitExceededException(LimitExceededException ex, HttpServletRequest request) {
                ApiError errorResponse = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(ex.getMessage())
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .localDateTime(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(TransactionLimitExceededException.class)
        public ResponseEntity<ApiError> handleLimitExceededException(TransactionLimitExceededException ex, HttpServletRequest request) {
                ApiError errorResponse = ApiError.builder()
                                .path(request.getRequestURI())
                                .message(ex.getMessage())
                                .statusCode(HttpStatus.FORBIDDEN.value())
                                .localDateTime(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

}
