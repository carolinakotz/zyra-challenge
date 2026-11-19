import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useSessao } from '../../hooks/useSessao'
import { ErroApi } from '../../services/api'
import { fazerLogin } from '../../services/autenticacaoService'
import type { LoginEntradaDTO } from '../../types/autenticacao'
import CampoTexto from '../CampoTexto'
import MensagemRetorno from '../MensagemRetorno'

// mesmo padrão de e-mail do FormularioContato
const REGEX_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

// tira espaços das pontas do e-mail (a senha é enviada exatamente como foi digitada)
const removerEspacos = (valor: string) => valor.trim()

// formulário de login do admin; depois do login, a PaginaLogin percebe a sessão e redireciona
function FormularioLogin() {
  const { entrar } = useSessao()
  // erro devolvido pela API no último envio (ex.: "E-mail ou senha inválidos.")
  const [erroApi, setErroApi] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginEntradaDTO>({ defaultValues: { email: '', senha: '' } })

  async function enviar(valores: LoginEntradaDTO) {
    setErroApi(null)

    try {
      const saida = await fazerLogin(valores)
      // por enquanto só a equipe (ADMIN) usa o login; outro perfil não ganha acesso a nada aqui
      if (saida.perfil !== 'ADMIN') {
        setErroApi('Você não tem permissão para acessar esta área.')
        return
      }
      entrar(saida)
    } catch (e) {
      setErroApi(e instanceof ErroApi ? e.message : 'Não foi possível entrar. Tente novamente.')
    }
  }

  return (
    <>
      {erroApi && <MensagemRetorno tipo="erro" texto={erroApi} />}

      {/* noValidate: a validação é do React Hook Form, não a do navegador */}
      <form
        noValidate
        onSubmit={handleSubmit(enviar)}
        className="mx-auto flex max-w-[76rem] flex-col gap-[2.4rem] mobile-lg:gap-[2.8rem] tablet:gap-[3.2rem]"
      >
        {/* limites iguais aos do LoginEntradaDTO (backend) */}
        {/* autoComplete="username" faz o gerenciador de senhas preencher o par e-mail + senha */}
        <CampoTexto
          id="login-email"
          label="E-mail"
          type="email"
          placeholder="Ex.: equipe@zyra.com"
          autoComplete="username"
          register={register('email', {
            setValueAs: removerEspacos,
            required: 'Informe o e-mail.',
            maxLength: { value: 100, message: 'O e-mail deve ter no máximo 100 caracteres.' },
            pattern: { value: REGEX_EMAIL, message: 'Informe um e-mail válido.' },
          })}
          erro={errors.email?.message}
        />

        <CampoTexto
          id="login-senha"
          label="Senha"
          type="password"
          autoComplete="current-password"
          register={register('senha', {
            required: 'Informe a senha.',
            maxLength: { value: 72, message: 'A senha deve ter no máximo 72 caracteres.' },
          })}
          erro={errors.senha?.message}
        />

        <hr className="h-[1.5px] border-none bg-separator-light" aria-hidden="true" />

        {/* mesmo botão verde de largura total dos outros formulários */}
        <button
          type="submit"
          disabled={isSubmitting}
          className="block w-full rounded-controle bg-card-green px-[2.4rem] py-[1.3rem] text-center font-texto text-[1.5rem] font-semibold text-title shadow-botao-verde transition-[background-color,transform,box-shadow] duration-300 hover:scale-[1.02] hover:bg-card-dark-green hover:text-text-light-yellow hover:shadow-botao-verde-hover active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-70 mobile-lg:px-[3rem] mobile-lg:py-[1.4rem] mobile-lg:text-[1.6rem] tablet:px-[3.6rem] tablet:py-[1.5rem]"
        >
          {isSubmitting ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </>
  )
}

export default FormularioLogin
