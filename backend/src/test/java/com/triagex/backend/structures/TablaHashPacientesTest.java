
package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TablaHashPacientesTest {

    private TablaHashPacientes tabla;

    @BeforeEach
    void setUp() {
        tabla = new TablaHashPacientes();
    }

    private Paciente crearPaciente(String codigo, String nombre) {
        return new Paciente(
                codigo,
                nombre,
                LocalDateTime.now(),
                25,
                "NIVEL 3",
                "PENDIENTE"
        );
    }

    @Test
    void insertarYBuscarPaciente() {
        Paciente paciente = crearPaciente("PX001", "Carlos");

        tabla.insertar(paciente);

        assertTrue(tabla.buscar("PX001").isPresent());
        assertEquals("Carlos", tabla.buscar("PX001").get().getNombre());
    }

    @Test
    void rechazarPacienteDuplicado() {
        Paciente paciente = crearPaciente("PX001", "Carlos");

        tabla.insertar(paciente);

        assertThrows(
                IllegalArgumentException.class,
                () -> tabla.insertar(paciente)
        );
    }

    @Test
    void eliminarPaciente() {
        tabla.insertar(crearPaciente("PX001", "Carlos"));

        assertTrue(tabla.eliminar("PX001"));
        assertFalse(tabla.buscar("PX001").isPresent());
    }

    @Test
    void actualizarPaciente() {
        tabla.insertar(crearPaciente("PX001", "Carlos"));

        Paciente actualizado = crearPaciente("PX001", "Andrés");

        assertTrue(tabla.actualizar(actualizado));
        assertEquals("Andrés", tabla.buscar("PX001").get().getNombre());
    }

    @Test
    void buscarPacienteInexistente() {
        assertFalse(tabla.buscar("PX999").isPresent());
    }

    @Test
    void comprobarTamaño() {
        tabla.insertar(crearPaciente("PX001", "Carlos"));
        tabla.insertar(crearPaciente("PX002", "Andrés"));

        assertEquals(2, tabla.getTamaño());
    }

    @Test
    void manejarColisiones() {

        Paciente paciente1 = crearPaciente("PX001Aa", "Carlos");
        Paciente paciente2 = crearPaciente("PX001BB", "Andrés");

        tabla.insertar(paciente1);
        tabla.insertar(paciente2);

        assertTrue(tabla.buscar("PX001Aa").isPresent());
        assertTrue(tabla.buscar("PX001BB").isPresent());

        assertEquals(2, tabla.getTamaño());
    }

    @Test
    void redimensionarTablaAlSuperarFactorDeCarga() {

        assertEquals(16, tabla.getCapacidad());

        for (int i = 1; i <= 13; i++) {
            tabla.insertar(crearPaciente("PX" + i, "Paciente " + i));
        }

        assertEquals(32, tabla.getCapacidad());
        assertEquals(13, tabla.getTamaño());

        for (int i = 1; i <= 13; i++) {
            assertTrue(tabla.buscar("PX" + i).isPresent());
        }
    }

}
