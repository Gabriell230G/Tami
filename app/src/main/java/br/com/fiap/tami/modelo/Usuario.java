package br.com.fiap.tami.modelo;

import java.time.LocalDateTime;

/** Quem usa o app. O consentimento para dados de saúde é separado e começa desligado (LGPD, art. 11). */
public class Usuario {

    private Long id;
    private String nome;
    private String contato;
    private boolean consentimentoSaude;
    private LocalDateTime dataConsentimento;
    private LocalDateTime dataCadastro;

    public Usuario() {}

    public Usuario(String nome, String contato) {
        setNome(nome);
        setContato(contato);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Diga como você quer ser chamado.");
        if (nome.strip().length() > 80) throw new IllegalArgumentException("O nome pode ter até 80 letras.");
        this.nome = nome.strip();
    }

    public String getContato() { return contato; }
    public void setContato(String contato) {
        if (contato == null || contato.isBlank()) throw new IllegalArgumentException("Informe um e-mail ou celular.");
        String c = contato.strip().toLowerCase();
        boolean email = c.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
        boolean celular = c.replaceAll("\\D", "").length() >= 10 && c.matches("[\\d\\s()+-]+");
        if (!email && !celular) throw new IllegalArgumentException("Use um e-mail ou um celular com DDD.");
        this.contato = celular ? c.replaceAll("\\D", "") : c;
    }

    public boolean isConsentimentoSaude() { return consentimentoSaude; }
    public void setConsentimentoSaude(boolean consentimentoSaude) { this.consentimentoSaude = consentimentoSaude; }

    public LocalDateTime getDataConsentimento() { return dataConsentimento; }
    public void setDataConsentimento(LocalDateTime dataConsentimento) { this.dataConsentimento = dataConsentimento; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
}
