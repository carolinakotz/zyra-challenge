import { enviar } from './api'
import type { LoginEntradaDTO, LoginSaidaDTO } from '../types/autenticacao'

// cada função corresponde a um método do AutenticacaoResource (backend)
const CAMINHO = '/autenticacao'

// POST /autenticacao/login -> AutenticacaoResource.fazerLogin(dto)
// e-mail ou senha errados: a API responde 401 com "E-mail ou senha inválidos."
export function fazerLogin(dados: LoginEntradaDTO): Promise<LoginSaidaDTO> {
  return enviar<LoginSaidaDTO>(`${CAMINHO}/login`, dados, 'POST')
}
