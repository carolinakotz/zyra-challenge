import { enviar, obter, remover } from './api'
import type { ContatoEntradaDTO, ContatoSaidaDTO } from '../types/contato'

// cada função corresponde a um método do ContatoResource (backend)
const CAMINHO = '/contatos'

// POST /contatos -> ContatoResource.cadastrar(dto)
export function cadastrarContato(dados: ContatoEntradaDTO): Promise<ContatoSaidaDTO> {
  return enviar<ContatoSaidaDTO>(CAMINHO, dados, 'POST')
}

// GET /contatos?pagina=&tamanho= -> ContatoResource.listar(pagina, tamanho)
// só admin; vem da mais recente para a mais antiga
export function listarContatos(
  pagina: number,
  tamanho: number,
  signal?: AbortSignal,
): Promise<ContatoSaidaDTO[]> {
  const consulta = new URLSearchParams({ pagina: String(pagina), tamanho: String(tamanho) })
  return obter<ContatoSaidaDTO[]>(`${CAMINHO}?${consulta}`, signal)
}

// GET /contatos/{id} -> ContatoResource.buscarPorId(id), só admin
export function buscarContato(id: number, signal?: AbortSignal): Promise<ContatoSaidaDTO> {
  return obter<ContatoSaidaDTO>(`${CAMINHO}/${id}`, signal)
}

// DELETE /contatos/{id} -> ContatoResource.excluir(id), responde 204 sem corpo; só admin
export function excluirContato(id: number): Promise<void> {
  return remover(`${CAMINHO}/${id}`)
}
