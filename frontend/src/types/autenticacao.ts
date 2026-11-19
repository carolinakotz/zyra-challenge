// espelham os records do backend (br.com.zyra.dto)
// os nomes dos campos são o contrato JSON: não renomear

// perfis que o backend pode devolver no login
export type Perfil = 'ADMIN' | 'USUARIO'

// o que o frontend envia no POST /autenticacao/login
export interface LoginEntradaDTO {
  email: string
  senha: string
}

// o que a API devolve no login
// expiraEm vem em UTC, no formato ISO (ex.: "2026-11-18T15:00:00Z")
export interface LoginSaidaDTO {
  token: string
  perfil: Perfil
  expiraEm: string
}
