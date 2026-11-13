
package com.triagex.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record PacienteRequestDTO(

        @NotBlank(message = "El código es obligatorio")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "La fecha de llegada es obligatoria")
        LocalDateTime fechaHoraLlegada,

        @PositiveOrZero(message = "La edad no puede ser negativa")
        @NotNull(message = "La edad es obligatoria")
        @PositiveOrZero(message = "La edad no puede ser negativa")
        Integer edad,

        @NotBlank(message = "La prioridad es obligatoria")
        @Pattern(regexp = "NIVEL [1-5]", message = "La prioridad debe estar entre NIVEL 1 y NIVEL 5")
        String prioridad

) {
}
