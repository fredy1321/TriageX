import { useState } from 'react'
import type { FormEvent } from 'react'
import { registrarPaciente } from '../services/api'
import { PRIORIDADES } from '../types/paciente'
import type { Prioridad } from '../types/paciente'
import { mensajeDeError, validarCodigo, validarEdad, validarNombre } from '../utils/validaciones'

interface Props {
  onRegistrado: () => void
}

function fechaHoraActual(): string {
  const ahora = new Date()
  const local = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 19)
}

export default function RegistroPaciente({ onRegistrado }: Props) {
  const [codigo, setCodigo] = useState('')
  const [nombre, setNombre] = useState('')
  const [edad, setEdad] = useState('')
  const [prioridad, setPrioridad] = useState<Prioridad>('NIVEL 3')
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function manejarEnvio(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError('')
    setExito('')

    const mensajeValidacion = validarCodigo(codigo) || validarNombre(nombre) || validarEdad(edad)
    if (mensajeValidacion) {
      setError(mensajeValidacion)
      return
    }

    setEnviando(true)
    try {
      const paciente = await registrarPaciente({
        codigo: codigo.trim(),
        nombre: nombre.trim(),
        edad: Number(edad),
        prioridad,
        fechaHoraLlegada: fechaHoraActual(),
      })
      setExito(`Paciente ${paciente.codigo} registrado correctamente`)
      setCodigo('')
      setNombre('')
      setEdad('')
      setPrioridad('NIVEL 3')
      onRegistrado()
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="tarjeta">
      <h2>Registrar llegada</h2>
      <form className="formulario" onSubmit={manejarEnvio} noValidate>
        <label>
          Código
          <input value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="PX001" />
        </label>
        <label>
          Nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nombre completo" />
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
        <button type="submit" disabled={enviando}>
          {enviando ? 'Registrando...' : 'Registrar paciente'}
        </button>
      </form>
      {error && <p className="mensaje error">{error}</p>}
      {exito && <p className="mensaje exito">{exito}</p>}
    </section>
  )
}
