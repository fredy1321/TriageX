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
    public static final String EN_ATENCION = "EN_ATENCION";
    public static final String ATENDIDO = "ATENDIDO";
    public static final String CANCELADO = "CANCELADO";

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
                .sorted(Comparator.comparing(Paciente::getFechaHoraLlegada,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())))
                .toList();

        for (Paciente paciente : pendientes) {
            colaPrioridad.insertar(paciente);
            colaLlegada.encolar(paciente);
        }
        log.info("[RENDIMIENTO] reconstrucción de colas | pacientes={} | tiempo={} µs",
                colaPrioridad.getTamaño(), (System.nanoTime() - inicio) / 1_000);
    }

    public synchronized Paciente registrarPaciente(Paciente paciente) {
        if (paciente == null) throw new IllegalArgumentException("El paciente no puede ser nulo");
        ComparadorPrioridad.nivelNumerico(paciente.getPrioridad());
        Paciente registrado = pacienteService.registrarPaciente(paciente);
        long inicio = System.nanoTime();
        colaPrioridad.insertar(registrado);
        colaLlegada.encolar(registrado);
        log.info("Paciente {} registrado | prioridad={} | posición={}/{}",
                registrado.getCodigo(), registrado.getPrioridad(),
                posicionEnCola(registrado.getCodigo()), colaPrioridad.getTamaño());
        log.info("[RENDIMIENTO] inserción en colas | tiempo={} ns", System.nanoTime() - inicio);
        return registrado;
    }

    /** Selecciona al más prioritario y lo pasa de PENDIENTE a EN_ATENCION. */
    public synchronized Paciente atenderSiguiente() {
        Paciente siguiente = colaPrioridad.verSiguiente()
                .orElseThrow(() -> new ColaVaciaException("No hay pacientes pendientes"));
        Paciente enAtencion = pacienteService.cambiarEstado(siguiente.getCodigo(), EN_ATENCION);
        colaPrioridad.eliminar(enAtencion.getCodigo());
        colaLlegada.eliminar(enAtencion.getCodigo());
        log.info("Paciente {} pasó a EN_ATENCION | prioridad={} | llegada={}",
                enAtencion.getCodigo(), enAtencion.getPrioridad(), enAtencion.getFechaHoraLlegada());
        return enAtencion;
    }

    public synchronized Paciente finalizarAtencion(String codigo) {
        Paciente atendido = pacienteService.cambiarEstado(codigo, ATENDIDO);
        log.info("Atención finalizada para paciente {}", atendido.getCodigo());
        return atendido;
    }

    public synchronized Paciente cancelarPaciente(String codigo) {
        Paciente cancelado = pacienteService.cambiarEstado(codigo, CANCELADO);
        colaPrioridad.eliminar(cancelado.getCodigo());
        colaLlegada.eliminar(cancelado.getCodigo());
        log.info("Paciente {} cancelado y retirado de las colas", cancelado.getCodigo());
        return cancelado;
    }

    /** Solo modifica datos/prioridad mientras el paciente está PENDIENTE. */
    public synchronized Paciente actualizarPaciente(Paciente paciente) {
        if (paciente == null) throw new IllegalArgumentException("El paciente no puede ser nulo");
        ComparadorPrioridad.nivelNumerico(paciente.getPrioridad());
        String codigo = PacienteService.normalizarCodigo(paciente.getCodigo());
        String prioridadAnterior = colaPrioridad.buscar(codigo)
                .map(Paciente::getPrioridad).orElse(null);

        Paciente actualizado = pacienteService.actualizarPaciente(paciente);
        long inicio = System.nanoTime();
        if (!colaPrioridad.actualizar(actualizado)) {
            throw new IllegalStateException("El paciente pendiente no estaba en la cola de prioridad");
        }
        if (!colaLlegada.actualizar(actualizado)) {
            throw new IllegalStateException("El paciente pendiente no estaba en la cola FIFO");
        }
        log.info("Paciente {} actualizado en ambas colas | prioridad {} -> {}",
                codigo, prioridadAnterior, actualizado.getPrioridad());
        log.info("[RENDIMIENTO] reevaluación en estructuras | tiempo={} ns", System.nanoTime() - inicio);
        return actualizado;
    }

    public synchronized boolean eliminarPaciente(String codigoEntrada) {
        String codigo = PacienteService.normalizarCodigo(codigoEntrada);
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
            if (orden.get(i).getCodigo().equals(codigo)) return i + 1;
        }
        return -1;
    }
}
