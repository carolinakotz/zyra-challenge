package br.com.zyra.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

// limita quantas requisições um mesmo IP pode fazer em cada grupo de rotas;
// passou do limite, responde 429 e a requisição nem chega ao resource
//
// roda logo depois do CorsFilter (HIGHEST_PRECEDENCE + 1): assim o 429 já sai com os
// headers de CORS e o frontend consegue ler o {"message": "..."}
// (o preflight OPTIONS é respondido pelo CorsFilter e nem chega aqui)
//
// a contagem fica em memória, numa janela fixa por IP e por regra:
// o contador começa na primeira requisição e zera quando o tempo da regra acaba.
// Limitação: cada instância do app conta sozinha e tudo zera ao reiniciar.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LimiteRequisicoesFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LimiteRequisicoesFilter.class);

    // ---- limites (fáceis de ajustar) ----
    private static final int CONTATO_ENVIO_MAXIMO = 3;
    private static final Duration CONTATO_ENVIO_JANELA = Duration.ofMinutes(10);

    private static final int LOGIN_MAXIMO = 5;
    private static final Duration LOGIN_JANELA = Duration.ofMinutes(15);

    private static final int ESCRITA_MAXIMO = 60;
    private static final Duration ESCRITA_JANELA = Duration.ofMinutes(1);

    private static final int LEITURA_MAXIMO = 120;
    private static final Duration LEITURA_JANELA = Duration.ofMinutes(1);

    // de quanto em quanto tempo apagar da memória as janelas que já venceram
    private static final long INTERVALO_LIMPEZA_MS = Duration.ofMinutes(1).toMillis();

    private static final String MENSAGEM_LIMITE =
            "Muitas tentativas. Aguarde alguns minutos e tente novamente.";
    // mesmo formato do ErroSaidaDTO: {"message": "..."}
    private static final String CORPO_LIMITE = "{\"message\":\"" + MENSAGEM_LIMITE + "\"}";

    // no Render, a Cloudflare fica na frente e informa neste header o IP de quem se conectou.
    // Não usamos o X-Forwarded-For: o Render não limpa o valor que o cliente manda,
    // então o primeiro IP da lista pode ser inventado.
    private static final String HEADER_IP_CLIENTE = "CF-Connecting-IP";

    // regras da matriz de acesso; cada uma tem o seu contador por IP
    private enum Regra {
        CONTATO_ENVIO(CONTATO_ENVIO_MAXIMO, CONTATO_ENVIO_JANELA),
        LOGIN(LOGIN_MAXIMO, LOGIN_JANELA),
        ESCRITA(ESCRITA_MAXIMO, ESCRITA_JANELA),
        LEITURA(LEITURA_MAXIMO, LEITURA_JANELA);

        private final int maximo;
        private final long duracaoMs;

        Regra(int maximo, Duration janela) {
            this.maximo = maximo;
            this.duracaoMs = janela.toMillis();
        }
    }

    // contagem de uma regra para um IP: até quando a janela vale e quantas requisições já teve
    private record Janela(long fimMs, int total) {
    }

    // chave: "REGRA|ip" (ex.: "CONTATO_ENVIO|203.0.113.7")
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final AtomicLong proximaLimpezaMs = new AtomicLong(0);

    // true quando o app roda no Render (variável definida pelo próprio Render em todo serviço)
    private final boolean rodandoNoRender = "true".equalsIgnoreCase(System.getenv("RENDER"));
    // garante que o aviso de header ausente apareça uma vez só no log
    private final AtomicBoolean avisoHeaderAusenteDado = new AtomicBoolean(false);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Regra regra = encontrarRegra(request);
        if (regra == null) {
            chain.doFilter(request, response);
            return;
        }

        long agora = System.currentTimeMillis();
        limparJanelasVencidas(agora);

        String chave = regra.name() + "|" + descobrirIpCliente(request);
        // compute() é atômico por chave: duas requisições ao mesmo tempo não se perdem na contagem
        Janela janela = janelas.compute(chave, (k, atual) ->
                atual == null || atual.fimMs() <= agora
                        ? new Janela(agora + regra.duracaoMs, 1)
                        // depois do limite não precisa continuar somando
                        : new Janela(atual.fimMs(), Math.min(atual.total() + 1, regra.maximo + 1)));

        if (janela.total() > regra.maximo) {
            responderLimiteExcedido(response, janela.fimMs() - agora);
            return;
        }

        chain.doFilter(request, response);
    }

    // decide em qual regra da matriz a requisição se encaixa; null = não é limitada
    private Regra encontrarRegra(HttpServletRequest request) {
        String metodo = request.getMethod().toUpperCase();
        // getServletPath() já vem decodificado e normalizado pelo Tomcat
        // (ex.: "/%63ontatos" vira "/contatos"), então esses truques não escapam do limite
        String caminho = removerBarraFinal(request.getServletPath());

        switch (metodo) {
            case "OPTIONS":
                // preflight do CORS não conta
                return null;
            case "GET":
            case "HEAD":
                // o Spring responde HEAD com o mesmo código do GET
                return Regra.LEITURA;
            case "POST":
                if (caminho.equals("/contatos")) {
                    return Regra.CONTATO_ENVIO;
                }
                if (caminho.equals("/autenticacao/login")) {
                    return Regra.LOGIN;
                }
                if (caminho.equals("/missoes")) {
                    return Regra.ESCRITA;
                }
                return null;
            case "PUT":
            case "DELETE":
                if (caminho.startsWith("/missoes/") || caminho.startsWith("/contatos/")) {
                    return Regra.ESCRITA;
                }
                return null;
            default:
                return null;
        }
    }

    private String removerBarraFinal(String caminho) {
        if (caminho == null || caminho.isEmpty()) {
            return "/";
        }
        String semBarra = caminho.replaceAll("/+$", "");
        return semBarra.isEmpty() ? "/" : semBarra;
    }

    // no Render: IP que a Cloudflare informa; na máquina local: IP da conexão
    // (localmente nenhum header é aceito, porque qualquer um poderia inventá-lo)
    private String descobrirIpCliente(HttpServletRequest request) {
        if (rodandoNoRender) {
            String ip = request.getHeader(HEADER_IP_CLIENTE);
            if (ip != null && !ip.isBlank()) {
                return ip.trim();
            }
            // sem o header, todo mundo apareceria com o IP do proxy e dividiria o mesmo limite;
            // o aviso não mostra nenhum IP (é dado pessoal)
            if (avisoHeaderAusenteDado.compareAndSet(false, true)) {
                log.warn("Header {} ausente no Render: o limite de requisições está usando o IP da conexão.",
                        HEADER_IP_CLIENTE);
            }
        }
        return request.getRemoteAddr();
    }

    // apaga as janelas que já venceram, no máximo uma vez por minuto,
    // para o mapa não crescer para sempre
    private void limparJanelasVencidas(long agora) {
        long proxima = proximaLimpezaMs.get();
        // só a requisição que conseguir trocar o horário faz a limpeza
        if (agora >= proxima && proximaLimpezaMs.compareAndSet(proxima, agora + INTERVALO_LIMPEZA_MS)) {
            janelas.values().removeIf(janela -> janela.fimMs() <= agora);
        }
    }

    // 429 com Retry-After (segundos até a janela zerar) e o corpo no formato {"message": "..."}
    private void responderLimiteExcedido(HttpServletResponse response, long restanteMs) throws IOException {
        long segundos = Math.max(1, (restanteMs + 999) / 1000);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(CORPO_LIMITE);
    }
}
