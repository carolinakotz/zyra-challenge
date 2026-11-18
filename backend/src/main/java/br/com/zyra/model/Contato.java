package br.com.zyra.model;

import java.time.LocalDateTime;

// representa uma linha da tabela ZYRA2_CONTATO
// telefone pode ser null (é opcional)
public class Contato {

    private Long idContato;
    private String nome;
    private String sobrenome;
    private String email;
    private String telefone;
    private String mensagem;
    private LocalDateTime dataEnvio;

    public Contato() {
    }

    public Contato(Long idContato, String nome, String sobrenome, String email,
                   String telefone, String mensagem, LocalDateTime dataEnvio) {
        this.idContato = idContato;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.telefone = telefone;
        this.mensagem = mensagem;
        this.dataEnvio = dataEnvio;
    }

    public Long getIdContato() {
        return idContato;
    }

    public void setIdContato(Long idContato) {
        this.idContato = idContato;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }
}
