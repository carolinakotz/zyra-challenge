import { ehDestinoLogin } from '../types/navegacao'

// para onde ir quando não se sabe de onde a pessoa veio
const DESTINO_PADRAO = '/missoes'

// decide para onde voltar depois do login, a partir do location.state do /login.
// só aceita caminho interno ("/missoes/nova"): "//site.com" ou "https://..." levariam
// a pessoa para fora do Zyra logo depois de ela digitar a senha
export function destinoAposLogin(estado: unknown): string {
  if (!ehDestinoLogin(estado)) return DESTINO_PADRAO

  const caminho = estado.de
  const ehInterno = caminho.startsWith('/') && !caminho.startsWith('//') && !caminho.startsWith('/\\')
  // voltar para o próprio /login não faz sentido
  if (!ehInterno || caminho === '/login' || caminho.startsWith('/login?')) return DESTINO_PADRAO

  return caminho
}
