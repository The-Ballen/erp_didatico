package com.erp;

import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageService {

    private static Locale currentLocale;
    private static ResourceBundle messages;

    // Inicializa o serviço com o idioma padrão (Português do Brasil)
    static {
        setLocale("pt", "BR");
    }

    /**
     * Define o idioma a ser usado pelo sistema.
     * @param language Código do idioma (ex: "pt", "en")
     * @param country Código do país (ex: "BR", "US")
     */
    public static void setLocale(String language, String country) {
        currentLocale = new Locale(language, country);
        // O "messages" corresponde ao nome base dos arquivos .properties
        // (ex: messages_pt_BR.properties)
        messages = ResourceBundle.getBundle("messages", currentLocale);
        //System.out.println(getString("language.changed") + currentLocale.getDisplayLanguage(currentLocale));
    }

    /**
     * Obtém uma string de texto do arquivo de idioma carregado.
     * @param key A chave da string (ex: "menu.main.title")
     * @return O texto traduzido.
     */
    public static String getString(String key) {
        try {
            return messages.getString(key);
        } catch (Exception e) {
            // Retorna a própria chave se não encontrar a tradução, para facilitar a depuração
            return key;
        }
    }

    /**
     * Obtém uma string formatada, substituindo os placeholders.
     * @param key A chave da string (ex: "product.details")
     * @param args Os valores a serem inseridos na string.
     * @return O texto formatado e traduzido.
     */
    public static String getFormattedString(String key, Object... args) {
        return String.format(getString(key), args);
    }
}