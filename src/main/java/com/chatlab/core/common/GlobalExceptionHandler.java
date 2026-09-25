package com.chatlab.core.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Traduz erros do banco em respostas HTTP claras, em vez do "Erro 500" padrão.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String causa = ex.getMostSpecificCause().getMessage();

        if (causa != null && causa.contains("users_email_key")) {
            return ResponseEntity.badRequest().body(Map.of("erro", "E-mail já cadastrado."));
        }
        return ResponseEntity.badRequest().body(Map.of("erro", "Os dados enviados conflitam com um registro existente."));
    }
}
