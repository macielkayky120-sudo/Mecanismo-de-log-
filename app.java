package com.logging;

/**
 * Classe principal para demonstrar o uso do mecanismo de log.
 */
public class App {
    public static void main(String[] args) {
        // Configuração: Ambiente e aplicação
        String ambiente = "PRODUCAO"; // Mude para "DESENVOLVIMENTO" para usar ConsoleLogger
        String aplicacao = "MinhaApp";

        // Cria logger via Factory
        ILogger logger = LoggerFactory.criarLogger(ambiente, aplicacao);

        // Inicializa processador assíncrono
        LogProcessor processor = new LogProcessor(logger);

        // Simula logs na aplicação (não bloqueia)
        processor.enviarLog("INFO", "Aplicação iniciada", "usuario=admin", "ip=192.168.1.1");
        processor.enviarLog("ERROR", "Falha ao conectar ao DB", "stackTrace=...");

        // Aguarda um pouco para processamento
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Para o processador
        processor.parar();
        System.out.println("Logs processados.");
    }
}
