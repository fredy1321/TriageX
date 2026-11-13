package com.triagex.backend.service;

import com.triagex.backend.exception.ColaVaciaException;
import com.triagex.backend.model.Paciente;
import com.triagex.backend.structures.ColaFIFOPacientes;
import com.triagex.backend.structures.ColaPrioridadPacientes;
import com.triagex.backend.structures.ComparadorPrioridad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ColaAtencionService {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ATENDIDO = "ATENDIDO";

    private static final Logger log = LoggerFactory.getLogger(ColaAtencionService.class);

    private final PacienteService pacienteService;
    private final ColaPrioridadPacientes colaPrioridad = new ColaPrioridadPacientes();
    private final ColaFIFOPacientes colaLlegada = new ColaFIFOPacientes();

    public ColaAtencionService(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
        cargarPendientes();
    }

    private void cargarPendientes() {
        long inicio = System.nanoTime();

        List<Paciente> pendientes = pacienteService.listarPacientes().stream()
                .filter(p -> PENDIENTE.equals(p.getEstado()))
                .sorted(Comparator.comparing(
                        Paciente::getFechaHoraLlegada,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())))
                .toList();

        for (Paciente paciente : pendientes) {
            try {
                colaPrioridad.insertar(paciente);
                colaLlegada.encolar(paciente);
            } catch (IllegalArgumentException e) {
                log.warn("Paciente {} no se cargó en la cola: {}", paciente.getCodigo(), e.getMessage());
            }
        }

        log.info("[RENDIMIENTO] carga inicial de colas | pacientes={} | tiempo={} µs",
                colaPrioridad.getTamaño(), (System.nanoTime() - inicio) / 1_000);
    }

    public synchronized Paciente registrarPaciente(Paciente paciente) {
        ComparadorPrioridad.nivelNumerico(paciente.getPrioridad());

        Paciente registrado = pacienteService.registrarPaciente(paciente);

        long inicio = System.nanoTime();
        colaPrioridad.insertar(registrado);
        colaLlegada.encolar(registrado);
        long duracion = System.nanoTime() - inicio;

        int posicion = posicionEnCola(registrado.getCodigo());
        log.info("Paciente {} registrado con {} | posición en cola de atención={} de {} | posición por llegada={}",
                registrado.getCodigo(), registrado.getPrioridad(), posicion,
                colaPrioridad.getTamaño(), colaLlegada.getTamaño());
        log.info("[RENDIMIENTO] inserción en colas | pacientes en cola={} | tiempo={} ns",
                colaPrioridad.getTamaño(), duracion);

        return registrado;
    }

    public synchronized Paciente atenderSiguiente() {
        Optional<Paciente> siguiente = colaPrioridad.verSiguiente();
        if (siguiente.isEmpty()) {
            log.info("Atención rechazada: no hay pacientes en espera");
            throw new ColaVaciaException("No hay pacientes en espera");
        }

        Paciente paciente = siguiente.get();
        paciente.setEstado(ATENDIDO);
        try {
            pacienteService.actualizarPaciente(paciente);
        } catch (RuntimeException e) {
            paciente.setEstado(PENDIENTE);
            log.warn("No se pudo atender a {}: {}. Sigue en la cola", paciente.getCodigo(), e.getMessage());
            throw e;
        }

        long inicio = System.nanoTime();
        colaPrioridad.extraerSiguiente();
        colaLlegada.eliminar(paciente.getCodigo());
        long duracion = System.nanoTime() - inicio;

        String quienSigue = colaPrioridad.verSiguiente()
                .map(p -> p.getCodigo() + " (" + p.getPrioridad() + ")")
                .orElse("nadie");
        log.info("Atendido {} ({}, llegó {}) | criterio: nivel más urgente y, en empate, llegada más antigua | siguiente: {}",
                paciente.getCodigo(), paciente.getPrioridad(), paciente.getFechaHoraLlegada(), quienSigue);
        log.info("[RENDIMIENTO] extracción del siguiente | pacientes restantes={} | tiempo={} ns",
                colaPrioridad.getTamaño(), duracion);

        return paciente;
    }

    public synchronized Paciente actualizarPaciente(Paciente paciente) {
        ComparadorPrioridad.nivelNumerico(paciente.getPrioridad());

        String codigo = paciente.getCodigo();
        String prioridadAnterior = colaPrioridad.buscar(codigo)
                .map(Paciente::getPrioridad)
                .orElse(null);

        Paciente actualizado = pacienteService.actualizarPaciente(paciente);

        long inicio = System.nanoTime();
        if (PENDIENTE.equals(actualizado.getEstado())) {
            if (colaPrioridad.contiene(codigo)) {
                colaPrioridad.actualizar(actualizado);
            } else {
                colaPrioridad.insertar(actualizado);
                colaLlegada.encolar(actualizado);
                log.info("Paciente {} vuelve a la cola de atención", codigo);
            }
        } else {
            colaPrioridad.eliminar(codigo);
            colaLlegada.eliminar(codigo);
        }
        long duracion = System.nanoTime() - inicio;

        if (prioridadAnterior != null && !prioridadAnterior.equals(actualizado.getPrioridad())) {
            log.info("Reevaluación de {}: {} -> {} | nueva posición en cola={} de {}",
                    codigo, prioridadAnterior, actualizado.getPrioridad(),
                    posicionEnCola(codigo), colaPrioridad.getTamaño());
            log.info("[RENDIMIENTO] reevaluación en cola de prioridad | pacientes en cola={} | tiempo={} ns",
                    colaPrioridad.getTamaño(), duracion);
        }

        return actualizado;
    }

    public synchronized boolean eliminarPaciente(String codigo) {
        boolean eliminado = pacienteService.eliminarPaciente(codigo);
        if (eliminado) {
            colaPrioridad.eliminar(codigo);
            colaLlegada.eliminar(codigo);
            log.info("Paciente {} eliminado y retirado de las colas", codigo);
        }
        return eliminado;
    }

    public synchronized List<Paciente> listarColaDeAtencion() {
        return colaPrioridad.listarEnOrden();
    }

    public synchronized List<Paciente> listarOrdenDeLlegada() {
        return colaLlegada.listarEnOrden();
    }

    private int posicionEnCola(String codigo) {
        List<Paciente> orden = colaPrioridad.listarEnOrden();
        for (int i = 0; i < orden.size(); i++) {
            if (orden.get(i).getCodigo().equals(codigo)) {
                return i + 1;
            }
        }
        return -1;
    }
}
