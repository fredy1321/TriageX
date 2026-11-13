
package com.triagex.backend.service;

import com.triagex.backend.model.Paciente;
import com.triagex.backend.repository.PacienteRepository;
import com.triagex.backend.structures.TablaHashPacientes;
import org.springframework.stereotype.Service;
import com.triagex.backend.exception.PacienteNoEncontradoException;

import java.util.List;
import java.util.Optional;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final TablaHashPacientes tablaHashPacientes;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
        this.tablaHashPacientes = new TablaHashPacientes();

        List<Paciente> pacientesGuardados = pacienteRepository.findAll();

        for (Paciente paciente : pacientesGuardados) {
            tablaHashPacientes.insertar(paciente);
        }
    }

    public Paciente registrarPaciente(Paciente paciente) {

        if (paciente == null) {
            throw new IllegalArgumentException("El paciente no puede ser nulo");
        }

        if (paciente.getCodigo() == null || paciente.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }

        if (paciente.getNombre() == null || paciente.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        if (paciente.getEdad() < 0) {
            throw new IllegalArgumentException("La edad no puede ser negativa");
        }

        if (pacienteRepository.existsById(paciente.getCodigo())) {
            throw new IllegalArgumentException("El paciente ya está registrado");
        }

        paciente.setEstado("PENDIENTE");

        Paciente guardado = pacienteRepository.save(paciente);

        tablaHashPacientes.insertar(guardado);

        return guardado;
    }

    public Paciente actualizarPaciente(Paciente paciente) {

        if (paciente == null || paciente.getCodigo() == null
                || paciente.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Paciente o código inválido");
        }

        if (!pacienteRepository.existsById(paciente.getCodigo())) {
            throw new PacienteNoEncontradoException(
                    "No existe un paciente con ese código"
            );
        }

        Paciente actualizado = pacienteRepository.save(paciente);

        tablaHashPacientes.actualizar(actualizado);

        return actualizado;
    }
    public boolean eliminarPaciente(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }

        if (!pacienteRepository.existsById(codigo)) {
            return false;
        }

        pacienteRepository.deleteById(codigo);
        tablaHashPacientes.eliminar(codigo);

        return true;
    }



    public Optional<Paciente> buscarPorCodigo(String codigo) {

        long inicio = System.nanoTime();

        Optional<Paciente> resultado = tablaHashPacientes.buscar(codigo);

        long fin = System.nanoTime();

        long duracion = fin - inicio;

        System.out.println(
                "Búsqueda hash | Código: " + codigo +
                        " | Tiempo: " + duracion + " ns" +
                        " | Encontrado: " + resultado.isPresent()
        );

        return resultado;
    }

    public List<Paciente> listarPacientes() {
        return pacienteRepository.findAll();
    }

    public boolean existePaciente(String codigo) {
        return tablaHashPacientes.buscar(codigo).isPresent();
    }
}
