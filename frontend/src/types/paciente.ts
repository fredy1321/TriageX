export type Prioridad = 'NIVEL 1' | 'NIVEL 2' | 'NIVEL 3' | 'NIVEL 4' | 'NIVEL 5'

export const PRIORIDADES: Prioridad[] = ['NIVEL 1', 'NIVEL 2', 'NIVEL 3', 'NIVEL 4', 'NIVEL 5']

export const ESTADOS = ['PENDIENTE', 'ATENDIDO'] as const

export interface Paciente {
  codigo: string
  nombre: string
  fechaHoraLlegada: string
  edad: number
  prioridad: Prioridad
  estado: string
}

export interface PacienteRequest {
  codigo: string
  nombre: string
  fechaHoraLlegada: string
  edad: number
  prioridad: Prioridad
}
