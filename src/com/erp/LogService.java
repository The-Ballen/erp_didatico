package com.erp;

import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LogService {
    // Mudança no formato da data para ser amigável ao SQL (ISO 8601)
    private static final SimpleDateFormat sdfData = new SimpleDateFormat("yyyy-MM-dd"); 
    private static final SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss");

    public static void logCompra(Produto produto, int quantidade, String pessoaId) {
        log("COMPRA", pessoaId, produto.getId(), quantidade);
    }

    public static void logVenda(Produto produto, int quantidade, String pessoaId) {
        log("VENDA", pessoaId, produto.getId(), quantidade);
    }

    private static void log(String tipo, String pessoaId, String produtoID, int quantidade) {
        String data = sdfData.format(new Date());
        String hora = sdfHora.format(new Date());
        String sql = "INSERT INTO Logs(Tipo, PessoaID, ProdutoID, Quantidade, Data, Hora) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tipo);
            pstmt.setString(2, pessoaId);
            pstmt.setString(3, produtoID);
            pstmt.setInt(4, quantidade);
            pstmt.setString(5, data);
            pstmt.setString(6, hora);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao gravar log no banco de dados: " + e.getMessage());
        }
    }

    // função para ver o log do sistema separado por data


    public static void VerLog(Scanner scanner) {
        // Formato que o usuário digita
        SimpleDateFormat sdfUsuario = new SimpleDateFormat("dd/MM/yyyy");
        // Formato que está no banco
        SimpleDateFormat sdfBanco = new SimpleDateFormat("yyyy-MM-dd");

        try {
            System.out.print("Digite a data inicial (dd/MM/yyyy): ");
            String dataInicialStr = scanner.nextLine();
            Date dataInicialUtil = sdfUsuario.parse(dataInicialStr);
            String dataInicialSql = sdfBanco.format(dataInicialUtil); // Converte para YYYY-MM-DD

            System.out.print("Digite a data final (dd/MM/yyyy): ");
            String dataFinalStr = scanner.nextLine();
            Date dataFinalUtil = sdfUsuario.parse(dataFinalStr);
            String dataFinalSql = sdfBanco.format(dataFinalUtil); // Converte para YYYY-MM-DD

            // SQL para buscar logs no intervalo de datas
            // Como armazenamos em YYYY-MM-DD, podemos comparar como texto
            String sql = "SELECT * FROM Logs WHERE Data >= ? AND Data <= ? ORDER BY Data, Hora";
            
            try (Connection conn = DbManager.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, dataInicialSql);
                pstmt.setString(2, dataFinalSql);
                
                ResultSet rs = pstmt.executeQuery();

                System.out.println("\n--- Logs de " + dataInicialStr + " a " + dataFinalStr + " ---");
                // Cabeçalho idêntico ao original
                System.out.println("Tipo,PessoaID,ProdutoID,Quantidade,Data,Hora"); 

                boolean encontrou = false;
                while (rs.next()) {
                    encontrou = true;
                    // Recria a linha no formato original (ou como preferir)
                    // Converte a data do banco (YYYY-MM-DD) de volta para (DD/MM/YYYY) para exibição
                    Date dataLogUtil = sdfBanco.parse(rs.getString("Data"));
                    String dataLogFormatada = sdfUsuario.format(dataLogUtil);

                    String linha = String.join(",",
                        rs.getString("Tipo"),
                        rs.getString("PessoaID"),
                        rs.getString("ProdutoID"),
                        String.valueOf(rs.getInt("Quantidade")),
                        dataLogFormatada, // Exibe no formato amigável
                        rs.getString("Hora")
                    );
                    System.out.println(linha);
                }

                if (!encontrou) {
                    System.out.println("Nenhum registro encontrado nesse período.");
                }

            } catch (SQLException e) {
                 System.err.println("Erro ao consultar os logs: " + e.getMessage());
            } catch (ParseException e) {
                System.err.println("Erro ao reformatar data do log: " + e.getMessage());
            }
            System.out.println("--- Fim dos Logs ---");

        } catch (ParseException e) {
            System.err.println("Formato de data inválido. Use dd/MM/yyyy.");
        }
    }
}