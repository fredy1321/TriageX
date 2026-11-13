import { useCallback, useEffect, useState } from 'react'
import './App.css'
import RegistroPaciente from './components/RegistroPaciente'
import ListaPacientes from './components/ListaPacientes'
import BuscarPaciente from './components/BuscarPaciente'
import ColaAtencion from './components/ColaAtencion'
import { listarPacientes } from './services/api'
import type { Paciente } from './types/paciente'
import { mensajeDeError } from './utils/validaciones'

type Vista = 'registro' | 'cola' | 'buscar'

const VISTAS: { id: Vista; titulo: string }[] = [
  { id: 'registro', titulo: 'Registro' },
  { id: 'cola', titulo: 'Cola de atención' },
  { id: 'buscar', titulo: 'Buscar y editar' },
]

export default function App() {
  const [vista, setVista] = useState<Vista>('registro')
  const [pacientes, setPacientes] = useState<Paciente[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const cargarPacientes = useCallback(() => {
    return listarPacientes()
      .then((datos) => {
        setPacientes(datos)
        setError('')
      })
      .catch((err: unknown) => setError(mensajeDeError(err)))
      .finally(() => setCargando(false))
  }, [])

  useEffect(() => {
    void cargarPacientes()
  }, [cargarPacientes])

  return (
    <main className="contenedor">
      <header className="cabecera">
        <div>
          <h1>TriageX</h1>
          <p>Sistema de urgencias y triaje simulado</p>
        </div>
        <nav className="pestanas">
          {VISTAS.map((v) => (
            <button
              key={v.id}
              type="button"
              className={vista === v.id ? 'activa' : ''}
              onClick={() => setVista(v.id)}
            >
              {v.titulo}
            </button>
          ))}
        </nav>
      </header>

      {vista === 'registro' && (
        <>
          <RegistroPaciente onRegistrado={cargarPacientes} />
          <ListaPacientes pacientes={pacientes} cargando={cargando} error={error} onRecargar={cargarPacientes} />
        </>
      )}
      {vista === 'cola' && <ColaAtencion onCambio={cargarPacientes} />}
      {vista === 'buscar' && (
        <>
          <BuscarPaciente onCambio={cargarPacientes} />
          <ListaPacientes pacientes={pacientes} cargando={cargando} error={error} onRecargar={cargarPacientes} />
        </>
      )}
    </main>
  )
}
