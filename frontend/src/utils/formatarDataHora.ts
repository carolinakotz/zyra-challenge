const FORMATO_DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

// "2026-11-18T14:30:00" -> "18/11/2026, 14:30"
// a API manda data e hora sem fuso, então o navegador lê como horário local (o mesmo que foi gravado)
// texto que não é data válida volta como veio
export function formatarDataHora(texto: string): string {
  const data = new Date(texto)
  return Number.isNaN(data.getTime()) ? texto : FORMATO_DATA_HORA.format(data)
}
