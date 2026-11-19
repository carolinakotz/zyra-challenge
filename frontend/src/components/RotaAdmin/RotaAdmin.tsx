import { Navigate, Outlet, useLocation } from 'react-router'
import { useSessao } from '../../hooks/useSessao'
import type { DestinoLogin } from '../../types/navegacao'

// envolve as rotas de admin (cadastro e edição de missão, mensagens de contato) no main.tsx.
// sem sessão de admin, manda para o /login guardando a página pedida, para voltar depois.
// é só experiência do usuário: quem protege de verdade é o backend (401/403)
function RotaAdmin() {
  const { ehAdmin } = useSessao()
  const location = useLocation()

  if (!ehAdmin) {
    const destino: DestinoLogin = { de: `${location.pathname}${location.search}` }
    return <Navigate to="/login" replace state={destino} />
  }

  return <Outlet />
}

export default RotaAdmin
