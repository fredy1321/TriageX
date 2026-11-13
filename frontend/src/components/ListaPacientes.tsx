import type { Paciente } from '../types/paciente'
import { formatearFecha } from '../utils/validaciones'

interface Props {
  pacientes: Paciente[]
  cargando: boolean
  error: string
  onRecargar: () => void
}

export default function ListaPacientes({ pacientes, cargando, error, onRecargar }: Props) {
  return (
    <section className="tarjeta">
      <div className="encabezado-tarjeta">
        <h2>Pacientes registrados ({pacientes.length})</h2>
        <button type="button" className="secundario" onClick={onRecargar} disabled={cargando}>
          Actualizar
        </button>
      </div>
      {cargando && <p>Cargando...</p>}
      {error && <p className="mensaje error">{error}</p>}
      {!cargando && !error && pacientes.length === 0 && <p>No hay pacientes registrados.</p>}
      {pacientes.length > 0 && (
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr>
                <th>Código</th>
                <th>Nombre</th>
                <th>Edad</th>
                <th>Prioridad</th>
                <th>Llegada</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {pacientes.map((p) => (
                <tr key={p.codigo}>
                  <td>{p.codigo}</td>
                  <td>{p.nombre}</td>
                  <td>{p.edad}</td>
                  <td><span className={`nivel nivel-${p.prioridad.slice(-1)}`}>{p.prioridad}</span></td>
                  <td>{formatearFecha(p.fechaHoraLlegada)}</td>
                  <td>{p.estado}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
