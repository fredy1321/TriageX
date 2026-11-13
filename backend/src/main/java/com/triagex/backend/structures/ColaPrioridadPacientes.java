package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cola de prioridad de pacientes pendientes, implementada como heap binario
 * mínimo indexado.
 * <p>
 * El heap vive en un arreglo: para la posición i, el padre está en (i - 1) / 2,
 * el hijo izquierdo en 2i + 1 y el derecho en 2i + 2. La raíz (posición 0) es
 * siempre el paciente que debe atenderse primero según {@link ComparadorPrioridad}.
 * <p>
 * Además se mantiene un índice código → posición. Con él se rechazan duplicados,
 * se encuentra a un paciente sin recorrer el heap y se reevalúa su prioridad en
 * su misma posición, sin dejar entradas viejas.
 * <p>
 * Complejidad:
 * <ul>
 *   <li>insertar, extraerSiguiente, actualizar, eliminar: O(log n).</li>
 *   <li>verSiguiente, contiene, buscar, getTamaño: O(1) (contiene y buscar, O(1) promedio).</li>
 *   <li>listarEnOrden: O(n log n); trabaja sobre una copia y no altera la cola.</li>
 * </ul>
 */
public class ColaPrioridadPacientes {

    private final List<EntradaCola> heap = new ArrayList<>();

    /**
     * Índice código → posición en el heap. Es un HashMap de Java porque guarda
     * posiciones (enteros) y no pacientes; la TablaHashPacientes del proyecto está
     * hecha para guardar pacientes.
     */
    private final Map<String, Integer> posiciones = new HashMap<>();

    /** Contador de inserciones; desempata pacientes con igual nivel y llegada. */
    private long siguienteSecuencia = 0;

    // ------------------------------------------------------------ operaciones

    /**
     * Agrega un paciente a la cola. O(log n).
     *
     * @throws IllegalArgumentException si el paciente o su código son inválidos,
     *         si la prioridad no es "NIVEL 1" … "NIVEL 5" o si el código ya está en la cola
     */
    public void insertar(Paciente paciente) {
        validar(paciente);
        if (posiciones.containsKey(paciente.getCodigo())) {
            throw new IllegalArgumentException("El paciente ya está en la cola de atención");
        }
        EntradaCola entrada = new EntradaCola(
                paciente,
                ComparadorPrioridad.nivelNumerico(paciente.getPrioridad()),
                paciente.getFechaHoraLlegada(),
                siguienteSecuencia++);
        heap.add(entrada);
        int i = heap.size() - 1;
        posiciones.put(paciente.getCodigo(), i);
        subir(i);
    }

    /** Paciente que sigue, sin sacarlo de la cola. Vacío si no hay pendientes. O(1). */
    public Optional<Paciente> verSiguiente() {
        return heap.isEmpty() ? Optional.empty() : Optional.of(heap.get(0).paciente());
    }

    /** Saca y devuelve al paciente que sigue. Vacío si no hay pendientes. O(log n). */
    public Optional<Paciente> extraerSiguiente() {
        if (heap.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(eliminarEn(0).paciente());
    }

    /**
     * Reevaluación: reemplaza los datos del paciente que tiene el mismo código y
     * lo reubica según su nueva prioridad. Conserva su llegada original, así que
     * frente a otro paciente del mismo nivel sigue contando quién llegó antes.
     * El paciente sigue apareciendo una sola vez. O(log n).
     *
     * @return true si el paciente estaba en la cola y se actualizó
     * @throws IllegalArgumentException si la nueva prioridad no es válida
     */
    public boolean actualizar(Paciente pacienteActualizado) {
        validar(pacienteActualizado);
        Integer i = posiciones.get(pacienteActualizado.getCodigo());
        if (i == null) {
            return false;
        }
        EntradaCola anterior = heap.get(i);
        EntradaCola nueva = new EntradaCola(
                pacienteActualizado,
                ComparadorPrioridad.nivelNumerico(pacienteActualizado.getPrioridad()),
                anterior.fechaHoraLlegada(),
                anterior.secuencia());
        heap.set(i, nueva);
        if (ComparadorPrioridad.INSTANCIA.compare(nueva, anterior) < 0) {
            subir(i);
        } else {
            bajar(i);
        }
        return true;
    }

    /**
     * Saca de la cola al paciente con ese código, esté donde esté (por ejemplo,
     * si se cancela su registro). O(log n).
     *
     * @return true si estaba en la cola
     */
    public boolean eliminar(String codigo) {
        Integer i = codigo == null ? null : posiciones.get(codigo);
        if (i == null) {
            return false;
        }
        eliminarEn(i);
        return true;
    }

    /** O(1) promedio. */
    public Optional<Paciente> buscar(String codigo) {
        Integer i = codigo == null ? null : posiciones.get(codigo);
        return i == null ? Optional.empty() : Optional.of(heap.get(i).paciente());
    }

    /** O(1) promedio. */
    public boolean contiene(String codigo) {
        return codigo != null && posiciones.containsKey(codigo);
    }

    /**
     * Pacientes en el orden exacto en que serían atendidos. Se extrae de una
     * copia del heap, así que la cola original no cambia. O(n log n).
     */
    public List<Paciente> listarEnOrden() {
        ColaPrioridadPacientes copia = new ColaPrioridadPacientes();
        for (EntradaCola e : heap) {
            copia.heap.add(e);
            copia.posiciones.put(e.codigo(), copia.heap.size() - 1);
        }
        // El arreglo copiado ya cumple la propiedad del heap: no hay que reordenarlo.
        List<Paciente> ordenados = new ArrayList<>(heap.size());
        while (!copia.estaVacia()) {
            ordenados.add(copia.eliminarEn(0).paciente());
        }
        return ordenados;
    }

    public void limpiar() {
        heap.clear();
        posiciones.clear();
        siguienteSecuencia = 0;
    }

    public int getTamaño() {
        return heap.size();
    }

    public boolean estaVacia() {
        return heap.isEmpty();
    }

    /**
     * Revisa que cada padre sea más prioritario que sus hijos y que el índice
     * coincida con el arreglo. Solo para pruebas. O(n).
     */
    boolean esHeapValido() {
        if (posiciones.size() != heap.size()) {
            return false;
        }
        for (int i = 0; i < heap.size(); i++) {
            Integer pos = posiciones.get(heap.get(i).codigo());
            if (pos == null || pos != i) {
                return false;
            }
            int izq = 2 * i + 1;
            int der = 2 * i + 2;
            if (izq < heap.size() && ComparadorPrioridad.INSTANCIA.compare(heap.get(izq), heap.get(i)) < 0) {
                return false;
            }
            if (der < heap.size() && ComparadorPrioridad.INSTANCIA.compare(heap.get(der), heap.get(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    // --------------------------------------------------------------- internos

    private static void validar(Paciente paciente) {
        if (paciente == null || paciente.getCodigo() == null || paciente.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Paciente o código inválido");
        }
    }

    /** Quita la entrada de la posición i: la cambia por la última y repara el heap. */
    private EntradaCola eliminarEn(int i) {
        int ultimo = heap.size() - 1;
        EntradaCola eliminada = heap.get(i);
        intercambiar(i, ultimo);
        heap.remove(ultimo);
        posiciones.remove(eliminada.codigo());
        if (i < heap.size()) {
            // La entrada que quedó en i puede tener que subir o bajar.
            subir(i);
            bajar(i);
        }
        return eliminada;
    }

    /** Sube la entrada mientras sea más prioritaria que su padre. */
    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (ComparadorPrioridad.INSTANCIA.compare(heap.get(i), heap.get(padre)) >= 0) {
                break;
            }
            intercambiar(i, padre);
            i = padre;
        }
    }

    /** Baja la entrada mientras alguno de sus hijos sea más prioritario. */
    private void bajar(int i) {
        int n = heap.size();
        while (true) {
            int izq = 2 * i + 1;
            int der = izq + 1;
            int menor = i;
            if (izq < n && ComparadorPrioridad.INSTANCIA.compare(heap.get(izq), heap.get(menor)) < 0) {
                menor = izq;
            }
            if (der < n && ComparadorPrioridad.INSTANCIA.compare(heap.get(der), heap.get(menor)) < 0) {
                menor = der;
            }
            if (menor == i) {
                return;
            }
            intercambiar(i, menor);
            i = menor;
        }
    }

    /** Intercambia dos posiciones del arreglo y actualiza el índice. */
    private void intercambiar(int a, int b) {
        if (a == b) {
            return;
        }
        EntradaCola ea = heap.get(a);
        EntradaCola eb = heap.get(b);
        heap.set(a, eb);
        heap.set(b, ea);
        posiciones.put(eb.codigo(), a);
        posiciones.put(ea.codigo(), b);
    }
}