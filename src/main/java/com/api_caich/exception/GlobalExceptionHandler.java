package com.api_caich.exception;

import com.api_caich.constants.SecurityConstants;
import com.api_caich.dto.BaseResponseDTO;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponseDTO> handleBusinessException(BusinessException ex, WebRequest request) {
        BaseResponseDTO response = BaseResponseDTO.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponseDTO> handleResourceNotFoundException(ResourceNotFoundException ex,
            WebRequest request) {
        BaseResponseDTO response = BaseResponseDTO.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        BaseResponseDTO response = BaseResponseDTO.error(SecurityConstants.ERROR_VALIDATION_FAILED + errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponseDTO> handleBadCredentialsException(BadCredentialsException ex) {
        BaseResponseDTO response = BaseResponseDTO.error(SecurityConstants.ERROR_INVALID_CREDENTIALS);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponseDTO> handleAccessDeniedException(AccessDeniedException ex) {
        BaseResponseDTO response = BaseResponseDTO.error(SecurityConstants.ERROR_ACCESS_DENIED);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ SignatureException.class, ExpiredJwtException.class })
    public ResponseEntity<BaseResponseDTO> handleJwtException(Exception ex) {
        BaseResponseDTO response = BaseResponseDTO.error(SecurityConstants.ERROR_INVALID_TOKEN);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponseDTO> handleGlobalException(Exception ex, WebRequest request) {
        BaseResponseDTO response = BaseResponseDTO.error(SecurityConstants.ERROR_UNEXPECTED + " " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}