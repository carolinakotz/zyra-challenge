package br.com.zyra.dao;

import br.com.zyra.exception.AcessoDadosException;
import br.com.zyra.model.Perfil;
import br.com.zyra.model.Usuario;
import org.springframework.stereotype.Repository;

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
        this.dataSource = dataSource;
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
