
package com.triagex.backend.exception;

public class PacienteNoEncontradoException extends RuntimeException {

    public PacienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
