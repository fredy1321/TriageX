package com.triagex.backend.service;

import com.triagex.backend.exception.PacienteNoEncontradoException;
import com.triagex.backend.model.Paciente;
import com.triagex.backend.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    private PacienteService pacienteService;

    @BeforeEach
    void setUp() {
        pacienteService = new PacienteService(pacienteRepository);
    }

    private Paciente crearPaciente(String codigo, String nombre, int edad) {
        return new Paciente(
                codigo,
                nombre,
                LocalDateTime.of(2026, 11, 12, 10, 0),
                edad,
                "NIVEL 3",
                null
        );
    }

    private void simularGuardado() {
        when(pacienteRepository.save(any(Paciente.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registrarPacienteValidoLoGuardaComoPendienteYQuedaEnLaTablaHash() {
        simularGuardado();
        when(pacienteRepository.existsByCodigoIgnoreCase("PX001")).thenReturn(false);

        Paciente registrado = pacienteService.registrarPaciente(crearPaciente("PX001", "Carlos", 25));

        assertEquals("PENDIENTE", registrado.getEstado());
        verify(pacienteRepository).save(registrado);
        assertTrue(pacienteService.buscarPorCodigo("PX001").isPresent());
    }

    @Test
    void registrarPacienteDuplicadoLanzaExcepcionYNoGuarda() {
        when(pacienteRepository.existsByCodigoIgnoreCase("PX001")).thenReturn(true);
        Paciente paciente = crearPaciente("PX001", "Carlos", 25);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> pacienteService.registrarPaciente(paciente)
        );

        assertTrue(error.getMessage().contains("Ya existe un paciente activo"));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void normalizaCodigoAntesDeGuardarYBuscar() {
        simularGuardado();
        when(pacienteRepository.existsByCodigoIgnoreCase("PX020")).thenReturn(false);

        Paciente registrado = pacienteService.registrarPaciente(
                crearPaciente("  px020  ", "Prueba", 20));

        assertEquals("PX020", registrado.getCodigo());
        assertTrue(pacienteService.buscarPorCodigo(" px020 ").isPresent());
    }

    @Test
    void tablaHashImpideDuplicadoConDiferenteCapitalizacion() {
        simularGuardado();
        when(pacienteRepository.existsByCodigoIgnoreCase("PX021")).thenReturn(false);
        pacienteService.registrarPaciente(crearPaciente("PX021", "Original", 20));

        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.registrarPaciente(crearPaciente("px021", "Duplicado", 21)));
        verify(pacienteRepository, times(1)).save(any(Paciente.class));
    }

    @Test
    void registrarPacienteNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> pacienteService.registrarPaciente(null));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void registrarPacienteSinCodigoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.registrarPaciente(crearPaciente(null, "Carlos", 25)));
        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.registrarPaciente(crearPaciente("   ", "Carlos", 25)));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void registrarPacienteSinNombreLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.registrarPaciente(crearPaciente("PX001", "", 25)));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void registrarPacienteConEdadNegativaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.registrarPaciente(crearPaciente("PX001", "Carlos", -1)));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void registrarPacienteConEdadCeroEsValido() {
        simularGuardado();
        when(pacienteRepository.existsByCodigoIgnoreCase("PX002")).thenReturn(false);

        Paciente registrado = pacienteService.registrarPaciente(crearPaciente("PX002", "Bebé", 0));

        assertEquals(0, registrado.getEdad());
    }

    @Test
    void actualizarPacienteExistenteActualizaLaTablaHash() {
        simularGuardado();
        when(pacienteRepository.existsByCodigoIgnoreCase("PX001")).thenReturn(false);
        pacienteService.registrarPaciente(crearPaciente("PX001", "Carlos", 25));

        Paciente cambios = crearPaciente("PX001", "Carlos Andrés", 26);
        cambios.setPrioridad("NIVEL 1");
        pacienteService.actualizarPaciente(cambios);

        Paciente encontrado = pacienteService.buscarPorCodigo("PX001").orElseThrow();
        assertEquals("Carlos Andrés", encontrado.getNombre());
        assertEquals("NIVEL 1", encontrado.getPrioridad());
    }

    @Test
    void actualizarPacienteInexistenteLanzaPacienteNoEncontrado() {
        Paciente paciente = crearPaciente("PX999", "Nadie", 30);

        assertThrows(PacienteNoEncontradoException.class,
                () -> pacienteService.actualizarPaciente(paciente));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void actualizarPacienteSinCodigoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.actualizarPaciente(crearPaciente(null, "Carlos", 25)));
        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.actualizarPaciente(null));
    }

    @Test
    void soloPermiteTransicionesValidasDeEstado() {
        simularGuardado();
        when(pacienteRepository.existsByCodigoIgnoreCase("PX030")).thenReturn(false);
        pacienteService.registrarPaciente(crearPaciente("PX030", "Paciente", 30));

        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.cambiarEstado("PX030", "ATENDIDO"));
        assertEquals("EN_ATENCION",
                pacienteService.cambiarEstado("PX030", "EN_ATENCION").getEstado());
        assertEquals("ATENDIDO",
                pacienteService.cambiarEstado("PX030", "ATENDIDO").getEstado());
        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.cambiarEstado("PX030", "CANCELADO"));
    }

    @Test
    void eliminarPacienteExistenteLoQuitaDeLaBaseYDeLaTablaHash() {
        simularGuardado();
        when(pacienteRepository.existsByCodigoIgnoreCase("PX001")).thenReturn(false);
        when(pacienteRepository.existsById("PX001")).thenReturn(true);
        pacienteService.registrarPaciente(crearPaciente("PX001", "Carlos", 25));

        assertTrue(pacienteService.eliminarPaciente("PX001"));

        verify(pacienteRepository).deleteById("PX001");
        assertFalse(pacienteService.buscarPorCodigo("PX001").isPresent());
    }

    @Test
    void eliminarPacienteInexistenteRetornaFalseSinBorrar() {
        when(pacienteRepository.existsById("PX999")).thenReturn(false);

        assertFalse(pacienteService.eliminarPaciente("PX999"));
        verify(pacienteRepository, never()).deleteById(any());
    }

    @Test
    void eliminarConCodigoVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> pacienteService.eliminarPaciente(" "));
        assertThrows(IllegalArgumentException.class, () -> pacienteService.eliminarPaciente(null));
    }

    @Test
    void alIniciarCargaLosPacientesGuardadosEnLaTablaHash() {
        when(pacienteRepository.findAll()).thenReturn(List.of(
                crearPaciente("PX010", "Ana", 40),
                crearPaciente("PX011", "Luis", 50)
        ));

        PacienteService servicioNuevo = new PacienteService(pacienteRepository);

        assertTrue(servicioNuevo.existePaciente("PX010"));
        assertTrue(servicioNuevo.existePaciente("PX011"));
        assertFalse(servicioNuevo.existePaciente("PX012"));
    }

    @Test
    void buscarCodigoInexistenteONuloRetornaVacio() {
        assertEquals(Optional.empty(), pacienteService.buscarPorCodigo("PX999"));
        assertEquals(Optional.empty(), pacienteService.buscarPorCodigo(null));
        assertEquals(Optional.empty(), pacienteService.buscarPorCodigo(""));
    }
}
