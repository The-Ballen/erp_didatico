package com.erp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class AnalisePreditiva {
    /**
     * Estrutura para armazenar dados de faturamento de um produto específico.
     * Facilita a ordenação e o cálculo dos percentuais para a Curva ABCD.
     */
    private static class ProdutoFaturamento {
        Produto produto;
        double faturamentoDoProduto;
        double percentualAcumulado;

        ProdutoFaturamento(Produto produto, double faturamentoDoProduto) {
            this.produto = produto;
            this.faturamentoDoProduto = faturamentoDoProduto;
        }

        public double getFaturamentoDoProduto() {
            return faturamentoDoProduto;
        }
    }

    /**
     * Ponto de entrada principal para a execução da análise da Curva ABCD.
     * Orquestra o carregamento, processamento, classificação e exibição dos dados.
     */
    public static void executarAnaliseCurvaABC() {
        try {
            Map<String, Produto> mapaDeProdutos = carregarProdutosDoArquivo();
            if (mapaDeProdutos.isEmpty()) {
                System.out.println("\nNenhum produto cadastrado para análise.");
                return;
            }

            Map<String, Double> faturamentoPorProduto = apurarFaturamentoDeVendasPorProduto(mapaDeProdutos);
            if (faturamentoPorProduto.isEmpty()) {
                System.out.println("\nNenhuma venda registrada nos logs para análise.");
                return;
            }

            List<ProdutoFaturamento> listaOrdenada = ordenarProdutosPorFaturamento(faturamentoPorProduto, mapaDeProdutos);

            double faturamentoGeral = faturamentoPorProduto.values().stream().mapToDouble(Double::doubleValue).sum();

            Map<Character, List<ProdutoFaturamento>> produtosClassificados = classificarProdutosNaCurvaABCD(listaOrdenada, faturamentoGeral);

            exibirRelatorioFinal(produtosClassificados, faturamentoGeral);

        } catch (IOException e) {
            System.err.println("Erro ao ler os arquivos de dados para análise: " + e.getMessage());
        }
    }

    /**
     * Carrega os produtos do banco de dados e os organiza em um Mapa.
     * @return Um Mapa onde a chave é o ID do produto e o valor é o objeto Produto.
     * @throws IOException Se ocorrer um erro na leitura do banco.
     */
    private static Map<String, Produto> carregarProdutosDoArquivo() throws IOException {
        Map<String, Produto> produtos = new HashMap<>();
        String sql = "SELECT * FROM Produtos";
        
        try (Connection conn = DbManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Produto produto = new Produto(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getDouble("precoCompra"),
                    rs.getDouble("precoVenda"),
                    rs.getInt("quantidade"),
                    rs.getString("categoria")
                );
                produtos.put(produto.getId(), produto);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar produtos para análise: " + e.getMessage());
            // Lança IOException para manter compatibilidade com a assinatura original
            throw new IOException("Erro de banco de dados", e); 
        }
        return produtos;
    }

    /**
     * Processa a tabela de logs para calcular o faturamento total por produto.
     * @param mapaDeProdutos Mapa com os produtos cadastrados para consultar o preço de venda.
     * @return Um Mapa onde a chave é o ID do produto e o valor é o seu faturamento total.
     * @throws IOException Se ocorrer um erro na leitura do banco.
     */
    private static Map<String, Double> apurarFaturamentoDeVendasPorProduto(Map<String, Produto> mapaDeProdutos) throws IOException {
        Map<String, Double> faturamento = new HashMap<>();
        
        // Busca apenas logs de VENDA
        String sql = "SELECT ProdutoID, Quantidade FROM Logs WHERE Tipo = 'VENDA'";

        try (Connection conn = DbManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String produtoId = rs.getString("ProdutoID");
                int quantidade = rs.getInt("Quantidade");

                Produto produto = mapaDeProdutos.get(produtoId);
                if (produto != null) {
                    double valorDaVenda = produto.getPrecoVenda() * quantidade;
                    faturamento.put(produtoId, faturamento.getOrDefault(produtoId, 0.0) + valorDaVenda);
                }
            }
        } catch (SQLException e) {
             System.err.println("Erro ao apurar faturamento dos logs: " + e.getMessage());
             throw new IOException("Erro de banco de dados", e);
        }
        return faturamento;
    }

    /**
     * Converte os mapas de dados em uma lista única e a ordena pelo faturamento em ordem decrescente.
     * @param faturamentoPorProduto Mapa com o faturamento de cada produto.
     * @param mapaDeProdutos Mapa com os dados cadastrais de cada produto.
     * @return Uma lista de objetos ProdutoFaturamento, ordenada do maior para o menor faturamento.
     */
    private static List<ProdutoFaturamento> ordenarProdutosPorFaturamento(Map<String, Double> faturamentoPorProduto, Map<String, Produto> mapaDeProdutos) {
        List<ProdutoFaturamento> listaParaOrdenar = new ArrayList<>();
        for (Map.Entry<String, Double> registro : faturamentoPorProduto.entrySet()) {
            Produto produto = mapaDeProdutos.get(registro.getKey());
            if (produto != null) {
                listaParaOrdenar.add(new ProdutoFaturamento(produto, registro.getValue()));
            }
        }

        listaParaOrdenar.sort(Comparator.comparingDouble(ProdutoFaturamento::getFaturamentoDoProduto).reversed());
        return listaParaOrdenar;
    }

    /**
     * Itera sobre a lista ordenada de produtos para classificá-los nas categorias A, B, C e D.
     * @param produtosOrdenados Lista de produtos já ordenada por faturamento.
     * @param faturamentoGeral A soma do faturamento de todos os produtos.
     * @return Um mapa que categoriza as listas de produtos por classe ('A', 'B', 'C', 'D').
     */
    private static Map<Character, List<ProdutoFaturamento>> classificarProdutosNaCurvaABCD(List<ProdutoFaturamento> produtosOrdenados, double faturamentoGeral) {
        Map<Character, List<ProdutoFaturamento>> produtosClassificados = new HashMap<>();
        produtosClassificados.put('A', new ArrayList<>());
        produtosClassificados.put('B', new ArrayList<>());
        produtosClassificados.put('C', new ArrayList<>());
        produtosClassificados.put('D', new ArrayList<>());

        double faturamentoAcumulado = 0.0;

        // ------------- Solução temporária (corrigida para evitar bugs) -------------
        double epsilon = 1e-9;
        if (faturamentoGeral > epsilon && // Evita divisão por zero se faturamentoGeral for 0
            !produtosOrdenados.isEmpty() &&
            Math.abs(produtosOrdenados.get(0).faturamentoDoProduto - faturamentoGeral) < epsilon) {
            
            produtosClassificados.get('A').add(produtosOrdenados.get(0));
            // os demais (se existirem) vão para a classe D
            for(int i = 1; i < produtosOrdenados.size(); i++) {
                produtosClassificados.get('D').add(produtosOrdenados.get(i));
            }
            return produtosClassificados;
        }
        // ----------------------------------------------

        for (ProdutoFaturamento pf : produtosOrdenados) {
            faturamentoAcumulado += pf.faturamentoDoProduto;
            
            // Proteção contra divisão por zero se faturamentoGeral for 0
            pf.percentualAcumulado = (faturamentoGeral > epsilon) ? (faturamentoAcumulado / faturamentoGeral) * 100.0 : 0.0;

            if (pf.percentualAcumulado <= 70.0) {
                produtosClassificados.get('A').add(pf);
            } else if (pf.percentualAcumulado <= 90.0) {
                produtosClassificados.get('B').add(pf);
            } else if (pf.percentualAcumulado <= 99.0) {
                produtosClassificados.get('C').add(pf);
            } else {
                produtosClassificados.get('D').add(pf);
            }
        }
        return produtosClassificados;
    }

    /**
     * Exibe o relatório final formatado, mostrando os produtos em suas respectivas classes.
     * @param produtosClassificados Mapa com as listas de produtos já classificadas.
     * @param faturamentoGeral O faturamento total para cálculo dos percentuais.
     */
    private static void exibirRelatorioFinal(Map<Character, List<ProdutoFaturamento>> produtosClassificados, double faturamentoGeral) {
        System.out.println("\n======================================================================");
        System.out.println("              Relatório de Análise de Curva ABCD");
        System.out.println("======================================================================");
        System.out.printf(" Faturamento Total Analisado: R$ %.2f%n", faturamentoGeral);

        exibirSecaoDaClasse('A', "Mais importantes (até 70% do faturamento)", produtosClassificados.get('A'), faturamentoGeral);
        exibirSecaoDaClasse('B', "Intermediários (de 70% a 90% do faturamento)", produtosClassificados.get('B'), faturamentoGeral);
        exibirSecaoDaClasse('C', "Menos importantes (de 90% a 99% do faturamento)", produtosClassificados.get('C'), faturamentoGeral);
        exibirSecaoDaClasse('D', "Menos relevantes (1% restante do faturamento)", produtosClassificados.get('D'), faturamentoGeral);

        System.out.println("\n---------------------- Fim do Relatório ----------------------");
    }

    /**
     * Função auxiliar para exibir uma seção (classe) específica do relatório.
     * @param classe A letra da classe (A, B, C ou D).
     * @param descricao A descrição daquela classe.
     * @param produtos A lista de produtos pertencentes à classe.
     * @param faturamentoGeral O faturamento total para cálculo do percentual.
     */
    private static void exibirSecaoDaClasse(char classe, String descricao, List<ProdutoFaturamento> produtos, double faturamentoGeral) {
        System.out.printf("\n--- CLASSE %C (%s) ---%n", classe, descricao);

        if (produtos.isEmpty()) {
            System.out.println("   Nenhum produto nesta classificação.");
            return;
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.printf("%-5s | %-35s | %-15s | %s%n", "ID", "Produto", "Faturamento", "% do Total");
        System.out.println("----------------------------------------------------------------------");

        double epsilon = 1e-9;
        for (ProdutoFaturamento pf : produtos) {
            // Proteção contra divisão por zero
            double percentualIndividual = (faturamentoGeral > epsilon) ? (pf.faturamentoDoProduto / faturamentoGeral) * 100.0 : 0.0;
            System.out.printf("%-5s | %-35.35s | R$ %-12.2f | %.2f%%%n",
                    pf.produto.getId(),
                    pf.produto.getNome(),
                    pf.faturamentoDoProduto,
                    percentualIndividual);
        }
    }

    // --- FIM: LÓGICA DE ANÁLISE DE CURVA ABCD ---


    // ===================================================================================
    // --- INÍCIO: LÓGICA DE PREVISÃO DE DEMANDA POR MÉDIA PONDERADA MENSAL ---
    // ===================================================================================

    // --- CONFIGURAÇÃO DA ANÁLISE ---
    private static final int NUMERO_MESES_ANALISE = 6;
    /**
     * Defina aqui os pesos para cada mês, do mais antigo para o mais recente.
     * A soma de todos os pesos deve ser igual a 1.0 (representando 100%).
     * Exemplo: O mês mais recente (último item) tem o maior peso.
     */
    private static final double[] PESOS_POR_MES = {
            0.02, // Peso para 6 meses atrás
            0.02, // Peso para 5 meses atrás
            0.10, // Peso para 4 meses atrás
            0.20, // Peso para 3 meses atrás
            0.30, // Peso para 2 meses atrás
            0.40  // Peso para o mês mais recente
    };
    // --- FIM DA CONFIGURAÇÃO ---

    /**
     * Ponto de entrada para a execução da análise de Previsão de Demanda com Média Ponderada.
     */
    public static void executarPrevisaoDeDemandaPonderada() {
        if (Math.abs(Arrays.stream(PESOS_POR_MES).sum() - 1.0) > 0.001) {
            System.out.println("\nAVISO: A soma dos pesos configurados é diferente de 1.0. A previsão pode ser imprecisa.");
        }

        try {
            Map<String, Produto> mapaDeProdutos = carregarProdutosDoArquivo();
            if (mapaDeProdutos.isEmpty()) {
                System.out.println("\nNenhum produto cadastrado para análise.");
                return;
            }

            Map<String, List<Integer>> historicoVendas = apurarVendasUltimosMeses(mapaDeProdutos);
            if (historicoVendas.isEmpty()) {
                System.out.println("\nNenhum histórico de vendas encontrado para calcular a previsão.");
                return;
            }

            Map<Produto, Double> previsoes = new HashMap<>();

            for(String produtoId : historicoVendas.keySet()){
                if(!historicoVendas.get(produtoId).stream().allMatch(v -> v == 0)) {
                    List<Integer> seriesDeVendas = historicoVendas.get(produtoId);
                    double previsao = calcularPrevisaoPorMediaPonderada(seriesDeVendas);
                    previsoes.put(mapaDeProdutos.get(produtoId), previsao);
                }
            }

            exibirRelatorioPrevisaoDemanda(previsoes);

        } catch (IOException | ParseException e) {
            System.err.println("Erro ao processar os dados para previsão de demanda: " + e.getMessage());
        }
    }

    /**
     * Agrega as vendas de cada produto por mês e retorna apenas os últimos 6 meses de dados.
     * @param mapaDeProdutos Necessário para validar a existência dos produtos.
     * @return Mapa com ID do produto e uma lista de 6 posições com as quantidades vendidas.
     */
    private static Map<String, List<Integer>> apurarVendasUltimosMeses(Map<String, Produto> mapaDeProdutos) throws IOException, ParseException {
        SimpleDateFormat formatadorData = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, TreeMap<YearMonth, Integer>> vendasAgregadas = new HashMap<>();

        String sql = "SELECT ProdutoID, Quantidade, Data FROM Logs WHERE Tipo = 'VENDA'";

        try (Connection conn = DbManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Date dataVenda = formatadorData.parse(rs.getString("Data")); // Parse YYYY-MM-DD
                YearMonth mesAno = YearMonth.from(dataVenda.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                String produtoId = rs.getString("ProdutoID");
                int quantidade = rs.getInt("Quantidade");

                if (mapaDeProdutos.containsKey(produtoId)) {
                    vendasAgregadas.computeIfAbsent(produtoId, _ -> new TreeMap<>())
                            .merge(mesAno, quantidade, Integer::sum);
                }
            }
        } catch (SQLException e) {
             System.err.println("Erro ao apurar vendas mensais: " + e.getMessage());
             throw new IOException("Erro de banco de dados", e);
        }

        // O restante da lógica deste método para montar o histórico final permanece idêntico
        Map<String, List<Integer>> historicoFinal = new HashMap<>();
        YearMonth mesAtual = YearMonth.now();

        for (String produtoId : mapaDeProdutos.keySet()) {
            TreeMap<YearMonth, Integer> vendasDoProduto = vendasAgregadas.getOrDefault(produtoId, new TreeMap<>());
            List<Integer> ultimasVendas = new ArrayList<>();
            for (int i = NUMERO_MESES_ANALISE - 1; i >= 0; i--) {
                YearMonth mesAlvo = mesAtual.minusMonths(i);
                ultimasVendas.add(vendasDoProduto.getOrDefault(mesAlvo, 0));
            }
            historicoFinal.put(produtoId, ultimasVendas);
        }
        return historicoFinal;
    }

    /**
     * Aplica a média ponderada sobre o histórico de vendas para prever a demanda futura.
     * @param historicoVendas Lista com as vendas dos últimos 6 meses.
     * @return A previsão de demanda (quantidade) para o próximo mês.
     */
    private static double calcularPrevisaoPorMediaPonderada(List<Integer> historicoVendas) {
        double previsaoPonderada = 0.0;
        for (int i = 0; i < NUMERO_MESES_ANALISE; i++) {
            previsaoPonderada += historicoVendas.get(i) * PESOS_POR_MES[i];
        }
        return previsaoPonderada;
    }

    /**
     * Exibe o relatório final com a previsão de demanda para cada produto.
     * @param previsoes Mapa contendo o produto e sua demanda prevista.
     */
    private static void exibirRelatorioPrevisaoDemanda(Map<Produto, Double> previsoes) {
        // Ordena o mapa de previsões pelo valor (previsão) em ordem decrescente
        List<Map.Entry<Produto, Double>> listaOrdenada = previsoes.entrySet()
                .stream()
                .sorted(Map.Entry.<Produto, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        System.out.println("\n======================================================================");
        System.out.println("       Relatório de Previsão de Vendas para o Próximo Mês");
        System.out.println("======================================================================");
        System.out.printf("       Análise baseada na Média Ponderada dos últimos %d meses%n", NUMERO_MESES_ANALISE);
        System.out.println("----------------------------------------------------------------------");
        System.out.printf("%-5s | %-45s | %s%n", "ID", "Produto", "Previsão de Vendas");
        System.out.println("----------------------------------------------------------------------");

        if(listaOrdenada.isEmpty()){
            System.out.println("   Nenhuma previsão pôde ser calculada com os dados atuais.");
        } else {
            for (Map.Entry<Produto, Double> previsao : listaOrdenada) {
                long previsaoArredondada = Math.round(previsao.getValue());
                System.out.printf("%-5s | %-45.45s | %d unidades%n",
                        previsao.getKey().getId(),
                        previsao.getKey().getNome(),
                        previsaoArredondada);
            }
        }
        System.out.println("\n---------------------- Fim do Relatório ----------------------");
    }

    /**
     * Executa o treinamento do classificador J48 com os dados do banco
     * e salva o modelo treinado em disco.
     */
    public static void executarTreinamentoJ48() {
        System.out.println("\n--- Iniciando Treinamento do Classificador J48 ---");
        long inicio = System.nanoTime();

        try {
            // 1. Carregar os dados brutos do banco
            Map<String, Produto> produtosMap = carregarProdutosDoArquivo();
            if (produtosMap.isEmpty()) {
                System.out.println("Nenhum produto no banco para treinar.");
                return;
            }
            List<Produto> produtos = new ArrayList<>(produtosMap.values());

            // 2. Definir os atributos (Features) que o Weka entenderá
            ArrayList<Attribute> atributos = new ArrayList<>();

            // Feature 1: Preço de Venda (Numérico)
            Attribute attPrecoVenda = new Attribute("precoVenda");
            atributos.add(attPrecoVenda);

            // Features 2-N: Palavras-chave do Nome (Numérico, 0 ou 1)
            Attribute attTeclado = new Attribute("tem_palavra_teclado"); atributos.add(attTeclado);
            Attribute attMouse = new Attribute("tem_palavra_mouse"); atributos.add(attMouse);
            Attribute attMonitor = new Attribute("tem_palavra_monitor"); atributos.add(attMonitor);
            Attribute attSsd = new Attribute("tem_palavra_ssd"); atributos.add(attSsd);
            Attribute attHd = new Attribute("tem_palavra_hd"); atributos.add(attHd);
            Attribute attRam = new Attribute("tem_palavra_ram"); atributos.add(attRam);
            Attribute attCabo = new Attribute("tem_palavra_cabo"); atributos.add(attCabo);
            // Adicione mais atributos de palavras-chave aqui se desejar (ex: "gabinete", "fonte")

            // Feature Final: A Classe (Rótulo) que queremos prever
            Set<String> catSet = produtos.stream().map(Produto::getCategoria).collect(Collectors.toSet());
            List<String> categoriasUnicas = new ArrayList<>(catSet);
            Attribute attCategoria = new Attribute("categoria", categoriasUnicas);
            atributos.add(attCategoria);

            // 3. Criar o objeto Instances (conjunto de dados do Weka)
            Instances dados = new Instances("produtos_erp", atributos, produtos.size());
            dados.setClass(attCategoria); // Informa ao Weka qual atributo é a classe

            // 4. Popular o Instances com nossos dados
            for (Produto p : produtos) {
                Instance inst = new DenseInstance(atributos.size());
                inst.setDataset(dados); // Vincula a instância ao cabeçalho/dataset
                String nomeLower = p.getNome().toLowerCase();

                // Define os valores para cada atributo
                inst.setValue(attPrecoVenda, p.getPrecoVenda());
                inst.setValue(attTeclado, nomeLower.contains("teclado") ? 1.0 : 0.0);
                inst.setValue(attMouse, nomeLower.contains("mouse") ? 1.0 : 0.0);
                inst.setValue(attMonitor, nomeLower.contains("monitor") ? 1.0 : 0.0);
                inst.setValue(attSsd, nomeLower.contains("ssd") ? 1.0 : 0.0);
                inst.setValue(attHd, nomeLower.contains("hd") ? 1.0 : 0.0);
                inst.setValue(attRam, nomeLower.contains("ram") ? 1.0 : 0.0);
                inst.setValue(attCabo, nomeLower.contains("cabo") ? 1.0 : 0.0);

                inst.setValue(attCategoria, p.getCategoria()); // Define a classe/rótulo

                dados.add(inst);
            }

            System.out.println("Dados carregados e transformados para o Weka.");
            System.out.println("Iniciando treinamento com " + dados.size() + " instâncias...");

            // 5. Treinar o classificador J48
            J48 classificador = new J48();
            // classificador.setUnpruned(true); // Exemplo de opção do J48
            classificador.buildClassifier(dados);

            // 6. Avaliar o modelo (Cross-validation 10-folds)
            Evaluation avaliacao = new Evaluation(dados);
            avaliacao.crossValidateModel(classificador, dados, 10, new Random(1));

            System.out.println("\n--- Relatório de Performance (Fase 1) ---");
            System.out.println(avaliacao.toSummaryString());
            System.out.println("\n--- Matriz de Confusão ---");
            System.out.println(avaliacao.toMatrixString());

            // 7. Salvar o modelo treinado para uso na Fase 2
            // Garante que a pasta 'model' exista
            new java.io.File("model").mkdirs();
            SerializationHelper.write("model/j48_erp_model.model", classificador);
            // Também salvamos o "cabeçalho" dos dados, essencial para a predição
            SerializationHelper.write("model/j48_erp_header.model", new Instances(dados, 0));
            System.out.println("\n>>> Modelo treinado e salvo em 'model/j48_erp_model.model'");

        } catch (Exception e) {
            System.err.println("ERRO CRÍTICO no treinamento J48: " + e.getMessage());
            e.printStackTrace();
        } finally {
            long fim = System.nanoTime();
            System.out.printf(">>> Tempo Total de Treinamento e Avaliação: %d ms%n", TimeUnit.NANOSECONDS.toMillis(fim - inicio));
            System.out.println("--- Treinamento Concluído ---");
        }
    }

    /**
     * Prevê a categoria de um novo produto com base no modelo J48 treinado.
     * @param nome O nome do novo produto
     * @param precoVenda O preço de venda do novo produto
     * @return A string da categoria prevista (ex: "Perifericos")
     * @throws Exception Se o modelo não for encontrado ou houver erro na predição
     */
    public static String preverCategoria(String nome, double precoVenda) throws Exception {
        // 1. Carregar o modelo e o cabeçalho (estrutura de dados)
        // Se os arquivos não existirem, lançará uma exceção
        J48 classificador = (J48) SerializationHelper.read("model/j48_erp_model.model");
        Instances header = (Instances) SerializationHelper.read("model/j48_erp_header.model");

        // 2. Criar a nova instância para predição
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setDataset(header); // Vincula ao cabeçalho (ESSENCIAL)

        // 3. Preencher os atributos da nova instância
        // DEVE SEGUIR A MESMA ORDEM E LÓGICA DO TREINAMENTO!
        String nomeLower = nome.toLowerCase();

        // Acessa os atributos pelo NOME para segurança
        inst.setValue(header.attribute("precoVenda"), precoVenda);
        inst.setValue(header.attribute("tem_palavra_teclado"), nomeLower.contains("teclado") ? 1.0 : 0.0);
        inst.setValue(header.attribute("tem_palavra_mouse"), nomeLower.contains("mouse") ? 1.0 : 0.0);
        inst.setValue(header.attribute("tem_palavra_monitor"), nomeLower.contains("monitor") ? 1.0 : 0.0);
        inst.setValue(header.attribute("tem_palavra_ssd"), nomeLower.contains("ssd") ? 1.0 : 0.0);
        inst.setValue(header.attribute("tem_palavra_hd"), nomeLower.contains("hd") ? 1.0 : 0.0);
        inst.setValue(header.attribute("tem_palavra_ram"), nomeLower.contains("ram") ? 1.0 : 0.0);
        inst.setValue(header.attribute("tem_palavra_cabo"), nomeLower.contains("cabo") ? 1.0 : 0.0);

        // O atributo da classe (categoria) fica vazio (missing), pois é isso que queremos prever

        // 4. Classificar a instância
        double predIndex = classificador.classifyInstance(inst);

        // 5. Retornar o nome da classe prevista
        return header.classAttribute().value((int) predIndex);
    }
}
