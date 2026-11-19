import type { LoginSaidaDTO } from '../types/autenticacao'

// guarda a sessão do admin (token, perfil e expiraEm) no sessionStorage:
// ela some quando a aba fecha e vale no máximo até o token vencer.
//
// fica fora do React de propósito: o api.ts (que não é componente) precisa ler o token
// e limpar a sessão quando a API responde 401. Os componentes leem pelo hook useSessao,
// que se inscreve aqui e é avisado sempre que a sessão muda.
//
// o token fica visível para qualquer script da página; por isso nada de dangerouslySetInnerHTML

export type Sessao = LoginSaidaDTO

const CHAVE = 'zyra.sessao'

// quem precisa saber quando a sessão muda (o useSessao de cada componente montado)
const ouvintes = new Set<() => void>()

// guardada em memória para devolver sempre o mesmo objeto enquanto nada muda
// (o useSyncExternalStore exige isso para não renderizar em loop)
let sessaoAtual: Sessao | null = carregar()
let temporizadorExpiracao: ReturnType<typeof setTimeout> | undefined
agendarExpiracao()

// sessionStorage é unknown: confere o formato antes de usar
function ehSessao(valor: unknown): valor is Sessao {
  return (
    typeof valor === 'object' &&
    valor !== null &&
    'token' in valor &&
    typeof valor.token === 'string' &&
    valor.token !== '' &&
    'perfil' in valor &&
    (valor.perfil === 'ADMIN' || valor.perfil === 'USUARIO') &&
    'expiraEm' in valor &&
    typeof valor.expiraEm === 'string' &&
    !Number.isNaN(Date.parse(valor.expiraEm))
  )
}

function estaVencida(sessao: Sessao): boolean {
  return Date.parse(sessao.expiraEm) <= Date.now()
}

// lê do sessionStorage; sessão inválida ou vencida é descartada
function carregar(): Sessao | null {
  try {
    const texto = sessionStorage.getItem(CHAVE)
    if (texto === null) return null

    const valor: unknown = JSON.parse(texto)
    if (ehSessao(valor) && !estaVencida(valor)) return valor

    sessionStorage.removeItem(CHAVE)
  } catch {
    // storage bloqueado (modo privado, permissões) ou JSON quebrado: segue sem sessão
  }
  return null
}

// limpa a sessão sozinha no momento em que o token vence,
// para os botões de admin sumirem sem precisar de uma requisição dar 401
function agendarExpiracao() {
  clearTimeout(temporizadorExpiracao)
  if (sessaoAtual === null) return

  const restante = Date.parse(sessaoAtual.expiraEm) - Date.now()
  // o setTimeout aceita no máximo ~24,8 dias; o token dura bem menos que isso
  temporizadorExpiracao = setTimeout(limparSessao, Math.max(0, Math.min(restante, 2_147_483_647)))
}

function trocarSessao(nova: Sessao | null) {
  sessaoAtual = nova
  agendarExpiracao()
  ouvintes.forEach((avisar) => avisar())
}

// leitura sem efeito colateral, para o useSyncExternalStore (o React chama a cada renderização);
// a sessão vencida já é limpa pelo temporizador da expiração
export function sessaoEmMemoria(): Sessao | null {
  return sessaoAtual
}

// sessão atual (ou null) para quem vai usar o token (api.ts); confere a validade antes de devolver
export function lerSessao(): Sessao | null {
  if (sessaoAtual !== null && estaVencida(sessaoAtual)) {
    limparSessao()
  }
  return sessaoAtual
}

export function salvarSessao(sessao: Sessao) {
  try {
    sessionStorage.setItem(CHAVE, JSON.stringify(sessao))
  } catch {
    // sem storage, a sessão vale só enquanto a página estiver aberta (fica em memória)
  }
  trocarSessao(sessao)
}

export function limparSessao() {
  try {
    sessionStorage.removeItem(CHAVE)
  } catch {
    // nada a fazer: a sessão em memória é limpa abaixo
  }
  if (sessaoAtual !== null) {
    trocarSessao(null)
  }
}

// usado pelo useSyncExternalStore; devolve a função que cancela a inscrição
export function ouvirSessao(avisar: () => void): () => void {
  ouvintes.add(avisar)
  return () => {
    ouvintes.delete(avisar)
  }
}
