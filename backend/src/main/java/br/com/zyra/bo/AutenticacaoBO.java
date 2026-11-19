package br.com.zyra.bo;

import br.com.zyra.dao.UsuarioDAO;
import br.com.zyra.dto.LoginEntradaDTO;
import br.com.zyra.dto.LoginSaidaDTO;
import br.com.zyra.exception.CredenciaisInvalidasException;
import br.com.zyra.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// regras do login: confere a senha com BCrypt e gera o token JWT
@Service
public class AutenticacaoBO {

    // nome do claim do token que guarda o perfil; o SegurancaConfig lê o mesmo nome
    public static final String CLAIM_PERFIL = "perfil";

    // o BCrypt só considera os primeiros 72 bytes da senha
    private static final int SENHA_MAXIMO_BYTES = 72;

    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration validadeToken;

    // hash de uma senha aleatória que ninguém conhece, gerado uma vez ao iniciar.
    // quando o e-mail não existe, comparamos a senha com ele mesmo assim:
    // a resposta demora o mesmo tempo que a de "senha errada" e não revela quais e-mails existem
    private final String hashFalso;

    public AutenticacaoBO(UsuarioDAO usuarioDAO, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                          @Value("${zyra.jwt.expiracao-minutos}") long expiracaoMinutos) {
        if (expiracaoMinutos < 1) {
            throw new IllegalStateException("JWT_EXPIRACAO_MINUTOS deve ser maior que zero.");
        }
        this.usuarioDAO = usuarioDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.validadeToken = Duration.ofMinutes(expiracaoMinutos);
        this.hashFalso = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public LoginSaidaDTO fazerLogin(LoginEntradaDTO dto) {
        // as regras ficam aqui mesmo com o @Valid do resource (mesmo motivo do MissaoBO)
        if (dto == null || dto.email() == null || dto.senha() == null) {
            throw new CredenciaisInvalidasException();
        }

        String senha = dto.senha();
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAXIMO_BYTES) {
            // senha longa demais nunca é válida; roda o BCrypt mesmo assim para manter o tempo igual
            passwordEncoder.matches("", hashFalso);
            throw new CredenciaisInvalidasException();
        }

        Optional<Usuario> encontrado = usuarioDAO.buscarPorEmail(normalizarEmail(dto.email()));

        // o BCrypt roda nos dois casos: com o hash real ou com o hash falso
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = passwordEncoder.matches(senha, hash);

        if (encontrado.isEmpty() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }

        return gerarToken(encontrado.get());
    }

    // e-mail sempre sem espaços nas pontas e em minúsculas: Admin@X.com e admin@x.com são o mesmo usuário
    // público porque o CriarAdminInicialRunner grava o e-mail com a mesma regra
    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // token assinado com HS256: id do usuário no "sub", perfil e validade.
    // o conteúdo do JWT é legível por qualquer um (só não pode ser alterado),
    // por isso não leva e-mail nem nenhum outro dado pessoal
    private LoginSaidaDTO gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(validadeToken);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuario.getIdUsuario()))
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new LoginSaidaDTO(token, usuario.getPerfil().name(), expiraEm);
    }
}
