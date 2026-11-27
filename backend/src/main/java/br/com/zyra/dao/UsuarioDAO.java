package br.com.zyra.dao;

import br.com.zyra.exception.AcessoDadosException;
import br.com.zyra.model.Perfil;
import br.com.zyra.model.Usuario;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

// único lugar com SQL da tabela ZYRA2_USUARIO
// o e-mail chega aqui já em minúsculas (quem normaliza é o BO / o runner)
@Repository
public class UsuarioDAO {

    private final DataSource dataSource;

    public UsuarioDAO(DataSource dataSource) {
        // envolve o DataSource do Spring (não substitui o bean): dentro de um método @Transactional,
        // todas as chamadas recebem a mesma conexão e o close() só a devolve no fim da transação.
        // fora de uma transação, funciona como antes (uma conexão por chamada, com autocommit)
        this.dataSource = new TransactionAwareDataSourceProxy(dataSource);
    }

    // pega o próximo id da sequence; quem chama faz isso antes de inserir
    public Long proximoId() {
        String sql = "SELECT ZYRA2_SEQ_USUARIO.NEXTVAL FROM DUAL";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            resultado.next();
            return resultado.getLong(1);
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao gerar o id do usuário.", e);
        }
    }

    // grava o usuário; o id já deve vir preenchido e a senha já deve vir como hash BCrypt
    // DATA_CRIACAO fica com o padrão do banco (SYSTIMESTAMP)
    public Usuario inserir(Usuario usuario) {
        String sql = "INSERT INTO ZYRA2_USUARIO (ID_USUARIO, EMAIL, SENHA_HASH, PERFIL) VALUES (?, ?, ?, ?)";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, usuario.getIdUsuario());
            comando.setString(2, usuario.getEmail());
            comando.setString(3, usuario.getSenhaHash());
            comando.setString(4, usuario.getPerfil().name());
            comando.executeUpdate();
            return usuario;
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao cadastrar o usuário.", e);
        }
    }

    // Optional vazio quando o e-mail não está cadastrado
    public Optional<Usuario> buscarPorEmail(String email) {
        String sql = "SELECT ID_USUARIO, EMAIL, SENHA_HASH, PERFIL, DATA_CRIACAO FROM ZYRA2_USUARIO WHERE EMAIL = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, email);
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(converter(resultado));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao buscar o usuário.", e);
        }
    }

    // true se já existe pelo menos um usuário com perfil ADMIN (usado pelo CriarAdminInicialRunner)
    public boolean existeAdmin() {
        String sql = "SELECT COUNT(*) FROM ZYRA2_USUARIO WHERE PERFIL = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, Perfil.ADMIN.name());
            try (ResultSet resultado = comando.executeQuery()) {
                resultado.next();
                return resultado.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao verificar se existe admin.", e);
        }
    }

    // trava a linha do usuário até o fim da transação (commit ou rollback).
    // toda operação que mexe em pontos ou realizações de um usuário começa por aqui:
    // duas requisições simultâneas do mesmo usuário passam a acontecer uma depois da outra.
    // MANDATORY: só pode ser chamado dentro de um método @Transactional; fora dele, a trava
    // seria solta logo em seguida (autocommit), então o Spring lança erro em vez de falhar em silêncio.
    // devolve false quando o usuário não existe (quem decide o que fazer é o BO)
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean travarPorId(Long idUsuario) {
        String sql = "SELECT ID_USUARIO FROM ZYRA2_USUARIO WHERE ID_USUARIO = ? FOR UPDATE";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, idUsuario);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next();
            }
        } catch (SQLException e) {
            throw new AcessoDadosException("Erro ao travar o usuário.", e);
        }
    }

    // transforma a linha atual do ResultSet em um model Usuario
    private Usuario converter(ResultSet resultado) throws SQLException {
        Timestamp dataCriacao = resultado.getTimestamp("DATA_CRIACAO");
        return new Usuario(
                resultado.getLong("ID_USUARIO"),
                resultado.getString("EMAIL"),
                resultado.getString("SENHA_HASH"),
                Perfil.valueOf(resultado.getString("PERFIL")),
                dataCriacao == null ? null : dataCriacao.toLocalDateTime());
    }
}
