package com.triagex.backend.structures;

import java.util.Comparator;

/**
 * Regla de orden explícita de la cola de atención.
 * <ol>
 *   <li>Categoría más urgente primero: NIVEL 1 antes que NIVEL 5.</li>
 *   <li>Mismo nivel: llegó antes (menor fechaHoraLlegada) primero.</li>
 *   <li>Mismo nivel y misma fecha y hora: el que entró antes a la cola (menor secuencia).</li>
 * </ol>
 * La secuencia es única, así que dos entradas distintas nunca empatan del todo
 * y el orden de atención es siempre el mismo para los mismos datos.
 */
public final class ComparadorPrioridad implements Comparator<EntradaCola> {

    public static final ComparadorPrioridad INSTANCIA = new ComparadorPrioridad();

    private ComparadorPrioridad() {
    }

    @Override
    public int compare(EntradaCola a, EntradaCola b) {
        int porNivel = Integer.compare(a.nivel(), b.nivel());
        if (porNivel != 0) {
            return porNivel;
        }

        if (a.fechaHoraLlegada() != null && b.fechaHoraLlegada() != null) {
            int porLlegada = a.fechaHoraLlegada().compareTo(b.fechaHoraLlegada());
            if (porLlegada != 0) {
                return porLlegada;
            }
        } else if (a.fechaHoraLlegada() != null) {
            return -1; // sin fecha de llegada va después
        } else if (b.fechaHoraLlegada() != null) {
            return 1;
        }

        return Long.compare(a.secuencia(), b.secuencia());
    }

    /**
     * Convierte la prioridad del modelo ("NIVEL 1" … "NIVEL 5") a su número.
     *
     * @throws IllegalArgumentException si el texto no es un nivel válido
     */
    public static int nivelNumerico(String prioridad) {
        if (prioridad == null) {
            throw new IllegalArgumentException("La prioridad es obligatoria");
        }
        String texto = prioridad.trim().toUpperCase();
        if (!texto.matches("NIVEL [1-5]")) {
            throw new IllegalArgumentException(
                    "La prioridad debe estar entre NIVEL 1 y NIVEL 5: " + prioridad);
        }
        return texto.charAt(texto.length() - 1) - '0';
    }
}