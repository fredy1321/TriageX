
package com.triagex.backend.model;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "pacientes")
public class Paciente {

    @Id
    @Column(nullable = false, unique = true)
    private String codigo;
    private String nombre;
    private LocalDateTime fechaHoraLlegada;
    private int edad;
    private String prioridad;
    @Column(nullable = false)
    private String estado;

    // Constructor vacío
    public Paciente() {
    }

    // Constructor con todos los atributos
    public Paciente(String codigo, String nombre,
                    LocalDateTime fechaHoraLlegada, int edad,
                    String prioridad, String estado) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.fechaHoraLlegada = fechaHoraLlegada;
        this.edad = edad;
        this.prioridad = prioridad;
        this.estado = estado;
    }

    // Getters
    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDateTime getFechaHoraLlegada() {
        return fechaHoraLlegada;
    }

    public int getEdad() {
        return edad;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public String getEstado() {
        return estado;
    }

    // Setters
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setFechaHoraLlegada(LocalDateTime fechaHoraLlegada) {
        this.fechaHoraLlegada = fechaHoraLlegada;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Representación en texto del paciente
    @Override
    public String toString() {
        return "Paciente{" +
                "codigo='" + codigo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", fechaHoraLlegada=" + fechaHoraLlegada +
                ", edad=" + edad +
                ", prioridad='" + prioridad + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }

    // Dos pacientes son iguales si tienen el mismo código
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paciente paciente)) return false;
        return Objects.equals(codigo, paciente.codigo);
    }

    // El código determina el hash del paciente
    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
