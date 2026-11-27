package br.com.zyra.seguranca;

import br.com.zyra.bo.AutenticacaoBO;
import br.com.zyra.model.Perfil;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// responde "quem está logado?" a partir do token JWT da requisição:
// o id vem do subject ("sub") e o perfil vem do claim "perfil"
//
// tudo que é "meu" (perfil, extrato, resgates) usa este utilitário, nunca um id enviado
// na requisição: assim ninguém consegue ver ou alterar dados de outra pessoa trocando um número na URL
//
// só faz sentido em rotas que exigem login; sem token válido, lança uma exceção
// que o Spring Security responde como 401 ("Faça login para continuar.")
@Component
public class UsuarioAtual {

    // id do usuário logado
    public Long idUsuario() {
        String subject = tokenAtual().getSubject();
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException | NullPointerException e) {
            throw semLogin();
        }
    }

    // perfil do usuário logado (USUARIO ou ADMIN)
    public Perfil perfil() {
        String perfil = tokenAtual().getClaimAsString(AutenticacaoBO.CLAIM_PERFIL);
        try {
            return Perfil.valueOf(perfil);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw semLogin();
        }
    }

    // o token já foi conferido (assinatura e validade) pelo Spring Security antes de chegar aqui
    private Jwt tokenAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof Jwt token) {
            return token;
        }
        throw semLogin();
    }

    private AuthenticationCredentialsNotFoundException semLogin() {
        return new AuthenticationCredentialsNotFoundException("Faça login para continuar.");
    }
}
