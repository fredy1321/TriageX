package com.triagex.backend.controller;

import com.triagex.backend.dto.PacienteRequestDTO;
import com.triagex.backend.model.Paciente;
import com.triagex.backend.service.ColaAtencionService;
import com.triagex.backend.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final ColaAtencionService colaAtencionService;

    public PacienteController(PacienteService pacienteService, ColaAtencionService colaAtencionService) {
        this.pacienteService = pacienteService;
        this.colaAtencionService = colaAtencionService;
    }

    @PostMapping
    public ResponseEntity<Paciente> registrarPaciente(@Valid @RequestBody PacienteRequestDTO datos) {
        Paciente paciente = new Paciente(datos.codigo(), datos.nombre(), datos.fechaHoraLlegada(),
                datos.edad(), datos.prioridad(), ColaAtencionService.PENDIENTE);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(colaAtencionService.registrarPaciente(paciente));
    }

    @GetMapping
    public ResponseEntity<List<Paciente>> listarPacientes() {
        return ResponseEntity.ok(pacienteService.listarPacientes());
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<Paciente> buscarPaciente(@PathVariable String codigo) {
        return pacienteService.buscarPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<Paciente> actualizarPaciente(@PathVariable String codigo,
                                                        @RequestBody Paciente paciente) {
        if (paciente.getCodigo() == null
                || !PacienteService.normalizarCodigo(codigo)
                .equals(PacienteService.normalizarCodigo(paciente.getCodigo()))) {
            throw new IllegalArgumentException("El código de la URL no coincide con el paciente");
        }
        return ResponseEntity.ok(colaAtencionService.actualizarPaciente(paciente));
    }

    @PostMapping("/{codigo}/cancelar")
    public ResponseEntity<Paciente> cancelarPaciente(@PathVariable String codigo) {
        return ResponseEntity.ok(colaAtencionService.cancelarPaciente(codigo));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable String codigo) {
        if (!colaAtencionService.eliminarPaciente(codigo)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
