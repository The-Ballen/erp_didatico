package com.erp;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Estoque estoque = new Estoque();

        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Gerenciar Produtos");
            System.out.println("2. Gerenciar Pessoas");
            System.out.println("3. Análise Preditiva");
            System.out.println("4. Listar Títulos em Aberto");
            System.out.println("5. Efetuar Pagamento");
            System.out.println("6. Sair");
            int choice = 0;
            boolean subMenu = false;
            System.out.print("Escolha uma opção: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consumir '\n' do buffer
            

            switch (choice) {
                case 1:
                    subMenu = true;
                    while (subMenu) {
                        System.out.println("\nGerenciamento de Produtos:");
                        System.out.println("1. Listar Produtos");
                        System.out.println("2. Adicionar Produto");
                        //System.out.println("3. Editar Produto (em breve)");
                        //System.out.println("4. Remover Produto (em breve)");
                        System.out.println("3. Comprar Produto (Fornecedor)");
                        System.out.println("4. Vender Produto (Cliente)");
                        System.out.println("5. Relatório");
                        System.out.println("6. Voltar");
                        
                        choice = 0;
                        System.out.print("Escolha uma opção: ");
                        choice = scanner.nextInt();
                        scanner.nextLine();

                        switch (choice) {
                            case 1:
                                estoque.listaProdutos();
                                break;
                            case 2:
                                estoque.addProduto(scanner);
                                break;
                            case 3:
                                estoque.compraProduto(scanner);;
                                break;
                            case 4:
                                estoque.vendaProduto(scanner);
                                break;
                            case 5:
                                LogService.VerLog(scanner);
                                break;
                            case 6:
                                subMenu = false;
                                break;
                    
                            default:
                                System.out.println("Opção inválida. Tente novamente.");
                                break;
                        }
                    }
                    break;
                case 2:
                    subMenu = true;
                    while (subMenu) {
                        System.out.println("\nGerenciamento de Pessoas:");
                        System.out.println("1. Listar Pessoas");
                        System.out.println("2. Cadastrar Pessoas");
                        System.out.println("3. Editar Pessoas");
                        System.out.println("4. Remover Pessoas");
                        System.out.println("5. Voltar");

                        choice = 0;
                        System.out.print("Escolha uma opção: ");
                        choice = scanner.nextInt();
                        scanner.nextLine();
                        switch (choice) {
                            case 1:
                                estoque.listaPessoas();;
                                break;
                            case 2:
                                estoque.addPessoa(scanner);
                                break;
                            case 3:
                                estoque.editaPessoa(scanner);
                                break;
                            case 4:
                                estoque.removePessoa(scanner);
                                break;
                            case 5:
                                subMenu = false;
                                break;
                    
                            default:
                                System.out.println("Opção inválida. Tente novamente.");
                                break;
                        }
                    }
                    break;
                case 3:
                    subMenu = true;
                    while (subMenu) {
                        System.out.println("\nAnálise Preditiva:");
                        System.out.println("1. Análise de Curva ABC");
                        System.out.println("2. Previsão de Demanda");
                        System.out.println("3. Voltar");

                        choice = 0;
                        System.out.print("Escolha uma opção: ");
                        choice = scanner.nextInt();
                        scanner.nextLine();

                        switch (choice) {
                            case 1:
                                AnalisePreditiva.executarAnaliseCurvaABC();
                                break;
                            case 2:
                                AnalisePreditiva.executarPrevisaoDeDemandaPonderada();
                                break;
                            case 3:
                                subMenu = false;
                                break;
                            default:
                                System.out.println("Opção inválida. Tente novamente.");
                                break;
                        }
                    }
                    break;
                case 4:
                    estoque.listarTitulosDeDestaque();
                    break;
                case 5:
                    estoque.fazPagamento(scanner);
                    break;
                case 6:
                    System.out.println("Saindo...");
                    return;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }
}
