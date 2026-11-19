import { useSyncExternalStore } from 'react'
import type { LoginSaidaDTO } from '../types/autenticacao'
import { limparSessao, ouvirSessao, salvarSessao, sessaoEmMemoria, type Sessao } from '../utils/sessaoArmazenada'

export interface ResultadoSessao {
  sessao: Sessao | null
  // só serve para mostrar ou esconder telas e botões; quem protege de verdade é o backend
  ehAdmin: boolean
  entrar: (saida: LoginSaidaDTO) => void
  sair: () => void
}

// sessão do admin para os componentes; re-renderiza sozinho quando a sessão muda
// (login, "Sair", token vencido ou 401 da API)
export function useSessao(): ResultadoSessao {
  const sessao = useSyncExternalStore(ouvirSessao, sessaoEmMemoria)

  return {
    sessao,
    ehAdmin: sessao?.perfil === 'ADMIN',
    entrar: salvarSessao,
    sair: limparSessao,
  }
}
