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
            System.out.println("3. Listar Títulos em Aberto");
            System.out.println("4. Efetuar Pagamento");
            System.out.println("5. Sair");
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
                        System.out.println("2. Adicionar Produtos");
                        //System.out.println("3. Editar Produtos (em breve)");
                        //System.out.println("4. Remover Produtos (em breve)");
                        System.out.println("3. Voltar");
                        
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
                        //System.out.println("1. Listar Pessoas (em breve)");
                        System.out.println("1. Cadastrar Pessoas");
                        //System.out.println("3. Editar Pessoas (em breve)");
                        //System.out.println("4. Remover Pessoas (em breve)");
                        System.out.println("2. Voltar");

                        choice = 0;
                        System.out.print("Escolha uma opção: ");
                        choice = scanner.nextInt();
                        scanner.nextLine();
                        switch (choice) {
                            case 1:
                                estoque.addPessoa(scanner);
                                break;
                            case 2:
                                subMenu = false;
                                break;
                    
                            default:
                                System.out.println("Opção inválida. Tente novamente.");
                                break;
                        }
                    }
                    break;
                case 3:
                    estoque.listarTitulosDeDestaque();
                    break;
                case 4:
                    estoque.fazPagamento(scanner);
                    break;
                case 5:
                    System.out.println("Saindo...");
                    return;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }
}
