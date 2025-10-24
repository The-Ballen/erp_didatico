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
        System.out.print(LanguageService.getString("stock.person.prompt.id"));
        String id = scanner.nextLine();
        System.out.print(LanguageService.getString("stock.person.prompt.name"));
        String nome = scanner.nextLine();
        System.out.print(LanguageService.getString("stock.person.prompt.type"));
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
            System.out.println(LanguageService.getString("stock.person.add.success"));

        } catch (SQLException e) {
            throw new RuntimeException(LanguageService.getFormattedString("error.person.add", e.getMessage()));
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
        System.out.print(LanguageService.getString("stock.product.prompt.id"));
        String id = scanner.nextLine();
        System.out.print(LanguageService.getString("stock.product.prompt.name"));
        String nome = scanner.nextLine();
        System.out.print(LanguageService.getString("stock.product.prompt.purchase_price"));
        double precoCompra = scanner.nextDouble();
        scanner.nextLine();
        System.out.print(LanguageService.getString("stock.product.prompt.sale_price"));
        double precoVenda = scanner.nextDouble();
        scanner.nextLine();

        // --- INÍCIO DA INTEGRAÇÃO WEKA (FASE 2) ---
        String categoriaSugerida = "Outros"; // Default em caso de falha
        try {
            // Chama o método de predição
            categoriaSugerida = AnalisePreditiva.preverCategoria(nome, precoVenda);
        } catch (Exception e) {
            System.err.println("\nAVISO: Não foi possível sugerir a categoria.");
            //System.err.println("Detalhe: " + e.getMessage()); // Descomente para debug
            System.err.println("Certifique-se de que o modelo foi treinado (Menu Análise -> Opção 3).");
        }

        // Mostra a sugestão e permite ao usuário aceitar (dando Enter) ou sobrescrever
        System.out.print("Categoria do Produto (Sugerido: " + categoriaSugerida + "): ");
        String categoria = scanner.nextLine();
        if (categoria.trim().isEmpty()) {
            categoria = categoriaSugerida; // Usuário aceitou a sugestão
            System.out.println("Usando categoria sugerida: " + categoria);
        }
        // --- FIM DA INTEGRAÇÃO WEKA ---

        System.out.print(LanguageService.getString("stock.product.prompt.initial_stock"));
        int quantidade = scanner.nextInt();
        scanner.nextLine();

        Produto produto = new Produto(id, nome, precoCompra, precoVenda, quantidade, categoria);

        String sql = "INSERT INTO Produtos(id, nome, precoCompra, precoVenda, quantidade, categoria) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbManager.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.setString(2, nome);
            pstmt.setDouble(3, precoCompra);
            pstmt.setDouble(4, precoVenda);
            pstmt.setInt(5, quantidade);
            pstmt.setString(6, categoria);
            pstmt.executeUpdate();

            produtos.add(produto);
            System.out.println(LanguageService.getString("stock.product.add.success"));

        } catch (SQLException e) {
            throw new RuntimeException(LanguageService.getFormattedString("error.product.add", e.getMessage()));
        }
    }

    /**
     * Prints a formatted list of all products currently loaded in memory.
     */
    public void listaProdutos() {
        System.out.println(LanguageService.getString("stock.product.list.title"));
        for (Produto produto : produtos) {
            System.out.println(LanguageService.getFormattedString("stock.product.list.details",
                produto.getId(),
                produto.getNome(),
                produto.getPrecoCompra(),
                produto.getPrecoVenda(),
                produto.getQuantidade())
            );
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
        System.out.print(LanguageService.getString("stock.product.prompt.buy"));
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
                System.out.print(LanguageService.getString("stock.product.prompt.quantity"));
                quantidade = scanner.nextInt();
                scanner.nextLine();
            } while (quantidade <= 0);
            
            Pessoa fornecedor = buscarPessoaPorTipo(scanner, 2); // 2 = Fornecedor
            if (fornecedor == null) {
                System.out.println(LanguageService.getString("stock.person.supplier.notfound"));
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
                    System.out.println(LanguageService.getFormattedString("stock.product.buy.success", titulo.getId()));

                } catch (SQLException e) {
                    conn.rollback(); // Desfaz a transação em caso de erro
                    System.err.println(LanguageService.getFormattedString("error.purchase.register", e.getMessage()));
                } finally {
                    conn.setAutoCommit(true); // Reabilita o auto-commit
                }
            } catch (SQLException e) {
                System.err.println(LanguageService.getFormattedString("error.purchase.connection", e.getMessage()));
            }
        } else {
            System.out.println(LanguageService.getString("stock.product.notfound"));
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
        System.out.print(LanguageService.getString("stock.product.prompt.sell"));
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
                System.out.print(LanguageService.getString("stock.product.prompt.quantity"));
                quantidade = scanner.nextInt();
                scanner.nextLine();
            } while (quantidade <= 0);

            if ((produto.getQuantidade() - quantidade) < 0) {
                System.out.println(LanguageService.getFormattedString("stock.product.insufficient_stock", produto.getQuantidade()));
                return;
            }

            Pessoa cliente = buscarPessoaPorTipo(scanner, 1); // 1 = Cliente
            if (cliente == null) {
                System.out.println(LanguageService.getString("stock.person.customer.notfound"));
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
                    System.out.println(LanguageService.getFormattedString("stock.product.sell.success", titulo.getId()));

                } catch (SQLException e) {
                    conn.rollback(); 
                    System.err.println(LanguageService.getFormattedString("error.sale.register", e.getMessage()));
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println(LanguageService.getFormattedString("error.sale.connection", e.getMessage()));
            }
        } else {
            System.out.println(LanguageService.getString("stock.product.notfound"));
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
        System.out.print(LanguageService.getString("stock.title.prompt.pay"));
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
                        System.out.println(LanguageService.getString("stock.title.pay.success"));
                    } else {
                        System.out.println(LanguageService.getString("error.title.notfound.db"));
                    }

                } catch (SQLException e) {
                    System.err.println(LanguageService.getFormattedString("error.payment.generic", e.getMessage()));
                }
            } else {
                System.out.println(LanguageService.getString("stock.title.already_paid"));
            }
        } else {
            System.out.println(LanguageService.getString("stock.title.notfound"));
        }
    }

    /**
     * Prints a formatted list of all open titles from the in-memory list.
     */
    public void listarTitulosDeDestaque() {
        System.out.println(LanguageService.getString("stock.title.list.open"));
        for (Titulo title : titulos) {
            if (!title.isPago()) {
                 System.out.println(LanguageService.getFormattedString("stock.title.list.details",
                    title.getId(), title.getValor(), title.getQuantidade(),
                    title.getValor() * title.getQuantidade(), title.getPessoaId(), title.getTipoTitulo()));
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
        System.out.print(LanguageService.getFormattedString("stock.person.prompt.by_type", tipo));
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
                    rs.getInt("quantidade"),
                    rs.getString("categoria")
                );
                produtos.add(produto);
            }
        } catch (SQLException e) {
            throw new RuntimeException(LanguageService.getString("error.db.load_products") + e.getMessage(), e);
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
            throw new RuntimeException(LanguageService.getString("error.db.load_titles") + e.getMessage(), e);
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
            throw new RuntimeException(LanguageService.getString("error.db.load_people") + e.getMessage(), e);
        }
    }

    /**
     * Prints a formatted list of all people from the in-memory list.
     */
    public void listaPessoas() {
        System.out.println(LanguageService.getString("stock.person.list.title"));
        for (Pessoa pessoa : pessoas) {
            System.out.println(LanguageService.getFormattedString("stock.person.list.details",
                pessoa.getId(), pessoa.getNome(), pessoa.getTipo()));
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
            System.out.println(LanguageService.getString("stock.person.edit.none"));
            return;
        }

        System.out.print(LanguageService.getString("stock.person.prompt.edit"));
        String id = scanner.nextLine();
        Pessoa pessoa = buscarPessoaPorId(id.trim()); // Usa a lista em memória, que é mais rápida

        if (pessoa != null) {
            System.out.println(LanguageService.getFormattedString("stock.person.editing", pessoa.getNome()));

            // Variáveis temporárias para o SQL
            String idOriginal = pessoa.getId();
            String novoId = pessoa.getId();
            String novoNome = pessoa.getNome();
            int novoTipo = pessoa.getTipo();

            System.out.print(LanguageService.getString("stock.person.prompt.new_id"));
            String inputId = scanner.nextLine();
            if (!inputId.trim().isEmpty()) {
                novoId = inputId.trim();
            }

            System.out.print(LanguageService.getString("stock.person.prompt.new_name"));
            String inputNome = scanner.nextLine();
            if (!inputNome.trim().isEmpty()) {
                novoNome = inputNome.trim();
            }

            String linha;
            while(true) {
                System.out.print(LanguageService.getString("stock.person.prompt.new_type"));
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
                    System.out.println(LanguageService.getString("stock.person.edit.success"));
                } else {
                    System.out.println(LanguageService.getString("error.id.notfound.db"));
                }
            } catch (SQLException e) {
                System.err.println(LanguageService.getFormattedString("error.person.edit", e.getMessage()));
            }
        } else {
            System.out.println(LanguageService.getString("stock.person.id_notfound"));
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
            System.out.println(LanguageService.getString("stock.person.remove.none"));
            return;
        }

        System.out.print(LanguageService.getString("stock.person.prompt.remove"));
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
                    System.out.println(LanguageService.getFormattedString("stock.person.remove.success", pessoa.getNome()));
                } else {
                    System.out.println(LanguageService.getString("error.id.notfound.db"));
                }
            } catch (SQLException e) {
                // Trata erro de chave estrangeira (se a pessoa tiver títulos associados)
                // O código de erro "19" é específico do SQLite para violação de constraint
                if (e.getErrorCode() == 19) {
                    System.err.println(LanguageService.getString("error.person.remove.constraint"));
                } else {
                    System.err.println(LanguageService.getFormattedString("error.person.remove.generic", e.getMessage()));
                }
            }
        } else {
            System.out.println(LanguageService.getString("stock.person.id_notfound"));
        }
    }
}
