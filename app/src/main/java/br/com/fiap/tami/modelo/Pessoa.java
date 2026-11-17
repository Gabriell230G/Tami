package br.com.fiap.tami.modelo;

import br.com.fiap.tami.motor.Perfil;

import java.time.LocalDateTime;

/** Uma das "Pessoas que eu cuido". Só guarda o que ajuda a orientar: idade, gravidez e diabetes. */
public class Pessoa {

    public static final int IDADE_MAXIMA_MESES = 1500;

    private Long id;
    private Long idUsuario;
    private String apelido;
    private Relacao relacao;
    private int idadeMeses;
    private boolean gestante;
    private boolean diabetes;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;

    public Pessoa() {}

    public Pessoa(Long idUsuario, String apelido, Relacao relacao, int idadeMeses) {
        this.idUsuario = idUsuario;
        setApelido(apelido);
        setRelacao(relacao);
        setIdadeMeses(idadeMeses);
    }

    /** O motor de regras só precisa disto: idade em meses, gravidez e diabetes. */
    public Perfil comoPerfil() { return new Perfil(idadeMeses, gestante, diabetes); }

    /** "8 meses", "3 anos", "67 anos". */
    public String idadeEscrita() {
        if (idadeMeses < 24) return idadeMeses + (idadeMeses == 1 ? " mês" : " meses");
        int anos = idadeMeses / 12;
        return anos + " anos";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public String getApelido() { return apelido; }
    public void setApelido(String apelido) {
        if (apelido == null || apelido.isBlank()) throw new IllegalArgumentException("Dê um nome ou apelido para a pessoa.");
        if (apelido.strip().length() > 60) throw new IllegalArgumentException("O apelido pode ter até 60 letras.");
        this.apelido = apelido.strip();
    }

    public Relacao getRelacao() { return relacao; }
    public void setRelacao(Relacao relacao) {
        if (relacao == null) throw new IllegalArgumentException("Diga quem é a pessoa para você.");
        this.relacao = relacao;
    }

    public int getIdadeMeses() { return idadeMeses; }
    public void setIdadeMeses(int idadeMeses) {
        if (idadeMeses < 0 || idadeMeses > IDADE_MAXIMA_MESES) throw new IllegalArgumentException("Idade fora do esperado.");
        this.idadeMeses = idadeMeses;
    }

    public boolean isGestante() { return gestante; }
    public void setGestante(boolean gestante) { this.gestante = gestante; }

    public boolean isDiabetes() { return diabetes; }
    public void setDiabetes(boolean diabetes) { this.diabetes = diabetes; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }

    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
