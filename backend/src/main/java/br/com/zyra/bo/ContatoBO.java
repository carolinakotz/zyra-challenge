package br.com.zyra.bo;

import br.com.zyra.dao.ContatoDAO;
import br.com.zyra.dto.ContatoEntradaDTO;
import br.com.zyra.dto.ContatoSaidaDTO;
import br.com.zyra.exception.RegraNegocioException;
import br.com.zyra.model.Contato;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import static br.com.zyra.dto.ContatoEntradaDTO.EMAIL_MAXIMO;
import static br.com.zyra.dto.ContatoEntradaDTO.MENSAGEM_MAXIMO;
import static br.com.zyra.dto.ContatoEntradaDTO.MENSAGEM_MINIMO;
import static br.com.zyra.dto.ContatoEntradaDTO.NOME_MAXIMO;
import static br.com.zyra.dto.ContatoEntradaDTO.NOME_MINIMO;
import static br.com.zyra.dto.ContatoEntradaDTO.REGEX_EMAIL;
import static br.com.zyra.dto.ContatoEntradaDTO.REGEX_NOME;
import static br.com.zyra.dto.ContatoEntradaDTO.REGEX_TELEFONE;
import static br.com.zyra.dto.ContatoEntradaDTO.SOBRENOME_MAXIMO;
import static br.com.zyra.dto.ContatoEntradaDTO.SOBRENOME_MINIMO;

// regras de negócio das mensagens de contato
@Service
public class ContatoBO {

    // sem limite mínimo (o e-mail só tem máximo)
    private static final int SEM_MINIMO = 0;

    private final ContatoDAO contatoDAO;

    public ContatoBO(ContatoDAO contatoDAO) {
        this.contatoDAO = contatoDAO;
    }

    // valida, tira os espaços das pontas e grava a mensagem
    public ContatoSaidaDTO cadastrar(ContatoEntradaDTO dto) {
        validar(dto);

        Contato contato = converterParaModel(dto);
        contato.setIdContato(contatoDAO.proximoId());
        contato.setDataEnvio(LocalDateTime.now());
        contatoDAO.inserir(contato);

        return converterParaDTO(contato);
    }

    // as regras ficam aqui mesmo com as anotações do DTO, porque o BO
    // pode ser chamado por outro código que não passa pelo @Valid do resource
    // mesmas regras, mensagens e ordem do DTO: obrigatórios, depois tamanhos, depois formatos
    private void validar(ContatoEntradaDTO dto) {
        if (dto == null) {
            throw new RegraNegocioException("Os dados do contato são obrigatórios.");
        }

        exigir(dto.nome(), "Informe seu nome.");
        exigir(dto.sobrenome(), "Informe seu sobrenome.");
        exigir(dto.email(), "Informe seu e-mail.");
        exigir(dto.mensagem(), "Escreva sua mensagem.");

        validarTamanho(dto.nome(), NOME_MINIMO, NOME_MAXIMO);
        validarTamanho(dto.sobrenome(), SOBRENOME_MINIMO, SOBRENOME_MAXIMO);
        validarTamanho(dto.email(), SEM_MINIMO, EMAIL_MAXIMO);
        validarTamanho(dto.mensagem(), MENSAGEM_MINIMO, MENSAGEM_MAXIMO);

        validarFormato(dto.nome(), REGEX_NOME, "Use apenas letras, espaço, hífen ou apóstrofo.");
        validarFormato(dto.sobrenome(), REGEX_NOME, "Use apenas letras, espaço, hífen ou apóstrofo.");
        validarFormato(dto.email(), REGEX_EMAIL, "Informe um e-mail válido.");
        // telefone é opcional (decisão D7): só confere o formato quando vier preenchido
        if (dto.telefone() != null) {
            validarFormato(dto.telefone(), REGEX_TELEFONE, "Use o formato (11) 91234-5678.");
        }
    }

    private void exigir(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException(mensagem);
        }
    }

    private void validarTamanho(String valor, int minimo, int maximo) {
        if (valor.length() < minimo) {
            throw new RegraNegocioException("Use pelo menos " + minimo + " caracteres.");
        }
        if (valor.length() > maximo) {
            throw new RegraNegocioException("Use no máximo " + maximo + " caracteres.");
        }
    }

    private void validarFormato(String valor, String regex, String mensagem) {
        if (!Pattern.matches(regex, valor)) {
            throw new RegraNegocioException(mensagem);
        }
    }

    // DTO de entrada -> model (sem id e sem data; quem preenche é o cadastrar)
    // o construtor do DTO já tira os espaços; repetir aqui garante a regra mesmo se isso mudar
    private Contato converterParaModel(ContatoEntradaDTO dto) {
        Contato contato = new Contato();
        contato.setNome(ContatoEntradaDTO.removerEspacos(dto.nome()));
        contato.setSobrenome(ContatoEntradaDTO.removerEspacos(dto.sobrenome()));
        contato.setEmail(ContatoEntradaDTO.removerEspacos(dto.email()));
        contato.setTelefone(ContatoEntradaDTO.removerEspacos(dto.telefone()));
        contato.setMensagem(ContatoEntradaDTO.removerEspacos(dto.mensagem()));
        return contato;
    }

    // model -> DTO de saída (o que vai no JSON)
    private ContatoSaidaDTO converterParaDTO(Contato contato) {
        return new ContatoSaidaDTO(contato.getIdContato(), contato.getNome(), contato.getSobrenome(),
                contato.getEmail(), contato.getTelefone(), contato.getMensagem(), contato.getDataEnvio());
    }
}
