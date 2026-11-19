package br.com.zyra.model;

import java.time.LocalDateTime;

// representa uma linha da tabela ZYRA2_USUARIO
// senhaHash guarda o hash BCrypt, nunca a senha; este model nunca vai para a resposta da API
public class Usuario {

    private Long idUsuario;
    private String email;
    private String senhaHash;
    private Perfil perfil;
    private LocalDateTime dataCriacao;

    public Usuario() {
    }

    public Usuario(Long idUsuario, String email, String senhaHash, Perfil perfil, LocalDateTime dataCriacao) {
        this.idUsuario = idUsuario;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.dataCriacao = dataCriacao;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
