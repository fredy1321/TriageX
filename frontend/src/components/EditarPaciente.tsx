import { useState } from 'react'
import type { FormEvent } from 'react'
import { actualizarPaciente } from '../services/api'
import { ESTADOS, PRIORIDADES } from '../types/paciente'
import type { Paciente, Prioridad } from '../types/paciente'
import { mensajeDeError, validarEdad, validarNombre } from '../utils/validaciones'

interface Props {
  paciente: Paciente
  onGuardado: (paciente: Paciente) => void
  onCancelar: () => void
}

export default function EditarPaciente({ paciente, onGuardado, onCancelar }: Props) {
  const [nombre, setNombre] = useState(paciente.nombre)
  const [edad, setEdad] = useState(String(paciente.edad))
  const [prioridad, setPrioridad] = useState<Prioridad>(paciente.prioridad)
  const [estado, setEstado] = useState(paciente.estado)
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  async function manejarEnvio(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError('')

    const mensajeValidacion = validarNombre(nombre) || validarEdad(edad)
    if (mensajeValidacion) {
      setError(mensajeValidacion)
      return
    }

    setGuardando(true)
    try {
      const actualizado = await actualizarPaciente({
        ...paciente,
        nombre: nombre.trim(),
        edad: Number(edad),
        prioridad,
        estado,
      })
      onGuardado(actualizado)
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="edicion">
      <h3>Editar paciente {paciente.codigo}</h3>
      <form className="formulario" onSubmit={manejarEnvio} noValidate>
        <label>
          Nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} />
        </label>
        <label>
          Edad
          <input type="number" min={0} max={120} value={edad} onChange={(e) => setEdad(e.target.value)} />
        </label>
        <label>
          Prioridad
          <select value={prioridad} onChange={(e) => setPrioridad(e.target.value as Prioridad)}>
            {PRIORIDADES.map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </label>
        <label>
          Estado
          <select value={estado} onChange={(e) => setEstado(e.target.value)}>
            {ESTADOS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </label>
        <div className="acciones">
          <button type="submit" disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar'}
          </button>
          <button type="button" className="secundario" onClick={onCancelar} disabled={guardando}>
            Cancelar
          </button>
        </div>
      </form>
      {error && <p className="mensaje error">{error}</p>}
    </div>
  )
}
