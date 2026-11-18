package br.com.zyra.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// o que chega no POST /contatos
// as regras são as mesmas do FormularioContato.tsx do frontend: mesmos tamanhos,
// mesmos padrões (regex) e mesmas mensagens. Se mudar lá, mudar aqui também.
//
// ordem de checagem igual à do formulário: primeiro "obrigatório", depois "tamanho",
// depois "formato". O @GroupSequence roda um grupo de cada vez e para no primeiro que falhar.
// As anotações sem "groups" ficam no último grupo (o próprio ContatoEntradaDTO).
@GroupSequence({ContatoEntradaDTO.Obrigatorio.class, ContatoEntradaDTO.Tamanho.class, ContatoEntradaDTO.class})
public record ContatoEntradaDTO(

        @NotBlank(message = "Informe seu nome.", groups = ContatoEntradaDTO.Obrigatorio.class)
        @Size(min = ContatoEntradaDTO.NOME_MINIMO, message = "Use pelo menos 2 caracteres.",
                groups = ContatoEntradaDTO.Tamanho.class)
        @Size(max = ContatoEntradaDTO.NOME_MAXIMO, message = "Use no máximo 50 caracteres.",
                groups = ContatoEntradaDTO.Tamanho.class)
        @Pattern(regexp = ContatoEntradaDTO.REGEX_NOME, message = "Use apenas letras, espaço, hífen ou apóstrofo.")
        String nome,

        @NotBlank(message = "Informe seu sobrenome.", groups = ContatoEntradaDTO.Obrigatorio.class)
        @Size(min = ContatoEntradaDTO.SOBRENOME_MINIMO, message = "Use pelo menos 2 caracteres.",
                groups = ContatoEntradaDTO.Tamanho.class)
        @Size(max = ContatoEntradaDTO.SOBRENOME_MAXIMO, message = "Use no máximo 80 caracteres.",
                groups = ContatoEntradaDTO.Tamanho.class)
        @Pattern(regexp = ContatoEntradaDTO.REGEX_NOME, message = "Use apenas letras, espaço, hífen ou apóstrofo.")
        String sobrenome,

        @NotBlank(message = "Informe seu e-mail.", groups = ContatoEntradaDTO.Obrigatorio.class)
        @Size(max = ContatoEntradaDTO.EMAIL_MAXIMO, message = "Use no máximo 100 caracteres.",
                groups = ContatoEntradaDTO.Tamanho.class)
        @Pattern(regexp = ContatoEntradaDTO.REGEX_EMAIL, message = "Informe um e-mail válido.")
        String email,

        // opcional (decisão D7): só confere o formato quando vier preenchido
        // (o @Pattern ignora null, e o construtor abaixo transforma texto vazio em null)
        @Pattern(regexp = ContatoEntradaDTO.REGEX_TELEFONE, message = "Use o formato (11) 91234-5678.")
        String telefone,

        @NotBlank(message = "Escreva sua mensagem.", groups = ContatoEntradaDTO.Obrigatorio.class)
        @Size(min = ContatoEntradaDTO.MENSAGEM_MINIMO, message = "Use pelo menos 10 caracteres.",
                groups = ContatoEntradaDTO.Tamanho.class)
        @Size(max = ContatoEntradaDTO.MENSAGEM_MAXIMO, message = "Use no máximo 500 caracteres.",
                groups = ContatoEntradaDTO.Tamanho.class)
        String mensagem,

        // campo isca: fica escondido no formulário, então uma pessoa real nunca preenche.
        // Sem validação aqui: quem confere é o ContatoBO, que recusa o envio se vier preenchido.
        // Não é gravado no banco nem volta na resposta.
        String site
) {

    // grupos de validação (só servem para definir a ordem do @GroupSequence)
    public interface Obrigatorio {
    }

    public interface Tamanho {
    }

    // tamanhos iguais aos do formulário e às colunas da ZYRA2_CONTATO
    public static final int NOME_MINIMO = 2;
    public static final int NOME_MAXIMO = 50;
    public static final int SOBRENOME_MINIMO = 2;
    public static final int SOBRENOME_MAXIMO = 80;
    public static final int EMAIL_MAXIMO = 100;
    public static final int MENSAGEM_MINIMO = 10;
    public static final int MENSAGEM_MAXIMO = 500;

    // os mesmos caracteres que o "\s" e o trim() do JavaScript consideram espaço
    // (o "\s" e o trim() do Java deixam de fora alguns, como o espaço não separável)
    private static final String ESPACOS_JS =
            "\\t\\n\\x0B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF";

    // letras (com acento) separadas por espaço, hífen ou apóstrofo
    public static final String REGEX_NOME = "^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ '-][A-Za-zÀ-ÖØ-öø-ÿ]+)*$";
    public static final String REGEX_EMAIL =
            "^[^" + ESPACOS_JS + "@]+@[^" + ESPACOS_JS + "@]+\\.[^" + ESPACOS_JS + "@]{2,}$";
    // (11) 91234-5678 ou (11) 1234-5678, com ou sem espaço depois do DDD
    public static final String REGEX_TELEFONE = "^\\(\\d{2}\\)[" + ESPACOS_JS + "]?\\d{4,5}-\\d{4}$";

    private static final java.util.regex.Pattern ESPACOS_NAS_PONTAS =
            java.util.regex.Pattern.compile("^[" + ESPACOS_JS + "]+|[" + ESPACOS_JS + "]+$");

    // roda quando o JSON vira DTO, antes da validação: tira os espaços das pontas
    // como o removerEspacos do formulário, para " Ana" valer igual nos dois lados
    // (o site fica como chegou: o BO precisa ver exatamente o que o robô mandou)
    public ContatoEntradaDTO {
        nome = removerEspacos(nome);
        sobrenome = removerEspacos(sobrenome);
        email = removerEspacos(email);
        mensagem = removerEspacos(mensagem);
        telefone = removerEspacos(telefone);
        // telefone em branco conta como "não informado"
        if (telefone != null && telefone.isEmpty()) {
            telefone = null;
        }
    }

    public static String removerEspacos(String valor) {
        return valor == null ? null : ESPACOS_NAS_PONTAS.matcher(valor).replaceAll("");
    }
}
