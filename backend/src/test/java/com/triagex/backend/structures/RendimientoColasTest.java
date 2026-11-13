package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mide con System.nanoTime() las operaciones críticas de la cola de prioridad y
 * de la cola FIFO en escenarios de 1 000, 10 000 y 100 000 pacientes. Cada
 * medición se imprime con el formato: operación, tamaño, resultado y tiempo.
 * <p>
 * No es un benchmark riguroso (la JVM no se calienta antes de medir); sirve
 * como evidencia de que los tiempos crecen como dice la complejidad esperada.
 */
class RendimientoColasTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 13, 8, 0);

    @ParameterizedTest(name = "Escenario de {0} pacientes")
    @ValueSource(ints = {1_000, 10_000, 100_000})
    void medirOperacionesCriticas(int n) {
        Random r = new Random(n);
        ColaPrioridadPacientes prioridad = new ColaPrioridadPacientes();
        ColaFIFOPacientes fifo = new ColaFIFOPacientes();

        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            Paciente p = new Paciente("PX" + i, "Paciente " + i, T0.plusSeconds(i), 30,
                    "NIVEL " + (1 + r.nextInt(5)), "PENDIENTE");
            fifo.encolar(p);
            prioridad.insertar(p);
        }
        reportar("insercion(fifo+heap)", n, "OK", System.nanoTime() - t0, n);

        int consultas = 10_000;
        Optional<Paciente> siguiente = Optional.empty();
        t0 = System.nanoTime();
        for (int i = 0; i < consultas; i++) {
            siguiente = prioridad.verSiguiente();
        }
        reportar("consulta_siguiente", n, "codigo=" + siguiente.orElseThrow().getCodigo(),
                System.nanoTime() - t0, consultas);

        int reevaluados = Math.min(1_000, n);
        t0 = System.nanoTime();
        for (int i = 0; i < reevaluados; i++) {
            Paciente p = prioridad.buscar("PX" + r.nextInt(n)).orElseThrow();
            Paciente actualizado = new Paciente(p.getCodigo(), p.getNombre(), p.getFechaHoraLlegada(),
                    p.getEdad(), "NIVEL " + (1 + r.nextInt(5)), "PENDIENTE");
            prioridad.actualizar(actualizado);
        }
        reportar("reevaluacion", n, "OK", System.nanoTime() - t0, reevaluados);

        int extraidos = Math.min(1_000, n);
        t0 = System.nanoTime();
        for (int i = 0; i < extraidos; i++) {
            prioridad.extraerSiguiente();
        }
        reportar("extraccion_heap", n, "extraidos=" + extraidos, System.nanoTime() - t0, extraidos);

        t0 = System.nanoTime();
        for (int i = 0; i < extraidos; i++) {
            fifo.desencolar();
        }
        reportar("desencolar_fifo", n, "OK", System.nanoTime() - t0, extraidos);

        assertEquals(n - extraidos, prioridad.getTamaño());
        assertEquals(n - extraidos, fifo.getTamaño());
    }

    private static void reportar(String operacion, int n, String resultado, long totalNs, int repeticiones) {
        System.out.printf("PERF operacion=%s n=%d repeticiones=%d resultado=%s total_ns=%d promedio_ns=%d%n",
                operacion, n, repeticiones, resultado, totalNs, totalNs / Math.max(1, repeticiones));
    }
}