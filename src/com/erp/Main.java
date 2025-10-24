package com.erp;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Estoque estoque = new Estoque();

        while (true) {
            System.out.println(LanguageService.getString("main.menu.title"));
            System.out.println(LanguageService.getString("main.menu.option1"));
            System.out.println(LanguageService.getString("main.menu.option2"));
            System.out.println(LanguageService.getString("main.menu.option3"));
            System.out.println(LanguageService.getString("main.menu.option4"));
            System.out.println(LanguageService.getString("main.menu.option5"));
            System.out.println(LanguageService.getString("main.menu.option6"));
            System.out.println(LanguageService.getString("main.menu.option7"));

            int choice = 0;
            boolean subMenu = false;
            System.out.print(LanguageService.getString("prompt.choice"));
            choice = scanner.nextInt();
            scanner.nextLine(); // Consumir '\n' do buffer
            

            switch (choice) {
                case 1:
                    subMenu = true;
                    while (subMenu) {
                        System.out.println(LanguageService.getString("product.menu.title"));
                        System.out.println(LanguageService.getString("product.menu.option1"));
                        System.out.println(LanguageService.getString("product.menu.option2"));
                        //System.out.println("3. Editar Produto (em breve)");
                        //System.out.println("4. Remover Produto (em breve)");
                        System.out.println(LanguageService.getString("product.menu.option3"));
                        System.out.println(LanguageService.getString("product.menu.option4"));
                        System.out.println(LanguageService.getString("product.menu.option5"));
                        System.out.println(LanguageService.getString("product.menu.option6"));
                        
                        choice = 0;
                        System.out.print(LanguageService.getString("prompt.choice"));
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
                                System.out.println(LanguageService.getString("option.invalid"));
                                break;
                        }
                    }
                    break;
                case 2:
                    subMenu = true;
                    while (subMenu) {
                        System.out.println(LanguageService.getString("person.menu.title"));
                        System.out.println(LanguageService.getString("person.menu.option1"));
                        System.out.println(LanguageService.getString("person.menu.option2"));
                        System.out.println(LanguageService.getString("person.menu.option3"));
                        System.out.println(LanguageService.getString("person.menu.option4"));
                        System.out.println(LanguageService.getString("person.menu.option5"));

                        choice = 0;
                        System.out.print(LanguageService.getString("prompt.choice"));
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
                                System.out.println(LanguageService.getString("option.invalid"));
                                break;
                        }
                    }
                    break;
                case 3:
                    subMenu = true;
                    while (subMenu) {
                        System.out.println(LanguageService.getString("predictive.menu.title"));
                        System.out.println(LanguageService.getString("predictive.menu.option1"));
                        System.out.println(LanguageService.getString("predictive.menu.option2"));
                        System.out.println(LanguageService.getString("predictive.menu.option3"));

                        choice = 0;
                        System.out.print(LanguageService.getString("prompt.choice"));
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
                                System.out.println(LanguageService.getString("option.invalid"));
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
                    // Menu de Alteração de Idioma
                    System.out.println("\n1. Português (Brasil)");
                    System.out.println("2. English (US)");
                    System.out.print(LanguageService.getString("prompt.choice"));
                    choice = scanner.nextInt();
                    scanner.nextLine();
                    switch(choice) {
                        case 1:
                            LanguageService.setLocale("pt", "BR");
                            break;
                        case 2:
                            LanguageService.setLocale("en", "US");
                            break;
                        default:
                            System.out.println(LanguageService.getString("option.invalid"));
                            break;
                    }
                    break;
                case 7:
                    System.out.println(LanguageService.getString("main.exit.message"));
                    return;
                default:
                    System.out.println(LanguageService.getString("option.invalid"));
            }
        }
    }
}
