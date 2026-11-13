package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TablaHashPacientesCasosLimiteTest {

    private TablaHashPacientes tabla;

    @BeforeEach
    void setUp() {
        tabla = new TablaHashPacientes();
    }

    private Paciente crearPaciente(String codigo) {
        return new Paciente(codigo, "Paciente " + codigo, LocalDateTime.now(), 30, "NIVEL 3", "PENDIENTE");
    }

    @Test
    void tablaNuevaEstaVacia() {
        assertTrue(tabla.estaVacia());
        assertEquals(0, tabla.getTamaño());
    }

    @Test
    void insertarPacienteNuloOSinCodigoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> tabla.insertar(null));
        assertThrows(IllegalArgumentException.class, () -> tabla.insertar(crearPaciente(null)));
        assertThrows(IllegalArgumentException.class, () -> tabla.insertar(crearPaciente("  ")));
        assertTrue(tabla.estaVacia());
    }

    @Test
    void duplicadoNoAumentaElTamaño() {
        tabla.insertar(crearPaciente("PX001"));

        assertThrows(IllegalArgumentException.class, () -> tabla.insertar(crearPaciente("PX001")));
        assertEquals(1, tabla.getTamaño());
    }

    @Test
    void buscarConCodigoNuloOVacioRetornaVacio() {
        tabla.insertar(crearPaciente("PX001"));

        assertTrue(tabla.buscar(null).isEmpty());
        assertTrue(tabla.buscar("").isEmpty());
    }

    @Test
    void eliminarInexistenteRetornaFalseYNoCambiaTamaño() {
        tabla.insertar(crearPaciente("PX001"));

        assertFalse(tabla.eliminar("PX999"));
        assertFalse(tabla.eliminar(null));
        assertEquals(1, tabla.getTamaño());
    }

    @Test
    void actualizarInexistenteRetornaFalse() {
        assertFalse(tabla.actualizar(crearPaciente("PX999")));
        assertFalse(tabla.actualizar(null));
    }

    @Test
    void eliminarElNodoDelMedioDeUnaCadenaDeColisionesConservaLosDemas() {
        String a = "PXAaAa";
        String b = "PXAaBB";
        String c = "PXBBAa";
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(b.hashCode(), c.hashCode());

        tabla.insertar(crearPaciente(a));
        tabla.insertar(crearPaciente(b));
        tabla.insertar(crearPaciente(c));

        assertTrue(tabla.eliminar(b));

        assertTrue(tabla.buscar(a).isPresent());
        assertFalse(tabla.buscar(b).isPresent());
        assertTrue(tabla.buscar(c).isPresent());
        assertEquals(2, tabla.getTamaño());
    }

    @Test
    void sePuedeVolverAInsertarUnCodigoEliminado() {
        tabla.insertar(crearPaciente("PX001"));
        tabla.eliminar("PX001");

        tabla.insertar(crearPaciente("PX001"));

        assertTrue(tabla.buscar("PX001").isPresent());
        assertEquals(1, tabla.getTamaño());
    }

    @Test
    void exactamenteEnElLimiteDelFactorDeCargaNoRedimensiona() {
        for (int i = 1; i <= 12; i++) {
            tabla.insertar(crearPaciente("PX" + i));
        }

        assertEquals(16, tabla.getCapacidad());
        assertEquals(12, tabla.getTamaño());
    }

    @Test
    void variosRedimensionamientosConservanTodosLosPacientes() {
        for (int i = 1; i <= 100; i++) {
            tabla.insertar(crearPaciente("PX" + i));
        }

        assertEquals(256, tabla.getCapacidad());
        assertEquals(100, tabla.getTamaño());
        for (int i = 1; i <= 100; i++) {
            assertTrue(tabla.buscar("PX" + i).isPresent(), "Falta PX" + i);
        }
    }
}
