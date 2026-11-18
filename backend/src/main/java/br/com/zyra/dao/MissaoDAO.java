package br.com.zyra.dao;

import br.com.zyra.exception.AcessoDadosException;
import br.com.zyra.model.Conquista;
import br.com.zyra.model.Missao;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// único lugar com SQL da tabela ZYRA2_MISSAO
// grava e altera só a missão: a conquista fica a cargo do ConquistaDAO
// exceção: a listagem lê as duas tabelas com JOIN, para não fazer uma consulta por missão
@Repository
public class MissaoDAO {

    private final DataSource dataSource;

    // o Spring entrega o DataSource (pool de conexões) configurado no application.properties
    public MissaoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // pega o próximo id da sequence; o BO chama antes de inserir
    public Long proximoId() {
        String sql = "SELECT ZYRA2_SEQ_MISSAO.NEXTVAL FROM DUAL";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            resultado.next();
            return resultado.getLong(1);
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao gerar o id da missão.", e);
        }
    }

    // grava a missão; o id já deve vir preenchido (via proximoId)
    public Missao inserir(Missao missao) {
        String sql = "INSERT INTO ZYRA2_MISSAO (ID_MISSAO, NOME_MISSAO, REQUISITO) VALUES (?, ?, ?)";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, missao.getId());
            comando.setString(2, missao.getNomeMissao());
            comando.setString(3, missao.getRequisito());
            comando.executeUpdate();
            return missao;
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao cadastrar a missão.", e);
        }
    }

    // devolve true se alguma linha foi alterada; false indica que o id não existe
    public boolean atualizar(Missao missao) {
        String sql = "UPDATE ZYRA2_MISSAO SET NOME_MISSAO = ?, REQUISITO = ? WHERE ID_MISSAO = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, missao.getNomeMissao());
            comando.setString(2, missao.getRequisito());
            comando.setLong(3, missao.getId());
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao atualizar a missão.", e);
        }
    }

    // devolve true se a missão foi apagada; false indica que o id não existe
    // as conquistas da missão precisam ser excluídas antes, por causa da FK
    public boolean excluir(Long id) {
        String sql = "DELETE FROM ZYRA2_MISSAO WHERE ID_MISSAO = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao excluir a missão.", e);
        }
    }

    // lista uma página de missões já com a conquista de cada uma, numa consulta só
    // 1) a subconsulta "m" pagina as missões ANTES do JOIN: o limite vale para missões, não para linhas
    // 2) o LEFT JOIN mantém na lista a missão que ainda não tem conquista (conquista = null)
    // 3) o WHERE deixa só a primeira conquista (menor id), a mesma regra do MissaoBO.buscarConquista();
    //    o filtro fica no WHERE (e não no ON), onde subconsulta correlacionada funciona em qualquer versão do Oracle
    public List<Missao> listarTodos(int pagina, int tamanho) {
        String sql = "SELECT m.ID_MISSAO, m.NOME_MISSAO, m.REQUISITO, c.ID_CONQUISTA, c.PONTOS_RECOMPENSA "
                + "FROM (SELECT ID_MISSAO, NOME_MISSAO, REQUISITO FROM ZYRA2_MISSAO "
                + "      ORDER BY ID_MISSAO OFFSET ? ROWS FETCH NEXT ? ROWS ONLY) m "
                + "LEFT JOIN ZYRA2_CONQUISTA c ON c.ID_MISSAO = m.ID_MISSAO "
                + "WHERE c.ID_CONQUISTA IS NULL "
                + "   OR c.ID_CONQUISTA = (SELECT MIN(c2.ID_CONQUISTA) FROM ZYRA2_CONQUISTA c2 "
                + "                        WHERE c2.ID_MISSAO = m.ID_MISSAO) "
                + "ORDER BY m.ID_MISSAO";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            // long para a conta não estourar com uma página muito alta
            comando.setLong(1, (long) pagina * tamanho);
            comando.setInt(2, tamanho);
            try (ResultSet resultado = comando.executeQuery()) {
                List<Missao> missoes = new ArrayList<>();
                while (resultado.next()) {
                    Missao missao = converter(resultado);
                    missao.setConquista(converterConquista(resultado, missao.getId()));
                    missoes.add(missao);
                }
                return missoes;
            }
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao listar as missões.", e);
        }
    }

    // Optional vazio quando o id não existe; o BO decide se isso vira 404
    public Optional<Missao> buscarPorId(Long id) {
        String sql = "SELECT ID_MISSAO, NOME_MISSAO, REQUISITO FROM ZYRA2_MISSAO WHERE ID_MISSAO = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, id);
            // o ResultSet só existe depois do setLong, por isso fica num try interno
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(converter(resultado));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao buscar a missão.", e);
        }
    }

    // transforma a linha atual do ResultSet em um model Missao (sem conquista)
    private Missao converter(ResultSet resultado) throws SQLException {
        Missao missao = new Missao();
        missao.setId(resultado.getLong("ID_MISSAO"));
        missao.setNomeMissao(resultado.getString("NOME_MISSAO"));
        missao.setRequisito(resultado.getString("REQUISITO"));
        return missao;
    }

    // monta a conquista que veio no JOIN da listagem; null quando a missão não tem conquista
    // (no LEFT JOIN, as colunas da conquista chegam nulas)
    private Conquista converterConquista(ResultSet resultado, Long idMissao) throws SQLException {
        long idConquista = resultado.getLong("ID_CONQUISTA");
        if (resultado.wasNull()) {
            return null;
        }
        return new Conquista(idConquista, resultado.getInt("PONTOS_RECOMPENSA"), idMissao);
    }
}
