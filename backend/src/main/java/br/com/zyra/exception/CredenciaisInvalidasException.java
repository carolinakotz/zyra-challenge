package br.com.zyra.exception;

// lançada pelo AutenticacaoBO quando o login falha (e-mail inexistente OU senha errada)
// o TratadorGlobalExceptions traduz esta exceção em HTTP 401
// a mensagem é sempre a mesma nos dois casos, para não revelar quais e-mails estão cadastrados
public class CredenciaisInvalidasException extends RuntimeException {

    public static final String MENSAGEM = "E-mail ou senha inválidos.";

    public CredenciaisInvalidasException() {
        super(MENSAGEM);
    }
}
