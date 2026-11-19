import { useCallback, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import CabecalhoPagina from '../components/CabecalhoPagina'
import EstadoRequisicao from '../components/EstadoRequisicao'
import MensagemRetorno from '../components/MensagemRetorno'
import { useFetch } from '../hooks/useFetch'
import { ErroApi } from '../services/api'
import { excluirContato, listarContatos } from '../services/contatoService'
import type { ContatoSaidaDTO, RetornoEnvio } from '../types/contato'
import type { DestinoLogin } from '../types/navegacao'
import { formatarDataHora } from '../utils/formatarDataHora'

// mensagens por página (o backend aceita até 100)
const TAMANHO_PAGINA = 20

const CLASSE_BOTAO_PAGINA =
  'rounded-controle border-[1.5px] border-nav px-[2.4rem] py-[1rem] font-texto text-[1.5rem] font-semibold text-nav transition-[background-color,color] duration-200 hover:bg-nav hover:text-white disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-transparent disabled:hover:text-nav'

// página da URL (?pagina=2 é a terceira, começando em 0); valor inválido vira 0
function lerPagina(texto: string | null): number {
  if (texto === null || !/^\d+$/.test(texto)) return 0
  return Number(texto)
}

interface CartaoContatoProps {
  contato: ContatoSaidaDTO
  excluindo: boolean
  aoExcluir: (contato: ContatoSaidaDTO) => void
}

// uma mensagem recebida.
// SEGURANÇA (AB-10): todo campo é mostrado como texto puro ({contato.campo}); o React escapa o conteúdo,
// então "<script>" aparece escrito e não roda. Nunca usar dangerouslySetInnerHTML aqui, nem transformar
// URLs ou o e-mail em links: quem escreveu a mensagem pode ser um atacante.
function CartaoContato({ contato, excluindo, aoExcluir }: CartaoContatoProps) {
  return (
    <article
      aria-label={`Mensagem de ${contato.nome} ${contato.sobrenome}`}
      className="flex flex-col gap-[1.6rem] rounded-xl bg-card-light-yellow p-[2rem] shadow-cartao tablet:p-[2.8rem]"
    >
      <div className="flex flex-col gap-[0.4rem]">
        <h2 className="font-texto text-[1.8rem] font-bold break-words text-title">
          {contato.nome} {contato.sobrenome}
        </h2>
        <p className="font-texto text-[1.4rem] text-text-dark">
          <time dateTime={contato.dataEnvio}>{formatarDataHora(contato.dataEnvio)}</time>
        </p>
      </div>

      <dl className="grid grid-cols-[auto_1fr] gap-x-[1.2rem] gap-y-[0.4rem] font-texto text-[1.5rem] text-text-dark">
        <dt className="font-semibold">E-mail:</dt>
        <dd className="break-all">{contato.email}</dd>
        <dt className="font-semibold">Telefone:</dt>
        <dd>{contato.telefone ?? 'Não informado'}</dd>
      </dl>

      {/* pre-wrap mantém as quebras de linha; break-words impede que um texto sem espaços estoure o cartão */}
      <p className="border-t border-separator-light pt-[1.6rem] font-texto text-[1.5rem] leading-[1.7] break-words whitespace-pre-wrap text-text-dark tablet:text-[1.6rem]">
        {contato.mensagem}
      </p>

      <button
        type="button"
        onClick={() => aoExcluir(contato)}
        disabled={excluindo}
        className="self-end rounded-lg border-[1.5px] border-erro px-[2.8rem] py-[1rem] text-[1.5rem] font-semibold text-erro transition-[background-color,color] duration-300 hover:bg-erro hover:text-white disabled:cursor-not-allowed disabled:opacity-70"
      >
        {excluindo ? 'Excluindo...' : 'Excluir'}
      </button>
    </article>
  )
}

// painel do admin com as mensagens do formulário de contato (rota /admin/contatos, dentro do RotaAdmin)
function PaginaContatosAdmin() {
  const navigate = useNavigate()
  const [parametros, setParametros] = useSearchParams()
  const pagina = lerPagina(parametros.get('pagina'))

  // useCallback: nova busca só quando a página muda (sem isso o useFetch entraria em loop)
  const busca = useCallback((sinal: AbortSignal) => listarContatos(pagina, TAMANHO_PAGINA, sinal), [pagina])
  const { dados: contatos, carregando, erro, recarregar } = useFetch(busca)

  const [idExcluindo, setIdExcluindo] = useState<number | null>(null)
  const [retorno, setRetorno] = useState<RetornoEnvio | null>(null)

  const vazio = !carregando && erro === null && contatos !== null && contatos.length === 0
  // a API não informa o total: se a página veio cheia, pode haver uma próxima
  const temProxima = !carregando && contatos !== null && contatos.length === TAMANHO_PAGINA

  function irParaPagina(nova: number) {
    setRetorno(null)
    setParametros(nova === 0 ? {} : { pagina: String(nova) })
  }

  async function excluir(contato: ContatoSaidaDTO) {
    if (!window.confirm(`Deseja realmente excluir a mensagem de ${contato.nome} ${contato.sobrenome}?`)) return

    setRetorno(null)
    setIdExcluindo(contato.idContato)

    try {
      await excluirContato(contato.idContato)
    } catch (e) {
      // 401: a sessão venceu (o api.ts já limpou); depois do login volta para esta página
      if (e instanceof ErroApi && e.status === 401) {
        const destino: DestinoLogin = { de: `/admin/contatos${pagina === 0 ? '' : `?pagina=${pagina}`}` }
        navigate('/login', { state: destino })
        return
      }
      setRetorno({ tipo: 'erro', texto: e instanceof ErroApi ? e.message : 'Não foi possível excluir a mensagem.' })
      setIdExcluindo(null)
      return
    }

    setIdExcluindo(null)
    setRetorno({ tipo: 'sucesso', texto: 'Mensagem excluída com sucesso!' })
    recarregar()
  }

  return (
    <section
      aria-label="Mensagens de contato"
      className="mx-auto max-w-[1300px] p-[3.2rem_1.6rem_5.2rem] mobile-lg:p-[4.8rem_2.4rem_6.4rem] tablet:p-[5.6rem_4rem_7.2rem] desktop:p-[6.4rem_6.4rem_8rem]"
    >
      <CabecalhoPagina
        titulo="Mensagens de contato"
        descricao="Mensagens enviadas pelo formulário de contato, da mais recente para a mais antiga."
      />

      {retorno && <MensagemRetorno tipo={retorno.tipo} texto={retorno.texto} />}

      <EstadoRequisicao
        carregando={carregando}
        erro={erro}
        vazio={vazio}
        aoTentarNovamente={recarregar}
        textoCarregando="Carregando mensagens..."
        mensagemVazio={pagina === 0 ? 'Nenhuma mensagem recebida.' : 'Nenhuma mensagem nesta página.'}
      >
        <div className="mx-auto flex max-w-[80rem] flex-col gap-[2rem] tablet:gap-[2.4rem]">
          {contatos?.map((contato) => (
            <CartaoContato
              key={contato.idContato}
              contato={contato}
              excluindo={idExcluindo === contato.idContato}
              aoExcluir={excluir}
            />
          ))}
        </div>
      </EstadoRequisicao>

      {(pagina > 0 || temProxima) && (
        <nav aria-label="Páginas de mensagens" className="mt-[3.2rem] flex items-center justify-center gap-[1.6rem]">
          <button
            type="button"
            onClick={() => irParaPagina(pagina - 1)}
            disabled={pagina === 0 || carregando}
            className={CLASSE_BOTAO_PAGINA}
          >
            Anterior
          </button>
          <span className="font-texto text-[1.5rem] text-text-dark">Página {pagina + 1}</span>
          <button
            type="button"
            onClick={() => irParaPagina(pagina + 1)}
            disabled={!temProxima}
            className={CLASSE_BOTAO_PAGINA}
          >
            Próxima
          </button>
        </nav>
      )}
    </section>
  )
}

export default PaginaContatosAdmin
