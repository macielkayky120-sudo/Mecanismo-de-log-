package com.logging;

/**
 * MECANISMO DE LOG COMPLETO - Padrões Strategy e Factory Method
 */

// 1. INTERFACE STRATEGY
interface ILogger {
    void registrar(String nivel, String mensagem, String... detalhes);
}

// 2. IMPLEMENTAÇÃO DATABASE LOGGER
class DatabaseLogger implements ILogger {
    private DatabaseConnection db;
    
    public DatabaseLogger(DatabaseConnection db) {
        this.db = db;
    }
    
    @Override
    public void registrar(String nivel, String mensagem, String... detalhes) {
        String sql = "INSERT INTO logs (nivel, mensagem, detalhes, data_criacao) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        String detalhesConcatenados = String.join(" | ", detalhes);
        
        System.out.println("[DatabaseLogger] Executando: " + sql);
        System.out.println("[DatabaseLogger] Parâmetros: nivel=" + nivel + ", mensagem=" + mensagem + ", detalhes=" + detalhesConcatenados);
        
        // Simulação da execução no banco
        db.execute(sql, nivel, mensagem, detalhesConcatenados);
    }
}

// 3. IMPLEMENTAÇÃO CONSOLE LOGGER
class ConsoleLogger implements ILogger {
    @Override
    public void registrar(String nivel, String mensagem, String... detalhes) {
        String logEntry = String.format("[%s] %s - %s", nivel, mensagem, String.join(" | ", detalhes));
        System.out.println(logEntry);
    }
}

// 4. FACTORY METHOD
class LoggerFactory {
    public static ILogger criarLogger(String ambiente) {
        if (ambiente.equalsIgnoreCase("PRODUCAO")) {
            DatabaseConnection db = new DatabaseConnection();
            return new DatabaseLogger(db);
        } else {
            return new ConsoleLogger();
        }
    }
}

// 5. SIMULAÇÃO DE CONEXÃO COM BANCO
class DatabaseConnection {
    public void execute(String sql, String... params) {
        // Simulação de execução no banco de dados
        System.out.println("[DatabaseConnection] Executando SQL: " + sql);
        for (int i = 0; i < params.length; i++) {
            System.out.println("[DatabaseConnection] Parâmetro " + (i + 1) + ": " + params[i]);
        }
        System.out.println("[DatabaseConnection] Log persistido com sucesso!");
    }
}

// 6. PROCESSADOR ASSÍNCRONO
class LogProcessor {
    private final ILogger logger;
    private final java.util.concurrent.BlockingQueue<LogTask> queue;
    private final Thread workerThread;
    private volatile boolean running;
    
    public LogProcessor(ILogger logger) {
        this.logger = logger;
        this.queue = new java.util.concurrent.LinkedBlockingQueue<>();
        this.running = true;
        this.workerThread = new Thread(this::processQueue);
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }
    
    public void registrar(String nivel, String mensagem, String... detalhes) {
        if (running) {
            boolean offered = queue.offer(new LogTask(nivel, mensagem, detalhes));
            if (!offered) {
                System.err.println("Fila de logs cheia. Log descartado: " + mensagem);
            }
        }
    }
    
    private void processQueue() {
        while (running || !queue.isEmpty()) {
            try {
                LogTask task = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (task != null) {
                    try {
                        logger.registrar(task.nivel, task.mensagem, task.detalhes);
                    } catch (Exception e) {
                        System.err.println("Erro ao processar log: " + e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[LogProcessor] Processador de logs finalizado.");
    }
    
    public void parar() {
        running = false;
        workerThread.interrupt();
        try {
            workerThread.join(5000);
            if (workerThread.isAlive()) {
                System.err.println("[LogProcessor] Timeout ao aguardar finalização do processador.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static class LogTask {
        final String nivel;
        final String mensagem;
        final String[] detalhes;
        
        LogTask(String nivel, String mensagem, String[] detalhes) {
            this.nivel = nivel;
            this.mensagem = mensagem;
            this.detalhes = detalhes;
        }
    }
}

// 7. CLASSE PRINCIPAL
public class App {
    public static void main(String[] args) {
        // Configuração: Ambiente 
        String ambiente = "PRODUCAO"; // Mude para "DESENVOLVIMENTO" para usar ConsoleLogger
        
        // Cria logger via Factory (conforme documentação)
        ILogger logger = LoggerFactory.criarLogger(ambiente);
        
        // Inicializa processador assíncrono
        LogProcessor processor = new LogProcessor(logger);
        
        // Simula logs na aplicação usando método REGISTRAR (conforme documentação)
        processor.registrar("INFO", "Aplicação iniciada", "usuario=admin", "ip=192.168.1.1");
        processor.registrar("ERROR", "Falha ao conectar ao DB", "stackTrace=ConnectionTimeout");
        processor.registrar("WARN", "Cache quase cheio", "uso=85%", "limite=90%");
        
        // Aguarda processamento dos logs
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Para o processador de forma graceful
        processor.parar();
        System.out.println("Aplicação finalizada. Logs processados.");
    }
}
