package br.com.fiap.tami.dao;

/** Erro de banco já explicado em português (embrulha a SQLException, que é verificada). */
public class DaoException extends RuntimeException {
    public DaoException(String mensagem, Throwable causa) {
        super(mensagem + (causa != null && causa.getMessage() != null ? ": " + causa.getMessage() : ""), causa);
    }
}
