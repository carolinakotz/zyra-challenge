package br.com.zyra.model;

// perfis de acesso; os nomes são exatamente os valores aceitos na coluna PERFIL da ZYRA2_USUARIO
// o perfil é definido só pelo backend: nenhum DTO de entrada tem esse campo
public enum Perfil {
    USUARIO,
    ADMIN
}
