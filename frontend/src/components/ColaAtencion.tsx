import { useCallback, useEffect, useState } from 'react'
import { ApiError, atenderSiguiente, obtenerCola, obtenerOrdenLlegada } from '../services/api'
import type { Paciente } from '../types/paciente'
import { formatearFecha, mensajeDeError } from '../utils/validaciones'

interface Props {
  onCambio: () => void
}

function mensajeCola(err: unknown): string {
  if (err instanceof ApiError && (err.status === 404 || err.status === 405)) {
    return 'El módulo de cola de atención todavía no está disponible en el backend.'
  }
  return mensajeDeError(err)
}

export default function ColaAtencion({ onCambio }: Props) {
  const [cola, setCola] = useState<Paciente[]>([])
  const [llegada, setLlegada] = useState<Paciente[]>([])
  const [atendido, setAtendido] = useState<Paciente | null>(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(true)
  const [atendiendo, setAtendiendo] = useState(false)

  const cargarCola = useCallback(() => {
    return Promise.all([obtenerCola(), obtenerOrdenLlegada()])
      .then(([datosCola, datosLlegada]) => {
        setCola(datosCola)
        setLlegada(datosLlegada)
        setError('')
      })
      .catch((err: unknown) => setError(mensajeCola(err)))
      .finally(() => setCargando(false))
  }, [])

  useEffect(() => {
    void cargarCola()
  }, [cargarCola])

  async function manejarAtender() {
    setAtendiendo(true)
    setError('')
    try {
      const paciente = await atenderSiguiente()
      setAtendido(paciente)
      await cargarCola()
      onCambio()
    } catch (err) {
      setError(mensajeCola(err))
    } finally {
      setAtendiendo(false)
    }
  }

  return (
    <section className="tarjeta">
      <div className="encabezado-tarjeta">
        <h2>Cola de atención</h2>
        <div className="acciones">
          <button type="button" className="secundario" onClick={() => void cargarCola()} disabled={cargando}>
            Actualizar
          </button>
          <button type="button" onClick={manejarAtender} disabled={atendiendo || cola.length === 0}>
            {atendiendo ? 'Atendiendo...' : 'Atender siguiente'}
          </button>
        </div>
      </div>

      {atendido && (
        <p className="mensaje exito">
          Atendido: {atendido.codigo} · {atendido.nombre} ({atendido.prioridad})
        </p>
      )}
      {error && <p className="mensaje error">{error}</p>}
      {cargando && <p>Cargando...</p>}
      {!cargando && !error && cola.length === 0 && <p>No hay pacientes en espera.</p>}

      {cola.length > 0 && (
        <ol className="cola">
          {cola.map((p, i) => (
            <li key={p.codigo} className={i === 0 ? 'siguiente' : ''}>
              <span className="posicion">{i + 1}</span>
              <span className={`nivel nivel-${p.prioridad.slice(-1)}`}>{p.prioridad}</span>
              <span className="nombre">{p.codigo} · {p.nombre}</span>
              <span className="llegada">{formatearFecha(p.fechaHoraLlegada)}</span>
            </li>
          ))}
        </ol>
      )}

      {llegada.length > 0 && (
        <details className="orden-llegada">
          <summary>Orden de llegada ({llegada.length})</summary>
          <ol>
            {llegada.map((p) => (
              <li key={p.codigo}>
                {p.codigo} · {p.nombre} · {formatearFecha(p.fechaHoraLlegada)} · {p.prioridad}
              </li>
            ))}
          </ol>
        </details>
      )}
    </section>
  )
}
