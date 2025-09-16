package com.erp;

// Novas bibliotecas para a função ver log do sistema
import java.util.Scanner;
import java.text.ParseException;
import java.io.BufferedReader;
import java.io.FileReader;
//
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LogService {
    private static final String LOG_DIR = "logs/";
    private static final String LOG_FILE = LOG_DIR + "logs_compras_vendas.txt";
    private static final SimpleDateFormat sdfData = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss");

    static {
        // Cria pasta de logs se não existir
        File dir = new File(LOG_DIR);
        if (!dir.exists()) dir.mkdir();

        // Cria arquivo se não existir
        File file = new File(LOG_FILE);
        try {
            if (!file.exists()) {
                file.createNewFile();
                // Escreve cabeçalho
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    writer.write("Tipo,PessoaID,Produto,Quantidade,Data,Hora");
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logCompra(Produto produto, int quantidade, String pessoaId) {
        log("COMPRA", pessoaId, produto.getNome(), quantidade);
    }

    public static void logVenda(Produto produto, int quantidade, String pessoaId) {
        log("VENDA", pessoaId, produto.getNome(), quantidade);
    }

    private static void log(String tipo, String pessoaId, String produto, int quantidade) {
        String data = sdfData.format(new Date());
        String hora = sdfHora.format(new Date());
        String linha = tipo + "," + pessoaId + "," + produto + "," + quantidade + "," + data + "," + hora;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(linha);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    // função para ver o log do sistema separado por data


    public static void VerLog(Scanner scanner) {
        try {
            System.out.print("Digite a data inicial (dd/MM/yyyy): ");
            String dataInicialStr = scanner.nextLine();
            Date dataInicial = sdfData.parse(dataInicialStr);

            System.out.print("Digite a data final (dd/MM/yyyy): ");
            String dataFinalStr = scanner.nextLine();
            Date dataFinal = sdfData.parse(dataFinalStr);

            System.out.println("\n--- Logs de " + dataInicialStr + " a " + dataFinalStr + " ---");
            System.out.println("Tipo,PessoaID,Produto,Quantidade,Data,Hora\n"); // Imprime o cabeçalho para o usuário

            File file = new File(LOG_FILE);
            if (!file.exists()) {
                System.out.println("Arquivo de log não encontrado.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine(); // Pula o cabeçalho do arquivo

                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) { // Garante que a linha tem todos os campos
                        try {
                            Date logDate = sdfData.parse(parts[4].trim());
                            // Checa se a data do log está no intervalo (inclusivo)
                            if (!logDate.before(dataInicial) && !logDate.after(dataFinal)) {
                                System.out.println(line);
                            }
                        } catch (ParseException e) {
                            System.err.println("Aviso: Erro ao analisar a data na linha do log: " + line);
                        }
                    }
                }
            }
            System.out.println("\n--- Fim dos Logs ---");

        } catch (ParseException e) {
            System.err.println("Formato de data inválido. Use dd/MM/yyyy.");
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo de log: " + e.getMessage());
        }
    }
}






