package com.erp;

import java.io.*;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages in-memory database,
 * and handles all persistence logic for these entities.
 */
public class Estoque {
    private List<Produto> produtos;
    private List<Titulo> titulos;
    private List<Pessoa> pessoas;

    /**
     * Initializes persistence layer.
     * <p>
     * Ensures the database schema is created and loads all
     * database into memory.
     *
     * @throws RuntimeException if database initialization or data loading fails.
     */
    public Estoque() throws RuntimeException {
        DbManager.initializeDatabase();
        
        produtos = new ArrayList<>();
        titulos = new ArrayList<>();
        pessoas = new ArrayList<>();
        
        carregaPessoas();
        carregaProduto();
        carregaTitulos();
    }

    /**
     * Prompts the user for details and inserts a new record into the database.
     * <p>
     * After a successful database insertion, the new record is also loaded
     * to the in-memory list.
     *
     * @param scanner The Scanner instance to read user input.
     * @throws RuntimeException if the database connection fails.
     */
    public void addPessoa(Scanner scanner) throws RuntimeException {
        System.out.print("ID da Pessoa: ");
        String id = scanner.nextLine();
        System.out.print("Nome da Pessoa: ");
        String nome = scanner.nextLine();
        System.out.print("Tipo de Pessoa (1-Cliente, 2-Fornecedor, 3-Funcionário): ");
        int tipo = scanner.nextInt();
        scanner.nextLine();

        Pessoa pessoa = new Pessoa(id, tipo, nome);
        String sql = "INSERT INTO Pessoas(id, nome, tipo) VALUES(?, ?, ?)";
        try (Connection conn = DbManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.setString(2, nome);
            pstmt.setInt(3, tipo);
            pstmt.executeUpdate();

            // Adiciona à memória APÓS sucesso no banco de dados
            pessoas.add(pessoa);
            System.out.println("Pessoa adicionada com sucesso.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar pessoa: " + e.getMessage());
        }
    }

    /**
     * Prompts the user for details and inserts a new record into the database.
     * <p>
     * After a successful database insertion, the new record is also loaded
     * to the in-memory list.
     *
     * @param scanner The Scanner instance to read user input.
     * @throws RuntimeException if the database connection fails.
     */
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

        String sql = "INSERT INTO Produtos(id, nome, precoCompra, precoVenda, quantidade) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DbManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.setString(2, nome);
            pstmt.setDouble(3, precoCompra);
            pstmt.setDouble(4, precoVenda);
            pstmt.setInt(5, quantidade);
            pstmt.executeUpdate();

            produtos.add(produto);
            System.out.println("Produto adicionado com sucesso.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar produto: " + e.getMessage());
        }
    }

    /**
     * Prints a formatted list of all products currently loaded in memory.
     */
    public void listaProdutos() {
        System.out.println("Produtos:");
        for (Produto produto : produtos) {
            System.out.println("Id:" + produto.getId() + " | " + produto.getNome() + " | Preço de compra: R$ " + produto.getPrecoCompra() + " | Preço de venda: R$ " + produto.getPrecoVenda() + " | Quantidade em estoque: " + produto.getQuantidade());
        }
    }

    /**
     * Registers a purchase from a supplier.
     * <p>
     * Executes a database transaction to:
     * <p>
     * 1. Create a new open title.
     * <p>
     * 2. Update stock.
     * <p>
     * It also updates in-memory lists and logs the purchase.
     *
     * @param scanner The Scanner instance to read user input.
     * @throws RuntimeException if database connection fails.
     */
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
            
            Pessoa fornecedor = buscarPessoaPorTipo(scanner, 2); // 2 = Fornecedor
            if (fornecedor == null) {
                System.out.println("Fornecedor não encontrado.");
                return;
            }

            Titulo titulo = new Titulo(UUID.randomUUID().toString(), produto.getPrecoCompra(), quantidade, false, fornecedor.getId(), "a pagar");
            
            String sqlTitulo = "INSERT INTO Titulos(id, valor, quantidade, paga, pessoaId, tipoTitulo) VALUES(?, ?, ?, ?, ?, ?)";
            String sqlProduto = "UPDATE Produtos SET quantidade = ? WHERE id = ?";

            try (Connection conn = DbManager.connect()) {
                // Desabilita temporariamente o auto-commit
                conn.setAutoCommit(false); 

                try (PreparedStatement pstmtTitulo = conn.prepareStatement(sqlTitulo);
                     PreparedStatement pstmtProduto = conn.prepareStatement(sqlProduto)) {

                    // 1. Insere o Título
                    pstmtTitulo.setString(1, titulo.getId());
                    pstmtTitulo.setDouble(2, titulo.getValor());
                    pstmtTitulo.setInt(3, titulo.getQuantidade());
                    pstmtTitulo.setBoolean(4, titulo.isPago());
                    pstmtTitulo.setString(5, titulo.getPessoaId());
                    pstmtTitulo.setString(6, titulo.getTipoTitulo());
                    pstmtTitulo.executeUpdate();

                    // 2. Atualiza o Produto
                    int novoEstoque = produto.getQuantidade() + quantidade;
                    pstmtProduto.setInt(1, novoEstoque);
                    pstmtProduto.setString(2, produto.getId());
                    pstmtProduto.executeUpdate();
                    
                    // 3. Confirma a transação
                    conn.commit(); 

                    // 4. Atualiza listas em memória
                    titulos.add(titulo);
                    produto.adicionarEstoque(quantidade);
                    
                    // 5. Log (o LogService será modificado também)
                    LogService.logCompra(produto, quantidade, fornecedor.getId());
                    System.out.println("Compra registrada. Título a pagar gerado: " + titulo.getId());

                } catch (SQLException e) {
                    conn.rollback(); // Desfaz a transação em caso de erro
                    System.err.println("Erro ao registrar compra: " + e.getMessage());
                } finally {
                    conn.setAutoCommit(true); // Reabilita o auto-commit
                }
            } catch (SQLException e) {
                System.err.println("Erro de conexão ao comprar: " + e.getMessage());
            }
        } else {
            System.out.println("Produto não encontrado.");
        }
    }

    /**
     * Registers a sale to a costumer.
     * <p>
     * Executes a database transaction to:
     * <p>
     * 1. Create a new open title.
     * <p>
     * 2. Update stock.
     * <p>
     * It also updates in-memory lists and logs the sale.
     *
     * @param scanner The Scanner instance to read user input.
     * @throws RuntimeException if database connection fails.
     */
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

            Pessoa cliente = buscarPessoaPorTipo(scanner, 1); // 1 = Cliente
            if (cliente == null) {
                System.out.println("Cliente não encontrado.");
                return;
            }

            Titulo titulo = new Titulo(UUID.randomUUID().toString(), produto.getPrecoVenda(), quantidade, false, cliente.getId(), "a receber");
            
            String sqlTitulo = "INSERT INTO Titulos(id, valor, quantidade, paga, pessoaId, tipoTitulo) VALUES(?, ?, ?, ?, ?, ?)";
            String sqlProduto = "UPDATE Produtos SET quantidade = ? WHERE id = ?";

            try (Connection conn = DbManager.connect()) {
                conn.setAutoCommit(false); 

                try (PreparedStatement pstmtTitulo = conn.prepareStatement(sqlTitulo);
                     PreparedStatement pstmtProduto = conn.prepareStatement(sqlProduto)) {

                    // 1. Insere o Título
                    pstmtTitulo.setString(1, titulo.getId());
                    pstmtTitulo.setDouble(2, titulo.getValor());
                    pstmtTitulo.setInt(3, titulo.getQuantidade());
                    pstmtTitulo.setBoolean(4, titulo.isPago());
                    pstmtTitulo.setString(5, titulo.getPessoaId());
                    pstmtTitulo.setString(6, titulo.getTipoTitulo());
                    pstmtTitulo.executeUpdate();

                    // 2. Atualiza o Produto
                    int novoEstoque = produto.getQuantidade() - quantidade;
                    pstmtProduto.setInt(1, novoEstoque);
                    pstmtProduto.setString(2, produto.getId());
                    pstmtProduto.executeUpdate();
                    
                    conn.commit(); 

                    // 3. Atualiza listas em memória
                    titulos.add(titulo);
                    produto.removerEstoque(quantidade);
                    
                    LogService.logVenda(produto, quantidade, cliente.getId());
                    System.out.println("Venda registrada. Título a receber gerado: " + titulo.getId());

                } catch (SQLException e) {
                    conn.rollback(); 
                    System.err.println("Erro ao registrar venda: " + e.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Erro de conexão ao vender: " + e.getMessage());
            }
        } else {
            System.out.println("Produto não encontrado.");
        }
    }

    /**
     * Marks a title as paid in the database.
     * <p>
     * Prompts the user for a Titulo ID. <p>If found and not already paid,
     * it updates the status in the database and in-memory list.
     *
     * @param scanner The Scanner instance to read user input.
     * @throws RuntimeException if the database connection fails.
     */
    public void fazPagamento(Scanner scanner) throws RuntimeException {
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
                String sql = "UPDATE Titulos SET paga = ? WHERE id = ?";
                
                try (Connection conn = DbManager.connect();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    
                    pstmt.setBoolean(1, true);
                    pstmt.setString(2, tituloId);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        titulo.setPaga(true); // Atualiza objeto em memória
                        System.out.println("Título pago com sucesso.");
                    } else {
                        System.out.println("Erro: Título não encontrado no banco de dados.");
                    }

                } catch (SQLException e) {
                    System.err.println("Erro ao fazer pagamento: " + e.getMessage());
                }
        } else {
            System.out.println("Título não encontrado.");
        }
    }
}

    /**
     * Prints a formatted list of all open titles from the in-memory list.
     */
    public void listarTitulosDeDestaque() {
        System.out.println("Títulos em Aberto:");
        for (Titulo title : titulos) {
            if (!title.isPago()) {
                System.out.println(title.getId() + " | R$ " + title.getValor() + " | Quantidade: " + title.getQuantidade() + " | Total: R$ " + title.getValor() * ((double) title.getQuantidade()) + " | Pessoa: " + title.getPessoaId() + " | Tipo: " + title.getTipoTitulo());
            }
        }
    }

    /**
     * Searches in-memory list for a person by ID and type.
     *
     * @param scanner The Scanner instance to read user input.
     * @param tipo The required type (1-Cliente, 2-Fornecedor, 3-Funcionário).
     * @return The matching {@code Pessoa} object, or {@code null} if not found.
     */
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

    /**
     * Searches in-memory list for a person by their ID.
     *
     * @param id The ID to search for.
     * @return The matching {@code Pessoa} object, or {@code null} if not found.
     */
    private Pessoa buscarPessoaPorId(String id) {
        for (Pessoa p : pessoas) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Loads all records from product table into memory.
     *
     * @throws RuntimeException if database query fails.
     */
    private void carregaProduto() throws RuntimeException {
        String sql = "SELECT * FROM Produtos";
        
        try (Connection conn = DbManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            produtos.clear(); // Limpa a lista antes de carregar
            while (rs.next()) {
                Produto produto = new Produto(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getDouble("precoCompra"),
                    rs.getDouble("precoVenda"),
                    rs.getInt("quantidade")
                );
                produtos.add(produto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fatal Error: Could not load products from database. " + e.getMessage(), e);
        }
    }

    /**
     * Loads all records from title table into memory.
     *
     * @throws RuntimeException if database query fails.
     */
    private void carregaTitulos() throws RuntimeException {
        String sql = "SELECT * FROM Titulos";
        
        try (Connection conn = DbManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            titulos.clear();
            while (rs.next()) {
                Titulo titulo = new Titulo(
                    rs.getString("id"),
                    rs.getDouble("valor"),
                    rs.getInt("quantidade"),
                    rs.getBoolean("paga"),
                    rs.getString("pessoaId"),
                    rs.getString("tipoTitulo")
                );
                titulos.add(titulo);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fatal Error: Could not load titulos from database. " + e.getMessage(), e);
        }
    }

    /**
     * Loads all records from person table into memory.
     *
     * @throws RuntimeException if database query fails.
     */
    private void carregaPessoas() throws RuntimeException {
        String sql = "SELECT * FROM Pessoas";
        
        try (Connection conn = DbManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            pessoas.clear();
            while (rs.next()) {
                Pessoa pessoa = new Pessoa(
                    rs.getString("id"),
                    rs.getInt("tipo"),
                    rs.getString("nome")
                );
                pessoas.add(pessoa);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fatal Error: Could not load pessoas from database. " + e.getMessage(), e);
        }
    }

    /**
     * Prints a formatted list of all people from the in-memory list.
     */
    public void listaPessoas() {
        System.out.println("Pessoas:");
        for (Pessoa pessoa : pessoas) {
            System.out.println("Id: " + pessoa.getId() + " | Nome: \"" + pessoa.getNome() + "\" | Tipo: " + pessoa.getTipo());
        }
    }

    /**
     * Edits an existing person's details.
     * <p>
     * Prompts the user for the ID of the person to edit, then asks for new values.
     * <p>
     * Blank inputs are ignored.
     * <p>
     * Updates the database and in-memory list.
     *
     * @param scanner The Scanner instance to read user input.
     * @throws RuntimeException if the database connection fails.
     */
    public void editaPessoa(Scanner scanner) throws RuntimeException {
        if (pessoas.isEmpty()) {
            System.out.println("Nenhuma pessoa cadastrada para editar.");
            return;
        }

        System.out.print("Digite o ID da pessoa que deseja editar: ");
        String id = scanner.nextLine();
        Pessoa pessoa = buscarPessoaPorId(id.trim()); // Usa a lista em memória, que é mais rápida

        if (pessoa != null) {
            System.out.println("Editando: " + pessoa.getNome());

            // Variáveis temporárias para o SQL
            String idOriginal = pessoa.getId();
            String novoId = pessoa.getId();
            String novoNome = pessoa.getNome();
            int novoTipo = pessoa.getTipo();

            System.out.print("Novo ID (deixe em branco para não alterar): ");
            String inputId = scanner.nextLine();
            if (!inputId.trim().isEmpty()) {
                novoId = inputId.trim();
            }

            System.out.print("Novo Nome (deixe em branco para não alterar): ");
            String inputNome = scanner.nextLine();
            if (!inputNome.trim().isEmpty()) {
                novoNome = inputNome.trim();
            }

            String linha;
            while(true) {
                System.out.print("Novo Tipo [1-Cliente, 2-Fornecedor, 3-Funcionário] (deixe em branco para não alterar): ");
                linha = scanner.nextLine();
                if (linha.trim().isEmpty())
                    break;
                try {
                    int inputTipoInt = Integer.parseInt(linha.trim());
                    if (inputTipoInt >= 1 && inputTipoInt <=3) {
                        novoTipo = inputTipoInt;
                        break;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }

            // Atualiza no banco de dados
            String sql = "UPDATE Pessoas SET id = ?, nome = ?, tipo = ? WHERE id = ?";
            try (Connection conn = DbManager.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, novoId);
                pstmt.setString(2, novoNome);
                pstmt.setInt(3, novoTipo);
                pstmt.setString(4, idOriginal); // Cláusula WHERE usa o ID original
                
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    // Atualiza o objeto na lista em memória
                    pessoa.setId(novoId);
                    pessoa.setNome(novoNome);
                    pessoa.setTipo(novoTipo);
                    System.out.println("Pessoa editada com sucesso!");
                } else {
                     System.out.println("ID não encontrado no banco de dados!");
                }
            } catch (SQLException e) {
                System.err.println("Erro ao editar pessoa: " + e.getMessage());
            }
        } else {
            System.out.println("ID não encontrado!");
        }
    }

    /**
     * Removes a person from database and in-memory list.
     * <p>
     * Prompts the user for the ID of the person to remove.
     * <p>
     * Handles database foreign key constraints by catching the SQLException
     * and informing the user if the Pessoa is associated with any Titulos.
     *
     * @param scanner The Scanner instance to read user input.
     * @throws RuntimeException if the database connection fails.
     */
    public void removePessoa(Scanner scanner) throws IOException {
        if (pessoas.isEmpty()) {
            System.out.println("Nenhuma pessoa cadastrada para remover.");
            return;
        }

        System.out.print("Digite o ID da pessoa que deseja remover: ");
        String id = scanner.nextLine();
        Pessoa pessoa = buscarPessoaPorId(id); // Busca na memória

        if (pessoa != null) {
            String sql = "DELETE FROM Pessoas WHERE id = ?";
            try (Connection conn = DbManager.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, id);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    // Remove da lista em memória
                    Pessoa removida = pessoas.remove(pessoas.indexOf(pessoa));
                    System.out.println("Pessoa \"" + removida.getNome() + "\" removida com sucesso!");
                } else {
                    System.out.println("ID não encontrado no banco de dados.");
                }
            } catch (SQLException e) {
                // Trata erro de chave estrangeira (se a pessoa tiver títulos associados)
                // O código de erro "19" é específico do SQLite para violação de constraint
                if (e.getErrorCode() == 19) { 
                     System.err.println("Erro: Não é possível remover esta pessoa pois ela possui títulos (compras/vendas) registrados.");
                } else {
                     System.err.println("Erro ao remover pessoa: " + e.getMessage());
                }
            }
        } else {
            System.out.println("ID não encontrado!");
        }
    }
}
