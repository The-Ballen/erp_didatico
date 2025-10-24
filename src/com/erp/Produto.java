package com.erp;

public class Produto {
    private String id;
    private String nome;
    private double precoCompra;
    private double precoVenda;
    private int quantidade;
    private String categoria;

    public Produto(String id, String nome, double precoCompra, double precoVenda, int quantidade, String categoria) {
        this.id = id;
        this.nome = nome;
        this.precoCompra = precoCompra;
        this.precoVenda = precoVenda;
        this.quantidade = quantidade;
        this.categoria = categoria;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getPrecoCompra() {
        return precoCompra;
    }

    public double getPrecoVenda() {
        return precoVenda;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getCategoria() {
        return categoria;
    }

    public boolean adicionarEstoque(int qt) {
        if (qt > 0) {
            this.quantidade += qt;
            return true;
        }
        return false;
    }

    public boolean removerEstoque(int qt) {
        if ((this.quantidade - qt) >= 0) {
            this.quantidade -= qt;
            return true;
        }
        return false;
    }

    /*
    @Override
    public String toString() {
        return id + "," + nome + "," + precoCompra + "," + precoVenda + "," + quantidade + "," + categoria;
    }
    

    public static Produto fromString(String str) {
        String[] parts = str.split(",");
        return new Produto(parts[0], parts[1], Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Integer.parseInt(parts[4]));
    }
    */
}
