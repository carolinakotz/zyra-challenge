package br.com.zyra.exception;

// lançada pelos BOs quando a requisição é válida, mas conflita com o estado atual dos dados
// (ex.: missão já realizada, saldo insuficiente, registro já avaliado)
// o TratadorGlobalExceptions traduz esta exceção em HTTP 409
// a mensagem vai direto para o usuário: escrever em português com acento
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
