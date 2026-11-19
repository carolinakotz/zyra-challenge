package br.com.zyra.dto;

import java.time.Instant;

// o que o POST /autenticacao/login devolve
// token: vai no header "Authorization: Bearer <token>" das próximas requisições
// perfil: "ADMIN" ou "USUARIO", para o frontend decidir o que mostrar (a proteção real é no backend)
// expiraEm: quando o token vence, em UTC (ex.: "2026-11-18T15:00:00Z")
public record LoginSaidaDTO(
        String token,
        String perfil,
        Instant expiraEm
) {
}
