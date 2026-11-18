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

// único lugar com SQL da tabela ZYRA2_CONTATO
// por enquanto só grava: ler mensagens é exclusivo do admin e chega numa etapa futura
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
}
