package br.com.fiap.tami.modelo;

import br.com.fiap.tami.motor.Destino;

import java.time.Duration;
import java.time.LocalDateTime;

/** Uma conversa com a Tami, do relato até o destino. */
public class Conversa {

    private Long id;
    private Long idUsuario;
    private Long idPessoa;
    private int idadeMeses;
    private boolean guardar;
    private String relato;
    private String codigoRegiao;
    private Integer sinaisTotal;
    private Integer sinaisRegiao;
    private String queixa;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private Destino destino;
    private String prioridadeUbs;
    private String codigoMensagem;
    private boolean crise;
    private boolean porNaoSei;
    private int perguntas;

    public boolean encerrada() { return destino != null; }

    /** Duração em segundos (null se ainda não acabou). */
    public Long segundos() {
        return inicio == null || fim == null ? null : Duration.between(inicio, fim).toSeconds();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public Long getIdPessoa() { return idPessoa; }
    public void setIdPessoa(Long idPessoa) { this.idPessoa = idPessoa; }
    public int getIdadeMeses() { return idadeMeses; }
    public void setIdadeMeses(int idadeMeses) { this.idadeMeses = idadeMeses; }
    public boolean isGuardar() { return guardar; }
    public void setGuardar(boolean guardar) { this.guardar = guardar; }
    public String getRelato() { return relato; }
    public void setRelato(String relato) { this.relato = relato; }
    public String getCodigoRegiao() { return codigoRegiao; }
    public void setCodigoRegiao(String codigoRegiao) { this.codigoRegiao = codigoRegiao; }
    public Integer getSinaisTotal() { return sinaisTotal; }
    public void setSinaisTotal(Integer sinaisTotal) { this.sinaisTotal = sinaisTotal; }
    public Integer getSinaisRegiao() { return sinaisRegiao; }
    public void setSinaisRegiao(Integer sinaisRegiao) { this.sinaisRegiao = sinaisRegiao; }
    public String getQueixa() { return queixa; }
    public void setQueixa(String queixa) { this.queixa = queixa; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public Destino getDestino() { return destino; }
    public void setDestino(Destino destino) { this.destino = destino; }
    public String getPrioridadeUbs() { return prioridadeUbs; }
    public void setPrioridadeUbs(String prioridadeUbs) { this.prioridadeUbs = prioridadeUbs; }
    public String getCodigoMensagem() { return codigoMensagem; }
    public void setCodigoMensagem(String codigoMensagem) { this.codigoMensagem = codigoMensagem; }
    public boolean isCrise() { return crise; }
    public void setCrise(boolean crise) { this.crise = crise; }
    public boolean isPorNaoSei() { return porNaoSei; }
    public void setPorNaoSei(boolean porNaoSei) { this.porNaoSei = porNaoSei; }
    public int getPerguntas() { return perguntas; }
    public void setPerguntas(int perguntas) { this.perguntas = perguntas; }
}
