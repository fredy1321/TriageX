package com.triagex.backend.service;

import com.triagex.backend.exception.ColaVaciaException;
import com.triagex.backend.model.Paciente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ColaAtencionServiceTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 13, 8, 0);

    @Mock
    private PacienteService pacienteService;

    private ColaAtencionService crearServicio(Paciente... guardados) {
        Map<String, Paciente> almacen = new LinkedHashMap<>();
        for (Paciente paciente : guardados) almacen.put(paciente.getCodigo(), paciente);

        lenient().when(pacienteService.listarPacientes())
                .thenAnswer(invocacion -> new ArrayList<>(almacen.values()));
        lenient().when(pacienteService.registrarPaciente(any(Paciente.class)))
                .thenAnswer(invocacion -> {
                    Paciente paciente = invocacion.getArgument(0);
                    almacen.put(paciente.getCodigo(), paciente);
                    return paciente;
                });
        lenient().when(pacienteService.actualizarPaciente(any(Paciente.class)))
                .thenAnswer(invocacion -> {
                    Paciente paciente = invocacion.getArgument(0);
                    almacen.put(paciente.getCodigo(), paciente);
                    return paciente;
                });
        lenient().when(pacienteService.cambiarEstado(anyString(), anyString()))
                .thenAnswer(invocacion -> {
                    Paciente paciente = almacen.get(invocacion.getArgument(0));
                    if (paciente == null) throw new IllegalArgumentException("No existe");
                    paciente.setEstado(invocacion.getArgument(1));
                    return paciente;
                });
        return new ColaAtencionService(pacienteService);
    }

    private Paciente paciente(String codigo, int nivel, int minuto, String estado) {
        return new Paciente(codigo, "Paciente " + codigo, T0.plusMinutes(minuto), 30,
                "NIVEL " + nivel, estado);
    }

    private Paciente pendiente(String codigo, int nivel, int minuto) {
        return paciente(codigo, nivel, minuto, "PENDIENTE");
    }

    private Paciente copiaCon(Paciente original, String prioridad, String estado) {
        return new Paciente(original.getCodigo(), original.getNombre(), original.getFechaHoraLlegada(),
                original.getEdad(), prioridad, estado);
    }

    private List<String> codigos(List<Paciente> pacientes) {
        return pacientes.stream().map(Paciente::getCodigo).toList();
    }

    @Test
    void alIniciarCargaSoloLosPendientesEnSuOrden() {
        ColaAtencionService servicio = crearServicio(
                pendiente("PX1", 3, 0),
                pendiente("PX2", 1, 5),
                paciente("PX3", 1, 1, "ATENDIDO")
        );

        assertEquals(List.of("PX2", "PX1"), codigos(servicio.listarColaDeAtencion()));
        assertEquals(List.of("PX1", "PX2"), codigos(servicio.listarOrdenDeLlegada()));
    }

    @Test
    void registrarAgregaElPacienteAAmbasColas() {
        ColaAtencionService servicio = crearServicio();

        servicio.registrarPaciente(pendiente("PX1", 3, 0));
        servicio.registrarPaciente(pendiente("PX2", 1, 1));
        servicio.registrarPaciente(pendiente("PX3", 3, 2));

        assertEquals(List.of("PX2", "PX1", "PX3"), codigos(servicio.listarColaDeAtencion()));
        assertEquals(List.of("PX1", "PX2", "PX3"), codigos(servicio.listarOrdenDeLlegada()));
    }

    @Test
    void registroRechazadoNoEntraALasColas() {
        ColaAtencionService servicio = crearServicio();
        when(pacienteService.registrarPaciente(any(Paciente.class)))
                .thenThrow(new IllegalArgumentException("El paciente ya está registrado"));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarPaciente(pendiente("PX1", 3, 0)));

        assertTrue(servicio.listarColaDeAtencion().isEmpty());
        assertTrue(servicio.listarOrdenDeLlegada().isEmpty());
    }

    @Test
    void registrarConPrioridadInvalidaNoGuarda() {
        ColaAtencionService servicio = crearServicio();
        Paciente invalido = pendiente("PX1", 3, 0);
        invalido.setPrioridad("URGENTE");

        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPaciente(invalido));
        verify(pacienteService, never()).registrarPaciente(any());
    }

    @Test
    void atenderSacaAlMasUrgenteYLoMarcaEnAtencion() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 3, 0));
        Paciente urgente = servicio.registrarPaciente(pendiente("PX2", 1, 1));
        servicio.registrarPaciente(pendiente("PX3", 3, 2));

        Paciente atendido = servicio.atenderSiguiente();

        assertEquals("PX2", atendido.getCodigo());
        assertEquals("EN_ATENCION", atendido.getEstado());
        verify(pacienteService).cambiarEstado("PX2", "EN_ATENCION");
        assertEquals(List.of("PX1", "PX3"), codigos(servicio.listarColaDeAtencion()));
        assertEquals(List.of("PX1", "PX3"), codigos(servicio.listarOrdenDeLlegada()));
    }

    @Test
    void empateDeNivelSeAtiendeQuienLlegoAntes() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 2, 5));
        servicio.registrarPaciente(pendiente("PX2", 2, 1));

        assertEquals("PX2", servicio.atenderSiguiente().getCodigo());
        assertEquals("PX1", servicio.atenderSiguiente().getCodigo());
    }

    @Test
    void atenderConColaVaciaLanzaExcepcionSinGuardarNada() {
        ColaAtencionService servicio = crearServicio();

        assertThrows(ColaVaciaException.class, servicio::atenderSiguiente);
        verify(pacienteService, never()).actualizarPaciente(any());
    }

    @Test
    void siFallaElGuardadoAlAtenderElPacienteSigueEnLaCola() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 1, 0));
        when(pacienteService.cambiarEstado("PX1", "EN_ATENCION"))
                .thenThrow(new RuntimeException("Base de datos no disponible"));

        assertThrows(RuntimeException.class, servicio::atenderSiguiente);

        List<Paciente> cola = servicio.listarColaDeAtencion();
        assertEquals(List.of("PX1"), codigos(cola));
        assertEquals("PENDIENTE", cola.get(0).getEstado());
        assertEquals(List.of("PX1"), codigos(servicio.listarOrdenDeLlegada()));
    }

    @Test
    void reevaluarAMasUrgenteReordenaLaColaSinCambiarElOrdenDeLlegada() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 3, 0));
        Paciente px2 = servicio.registrarPaciente(pendiente("PX2", 3, 1));
        servicio.registrarPaciente(pendiente("PX3", 2, 2));
        assertEquals(List.of("PX3", "PX1", "PX2"), codigos(servicio.listarColaDeAtencion()));

        servicio.actualizarPaciente(copiaCon(px2, "NIVEL 1", "PENDIENTE"));

        assertEquals(List.of("PX2", "PX3", "PX1"), codigos(servicio.listarColaDeAtencion()));
        assertEquals(List.of("PX1", "PX2", "PX3"), codigos(servicio.listarOrdenDeLlegada()));
    }

    @Test
    void reevaluacionConservaLaLlegadaOriginalParaDesempatar() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 1, 0));
        Paciente px2 = servicio.registrarPaciente(pendiente("PX2", 3, 1));

        servicio.actualizarPaciente(copiaCon(px2, "NIVEL 1", "PENDIENTE"));

        assertEquals(List.of("PX1", "PX2"), codigos(servicio.listarColaDeAtencion()));
    }

    @Test
    void reevaluarConPrioridadInvalidaNoGuardaNiCambiaLaCola() {
        ColaAtencionService servicio = crearServicio();
        Paciente px1 = servicio.registrarPaciente(pendiente("PX1", 2, 0));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarPaciente(copiaCon(px1, "NIVEL 9", "PENDIENTE")));

        verify(pacienteService, never()).actualizarPaciente(any());
        assertEquals("NIVEL 2", servicio.listarColaDeAtencion().get(0).getPrioridad());
    }

    @Test
    void finalizarAtencionCambiaEstadoSinReingresarALasColas() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 2, 0));
        servicio.atenderSiguiente();

        Paciente finalizado = servicio.finalizarAtencion("PX1");

        assertEquals("ATENDIDO", finalizado.getEstado());
        assertTrue(servicio.listarColaDeAtencion().isEmpty());
        assertTrue(servicio.listarOrdenDeLlegada().isEmpty());
    }

    @Test
    void cancelarPacienteLoMarcaCanceladoYLoRetiraDeLasColas() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 2, 0));

        Paciente cancelado = servicio.cancelarPaciente("PX1");

        assertEquals("CANCELADO", cancelado.getEstado());
        assertTrue(servicio.listarColaDeAtencion().isEmpty());
        assertTrue(servicio.listarOrdenDeLlegada().isEmpty());
    }

    @Test
    void eliminarPacienteLoRetiraDeAmbasColas() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 2, 0));
        servicio.registrarPaciente(pendiente("PX2", 1, 1));
        when(pacienteService.eliminarPaciente("PX1")).thenReturn(true);

        assertTrue(servicio.eliminarPaciente("PX1"));

        assertEquals(List.of("PX2"), codigos(servicio.listarColaDeAtencion()));
        assertEquals(List.of("PX2"), codigos(servicio.listarOrdenDeLlegada()));
    }

    @Test
    void eliminarPacienteInexistenteNoAlteraLasColas() {
        ColaAtencionService servicio = crearServicio();
        servicio.registrarPaciente(pendiente("PX1", 2, 0));
        when(pacienteService.eliminarPaciente("PX9")).thenReturn(false);

        assertFalse(servicio.eliminarPaciente("PX9"));

        assertEquals(List.of("PX1"), codigos(servicio.listarColaDeAtencion()));
    }
}
