package com.erp;

import java.io.*;
import java.util.*;

public class Estoque {
    private List<Produto> produtos;
    private List<Titulo> titulos;
    private List<Pessoa> pessoas;

    private static final String PRODUTOS_ARQUIVO = "database/produtos.txt";
    private static final String TITULOS_ARQUIVO = "database/titulos.txt";
    private static final String PESSOAS_ARQUIVO = "database/pessoas.txt";

    public Estoque() throws IOException {
        File dbDir = new File("database/");
        dbDir.mkdir();
        
        produtos = new ArrayList<>();
        titulos = new ArrayList<>();
        pessoas = new ArrayList<>();
        carregaProduto();
        carregaTitulos();
        carregaPessoas();
    }

    public void addPessoa(Scanner scanner) throws IOException {
        System.out.print("ID da Pessoa: ");
        String id = scanner.nextLine();
        System.out.print("Nome da Pessoa: ");
        String nome = scanner.nextLine();
        System.out.print("Tipo de Pessoa (1-Cliente, 2-Fornecedor, 3-Funcionário): ");
        int tipo = scanner.nextInt();
        scanner.nextLine();

        Pessoa pessoa = new Pessoa(id, tipo, nome);
        pessoas.add(pessoa);
        savePessoas();
        System.out.println("Pessoa adicionada com sucesso.");
    }

    public void addProduto(Scanner scanner) throws IOException {
        System.out.print("ID do Produto: ");
        String id = scanner.nextLine();
        System.out.print("Nome do Produto: ");
        String nome = scanner.nextLine();
        System.out.print("Preço de compra do Produto: ");
        double precoCompra = scanner.nextDouble();
        scanner.nextLine();
        System.out.print("Preço de venda do Produto: ");
        double precoVenda = scanner.nextDouble();
        scanner.nextLine();
        System.out.print("Quantidade de estoque inicial: ");
        int quantidade = scanner.nextInt();
        scanner.nextLine();

        Produto produto = new Produto(id, nome, precoCompra, precoVenda, quantidade);
        produtos.add(produto);
        saveProdutos();
        System.out.println("Produto adicionado com sucesso.");
    }

    public void listaProdutos() {
        System.out.println("Produtos:");
        for (Produto produto : produtos) {
            System.out.println("Id:" + produto.getId() + " | " + produto.getNome() + " | Preço de compra: R$ " + produto.getPrecoCompra() + " | Preço de venda: R$ " + produto.getPrecoVenda() + " | Quantidade em estoque: " + produto.getQuantidade());
        }
    }

    public void compraProduto(Scanner scanner) throws IOException {
        System.out.print("ID do Produto a comprar: ");
        String produtoId = scanner.nextLine();

        Produto produto = null;
        for (Produto p : produtos) {
            if (p.getId().equals(produtoId)) {
                produto = p;
                break;
            }
        }

        if (produto != null) {
            int quantidade = 0;
            do {
                System.out.print("Quantidade: ");
                quantidade = scanner.nextInt();
                scanner.nextLine();
            } while (quantidade <= 0);
            

            Pessoa fornecedor = buscarPessoaPorTipo(scanner, 2);
            if (fornecedor == null) {
                System.out.println("Fornecedor não encontrado.");
                return;
            }

            Titulo titulo = new Titulo(UUID.randomUUID().toString(), produto.getPrecoCompra(), quantidade, false, fornecedor.getId(), "a pagar");
            titulos.add(titulo);
            saveTitulos();
            produto.adicionarEstoque(quantidade);
            saveProdutos();
            LogService.logCompra(produto, quantidade, fornecedor.getId());
            System.out.println("Compra registrada. Título a pagar gerado: " + titulo.getId());
        } else {
            System.out.println("Produto não encontrado.");
        }
    }

    public void vendaProduto(Scanner scanner) throws IOException {
        System.out.print("ID do Produto a vender: ");
        String produtoId = scanner.nextLine();

        Produto produto = null;
        for (Produto p : produtos) {
            if (p.getId().equals(produtoId)) {
                produto = p;
                break;
            }
        }

        if (produto != null) {
            int quantidade = 0;
            do {
                System.out.print("Quantidade: ");
                quantidade = scanner.nextInt();
                scanner.nextLine();
            } while (quantidade <= 0);

            if ((produto.getQuantidade() - quantidade) < 0) {
                System.out.println("Estoque insuficiente! restam apenas: " + produto.getQuantidade());
                return;
            }

            Pessoa cliente = buscarPessoaPorTipo(scanner, 1);
            if (cliente == null) {
                System.out.println("Cliente não encontrado.");
                return;
            }

            Titulo titulo = new Titulo(UUID.randomUUID().toString(), produto.getPrecoVenda(), quantidade, false, cliente.getId(), "a receber");
            titulos.add(titulo);
            saveTitulos();
            produto.removerEstoque(quantidade);
            saveProdutos();
            LogService.logVenda(produto, quantidade, cliente.getId());
            System.out.println("Venda registrada. Título a receber gerado: " + titulo.getId());
        } else {
            System.out.println("Produto não encontrado.");
        }
    }

    public void fazPagamento(Scanner scanner) throws IOException {
        System.out.print("ID do Título a pagar: ");
        String tituloId = scanner.nextLine();

        Titulo titulo = null;
        for (Titulo t : titulos) {
            if (t.getId().equals(tituloId)) {
                titulo = t;
                break;
            }
        }

        if (titulo != null) {
            if (!titulo.isPago()) {
                titulo.setPaga(true);
                saveTitulos();
                System.out.println("Título pago com sucesso.");
            } else {
                System.out.println("O título já foi pago.");
            }
        } else {
            System.out.println("Título não encontrado.");
        }
    }

    public void listarTitulosDeDestaque() {
        System.out.println("Títulos em Aberto:");
        for (Titulo title : titulos) {
            if (!title.isPago()) {
                System.out.println(title.getId() + " | R$ " + title.getValor() + " | Quantidade: " + title.getQuantidade() + " | Total: R$ " + title.getValor() * ((double) title.getQuantidade()) + " | Pessoa: " + title.getPessoaId() + " | Tipo: " + title.getTipoTitulo());
            }
        }
    }

    private Pessoa buscarPessoaPorTipo(Scanner scanner, int tipo) {
        System.out.print("ID da Pessoa (tipo " + tipo + "): ");
        String id = scanner.nextLine();
        for (Pessoa p : pessoas) {
            if (p.getId().equals(id) && p.getTipo() == tipo) {
                return p;
            }
        }
        return null;
    }

    private Pessoa buscarPessoaPorId(Scanner scanner, String id) {
        for (Pessoa p : pessoas) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private void carregaProduto() throws IOException {
        File file = new File(PRODUTOS_ARQUIVO);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    produtos.add(Produto.fromString(line));
                }
            }
        }
    }

    private void carregaTitulos() throws IOException {
        File file = new File(TITULOS_ARQUIVO);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    titulos.add(Titulo.fromString(line));
                }
            }
        }
    }

    private void carregaPessoas() throws IOException {
        File file = new File(PESSOAS_ARQUIVO);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    pessoas.add(Pessoa.fromString(line));
                }
            }
        }
    }

    private void saveProdutos() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRODUTOS_ARQUIVO))) {
            for (Produto product : produtos) {
                writer.write(product.toString());
                writer.newLine();
            }
        }
    }

    private void saveTitulos() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TITULOS_ARQUIVO))) {
            for (Titulo title : titulos) {
                writer.write(title.toString());
                writer.newLine();
            }
        }
    }

    private void savePessoas() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PESSOAS_ARQUIVO))) {
            for (Pessoa pessoa : pessoas) {
                writer.write(pessoa.toString());
                writer.newLine();
            }
        }
    }

    public void listaPessoas() {
        System.out.println("Pessoas:");
        for (Pessoa pessoa : pessoas) {
            System.out.println("Id: " + pessoa.getId() + " | Nome: \"" + pessoa.getNome() + "\" | Tipo: " + pessoa.getTipo());
        }
    }

    public void editaPessoa(Scanner scanner) throws IOException {
        if (pessoas.isEmpty()) {
            System.out.println("Nenhuma pessoa cadastrada para editar.");
            return;
        }

        System.out.print("Digite o ID da pessoa que deseja editar: ");
        String id = scanner.nextLine();
        Pessoa pessoa = buscarPessoaPorId(scanner, id.trim());

        if (pessoa != null) {
            System.out.println("Editando: " + pessoa.getNome());

            System.out.print("Novo ID (deixe em branco para não alterar): ");
            String novoId = scanner.nextLine();
            if (!novoId.trim().isEmpty()) {
                pessoa.setId(novoId.trim());
            }

            System.out.print("Novo Nome (deixe em branco para não alterar): ");
            String novoNome = scanner.nextLine();
            if (!novoNome.trim().isEmpty()) {
                pessoa.setNome(novoNome.trim());
            }

            int novoTipo = 0;
            String linha;
            while(true) {
                System.out.print("Novo Tipo [1-Cliente, 2-Fornecedor, 3-Funcionário] (deixe em branco para não alterar): ");
                linha = scanner.nextLine();
                if (linha.trim().isEmpty())
                    break;
                try {
                    novoTipo = Integer.parseInt(linha.trim());
                    if (novoTipo >= 1 && novoTipo <=3)
                        break;
                } catch (NumberFormatException e) {
                    continue;
                }
            }
            if (novoTipo != 0)
                pessoa.setTipo(novoTipo);

            savePessoas(); // Salva as alterações no arquivo
            System.out.println("Pessoa editada com sucesso!");
        } else {
            System.out.println("ID não encontrado!");
        }
    }

    public void removePessoa(Scanner scanner) throws IOException {
        if (pessoas.isEmpty()) {
            System.out.println("Nenhuma pessoa cadastrada para remover.");
            return;
        }

        System.out.print("Digite o ID da pessoa que deseja remover: ");
        String id = scanner.nextLine();
        Pessoa pessoa = buscarPessoaPorId(scanner, id);

        if (pessoa != null) {
            Pessoa removida = pessoas.remove(pessoas.indexOf(pessoa));
            savePessoas(); // Salva as alterações no arquivo
            System.out.println("Pessoa \"" + removida.getNome() + "\" removida com sucesso!");
        } else {
            System.out.println("ID não encontrado!");
        }
    }
}
