package com.erp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbManager {

    // Caminho para o arquivo do banco de dados
    private static final String DATABASE_URL = "jdbc:sqlite:database/erp.db";

    /**
     *  Attempt to load the SQLite JDBC driver and establish a connection
     *  using the static {@code DATABASE_URL} defined.
     *  
     *  @return A new {@code Connection} object linked to the database file.
     *  @throws RuntimeException if the SQLite JDBC driver cannot be found 
     *  or if a connection to the database cannot be established.
     */
    public static Connection connect() throws RuntimeException {
        Connection conn = null;
        try {
            // Carrega o driver JDBC do SQLite
            Class.forName("org.sqlite.JDBC");
            // Cria a conexão com o banco
            conn = DriverManager.getConnection(DATABASE_URL);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException("Fatal Error: Could not connect to the database." + e.getMessage(), e);
        }
        return conn;
    }

    /**
     * Initializes the database by creating all necessary tables if they don't exist.
     * <p>
     * Should be called once at startup to ensure the schema is ready.
     *
     * @throws RuntimeException if an {@code SQLException} occurs during execution.
     */
    public static void initializeDatabase() throws RuntimeException {
        String sqlPessoas = "CREATE TABLE IF NOT EXISTS Pessoas ("
                          + " id TEXT PRIMARY KEY,"
                          + " tipo INTEGER NOT NULL,"
                          + " nome TEXT NOT NULL"
                          + ");";

        String sqlProdutos = "CREATE TABLE IF NOT EXISTS Produtos ("
                           + " id TEXT PRIMARY KEY,"
                           + " nome TEXT NOT NULL,"
                           + " precoCompra REAL NOT NULL,"
                           + " precoVenda REAL NOT NULL,"
                           + " quantidade INTEGER NOT NULL,"
                           + " categoria TEXT NOT NULL DEFAULT 'Outros'"
                           + ");";

        String sqlTitulos = "CREATE TABLE IF NOT EXISTS Titulos ("
                          + " id TEXT PRIMARY KEY,"
                          + " valor REAL NOT NULL,"
                          + " quantidade INTEGER NOT NULL,"
                          + " paga BOOLEAN NOT NULL,"
                          + " pessoaId TEXT NOT NULL,"
                          + " tipoTitulo TEXT NOT NULL,"
                          + " FOREIGN KEY (pessoaId) REFERENCES Pessoas(id)"
                          + ");";

        String sqlLogs = "CREATE TABLE IF NOT EXISTS Logs ("
                       + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                       + " Tipo TEXT NOT NULL,"
                       + " PessoaID TEXT NOT NULL,"
                       + " ProdutoID TEXT NOT NULL,"
                       + " Quantidade INTEGER NOT NULL,"
                       + " Data TEXT NOT NULL," // Formato da data alterado para 'yyyy-MM-dd' para facilitar consultas
                       + " Hora TEXT NOT NULL"
                       + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            // Executa os comandos SQL
            stmt.execute(sqlPessoas);
            stmt.execute(sqlProdutos);
            stmt.execute(sqlTitulos);
            stmt.execute(sqlLogs);
            
        } catch (SQLException e) {
            throw new RuntimeException("Fatal Error: Could not initialize database tables." + e.getMessage(), e);
        }
    }
}