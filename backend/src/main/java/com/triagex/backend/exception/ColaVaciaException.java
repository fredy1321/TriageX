package com.triagex.backend.exception;

public class ColaVaciaException extends RuntimeException {

    public ColaVaciaException(String mensaje) {
        super(mensaje);
    }
}
