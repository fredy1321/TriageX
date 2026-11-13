package com.triagex.backend.service;

import com.triagex.backend.exception.PacienteNoEncontradoException;
import com.triagex.backend.model.Paciente;
import com.triagex.backend.repository.PacienteRepository;
import com.triagex.backend.structures.TablaHashPacientes;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class PacienteService {

    private static final Logger log = LoggerFactory.getLogger(PacienteService.class);

    private final PacienteRepository pacienteRepository;
    private final TablaHashPacientes tablaHashPacientes;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
        this.tablaHashPacientes = new TablaHashPacientes();

        List<Paciente> pacientesGuardados = pacienteRepository.findAll();
        Set<String> codigosNormalizados = new HashSet<>();

        for (Paciente paciente : pacientesGuardados) {
            String codigoNormalizado = normalizarCodigo(paciente.getCodigo());
            if (!codigoNormalizado.equals(paciente.getCodigo())) {
                throw new IllegalStateException(
                        "Hay códigos antiguos sin normalizar en PostgreSQL. Ejecuta la migración " +
                        "backend/src/main/resources/db/manual/V2__normalizar_codigos_y_estados.sql"
                );
            }
            if (!codigosNormalizados.add(codigoNormalizado)) {
                throw new IllegalStateException("Hay códigos duplicados ignorando mayúsculas/minúsculas: "
                        + codigoNormalizado + ". Corrige esos registros antes de iniciar TriageX.");
            }
            tablaHashPacientes.insertar(paciente);
        }
    }

    public static String normalizarCodigo(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("El código es obligatorio");
        }
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT);
        if (normalizado.isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }
        return normalizado;
    }

    private static Paciente copiar(Paciente origen, String codigo, String estado) {
        return new Paciente(codigo, origen.getNombre(), origen.getFechaHoraLlegada(),
                origen.getEdad(), origen.getPrioridad(), estado);
    }

    public synchronized Paciente registrarPaciente(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente no puede ser nulo");
        }
        String codigo = normalizarCodigo(paciente.getCodigo());
        if (paciente.getNombre() == null || paciente.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (paciente.getEdad() < 0 || paciente.getEdad() > 120) {
            throw new IllegalArgumentException("La edad debe estar entre 0 y 120 años");
        }
        if (paciente.getFechaHoraLlegada() == null) {
            throw new IllegalArgumentException("La fecha de llegada es obligatoria");
        }
        if (pacienteRepository.existsByCodigoIgnoreCase(codigo) || tablaHashPacientes.buscar(codigo).isPresent()) {
            log.warn("Registro rechazado: código duplicado {}", codigo);
            throw new IllegalArgumentException("Ya existe un paciente activo con el código " + codigo);
        }

        Paciente nuevo = copiar(paciente, codigo, "PENDIENTE");
        Paciente guardado = pacienteRepository.save(nuevo);
        tablaHashPacientes.insertar(guardado);
        return guardado;
    }

    /** Solo permite editar y reevaluar pacientes que continúan pendientes. */
    public synchronized Paciente actualizarPaciente(Paciente cambios) {
        if (cambios == null) {
            throw new IllegalArgumentException("El paciente no puede ser nulo");
        }
        String codigo = normalizarCodigo(cambios.getCodigo());
        Paciente actual = tablaHashPacientes.buscar(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException("No existe un paciente con código " + codigo));

        if (!"PENDIENTE".equals(actual.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden editar o reevaluar pacientes PENDIENTE");
        }
        if (cambios.getNombre() == null || cambios.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (cambios.getEdad() < 0 || cambios.getEdad() > 120) {
            throw new IllegalArgumentException("La edad debe estar entre 0 y 120 años");
        }

        // El código, la llegada y el estado son invariantes: el cliente no puede cambiarlos.
        Paciente actualizado = new Paciente(codigo, cambios.getNombre().trim(),
                actual.getFechaHoraLlegada(), cambios.getEdad(),
                cambios.getPrioridad(), "PENDIENTE");
        Paciente guardado = pacienteRepository.save(actualizado);
        if (!tablaHashPacientes.actualizar(guardado)) {
            throw new IllegalStateException("No fue posible sincronizar la tabla hash");
        }
        return guardado;
    }

    public synchronized Paciente cambiarEstado(String codigoEntrada, String nuevoEstado) {
        String codigo = normalizarCodigo(codigoEntrada);
        Paciente actual = tablaHashPacientes.buscar(codigo)
                .orElseThrow(() -> new PacienteNoEncontradoException("No existe un paciente con código " + codigo));

        boolean transicionValida = switch (nuevoEstado) {
            case "EN_ATENCION" -> "PENDIENTE".equals(actual.getEstado());
            case "ATENDIDO" -> "EN_ATENCION".equals(actual.getEstado());
            case "CANCELADO" -> "PENDIENTE".equals(actual.getEstado()) || "EN_ATENCION".equals(actual.getEstado());
            default -> false;
        };
        if (!transicionValida) {
            throw new IllegalArgumentException("Transición de estado no permitida: "
                    + actual.getEstado() + " -> " + nuevoEstado);
        }

        Paciente cambiado = copiar(actual, codigo, nuevoEstado);
        Paciente guardado = pacienteRepository.save(cambiado);
        log.info("Estado del paciente {}: {} -> {}", codigo, actual.getEstado(), nuevoEstado);
        if (!tablaHashPacientes.actualizar(guardado)) {
            throw new IllegalStateException("No fue posible sincronizar la tabla hash");
        }
        return guardado;
    }

    public synchronized boolean eliminarPaciente(String codigoEntrada) {
        String codigo = normalizarCodigo(codigoEntrada);
        if (!pacienteRepository.existsById(codigo)) {
            return false;
        }
        pacienteRepository.deleteById(codigo);
        tablaHashPacientes.eliminar(codigo);
        return true;
    }

    public Optional<Paciente> buscarPorCodigo(String codigoEntrada) {
        if (codigoEntrada == null || codigoEntrada.isBlank()) {
            return Optional.empty();
        }
        String codigo = normalizarCodigo(codigoEntrada);
        long inicio = System.nanoTime();
        Optional<Paciente> resultado = tablaHashPacientes.buscar(codigo);
        long duracion = System.nanoTime() - inicio;
        System.out.println("Búsqueda hash | Código: " + codigo + " | Tiempo: " + duracion
                + " ns | Encontrado: " + resultado.isPresent());
        return resultado;
    }

    public List<Paciente> listarPacientes() {
        return pacienteRepository.findAll();
    }

    public boolean existePaciente(String codigo) {
        return codigo != null && !codigo.isBlank()
                && tablaHashPacientes.buscar(normalizarCodigo(codigo)).isPresent();
    }
}
