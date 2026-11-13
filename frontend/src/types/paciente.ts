export type Prioridad = 'NIVEL 1' | 'NIVEL 2' | 'NIVEL 3' | 'NIVEL 4' | 'NIVEL 5'
export type EstadoPaciente = 'PENDIENTE' | 'EN_ATENCION' | 'ATENDIDO' | 'CANCELADO'

export const PRIORIDADES: Prioridad[] = ['NIVEL 1', 'NIVEL 2', 'NIVEL 3', 'NIVEL 4', 'NIVEL 5']
export const ESTADOS: EstadoPaciente[] = ['PENDIENTE', 'EN_ATENCION', 'ATENDIDO', 'CANCELADO']

export interface Paciente {
  codigo: string
  nombre: string
  fechaHoraLlegada: string
  edad: number
  prioridad: Prioridad
  estado: EstadoPaciente
}

export interface PacienteRequest {
  codigo: string
  nombre: string
  fechaHoraLlegada: string
  edad: number
  prioridad: Prioridad
}
