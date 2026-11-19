import { useEffect, useState } from 'react'
import { NavLink, useNavigate } from 'react-router'
import { useSessao } from '../../hooks/useSessao'
import type { AvisoNavegacao, LinkNavegacao } from '../../types/navegacao'

const LINKS_NAVEGACAO: LinkNavegacao[] = [
  { caminho: '/', rotulo: 'Início' },
  { caminho: '/sobre', rotulo: 'Sobre' },
  { caminho: '/missoes', rotulo: 'Missões' },
  { caminho: '/integrantes', rotulo: 'Integrantes' },
  { caminho: '/faq', rotulo: 'FAQ' },
  { caminho: '/contato', rotulo: 'Contato' },
]

// visual de cada item do menu (links e o botão "Sair"): no desktop, traço embaixo no hover
const CLASSE_ITEM_MENU =
  'relative block px-[1.5rem] py-[1.4rem] text-[1.6rem] text-nav transition-colors duration-200 desktop:inline-block desktop:w-auto desktop:px-0 desktop:pb-[0.7rem] desktop:after:absolute desktop:after:bottom-0 desktop:after:left-0 desktop:after:h-[2px] desktop:after:w-full desktop:after:origin-left desktop:after:scale-y-0 desktop:after:bg-hover desktop:after:opacity-0 desktop:after:transition-all desktop:after:duration-200 desktop:hover:after:scale-y-100 desktop:hover:after:opacity-100'

// obs.: os espaçamentos vêm do projeto original em rem fixo (ex.: px-[1.5rem]), porque o --spacing
// do Zyra é diferente do padrão do Tailwind usado lá (px-6 lá = 1.5rem; aqui seria 2.4rem)

// cabeçalho fixo no topo: links em linha no desktop; menu hambúrguer no celular e no tablet
function Cabecalho() {
  const [menuAberto, setMenuAberto] = useState(false)
  const { sessao, sair } = useSessao()
  const navigate = useNavigate()

  // com o menu aberto, Esc fecha
  useEffect(() => {
    if (!menuAberto) return

    function fecharComEsc(evento: KeyboardEvent) {
      if (evento.key === 'Escape') setMenuAberto(false)
    }

    window.addEventListener('keydown', fecharComEsc)
    return () => window.removeEventListener('keydown', fecharComEsc)
  }, [menuAberto])

  function alternarMenu() {
    setMenuAberto((atual) => !atual)
  }

  // vai para a lista de missões com o aviso e encerra a sessão.
  // navega antes de limpar a sessão: assim, se estiver numa página de admin,
  // o RotaAdmin não chega a mandar para o /login
  function encerrarSessao() {
    setMenuAberto(false)
    const aviso: AvisoNavegacao = { mensagem: 'Você saiu da área de administração.', tipo: 'sucesso' }
    navigate('/missoes', { state: aviso })
    sair()
  }

  return (
    <header className="sticky top-0 z-50 flex items-center justify-between border-b border-separator-light bg-bg px-[1.5rem] py-[1.8rem] mobile-lg:px-[2rem] tablet:px-[3rem] desktop:justify-around desktop:px-[6rem]">
      <NavLink to="/" onClick={() => setMenuAberto(false)}>
        <img
          src="/img/zyra.png"
          alt="Logo Zyra"
          className="h-[3.4rem] w-auto mobile-lg:h-[3.8rem] tablet:h-[4.2rem] desktop:h-[5rem]"
        />
      </NavLink>

      {/* botão hambúrguer: os três traços viram um "X" quando o menu está aberto */}
      <button
        type="button"
        onClick={alternarMenu}
        aria-label={menuAberto ? 'Fechar menu de navegação' : 'Abrir menu de navegação'}
        aria-expanded={menuAberto}
        aria-controls="menu-principal"
        className="z-50 flex h-[2rem] w-[2.8rem] flex-col justify-between max-[480px]:h-[1.8rem] max-[480px]:w-[2.4rem] desktop:hidden"
      >
        <span
          className={`block h-[2px] w-full rounded-full bg-nav transition-transform duration-300 ${
            menuAberto ? 'translate-y-[9px] rotate-45 max-[480px]:translate-y-[8px]' : ''
          }`}
        />
        <span
          className={`block h-[2px] w-full rounded-full bg-nav transition-all duration-200 ${
            menuAberto ? 'scale-x-0 opacity-0' : ''
          }`}
        />
        <span
          className={`block h-[2px] w-full rounded-full bg-nav transition-transform duration-300 ${
            menuAberto ? '-translate-y-[9px] -rotate-45 max-[480px]:-translate-y-[8px]' : ''
          }`}
        />
      </button>

      {/* no celular o menu desce embaixo do cabeçalho; no desktop fica sempre visível, em linha */}
      <nav
        id="menu-principal"
        aria-label="Navegação principal"
        className={`absolute top-full left-0 w-full overflow-hidden border-b border-separator-light bg-bg shadow-lg transition-[max-height,opacity] duration-300 ${
          menuAberto ? 'max-h-[40rem] opacity-100' : 'pointer-events-none max-h-0 opacity-0'
        } desktop:pointer-events-auto desktop:static desktop:max-h-none desktop:w-auto desktop:overflow-visible desktop:border-none desktop:opacity-100 desktop:shadow-none`}
      >
        <ul className="flex flex-col desktop:flex-row desktop:items-center desktop:gap-[4rem]">
          {LINKS_NAVEGACAO.map((link) => (
            <li
              key={link.caminho}
              className="w-full border-b border-separator-light last:border-b-0 desktop:w-auto desktop:border-none"
            >
              {/* Início só fica ativo exatamente em "/"; Missões continua ativo em /missoes/nova, /missoes/3... */}
              <NavLink
                to={link.caminho}
                end={link.caminho === '/'}
                onClick={() => setMenuAberto(false)}
                className={({ isActive }) =>
                  `${CLASSE_ITEM_MENU} ${
                    isActive
                      ? 'font-bold text-hover desktop:font-normal desktop:text-nav desktop:after:scale-y-100 desktop:after:opacity-100'
                      : ''
                  }`
                }
              >
                {link.rotulo}
              </NavLink>
            </li>
          ))}

          {/* só com sessão ativa; sem sessão não há link de "Entrar" (a equipe acessa digitando /login) */}
          {sessao && (
            <li className="w-full desktop:w-auto">
              <button type="button" onClick={encerrarSessao} className={`${CLASSE_ITEM_MENU} w-full text-left`}>
                Sair
              </button>
            </li>
          )}
        </ul>
      </nav>
    </header>
  )
}

export default Cabecalho
