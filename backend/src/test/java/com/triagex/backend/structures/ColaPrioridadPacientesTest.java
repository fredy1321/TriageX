package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ColaPrioridadPacientesTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 13, 8, 0);

    private ColaPrioridadPacientes cola;

    @BeforeEach
    void setUp() {
        cola = new ColaPrioridadPacientes();
    }

    /** Crea un paciente que llegó "minuto" minutos después de las 8:00. */
    private Paciente paciente(String codigo, int nivel, int minuto) {
        return new Paciente(codigo, "Paciente " + codigo, T0.plusMinutes(minuto), 30,
                "NIVEL " + nivel, "PENDIENTE");
    }

    private List<String> ordenDeAtencion() {
        return cola.listarEnOrden().stream().map(Paciente::getCodigo).toList();
    }

    private String extraerCodigo() {
        return cola.extraerSiguiente().orElseThrow().getCodigo();
    }

    // -------------------------------------------- casos de la sección 10 del docx

    @Test
    void prioridadDiferenteSaleLaMasUrgente() {
        cola.insertar(paciente("PX1", 4, 0));
        cola.insertar(paciente("PX2", 1, 1));
        cola.insertar(paciente("PX3", 3, 2));

        assertEquals("PX2", cola.verSiguiente().get().getCodigo());
        assertEquals(List.of("PX2", "PX3", "PX1"), ordenDeAtencion());
    }

    @Test
    void empateDePrioridadSaleQuienLlegoAntes() {
        cola.insertar(paciente("PX2", 3, 5));
        cola.insertar(paciente("PX1", 3, 0));
        cola.insertar(paciente("PX3", 3, 9));

        assertEquals("PX1", extraerCodigo());
        assertEquals("PX2", extraerCodigo());
        assertEquals("PX3", extraerCodigo());
    }

    @Test
    void mismaHoraDeLlegadaSaleQuienEntroPrimeroALaCola() {
        cola.insertar(paciente("PX1", 2, 0));
        cola.insertar(paciente("PX2", 2, 0));

        assertEquals(List.of("PX1", "PX2"), ordenDeAtencion());
    }

    @Test
    void colaVaciaRespondeSinError() {
        assertTrue(cola.verSiguiente().isEmpty());
        assertTrue(cola.extraerSiguiente().isEmpty());
        assertEquals(0, cola.getTamaño());
    }

    @Test
    void verSiguienteNoRetiraYExtraerSi() {
        cola.insertar(paciente("PX1", 2, 0));

        assertEquals("PX1", cola.verSiguiente().get().getCodigo());
        assertEquals(1, cola.getTamaño());
        assertEquals("PX1", extraerCodigo());
        assertTrue(cola.estaVacia());
    }

    @Test
    void rechazaCodigoDuplicadoYConservaElOriginal() {
        cola.insertar(paciente("PX1", 3, 0));

        assertThrows(IllegalArgumentException.class, () -> cola.insertar(paciente("PX1", 1, 1)));
        assertEquals(1, cola.getTamaño());
        assertEquals("NIVEL 3", cola.verSiguiente().get().getPrioridad());
    }

    @Test
    void rechazaPacientesYPrioridadesInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> cola.insertar(null));
        assertThrows(IllegalArgumentException.class,
                () -> cola.insertar(new Paciente("PX1", "X", T0, 30, "URGENTE", "PENDIENTE")));
        assertTrue(cola.estaVacia());
    }

    // ------------------------------------------------------------- reevaluación

    @Test
    void reevaluacionAMasUrgenteSubeYApareceUnaSolaVez() {
        cola.insertar(paciente("PX1", 2, 0));
        cola.insertar(paciente("PX2", 3, 1));
        Paciente p3 = paciente("PX3", 5, 2);
        cola.insertar(p3);

        p3.setPrioridad("NIVEL 1");
        assertTrue(cola.actualizar(p3));

        assertEquals(List.of("PX3", "PX1", "PX2"), ordenDeAtencion());
        assertEquals(3, cola.getTamaño());
        assertTrue(cola.esHeapValido());
    }

    @Test
    void reevaluacionAMenosUrgenteBajaSinDejarPrioridadVieja() {
        Paciente p1 = paciente("PX1", 1, 0);
        cola.insertar(p1);
        cola.insertar(paciente("PX2", 2, 1));
        cola.insertar(paciente("PX3", 3, 2));

        p1.setPrioridad("NIVEL 5");
        assertTrue(cola.actualizar(p1));

        assertEquals(List.of("PX2", "PX3", "PX1"), ordenDeAtencion());
        assertEquals("NIVEL 5", cola.buscar("PX1").get().getPrioridad());
        assertTrue(cola.esHeapValido());
    }

    @Test
    void reevaluacionConUnObjetoNuevoTambienFunciona() {
        cola.insertar(paciente("PX1", 2, 0));
        cola.insertar(paciente("PX2", 4, 1));

        // Como llega del controlador: otro objeto con el mismo código.
        assertTrue(cola.actualizar(paciente("PX2", 1, 1)));
        assertEquals(List.of("PX2", "PX1"), ordenDeAtencion());
    }

    @Test
    void reevaluacionConservaLaLlegadaOriginalParaDesempatar() {
        Paciente p1 = paciente("PX1", 4, 0);
        cola.insertar(p1);
        cola.insertar(paciente("PX2", 2, 10));

        // Aunque el objeto actualizado traiga otra hora, cuenta la llegada original.
        Paciente actualizado = new Paciente("PX1", "Paciente PX1", T0.plusMinutes(30), 30, "NIVEL 2", "PENDIENTE");
        cola.actualizar(actualizado);

        assertEquals(List.of("PX1", "PX2"), ordenDeAtencion());
    }

    @Test
    void reevaluarUnPacienteQueNoEstaDevuelveFalseYNoAlteraLaCola() {
        cola.insertar(paciente("PX1", 3, 0));

        assertFalse(cola.actualizar(paciente("PX9", 1, 0)));
        assertEquals(1, cola.getTamaño());
        assertEquals("PX1", cola.verSiguiente().get().getCodigo());
    }

    // ---------------------------------------------------------------- eliminar

    @Test
    void eliminarPorCodigoEnCualquierPosicion() {
        for (int i = 1; i <= 10; i++) {
            cola.insertar(paciente("PX" + i, (i % 5) + 1, i));
        }

        assertTrue(cola.eliminar("PX7"));
        assertFalse(cola.contiene("PX7"));
        assertFalse(cola.eliminar("PX7"));
        assertEquals(9, cola.getTamaño());
        assertTrue(cola.esHeapValido());
    }

    @Test
    void listarEnOrdenNoModificaLaCola() {
        cola.insertar(paciente("PX1", 3, 0));
        cola.insertar(paciente("PX2", 1, 1));

        cola.listarEnOrden();

        assertEquals(2, cola.getTamaño());
        assertEquals("PX2", cola.verSiguiente().get().getCodigo());
    }

    // ------------------------------------------------------- prueba con oráculo

    /** Datos que usa el oráculo para ordenar: nivel, minuto de llegada y secuencia. */
    private record Clave(String codigo, int nivel, int minuto, long secuencia) {
    }

    @Test
    void operacionesAleatoriasCoincidenConUnOrdenamientoSimple() {
        Comparator<Clave> orden = Comparator.comparingInt(Clave::nivel)
                .thenComparingInt(Clave::minuto)
                .thenComparingLong(Clave::secuencia);
        Map<String, Clave> oraculo = new HashMap<>();
        List<String> vivos = new ArrayList<>();
        Random r = new Random(2026);
        long secuencia = 0;

        for (int i = 0; i < 3_000; i++) {
            int op = r.nextInt(4);
            if (op == 0 || vivos.isEmpty()) {
                int nivel = 1 + r.nextInt(5);
                int minuto = r.nextInt(60);
                cola.insertar(paciente("PX" + i, nivel, minuto));
                oraculo.put("PX" + i, new Clave("PX" + i, nivel, minuto, secuencia++));
                vivos.add("PX" + i);
            } else if (op == 1) {
                Clave esperada = oraculo.values().stream().min(orden).orElseThrow();
                assertEquals(esperada.codigo(), extraerCodigo());
                oraculo.remove(esperada.codigo());
                vivos.remove(esperada.codigo());
            } else if (op == 2) {
                String codigo = vivos.get(r.nextInt(vivos.size()));
                Clave vieja = oraculo.get(codigo);
                int nuevoNivel = 1 + r.nextInt(5);
                cola.actualizar(paciente(codigo, nuevoNivel, vieja.minuto()));
                oraculo.put(codigo, new Clave(codigo, nuevoNivel, vieja.minuto(), vieja.secuencia()));
            } else {
                String codigo = vivos.remove(r.nextInt(vivos.size()));
                assertTrue(cola.eliminar(codigo));
                oraculo.remove(codigo);
            }
            assertEquals(oraculo.size(), cola.getTamaño());
        }

        assertTrue(cola.esHeapValido());
        List<String> esperado = oraculo.values().stream().sorted(orden).map(Clave::codigo).toList();
        assertEquals(esperado, ordenDeAtencion());
    }
}