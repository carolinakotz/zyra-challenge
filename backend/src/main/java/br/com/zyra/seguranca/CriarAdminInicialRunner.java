package br.com.zyra.seguranca;

import br.com.zyra.bo.AutenticacaoBO;
import br.com.zyra.dao.UsuarioDAO;
import br.com.zyra.model.Perfil;
import br.com.zyra.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

// cria o primeiro admin quando o app inicia, a partir de ADMIN_EMAIL e ADMIN_SENHA_INICIAL.
// - só age se as duas variáveis existirem (sem valor padrão)
// - só cria se ainda não houver nenhum ADMIN no banco
// - grava a senha como hash BCrypt
// - nunca escreve o e-mail ou a senha no log
// depois do primeiro start, as duas variáveis devem ser apagadas do ambiente.
// atenção: o banco é compartilhado, então rodar localmente já cria o admin da produção
@Component
public class CriarAdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CriarAdminInicialRunner.class);

    private static final int SENHA_MINIMO = 12;
    private static final int SENHA_MAXIMO_BYTES = 72;   // limite do BCrypt
    private static final int EMAIL_MAXIMO = 100;        // tamanho da coluna EMAIL

    private final Environment ambiente;
    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;

    public CriarAdminInicialRunner(Environment ambiente, UsuarioDAO usuarioDAO, PasswordEncoder passwordEncoder) {
        this.ambiente = ambiente;
        this.usuarioDAO = usuarioDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = ambiente.getProperty("ADMIN_EMAIL");
        String senha = ambiente.getProperty("ADMIN_SENHA_INICIAL");

        // sem as variáveis, não há nada a fazer (é o caso normal depois do primeiro start)
        if (email == null || email.isBlank() || senha == null || senha.isEmpty()) {
            return;
        }

        if (usuarioDAO.existeAdmin()) {
            log.info("Admin inicial já existe. ADMIN_EMAIL e ADMIN_SENHA_INICIAL podem ser removidas do ambiente.");
            return;
        }

        String emailNormalizado = AutenticacaoBO.normalizarEmail(email);
        if (!emailNormalizado.contains("@") || emailNormalizado.length() > EMAIL_MAXIMO) {
            log.warn("ADMIN_EMAIL inválido. Admin inicial não criado.");
            return;
        }
        if (senha.length() < SENHA_MINIMO) {
            log.warn("ADMIN_SENHA_INICIAL precisa ter pelo menos {} caracteres. Admin inicial não criado.",
                    SENHA_MINIMO);
            return;
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAXIMO_BYTES) {
            log.warn("ADMIN_SENHA_INICIAL precisa ter no máximo {} caracteres. Admin inicial não criado.",
                    SENHA_MAXIMO_BYTES);
            return;
        }

        Usuario admin = new Usuario();
        admin.setIdUsuario(usuarioDAO.proximoId());
        admin.setEmail(emailNormalizado);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setPerfil(Perfil.ADMIN);
        usuarioDAO.inserir(admin);

        log.info("Admin inicial criado.");
    }
}
