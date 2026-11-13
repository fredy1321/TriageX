package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ColaFIFOPacientesTest {

    private ColaFIFOPacientes cola;

    @BeforeEach
    void setUp() {
        cola = new ColaFIFOPacientes();
    }

    private Paciente paciente(String codigo) {
        return new Paciente(codigo, "Paciente " + codigo, LocalDateTime.now(), 30, "NIVEL 3", "PENDIENTE");
    }

    private List<String> codigos() {
        return cola.listarEnOrden().stream().map(Paciente::getCodigo).toList();
    }

    @Test
    void colaNuevaEstaVacia() {
        assertTrue(cola.estaVacia());
        assertEquals(0, cola.getTamaño());
        assertTrue(cola.verFrente().isEmpty());
        assertTrue(cola.desencolar().isEmpty());
    }

    @Test
    void conservaElOrdenDeLlegada() {
        cola.encolar(paciente("PX1"));
        cola.encolar(paciente("PX2"));
        cola.encolar(paciente("PX3"));

        assertEquals(List.of("PX1", "PX2", "PX3"), codigos());
        assertEquals("PX1", cola.desencolar().get().getCodigo());
        assertEquals("PX2", cola.desencolar().get().getCodigo());
        assertEquals("PX3", cola.desencolar().get().getCodigo());
        assertTrue(cola.estaVacia());
    }

    @Test
    void verFrenteNoRetira() {
        cola.encolar(paciente("PX1"));
        cola.encolar(paciente("PX2"));

        assertEquals("PX1", cola.verFrente().get().getCodigo());
        assertEquals(2, cola.getTamaño());
    }

    @Test
    void sePuedeVaciarYVolverAUsar() {
        cola.encolar(paciente("PX1"));
        cola.desencolar();
        cola.encolar(paciente("PX2"));
        cola.encolar(paciente("PX3"));

        assertEquals("PX2", cola.verFrente().get().getCodigo());
        assertEquals(List.of("PX2", "PX3"), codigos());
    }

    @Test
    void eliminarDelInicioDelMedioYDelFinal() {
        for (int i = 1; i <= 5; i++) {
            cola.encolar(paciente("PX" + i));
        }

        assertTrue(cola.eliminar("PX3"));   // medio
        assertTrue(cola.eliminar("PX1"));   // frente
        assertTrue(cola.eliminar("PX5"));   // final
        assertFalse(cola.eliminar("PX9"));  // no existe

        assertEquals(List.of("PX2", "PX4"), codigos());
        assertEquals(2, cola.getTamaño());

        // Después de eliminar el último, encolar debe seguir funcionando.
        cola.encolar(paciente("PX6"));
        assertEquals(List.of("PX2", "PX4", "PX6"), codigos());
    }

    @Test
    void contiene() {
        cola.encolar(paciente("PX1"));
        assertTrue(cola.contiene("PX1"));
        assertFalse(cola.contiene("PX2"));
    }

    @Test
    void rechazaPacientesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> cola.encolar(null));
        assertThrows(IllegalArgumentException.class, () -> cola.encolar(paciente(" ")));
    }

    @Test
    void operacionesAleatoriasCoincidenConArrayDeque() {
        Deque<String> oraculo = new ArrayDeque<>();
        Random r = new Random(7);
        for (int i = 0; i < 10_000; i++) {
            if (oraculo.isEmpty() || r.nextBoolean()) {
                oraculo.addLast("PX" + i);
                cola.encolar(paciente("PX" + i));
            } else {
                assertEquals(oraculo.pollFirst(), cola.desencolar().get().getCodigo());
            }
            assertEquals(oraculo.size(), cola.getTamaño());
        }
        assertEquals(List.copyOf(oraculo), codigos());
    }
}