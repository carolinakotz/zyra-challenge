import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { ErroApi } from '../../services/api'
import { cadastrarContato } from '../../services/contatoService'
import type { ContatoEntradaDTO, RetornoEnvio } from '../../types/contato'
import CampoTexto from '../CampoTexto'
import CampoTextarea from '../CampoTextarea'
import MensagemRetorno from '../MensagemRetorno'

// letras (com acento) separadas por espaço, hífen ou apóstrofo
const REGEX_NOME = /^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ '-][A-Za-zÀ-ÖØ-öø-ÿ]+)*$/
const REGEX_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
// (11) 91234-5678 ou (11) 1234-5678, com ou sem espaço depois do DDD
const REGEX_TELEFONE = /^\(\d{2}\)\s?\d{4,5}-\d{4}$/

// tempo que a mensagem de sucesso ou erro fica na tela
const TEMPO_MENSAGEM_MS = 5000

const MENSAGEM_SUCESSO = 'Mensagem enviada com sucesso! Em breve entraremos em contato.'
// usada só quando o erro não veio da API (a API manda a própria mensagem)
const MENSAGEM_ERRO_GENERICA = 'Não foi possível enviar sua mensagem. Tente novamente.'

// remove espaços nas pontas antes de validar e de enviar
const removerEspacos = (valor: string) => valor.trim()

// valores dos campos na tela (todos texto; o telefone vazio vira null só no envio)
interface ValoresFormulario {
  nome: string
  sobrenome: string
  email: string
  telefone: string
  mensagem: string
  site: string
}

function FormularioContato() {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ValoresFormulario>()

  const [retorno, setRetorno] = useState<RetornoEnvio | null>(null)

  // esconde a mensagem depois de alguns segundos
  useEffect(() => {
    if (retorno === null) return

    const temporizador = setTimeout(() => setRetorno(null), TEMPO_MENSAGEM_MS)
    return () => clearTimeout(temporizador)
  }, [retorno])

  // só é chamado se todos os campos passarem na validação;
  // enquanto a Promise não termina, isSubmitting fica true e o botão mostra "Enviando..."
  async function aoEnviar(valores: ValoresFormulario) {
    // limpa o aviso do envio anterior antes de tentar de novo
    setRetorno(null)

    const dados: ContatoEntradaDTO = {
      nome: valores.nome,
      sobrenome: valores.sobrenome,
      email: valores.email,
      // telefone é opcional: campo vazio vai como null
      telefone: valores.telefone === '' ? null : valores.telefone,
      mensagem: valores.mensagem,
      site: valores.site,
    }

    try {
      await cadastrarContato(dados)
      setRetorno({ tipo: 'sucesso', texto: MENSAGEM_SUCESSO })
      reset()
    } catch (e) {
      // em caso de erro os campos continuam preenchidos, para a pessoa não digitar tudo de novo
      setRetorno({ tipo: 'erro', texto: e instanceof ErroApi ? e.message : MENSAGEM_ERRO_GENERICA })
    }
  }

  return (
    <>
      {retorno && <MensagemRetorno tipo={retorno.tipo} texto={retorno.texto} />}

      <form
        noValidate
        onSubmit={handleSubmit(aoEnviar)}
        className="flex flex-col gap-[2.4rem] mobile-lg:gap-[2.8rem] tablet:gap-[3.2rem]"
      >
        <div className="grid grid-cols-1 gap-[2rem] tablet:grid-cols-2 tablet:gap-[2.4rem]">
          <CampoTexto
            id="contato-nome"
            label="Nome"
            placeholder="Digite seu nome"
            autoComplete="given-name"
            register={register('nome', {
              setValueAs: removerEspacos,
              required: 'Informe seu nome.',
              minLength: { value: 2, message: 'Use pelo menos 2 caracteres.' },
              maxLength: { value: 50, message: 'Use no máximo 50 caracteres.' },
              pattern: { value: REGEX_NOME, message: 'Use apenas letras, espaço, hífen ou apóstrofo.' },
            })}
            erro={errors.nome?.message}
          />
          <CampoTexto
            id="contato-sobrenome"
            label="Sobrenome"
            placeholder="Digite seu sobrenome"
            autoComplete="family-name"
            register={register('sobrenome', {
              setValueAs: removerEspacos,
              required: 'Informe seu sobrenome.',
              minLength: { value: 2, message: 'Use pelo menos 2 caracteres.' },
              maxLength: { value: 80, message: 'Use no máximo 80 caracteres.' },
              pattern: { value: REGEX_NOME, message: 'Use apenas letras, espaço, hífen ou apóstrofo.' },
            })}
            erro={errors.sobrenome?.message}
          />
        </div>

        <div className="grid grid-cols-1 gap-[2rem] tablet:grid-cols-2 tablet:gap-[2.4rem]">
          <CampoTexto
            id="contato-email"
            label="Email"
            type="email"
            placeholder="Digite seu Email"
            autoComplete="email"
            register={register('email', {
              setValueAs: removerEspacos,
              required: 'Informe seu e-mail.',
              maxLength: { value: 100, message: 'Use no máximo 100 caracteres.' },
              pattern: { value: REGEX_EMAIL, message: 'Informe um e-mail válido.' },
            })}
            erro={errors.email?.message}
          />
          <CampoTexto
            id="contato-telefone"
            label="Telefone (opcional)"
            type="tel"
            placeholder="(11) 91234-5678"
            autoComplete="tel"
            register={register('telefone', {
              setValueAs: removerEspacos,
              // opcional: o formato só é conferido quando o campo está preenchido
              pattern: { value: REGEX_TELEFONE, message: 'Use o formato (11) 91234-5678.' },
            })}
            erro={errors.telefone?.message}
          />
        </div>

        <CampoTextarea
          id="contato-mensagem"
          label="Mensagem"
          placeholder="Eu gostaria de saber mais sobre..."
          rows={6}
          register={register('mensagem', {
            setValueAs: removerEspacos,
            required: 'Escreva sua mensagem.',
            minLength: { value: 10, message: 'Use pelo menos 10 caracteres.' },
            maxLength: { value: 500, message: 'Use no máximo 500 caracteres.' },
          })}
          erro={errors.mensagem?.message}
        />

        {/* campo isca (honeypot): pessoas não veem nem alcançam com Tab, mas robôs que
            preenchem todos os campos do HTML caem nele, e o backend recusa o envio.
            Fica fora da tela em vez de display: none, que alguns robôs detectam.
            aria-hidden esconde de leitores de tela; tabIndex -1 tira da ordem do Tab */}
        <div aria-hidden="true" className="absolute top-auto left-[-9999px] h-px w-px overflow-hidden">
          <label htmlFor="contato-site">Site</label>
          <input id="contato-site" type="text" tabIndex={-1} autoComplete="off" {...register('site')} />
        </div>

        <hr className="h-[1.5px] border-none bg-separator-light" aria-hidden="true" />

        <button
          type="submit"
          disabled={isSubmitting}
          className="block w-full rounded-controle bg-card-green px-[2.4rem] py-[1.3rem] text-center font-texto text-[1.5rem] font-semibold text-title shadow-botao-verde transition-[background-color,transform,box-shadow] duration-300 hover:scale-[1.02] hover:bg-card-dark-green hover:text-text-light-yellow hover:shadow-botao-verde-hover active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-70 mobile-lg:px-[3rem] mobile-lg:py-[1.4rem] mobile-lg:text-[1.6rem] tablet:px-[3.6rem] tablet:py-[1.5rem]"
        >
          {isSubmitting ? 'Enviando...' : 'Enviar'}
        </button>
      </form>

      {/* aviso de privacidade (LGPD): para que os dados do formulário são usados */}
      <p className="mt-[1.6rem] text-center font-texto text-[1.3rem] text-text-dark">
        Usamos seus dados apenas para responder a esta mensagem. Não enviamos e-mails automáticos nem
        compartilhamos suas informações com terceiros.
      </p>
    </>
  )
}

export default FormularioContato
