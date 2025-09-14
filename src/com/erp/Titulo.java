package com.erp;

public class Titulo {
    private String id;
    private double valor;
    private int quantidade;
    private boolean paga;
    private String pessoaId;
    private String tipoTitulo; // "a pagar" ou "a receber"

    public Titulo(String id, double valor, int quantidade, boolean paga, String pessoaId, String tipoTitulo) {
        this.id = id;
        this.valor = valor;
        this.quantidade = quantidade;
        this.paga = paga;
        this.pessoaId = pessoaId;
        this.tipoTitulo = tipoTitulo;
    }

    public String getId() {
        return id;
    }

    public double getValor() {
        return valor;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public boolean isPago() {
        return paga;
    }

    public void setPaga(boolean paga) {
        this.paga = paga;
    }

    public String getPessoaId() {
        return pessoaId;
    }

    public String getTipoTitulo() {
        return tipoTitulo;
    }

    @Override
    public String toString() {
        return id + "," + valor + "," + quantidade + "," + paga + "," + pessoaId + "," + tipoTitulo;
    }

    public static Titulo fromString(String str) {
        String[] parts = str.split(",");
        return new Titulo(parts[0], Double.parseDouble(parts[1]), Integer.parseInt(parts[2]), Boolean.parseBoolean(parts[3]), parts[4], parts[5]);
    }
}