import { useState } from 'react'
import type { FormEvent } from 'react'
import { buscarPaciente, eliminarPaciente, finalizarAtencion, cancelarPaciente } from '../services/api'
import type { Paciente } from '../types/paciente'
import { formatearFecha, mensajeDeError, validarCodigo } from '../utils/validaciones'
import EditarPaciente from './EditarPaciente'

interface Props {
  onCambio: () => void
}

export default function BuscarPaciente({ onCambio }: Props) {
  const [codigo, setCodigo] = useState('')
  const [paciente, setPaciente] = useState<Paciente | null>(null)
  const [editando, setEditando] = useState(false)
  const [confirmandoEliminar, setConfirmandoEliminar] = useState(false)
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')
  const [cargando, setCargando] = useState(false)

  function limpiarMensajes() {
    setError('')
    setExito('')
  }

  async function manejarBusqueda(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    limpiarMensajes()
    setPaciente(null)
    setEditando(false)
    setConfirmandoEliminar(false)

    const mensajeValidacion = validarCodigo(codigo)
    if (mensajeValidacion) {
      setError(mensajeValidacion)
      return
    }

    setCargando(true)
    try {
      setPaciente(await buscarPaciente(codigo.trim()))
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setCargando(false)
    }
  }

  async function manejarFinalizarAtencion() {
    if (!paciente) return
    limpiarMensajes()
    setCargando(true)
    try {
      const actualizado = await finalizarAtencion(paciente.codigo)
      setPaciente(actualizado)
      setExito(`Atención finalizada para ${actualizado.codigo}`)
      onCambio()
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setCargando(false)
    }
  }

  async function manejarCancelarPaciente() {
    if (!paciente) return
    limpiarMensajes()
    setCargando(true)
    try {
      const actualizado = await cancelarPaciente(paciente.codigo)
      setPaciente(actualizado)
      setExito(`Paciente ${actualizado.codigo} cancelado`)
      onCambio()
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setCargando(false)
    }
  }

  async function manejarEliminar() {
    if (!paciente) return
    limpiarMensajes()
    setCargando(true)
    try {
      await eliminarPaciente(paciente.codigo)
      setExito(`Paciente ${paciente.codigo} eliminado`)
      setPaciente(null)
      setConfirmandoEliminar(false)
      onCambio()
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setCargando(false)
    }
  }

  function manejarGuardado(actualizado: Paciente) {
    setPaciente(actualizado)
    setEditando(false)
    setExito(`Paciente ${actualizado.codigo} actualizado`)
    onCambio()
  }

  return (
    <section className="tarjeta">
      <h2>Buscar paciente por código</h2>
      <form className="busqueda" onSubmit={manejarBusqueda} noValidate>
        <input value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="PX001" aria-label="Código del paciente" />
        <button type="submit" disabled={cargando}>
          {cargando ? 'Buscando...' : 'Buscar'}
        </button>
      </form>

      {error && <p className="mensaje error">{error}</p>}
      {exito && <p className="mensaje exito">{exito}</p>}

      {paciente && !editando && (
        <div className="detalle">
          <dl>
            <dt>Código</dt><dd>{paciente.codigo}</dd>
            <dt>Nombre</dt><dd>{paciente.nombre}</dd>
            <dt>Edad</dt><dd>{paciente.edad}</dd>
            <dt>Prioridad</dt><dd>{paciente.prioridad}</dd>
            <dt>Llegada</dt><dd>{formatearFecha(paciente.fechaHoraLlegada)}</dd>
            <dt>Estado</dt><dd>{paciente.estado}</dd>
          </dl>
          {!confirmandoEliminar ? (
            <div className="acciones">
              {paciente.estado === 'PENDIENTE' && (
                <>
                  <button type="button" onClick={() => { limpiarMensajes(); setEditando(true) }}>
                    Editar / reevaluar
                  </button>
                  <button type="button" className="peligro" onClick={manejarCancelarPaciente} disabled={cargando}>
                    Cancelar ingreso
                  </button>
                </>
              )}
              {paciente.estado === 'EN_ATENCION' && (
                <>
                  <button type="button" onClick={manejarFinalizarAtencion} disabled={cargando}>
                    Finalizar atención
                  </button>
                  <button type="button" className="peligro" onClick={manejarCancelarPaciente} disabled={cargando}>
                    Cancelar atención
                  </button>
                </>
              )}
              <button type="button" className="secundario" onClick={() => setConfirmandoEliminar(true)}>
                Eliminar registro
              </button>
            </div>
          ) : (
            <div className="acciones">
              <span>¿Eliminar a {paciente.nombre}? Esta acción no se puede deshacer.</span>
              <button type="button" className="peligro" onClick={manejarEliminar} disabled={cargando}>
                Sí, eliminar
              </button>
              <button type="button" className="secundario" onClick={() => setConfirmandoEliminar(false)}>
                Cancelar
              </button>
            </div>
          )}
        </div>
      )}

      {paciente && editando && (
        <EditarPaciente paciente={paciente} onGuardado={manejarGuardado} onCancelar={() => setEditando(false)} />
      )}
    </section>
  )
}
