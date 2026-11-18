package br.com.zyra.dto;

import java.time.LocalDateTime;

// o que a API devolve depois de gravar uma mensagem de contato
// telefone vem null quando não foi informado
public record ContatoSaidaDTO(
        Long idContato,
        String nome,
        String sobrenome,
        String email,
        String telefone,
        String mensagem,
        LocalDateTime dataEnvio
) {
}
