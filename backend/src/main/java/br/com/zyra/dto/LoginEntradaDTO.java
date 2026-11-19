package br.com.zyra.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// o que chega no POST /autenticacao/login
// sem campo "perfil": o perfil é definido só pelo backend
// e-mail até 100 (tamanho da coluna); senha até 72, o limite do BCrypt
public record LoginEntradaDTO(

        @NotBlank(message = "Informe o e-mail.")
        @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        @NotBlank(message = "Informe a senha.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String senha
) {
}
