package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cola FIFO enlazada que conserva el orden de llegada de los pacientes
 * (ingreso / pretriaje), sin importar la prioridad que se les asigne después.
 * <p>
 * Implementación: nodos simplemente enlazados con referencias al frente
 * (por donde se sale) y al final (por donde se entra).
 * <p>
 * Complejidad:
 * <ul>
 *   <li>encolar, desencolar, verFrente, getTamaño, estaVacia: O(1).</li>
 *   <li>listarEnOrden, contiene, eliminar: O(n), porque hay que recorrer la lista.</li>
 * </ul>
 */
public class ColaFIFOPacientes {

    private static class Nodo {
        private Paciente paciente;
        private Nodo siguiente;

        Nodo(Paciente paciente) {
            this.paciente = paciente;
        }
    }

    private Nodo frente;
    private Nodo fin;
    private int tamaño;

    /** Agrega el paciente al final de la cola. O(1). */
    public void encolar(Paciente paciente) {
        if (paciente == null || paciente.getCodigo() == null || paciente.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Paciente o código inválido");
        }
        Nodo nuevo = new Nodo(paciente);
        if (fin == null) {
            frente = nuevo;
        } else {
            fin.siguiente = nuevo;
        }
        fin = nuevo;
        tamaño++;
    }

    /** Retira y devuelve el paciente que llegó primero. Vacío si no hay nadie. O(1). */
    public Optional<Paciente> desencolar() {
        if (frente == null) {
            return Optional.empty();
        }
        Paciente paciente = frente.paciente;
        frente = frente.siguiente;
        if (frente == null) {
            fin = null;
        }
        tamaño--;
        return Optional.of(paciente);
    }

    /** Devuelve el paciente que llegó primero sin retirarlo. O(1). */
    public Optional<Paciente> verFrente() {
        return frente == null ? Optional.empty() : Optional.of(frente.paciente);
    }

    /**
     * Retira de la cola al paciente con ese código, esté donde esté (por ejemplo,
     * cuando se atiende o se cancela). O(n).
     *
     * @return true si estaba en la cola
     */
    public boolean eliminar(String codigo) {
        if (codigo == null) {
            return false;
        }
        Nodo anterior = null;
        Nodo actual = frente;
        while (actual != null) {
            if (actual.paciente.getCodigo().equals(codigo)) {
                if (anterior == null) {
                    frente = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                if (actual == fin) {
                    fin = anterior;
                }
                tamaño--;
                return true;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    /** Reemplaza los datos del paciente conservando su posición de llegada. O(n). */
    public boolean actualizar(Paciente pacienteActualizado) {
        if (pacienteActualizado == null || pacienteActualizado.getCodigo() == null) {
            return false;
        }
        for (Nodo n = frente; n != null; n = n.siguiente) {
            if (n.paciente.getCodigo().equals(pacienteActualizado.getCodigo())) {
                n.paciente = pacienteActualizado;
                return true;
            }
        }
        return false;
    }

    /** O(n). */
    public boolean contiene(String codigo) {
        for (Nodo n = frente; n != null; n = n.siguiente) {
            if (n.paciente.getCodigo().equals(codigo)) {
                return true;
            }
        }
        return false;
    }

    /** Copia de los pacientes en orden de llegada (frente → final). O(n). */
    public List<Paciente> listarEnOrden() {
        List<Paciente> lista = new ArrayList<>(tamaño);
        for (Nodo n = frente; n != null; n = n.siguiente) {
            lista.add(n.paciente);
        }
        return lista;
    }

    public void limpiar() {
        frente = null;
        fin = null;
        tamaño = 0;
    }

    public int getTamaño() {
        return tamaño;
    }

    public boolean estaVacia() {
        return tamaño == 0;
    }
}