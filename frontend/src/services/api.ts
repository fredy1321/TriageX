import type { Paciente, PacienteRequest } from '../types/paciente'

const BASE_URL = '/api'

export class ApiError extends Error {
  status: number

  constructor(status: number, mensaje: string) {
    super(mensaje)
    this.status = status
  }
}

async function request<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  let respuesta: Response
  try {
    respuesta = await fetch(BASE_URL + ruta, {
      headers: { 'Content-Type': 'application/json' },
      ...opciones,
    })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. ¿Está encendido el backend?')
  }

  if (respuesta.status === 502 || respuesta.status === 503 || respuesta.status === 504) {
    throw new ApiError(respuesta.status, 'No se pudo conectar con el servidor. ¿Está encendido el backend?')
  }

  if (!respuesta.ok) {
    let mensaje = `Error ${respuesta.status}`
    try {
      const cuerpo = await respuesta.json()
      if (cuerpo?.error) mensaje = cuerpo.error
    } catch {
      if (respuesta.status === 404) mensaje = 'Paciente no encontrado'
    }
    throw new ApiError(respuesta.status, mensaje)
  }

  if (respuesta.status === 204) return undefined as T
  return respuesta.json() as Promise<T>
}

export function registrarPaciente(datos: PacienteRequest) {
  return request<Paciente>('/pacientes', { method: 'POST', body: JSON.stringify(datos) })
}

export function listarPacientes() {
  return request<Paciente[]>('/pacientes')
}

export function buscarPaciente(codigo: string) {
  return request<Paciente>(`/pacientes/${encodeURIComponent(codigo)}`)
}

export function actualizarPaciente(paciente: Paciente) {
  return request<Paciente>(`/pacientes/${encodeURIComponent(paciente.codigo)}`, {
    method: 'PUT',
    body: JSON.stringify(paciente),
  })
}

export function eliminarPaciente(codigo: string) {
  return request<void>(`/pacientes/${encodeURIComponent(codigo)}`, { method: 'DELETE' })
}

export function obtenerCola() {
  return request<Paciente[]>('/cola')
}

export function atenderSiguiente() {
  return request<Paciente>('/cola/atender', { method: 'POST' })
}

export function obtenerOrdenLlegada() {
  return request<Paciente[]>('/cola/llegada')
}
