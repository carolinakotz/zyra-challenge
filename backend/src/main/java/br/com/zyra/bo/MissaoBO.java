package br.com.zyra.bo;

import br.com.zyra.dao.ConquistaDAO;
import br.com.zyra.dao.MissaoDAO;
import br.com.zyra.dto.ConquistaSaidaDTO;
import br.com.zyra.dto.MissaoEntradaDTO;
import br.com.zyra.dto.MissaoSaidaDTO;
import br.com.zyra.exception.RecursoNaoEncontradoException;
import br.com.zyra.exception.RegraNegocioException;
import br.com.zyra.model.Conquista;
import br.com.zyra.model.Missao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// regras de negócio da missão
// recebe e devolve DTO (fala com o resource) e trabalha com model (fala com os DAOs)
@Service
public class MissaoBO {

    // pontos da conquista quando quem chama não informa outro valor
    public static final int PONTOS_PADRAO = 20;

    // faixa de pontos aceita por conquista; o máximo evita inflar pontos (que depois viram saldo)
    // o FormularioMissao do frontend e a constraint ZYRA2_CK_CONQUISTA_PONTOS usam os mesmos valores
    public static final int PONTOS_MINIMO = 1;
    public static final int PONTOS_MAXIMO = 500;

    // paginação do GET /missoes: sem parâmetros, devolve a primeira página com 50 missões
    // tamanhos acima do máximo são reduzidos, para nenhuma chamada puxar a tabela inteira
    public static final int PAGINA_PADRAO = 0;
    public static final int TAMANHO_PADRAO = 50;
    public static final int TAMANHO_MAXIMO = 100;

    private static final int TAMANHO_MINIMO_NOME = 3;

    private final MissaoDAO missaoDAO;
    private final ConquistaDAO conquistaDAO;

    public MissaoBO(MissaoDAO missaoDAO, ConquistaDAO conquistaDAO) {
        this.missaoDAO = missaoDAO;
        this.conquistaDAO = conquistaDAO;
    }

    // cadastra a missão com a conquista padrão de 20 pontos
    // @Transactional aqui também: a chamada para o outro cadastrar acontece dentro da própria
    // classe, e o Spring só abre transação quando o método é chamado de fora (pelo resource)
    @Transactional
    public MissaoSaidaDTO cadastrar(MissaoEntradaDTO dto) {
        return cadastrar(dto, PONTOS_PADRAO);
    }

    // cadastra a missão e já cria a conquista vinculada a ela
    // @Transactional: missão e conquista são gravadas juntas; se a conquista falhar,
    // a missão também é desfeita (rollback) e o banco não fica com missão sem conquista
    @Transactional
    public MissaoSaidaDTO cadastrar(MissaoEntradaDTO dto, Integer pontosRecompensa) {
        validar(dto);
        if (pontosRecompensa == null || pontosRecompensa < PONTOS_MINIMO || pontosRecompensa > PONTOS_MAXIMO) {
            throw new RegraNegocioException(
                    "Os pontos da conquista devem estar entre " + PONTOS_MINIMO + " e " + PONTOS_MAXIMO + ".");
        }

        Missao missao = converterParaModel(dto);
        missao.setId(missaoDAO.proximoId());
        missaoDAO.inserir(missao);

        // a missão precisa existir antes da conquista, por causa da FK
        Conquista conquista = new Conquista(conquistaDAO.proximoId(), pontosRecompensa, missao.getId());
        conquistaDAO.inserir(conquista);
        missao.setConquista(conquista);

        return converterParaDTO(missao);
    }

    public MissaoSaidaDTO atualizar(Long id, MissaoEntradaDTO dto) {
        validar(dto);

        Missao missao = converterParaModel(dto);
        missao.setId(id);
        if (!missaoDAO.atualizar(missao)) {
            throw new RecursoNaoEncontradoException("Missão não encontrada.");
        }

        missao.setConquista(buscarConquista(id));
        return converterParaDTO(missao);
    }

    // @Transactional: conquistas e missão são apagadas juntas; se algo falhar no meio,
    // nada é apagado (rollback)
    @Transactional
    public void excluir(Long id) {
        buscarMissao(id);

        // primeiro as conquistas, depois a missão: a FK não deixa apagar a missão
        // enquanto houver conquista apontando para ela
        for (Conquista conquista : conquistaDAO.buscarPorMissao(id)) {
            conquistaDAO.excluir(conquista.getIdConquista());
        }
        missaoDAO.excluir(id);
    }

    public MissaoSaidaDTO buscarPorId(Long id) {
        Missao missao = buscarMissao(id);
        missao.setConquista(buscarConquista(id));
        return converterParaDTO(missao);
    }

    // lista uma página de missões, cada uma já com a sua conquista (uma consulta só no DAO)
    // pagina e tamanho podem vir nulos: aí valem PAGINA_PADRAO e TAMANHO_PADRAO
    public List<MissaoSaidaDTO> listarTodos(Integer pagina, Integer tamanho) {
        int paginaFinal = pagina == null ? PAGINA_PADRAO : pagina;
        int tamanhoFinal = tamanho == null ? TAMANHO_PADRAO : tamanho;

        if (paginaFinal < 0) {
            throw new RegraNegocioException("A página deve ser maior ou igual a zero.");
        }
        if (tamanhoFinal < 1) {
            throw new RegraNegocioException("O tamanho da página deve ser maior que zero.");
        }
        // pedir mais que o máximo não é erro: só recebe o máximo
        tamanhoFinal = Math.min(tamanhoFinal, TAMANHO_MAXIMO);

        return missaoDAO.listarTodos(paginaFinal, tamanhoFinal).stream()
                .map(this::converterParaDTO)
                .toList();
    }

    // as regras ficam aqui mesmo com as anotações do DTO, porque o BO
    // pode ser chamado por outro código que não passa pelo @Valid do resource
    private void validar(MissaoEntradaDTO dto) {
        if (dto == null) {
            throw new RegraNegocioException("Os dados da missão são obrigatórios.");
        }
        if (dto.nomeMissao() == null || dto.nomeMissao().trim().length() < TAMANHO_MINIMO_NOME) {
            throw new RegraNegocioException("O nome da missão deve ter pelo menos 3 caracteres.");
        }
        if (dto.requisito() == null || dto.requisito().isBlank()) {
            throw new RegraNegocioException("O requisito da missão é obrigatório.");
        }
    }

    private Missao buscarMissao(Long id) {
        return missaoDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Missão não encontrada."));
    }

    // o schema permite várias conquistas por missão, mas o contrato JSON tem só uma:
    // usamos a primeira (menor id); null se a missão ainda não tiver conquista
    private Conquista buscarConquista(Long idMissao) {
        List<Conquista> conquistas = conquistaDAO.buscarPorMissao(idMissao);
        return conquistas.isEmpty() ? null : conquistas.get(0);
    }

    // DTO de entrada -> model (sem id e sem conquista; quem preenche é o BO)
    private Missao converterParaModel(MissaoEntradaDTO dto) {
        Missao missao = new Missao();
        missao.setNomeMissao(dto.nomeMissao().trim());
        missao.setRequisito(dto.requisito().trim());
        return missao;
    }

    // model -> DTO de saída (o que vai no JSON)
    private MissaoSaidaDTO converterParaDTO(Missao missao) {
        Conquista conquista = missao.getConquista();
        ConquistaSaidaDTO conquistaDTO = conquista == null
                ? null
                : new ConquistaSaidaDTO(conquista.getIdConquista(), conquista.getPontosRecompensa(), conquista.getIdMissao());
        return new MissaoSaidaDTO(missao.getId(), missao.getNomeMissao(), missao.getRequisito(), conquistaDTO);
    }
}
