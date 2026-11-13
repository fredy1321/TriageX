export function validarNombre(nombre: string): string {
  if (nombre.trim() === '') return 'El nombre es obligatorio'
  if (nombre.trim().length > 100) return 'El nombre no puede superar 100 caracteres'
  return ''
}

export function validarEdad(edad: string): string {
  if (edad.trim() === '') return 'La edad es obligatoria'
  const numero = Number(edad)
  if (!Number.isInteger(numero) || numero < 0 || numero > 120) {
    return 'La edad debe ser un número entero entre 0 y 120'
  }
  return ''
}

export function validarCodigo(codigo: string): string {
  if (codigo.trim() === '') return 'El código es obligatorio'
  if (!/^[A-Za-z0-9-]{1,20}$/.test(codigo.trim())) {
    return 'El código solo puede tener letras, números o guiones (máximo 20)'
  }
  return ''
}

export function formatearFecha(iso: string): string {
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return iso
  return fecha.toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' })
}

export function mensajeDeError(err: unknown): string {
  return err instanceof Error ? err.message : 'Error inesperado'
}
