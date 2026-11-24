const FORMATO_DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

// "2026-11-18T17:30:00Z" -> "18/11/2026, 14:30" (em Brasília)
// a API manda a data em UTC com o "Z" no fim; o navegador converte para o fuso de quem está vendo
// texto que não é data válida volta como veio
export function formatarDataHora(texto: string): string {
  const data = new Date(texto)
  return Number.isNaN(data.getTime()) ? texto : FORMATO_DATA_HORA.format(data)
}
