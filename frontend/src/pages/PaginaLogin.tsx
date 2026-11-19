import { Navigate, useLocation } from 'react-router'
import CabecalhoPagina from '../components/CabecalhoPagina'
import FormularioLogin from '../components/FormularioLogin'
import { useSessao } from '../hooks/useSessao'
import { destinoAposLogin } from '../utils/destinoAposLogin'

// login da equipe (admin); não tem link no menu, o acesso é digitando /login
// página com uma seção só, no padrão das páginas de formulário
function PaginaLogin() {
  const location = useLocation()
  const { ehAdmin } = useSessao()

  // já logado (ou acabou de entrar): volta para a página de onde veio.
  // replace: o /login sai do histórico, então o Voltar do navegador não cai de novo nele
  if (ehAdmin) {
    return <Navigate to={destinoAposLogin(location.state)} replace />
  }

  return (
    <section
      aria-label="Entrar"
      className="mx-auto max-w-[1300px] p-[3.2rem_1.6rem_5.2rem] mobile-lg:p-[4.8rem_2.4rem_6.4rem] tablet:p-[5.6rem_4rem_7.2rem] desktop:p-[6.4rem_6.4rem_8rem]"
    >
      <CabecalhoPagina titulo="Entrar:" descricao="Área restrita à equipe do Zyra." />
      <FormularioLogin />
    </section>
  )
}

export default PaginaLogin
