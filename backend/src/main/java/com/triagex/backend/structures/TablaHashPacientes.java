
package com.triagex.backend.structures;

import com.triagex.backend.model.Paciente;
import java.util.Optional;

public class TablaHashPacientes {

    private static final int CAPACIDAD_INICIAL = 16;
    private static final double FACTOR_CARGA_MAXIMO = 0.75;

    private Nodo[] tabla;
    private int tamaño;

    private static class Nodo {
        private Paciente paciente;
        private Nodo siguiente;

        public Nodo(Paciente paciente) {
            this.paciente = paciente;
            this.siguiente = null;
        }
    }

    public TablaHashPacientes() {
        tabla = new Nodo[CAPACIDAD_INICIAL];
        tamaño = 0;
    }

    private int calcularIndice(String codigo) {
        return Math.floorMod(codigo.hashCode(), tabla.length);
    }

    public void insertar(Paciente paciente) {

        if (paciente == null || paciente.getCodigo() == null
                || paciente.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Paciente o código inválido");
        }

        int indice = calcularIndice(paciente.getCodigo());

        Nodo actual = tabla[indice];

        while (actual != null) {
            if (actual.paciente.getCodigo().equals(paciente.getCodigo())) {
                throw new IllegalArgumentException("El paciente ya está registrado");
            }

            actual = actual.siguiente;
        }

        if ((double) (tamaño + 1) / tabla.length > FACTOR_CARGA_MAXIMO) {
            redimensionar();
        }

        indice = calcularIndice(paciente.getCodigo());

        Nodo nuevo = new Nodo(paciente);
        nuevo.siguiente = tabla[indice];
        tabla[indice] = nuevo;

        tamaño++;
    }

    public Optional<Paciente> buscar(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }

        int indice = calcularIndice(codigo);

        Nodo actual = tabla[indice];

        while (actual != null) {

            if (actual.paciente.getCodigo().equals(codigo)) {
                return Optional.of(actual.paciente);
            }

            actual = actual.siguiente;
        }

        return Optional.empty();
    }

    public boolean eliminar(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            return false;
        }

        int indice = calcularIndice(codigo);

        Nodo actual = tabla[indice];
        Nodo anterior = null;

        while (actual != null) {

            if (actual.paciente.getCodigo().equals(codigo)) {

                if (anterior == null) {
                    tabla[indice] = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }

                tamaño--;
                return true;
            }

            anterior = actual;
            actual = actual.siguiente;
        }

        return false;
    }

    public boolean actualizar(Paciente paciente) {

        if (paciente == null || paciente.getCodigo() == null
                || paciente.getCodigo().isBlank()) {
            return false;
        }

        int indice = calcularIndice(paciente.getCodigo());

        Nodo actual = tabla[indice];

        while (actual != null) {

            if (actual.paciente.getCodigo().equals(paciente.getCodigo())) {
                actual.paciente = paciente;
                return true;
            }

            actual = actual.siguiente;
        }

        return false;
    }

    private void redimensionar() {

        Nodo[] tablaAnterior = tabla;

        tabla = new Nodo[tablaAnterior.length * 2];

        for (Nodo cabeza : tablaAnterior) {

            Nodo actual = cabeza;

            while (actual != null) {

                Nodo siguiente = actual.siguiente;

                int indice = calcularIndice(actual.paciente.getCodigo());

                actual.siguiente = tabla[indice];
                tabla[indice] = actual;

                actual = siguiente;
            }
        }
    }

    public int getTamaño() {
        return tamaño;
    }

    public int getCapacidad() {
        return tabla.length;
    }

    public boolean estaVacia() {
        return tamaño == 0;
    }


}
