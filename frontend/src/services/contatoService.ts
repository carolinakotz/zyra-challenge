import { enviar } from './api'
import type { ContatoEntradaDTO, ContatoSaidaDTO } from '../types/contato'

// cada função corresponde a um método do ContatoResource (backend)
const CAMINHO = '/contatos'

// POST /contatos -> ContatoResource.cadastrar(dto)
export function cadastrarContato(dados: ContatoEntradaDTO): Promise<ContatoSaidaDTO> {
  return enviar<ContatoSaidaDTO>(CAMINHO, dados, 'POST')
}
