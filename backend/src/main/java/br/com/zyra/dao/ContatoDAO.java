package br.com.zyra.dao;

import br.com.zyra.exception.AcessoDadosException;
import br.com.zyra.model.Contato;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// único lugar com SQL da tabela ZYRA2_CONTATO
// gravar é público (formulário de contato); ler e excluir é só para o admin (SegurancaConfig)
@Repository
public class ContatoDAO {

    private final DataSource dataSource;

    public ContatoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // pega o próximo id da sequence; o BO chama antes de inserir
    public Long proximoId() {
        String sql = "SELECT ZYRA2_SEQ_CONTATO.NEXTVAL FROM DUAL";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            resultado.next();
            return resultado.getLong(1);
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao gerar o id do contato.", e);
        }
    }

    // grava a mensagem; id e dataEnvio já devem vir preenchidos pelo BO
    public Contato inserir(Contato contato) {
        String sql = "INSERT INTO ZYRA2_CONTATO "
                + "(ID_CONTATO, NOME, SOBRENOME, EMAIL, TELEFONE, MENSAGEM, DATA_ENVIO) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, contato.getIdContato());
            comando.setString(2, contato.getNome());
            comando.setString(3, contato.getSobrenome());
            comando.setString(4, contato.getEmail());
            // telefone é opcional: grava NULL quando não veio
            if (contato.getTelefone() == null) {
                comando.setNull(5, Types.VARCHAR);
            } else {
                comando.setString(5, contato.getTelefone());
            }
            comando.setString(6, contato.getMensagem());
            comando.setTimestamp(7, Timestamp.valueOf(contato.getDataEnvio()));
            comando.executeUpdate();
            return contato;
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao cadastrar o contato.", e);
        }
    }

    // lista uma página de mensagens, da mais recente para a mais antiga
    // o ID_CONTATO desempata mensagens com o mesmo horário, para a ordem não mudar entre páginas
    public List<Contato> listar(int pagina, int tamanho) {
        String sql = "SELECT ID_CONTATO, NOME, SOBRENOME, EMAIL, TELEFONE, MENSAGEM, DATA_ENVIO "
                + "FROM ZYRA2_CONTATO "
                + "ORDER BY DATA_ENVIO DESC, ID_CONTATO DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            // long para a conta não estourar com uma página muito alta
            comando.setLong(1, (long) pagina * tamanho);
            comando.setInt(2, tamanho);
            try (ResultSet resultado = comando.executeQuery()) {
                List<Contato> contatos = new ArrayList<>();
                while (resultado.next()) {
                    contatos.add(converter(resultado));
                }
                return contatos;
            }
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao listar os contatos.", e);
        }
    }

    // Optional vazio quando o id não existe; o BO decide se isso vira 404
    public Optional<Contato> buscarPorId(Long id) {
        String sql = "SELECT ID_CONTATO, NOME, SOBRENOME, EMAIL, TELEFONE, MENSAGEM, DATA_ENVIO "
                + "FROM ZYRA2_CONTATO WHERE ID_CONTATO = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(converter(resultado));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao buscar o contato.", e);
        }
    }

    // devolve true se a mensagem foi apagada; false indica que o id não existe
    public boolean excluir(Long id) {
        String sql = "DELETE FROM ZYRA2_CONTATO WHERE ID_CONTATO = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao excluir o contato.", e);
        }
    }

    // transforma a linha atual do ResultSet em um model Contato
    // (getString devolve null quando o telefone não foi informado)
    private Contato converter(ResultSet resultado) throws SQLException {
        Timestamp dataEnvio = resultado.getTimestamp("DATA_ENVIO");
        return new Contato(
                resultado.getLong("ID_CONTATO"),
                resultado.getString("NOME"),
                resultado.getString("SOBRENOME"),
                resultado.getString("EMAIL"),
                resultado.getString("TELEFONE"),
                resultado.getString("MENSAGEM"),
                dataEnvio == null ? null : dataEnvio.toLocalDateTime());
    }
}
