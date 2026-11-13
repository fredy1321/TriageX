package com.triagex.backend.controller;

import com.triagex.backend.model.Paciente;
import com.triagex.backend.service.ColaAtencionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cola")
public class ColaController {

    private final ColaAtencionService colaAtencionService;

    public ColaController(ColaAtencionService colaAtencionService) {
        this.colaAtencionService = colaAtencionService;
    }

    @GetMapping
    public ResponseEntity<List<Paciente>> verColaDeAtencion() {
        return ResponseEntity.ok(colaAtencionService.listarColaDeAtencion());
    }

    @GetMapping("/llegada")
    public ResponseEntity<List<Paciente>> verOrdenDeLlegada() {
        return ResponseEntity.ok(colaAtencionService.listarOrdenDeLlegada());
    }

    /** Saca al más prioritario de pendientes y lo marca EN_ATENCION. */
    @PostMapping("/atender")
    public ResponseEntity<Paciente> atenderSiguiente() {
        return ResponseEntity.ok(colaAtencionService.atenderSiguiente());
    }

    @PostMapping("/{codigo}/finalizar")
    public ResponseEntity<Paciente> finalizarAtencion(@PathVariable String codigo) {
        return ResponseEntity.ok(colaAtencionService.finalizarAtencion(codigo));
    }
}
