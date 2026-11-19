package br.com.zyra.seguranca;

import br.com.zyra.bo.AutenticacaoBO;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// regras de acesso da API (quem pode chamar cada endpoint) e configuração do JWT
//
// ordem em cada requisição:
//   CorsFilter -> LimiteRequisicoesFilter -> Spring Security (este arquivo) -> resource
// os dois filtros nossos rodam antes (ordem HIGHEST_PRECEDENCE), então o preflight OPTIONS é
// respondido pelo CorsFilter sem passar por aqui, e os 401/403 daqui já saem com os headers de CORS.
// por isso o CORS do Spring Security NÃO é ativado: o CorsFilter continua sendo o único responsável.
@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    private static final int SEGREDO_MINIMO_BYTES = 32;

    private static final String MENSAGEM_401 = "Faça login para continuar.";
    private static final String MENSAGEM_403 = "Você não tem permissão para realizar esta ação.";

    private static final String ADMIN = "ADMIN";

    @Bean
    public SecurityFilterChain filtroSeguranca(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint naoAutenticado = (request, response, erro) ->
                responderErro(response, HttpStatus.UNAUTHORIZED, MENSAGEM_401);
        AccessDeniedHandler semPermissao = (request, response, erro) ->
                responderErro(response, HttpStatus.FORBIDDEN, MENSAGEM_403);

        http
                // CSRF desativado: o token vai no header Authorization, que o navegador nunca envia sozinho.
                // se um dia o token for para cookie, o CSRF precisa voltar
                .csrf(AbstractHttpConfigurer::disable)
                // API sem sessão: cada requisição se identifica pelo próprio token
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(regras -> regras
                        // erros que o Spring encaminha internamente para /error mantêm o status real
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // públicos: visitante, usuário e admin
                        .requestMatchers(HttpMethod.GET, "/missoes", "/missoes/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/contatos").permitAll()
                        .requestMatchers(HttpMethod.POST, "/autenticacao/login").permitAll()

                        // só admin
                        .requestMatchers(HttpMethod.POST, "/missoes").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/missoes/{id}").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/missoes/{id}").hasRole(ADMIN)
                        // leitura e exclusão de contatos (os endpoints chegam na Etapa 7)
                        .requestMatchers("/contatos", "/contatos/**").hasRole(ADMIN)

                        // regra padrão: tudo que não foi liberado acima exige admin.
                        // endpoint novo começa fechado e só é aberto de propósito
                        .anyRequest().hasRole(ADMIN))

                // lê o "Authorization: Bearer <token>" e confere assinatura e validade
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converterPerfil()))
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(semPermissao))

                // sem token numa rota protegida: 401; com token mas sem o perfil: 403
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(semPermissao));

        return http.build();
    }

    // senhas sempre como hash BCrypt (nunca em texto)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // chave HS256 a partir do JWT_SECRET (lido só pelo nome, sem valor padrão).
    // sem a variável, o Spring nem chega aqui: falha ao resolver ${JWT_SECRET}.
    // a mensagem de erro nunca mostra o valor
    @Bean
    public SecretKey chaveJwt(@Value("${zyra.jwt.segredo}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < SEGREDO_MINIMO_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET precisa ter pelo menos " + SEGREDO_MINIMO_BYTES + " caracteres.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // assina os tokens gerados no login (AutenticacaoBO)
    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return NimbusJwtEncoder.withSecretKey(chaveJwt).algorithm(MacAlgorithm.HS256).build();
    }

    // confere os tokens recebidos: assinatura com a mesma chave e validade (exp)
    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // transforma o claim "perfil" do token em papel do Spring: ADMIN -> ROLE_ADMIN, USUARIO -> ROLE_USUARIO
    private JwtAuthenticationConverter converterPerfil() {
        JwtGrantedAuthoritiesConverter perfis = new JwtGrantedAuthoritiesConverter();
        perfis.setAuthoritiesClaimName(AutenticacaoBO.CLAIM_PERFIL);
        perfis.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(perfis);
        return conversor;
    }

    // mesmo formato do ErroSaidaDTO: {"message": "..."}
    // escrito à mão porque estes erros acontecem antes do resource (o TratadorGlobalExceptions não vê)
    private static void responderErro(HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        response.setStatus(status.value());
        if (status == HttpStatus.UNAUTHORIZED) {
            // o HTTP pede que o 401 diga qual tipo de autenticação a API aceita
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + mensagem + "\"}");
    }
}
