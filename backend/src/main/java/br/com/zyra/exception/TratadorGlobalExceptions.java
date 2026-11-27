package br.com.zyra.exception;

import br.com.zyra.dto.ErroSaidaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

// "vigia" todos os resources: quando uma exceção escapa de um deles, cai aqui
// e é traduzida em status HTTP + JSON {"message": "..."}
// assim os resources não precisam de try/catch
@RestControllerAdvice
public class TratadorGlobalExceptions {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalExceptions.class);

    private static final String MENSAGEM_ERRO_INTERNO =
            "Não foi possível acessar os dados no momento. Tente novamente mais tarde.";

    // regra de negócio violada: 400 com a mensagem do BO
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroSaidaDTO> tratarRegraNegocio(RegraNegocioException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // id inexistente: 404 com a mensagem do BO
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroSaidaDTO> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // requisição válida, mas em conflito com o estado atual (ex.: já realizada): 409 com a mensagem do BO
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroSaidaDTO> tratarConflito(ConflitoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    // login com e-mail ou senha errados: 401, sempre com a mesma mensagem
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroSaidaDTO> tratarCredenciaisInvalidas(CredenciaisInvalidasException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    // falha no banco: 500 com mensagem genérica
    // o detalhe técnico (SQL, erro do Oracle) vai só para o log do servidor, nunca para o usuário
    @ExceptionHandler(AcessoDadosException.class)
    public ResponseEntity<ErroSaidaDTO> tratarAcessoDados(AcessoDadosException e) {
        log.error(e.getMessage(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, MENSAGEM_ERRO_INTERNO);
    }

    // DTO com @Valid que não passou nas anotações (@NotBlank, @Size...): 400 com a primeira mensagem
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroSaidaDTO> tratarValidacao(MethodArgumentNotValidException e) {
        return responder(HttpStatus.BAD_REQUEST, primeiraMensagem(e.getBindingResult()));
    }

    // método HTTP que o endereço não aceita (ex.: GET /contatos, que só tem POST): 405
    // sem este handler o Spring devolveria o erro padrão dele, fora do formato {"message": "..."}
    // o header Allow (exigido pelo HTTP no 405) diz quais métodos o endereço aceita
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroSaidaDTO> tratarMetodoNaoPermitido(HttpRequestMethodNotSupportedException e) {
        Set<HttpMethod> permitidos = e.getSupportedHttpMethods();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(permitidos == null ? new HttpMethod[0] : permitidos.toArray(new HttpMethod[0]))
                .body(new ErroSaidaDTO("Método não permitido para este endereço."));
    }

    // corpo ausente ou JSON malformado (ex.: vírgula sobrando, texto no lugar de objeto): 400
    // sem este handler o Spring devolveria o erro padrão dele, fora do formato {"message": "..."}
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroSaidaDTO> tratarCorpoInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "O corpo da requisição está inválido ou vazio.");
    }

    // parâmetro de URL ou query com tipo errado (ex.: /missoes/abc ou ?pontos=dez): 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroSaidaDTO> tratarTipoParametroInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "O valor informado para o parâmetro é inválido.");
    }

    // o Bean Validation não garante a ordem da lista de erros; para a resposta ser sempre a mesma,
    // usamos o erro do campo que aparece primeiro no DTO (ordem dos campos do record)
    private String primeiraMensagem(BindingResult resultado) {
        List<String> campos = camposNaOrdem(resultado.getTarget());
        Comparator<FieldError> ordemDosCampos = Comparator.comparingInt(erro -> {
            int posicao = campos.indexOf(erro.getField());
            return posicao < 0 ? Integer.MAX_VALUE : posicao;
        });

        return resultado.getFieldErrors().stream()
                .min(ordemDosCampos)
                .map(FieldError::getDefaultMessage)
                .or(() -> resultado.getGlobalErrors().stream().findFirst().map(ObjectError::getDefaultMessage))
                .orElse("Dados inválidos.");
    }

    private List<String> camposNaOrdem(Object dto) {
        if (dto == null || !dto.getClass().isRecord()) {
            return List.of();
        }
        return Arrays.stream(dto.getClass().getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
    }

    private ResponseEntity<ErroSaidaDTO> responder(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroSaidaDTO(mensagem));
    }
}
