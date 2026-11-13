
package com.triagex.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PacienteNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarPacienteNoEncontrado(
            PacienteNoEncontradoException excepcion) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", excepcion.getMessage()));
    }

    @ExceptionHandler(ColaVaciaException.class)
    public ResponseEntity<Map<String, String>> manejarColaVacia(
            ColaVaciaException excepcion) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", excepcion.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarDatosInvalidos(
            IllegalArgumentException excepcion) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", excepcion.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidaciones(
            MethodArgumentNotValidException excepcion) {

        String mensaje = excepcion.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        return ResponseEntity.badRequest()
                .body(Map.of("error", mensaje));
    }
}
