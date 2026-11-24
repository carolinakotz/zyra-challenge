package br.com.zyra.dto;

import java.time.Instant;

// o que a API devolve depois de gravar uma mensagem de contato
// telefone vem null quando não foi informado
// dataEnvio sai em UTC com o fuso no texto (ex.: "2026-11-24T20:42:00Z"); o navegador converte para o horário local
public record ContatoSaidaDTO(
        Long idContato,
        String nome,
        String sobrenome,
        String email,
        String telefone,
        String mensagem,
        Instant dataEnvio
) {
}
