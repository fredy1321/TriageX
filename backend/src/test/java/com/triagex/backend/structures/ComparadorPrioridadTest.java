package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ComparadorPrioridadTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 13, 8, 0);
    private final ComparadorPrioridad comparador = ComparadorPrioridad.INSTANCIA;

    private EntradaCola entrada(String codigo, int nivel, LocalDateTime llegada, long secuencia) {
        Paciente p = new Paciente(codigo, "Paciente " + codigo, llegada, 30, "NIVEL " + nivel, "PENDIENTE");
        return new EntradaCola(p, nivel, llegada, secuencia);
    }

    @Test
    void nivelMasUrgenteVaPrimeroAunqueHayaLlegadoDespues() {
        EntradaCola urgenteTarde = entrada("PX1", 1, T0.plusHours(2), 9);
        EntradaCola leveTemprano = entrada("PX2", 5, T0, 0);

        assertTrue(comparador.compare(urgenteTarde, leveTemprano) < 0);
        assertTrue(comparador.compare(leveTemprano, urgenteTarde) > 0);
    }

    @Test
    void mismoNivelDesempataQuienLlegoAntes() {
        EntradaCola primero = entrada("PX1", 3, T0, 5);
        EntradaCola segundo = entrada("PX2", 3, T0.plusMinutes(1), 1);

        assertTrue(comparador.compare(primero, segundo) < 0);
    }

    @Test
    void mismoNivelYMismaHoraDesempataLaSecuencia() {
        EntradaCola primero = entrada("PX1", 2, T0, 1);
        EntradaCola segundo = entrada("PX2", 2, T0, 2);

        assertTrue(comparador.compare(primero, segundo) < 0);
    }

    @Test
    void sinFechaDeLlegadaVaDespuesDeQuienSiLaTiene() {
        EntradaCola conFecha = entrada("PX1", 3, T0, 2);
        EntradaCola sinFecha = entrada("PX2", 3, null, 1);

        assertTrue(comparador.compare(conFecha, sinFecha) < 0);
    }

    @Test
    void unaEntradaEsIgualASiMisma() {
        EntradaCola e = entrada("PX1", 4, T0, 0);
        assertEquals(0, comparador.compare(e, e));
    }

    @Test
    void convierteLaPrioridadDelModeloASuNumero() {
        assertEquals(1, ComparadorPrioridad.nivelNumerico("NIVEL 1"));
        assertEquals(5, ComparadorPrioridad.nivelNumerico("NIVEL 5"));
        assertEquals(3, ComparadorPrioridad.nivelNumerico(" nivel 3 "));
    }

    @Test
    void rechazaPrioridadesInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> ComparadorPrioridad.nivelNumerico(null));
        assertThrows(IllegalArgumentException.class, () -> ComparadorPrioridad.nivelNumerico("NIVEL 0"));
        assertThrows(IllegalArgumentException.class, () -> ComparadorPrioridad.nivelNumerico("NIVEL 6"));
        assertThrows(IllegalArgumentException.class, () -> ComparadorPrioridad.nivelNumerico("ALTA"));
    }
}