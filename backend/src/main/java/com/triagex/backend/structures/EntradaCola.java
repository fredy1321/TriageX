package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;

import java.time.LocalDateTime;

/**
 * Elemento que guarda la cola de prioridad por cada paciente pendiente.
 * <p>
 * Guarda una "foto" de los datos que deciden el orden (nivel, llegada y
 * secuencia) para que la cola no dependa de que alguien modifique el objeto
 * Paciente por fuera. Así, al reevaluar, la cola sabe cuál era el nivel anterior
 * y si el paciente debe subir o bajar.
 *
 * @param paciente         paciente al que pertenece la entrada
 * @param nivel            nivel numérico: 1 = máxima prioridad … 5 = mínima
 * @param fechaHoraLlegada instante de llegada del paciente
 * @param secuencia        número de inserción en la cola; desempata si dos pacientes
 *                         tienen el mismo nivel y la misma fecha y hora de llegada
 */
public record EntradaCola(Paciente paciente, int nivel, LocalDateTime fechaHoraLlegada, long secuencia) {

    public String codigo() {
        return paciente.getCodigo();
    }
}