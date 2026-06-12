package com.mirceone.inventoryapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppIntegrationProperties {

    private final Features features = new Features();
    private final Storage storage = new Storage();
    private final Ops ops = new Ops();
    private final Documents documents = new Documents();
    private final Invoices invoices = new Invoices();
    private final Files files = new Files();
    private final Ai ai = new Ai();

    public Features getFeatures() {
        return features;
    }

    public Storage getStorage() {
        return storage;
    }

    public Ops getOps() {
        return ops;
    }

    public Documents getDocuments() {
        return documents;
    }

    public Invoices getInvoices() {
        return invoices;
    }

    public Files getFiles() {
        return files;
    }

    public Ai getAi() {
        return ai;
    }

    public static class Features {
        private boolean routesEnabled = true;
        private boolean workOrderEnabled = true;
        private boolean workOrderAiEnabled = true;
        private boolean opsEnabled = true;

        public boolean isRoutesEnabled() {
            return routesEnabled;
        }

        public void setRoutesEnabled(boolean routesEnabled) {
            this.routesEnabled = routesEnabled;
        }

        public boolean isWorkOrderEnabled() {
            return workOrderEnabled;
        }

        public void setWorkOrderEnabled(boolean workOrderEnabled) {
            this.workOrderEnabled = workOrderEnabled;
        }

        public boolean isWorkOrderAiEnabled() {
            return workOrderAiEnabled;
        }

        public void setWorkOrderAiEnabled(boolean workOrderAiEnabled) {
            this.workOrderAiEnabled = workOrderAiEnabled;
        }

        public boolean isOpsEnabled() {
            return opsEnabled;
        }

        public void setOpsEnabled(boolean opsEnabled) {
            this.opsEnabled = opsEnabled;
        }
    }

    public static class Storage {
        private String root = System.getProperty("user.home") + "/.inventoryapp/uploads";
        private long maxFileSizeBytes = 1024L * 1024 * 1024;
        /**
         * If non-empty, uploaded {@code Content-Type} must equal or start with one of these entries.
         * Empty list allows any MIME type (still validated for size and filename).
         */
        private List<String> allowedMimePrefixes = new ArrayList<>();

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public List<String> getAllowedMimePrefixes() {
            return allowedMimePrefixes;
        }

        public void setAllowedMimePrefixes(List<String> allowedMimePrefixes) {
            this.allowedMimePrefixes = allowedMimePrefixes;
        }
    }

    public static class Documents {
        /** Maximum page size for file listings. */
        private int pageMaxSize = 100;
        private int batchMaxFiles = 50;
        private long batchMaxTotalBytes = 3L * 1024 * 1024 * 1024;

        public int getPageMaxSize() {
            return pageMaxSize;
        }

        public void setPageMaxSize(int pageMaxSize) {
            this.pageMaxSize = pageMaxSize;
        }

        public int getBatchMaxFiles() {
            return batchMaxFiles;
        }

        public void setBatchMaxFiles(int batchMaxFiles) {
            this.batchMaxFiles = batchMaxFiles;
        }

        public long getBatchMaxTotalBytes() {
            return batchMaxTotalBytes;
        }

        public void setBatchMaxTotalBytes(long batchMaxTotalBytes) {
            this.batchMaxTotalBytes = batchMaxTotalBytes;
        }
    }

    public static class Files {
        private Duration classificationPollInterval = Duration.ofSeconds(2);
        private int classificationBatchSize = 10;

        public Duration getClassificationPollInterval() {
            return classificationPollInterval;
        }

        public void setClassificationPollInterval(Duration classificationPollInterval) {
            this.classificationPollInterval = classificationPollInterval;
        }

        public int getClassificationBatchSize() {
            return classificationBatchSize;
        }

        public void setClassificationBatchSize(int classificationBatchSize) {
            this.classificationBatchSize = classificationBatchSize;
        }
    }

    public static class Invoices {
        private int pageMaxSize = 100;
        private int batchMaxFiles = 50;
        private long batchMaxTotalBytes = 3L * 1024 * 1024 * 1024;
        private List<String> allowedMimePrefixes = new ArrayList<>(List.of(
                "application/pdf",
                "image/"
        ));
        private Duration processingPollInterval = Duration.ofSeconds(2);
        private int processingBatchSize = 5;
        private String markitdownCommand = "markitdown";
        private Duration markitdownTimeout = Duration.ofSeconds(120);
        private String extractor = "markitdown";
        private boolean ocrFallbackEnabled = true;
        private String ocrPythonCommand = "python3";
        private String ocrScriptPath = "";
        private String ocrLanguages = "ro,en";
        private Duration ocrTimeout = Duration.ofMinutes(5);

        public int getPageMaxSize() {
            return pageMaxSize;
        }

        public void setPageMaxSize(int pageMaxSize) {
            this.pageMaxSize = pageMaxSize;
        }

        public int getBatchMaxFiles() {
            return batchMaxFiles;
        }

        public void setBatchMaxFiles(int batchMaxFiles) {
            this.batchMaxFiles = batchMaxFiles;
        }

        public long getBatchMaxTotalBytes() {
            return batchMaxTotalBytes;
        }

        public void setBatchMaxTotalBytes(long batchMaxTotalBytes) {
            this.batchMaxTotalBytes = batchMaxTotalBytes;
        }

        public List<String> getAllowedMimePrefixes() {
            return allowedMimePrefixes;
        }

        public void setAllowedMimePrefixes(List<String> allowedMimePrefixes) {
            this.allowedMimePrefixes = allowedMimePrefixes;
        }

        public Duration getProcessingPollInterval() {
            return processingPollInterval;
        }

        public void setProcessingPollInterval(Duration processingPollInterval) {
            this.processingPollInterval = processingPollInterval;
        }

        public int getProcessingBatchSize() {
            return processingBatchSize;
        }

        public void setProcessingBatchSize(int processingBatchSize) {
            this.processingBatchSize = processingBatchSize;
        }

        public String getMarkitdownCommand() {
            return markitdownCommand;
        }

        public void setMarkitdownCommand(String markitdownCommand) {
            this.markitdownCommand = markitdownCommand;
        }

        public Duration getMarkitdownTimeout() {
            return markitdownTimeout;
        }

        public void setMarkitdownTimeout(Duration markitdownTimeout) {
            this.markitdownTimeout = markitdownTimeout;
        }

        public String getExtractor() {
            return extractor;
        }

        public void setExtractor(String extractor) {
            this.extractor = extractor;
        }

        public boolean isOcrFallbackEnabled() {
            return ocrFallbackEnabled;
        }

        public void setOcrFallbackEnabled(boolean ocrFallbackEnabled) {
            this.ocrFallbackEnabled = ocrFallbackEnabled;
        }

        public String getOcrPythonCommand() {
            return ocrPythonCommand;
        }

        public void setOcrPythonCommand(String ocrPythonCommand) {
            this.ocrPythonCommand = ocrPythonCommand;
        }

        public String getOcrScriptPath() {
            return ocrScriptPath;
        }

        public void setOcrScriptPath(String ocrScriptPath) {
            this.ocrScriptPath = ocrScriptPath;
        }

        public String getOcrLanguages() {
            return ocrLanguages;
        }

        public void setOcrLanguages(String ocrLanguages) {
            this.ocrLanguages = ocrLanguages;
        }

        public Duration getOcrTimeout() {
            return ocrTimeout;
        }

        public void setOcrTimeout(Duration ocrTimeout) {
            this.ocrTimeout = ocrTimeout;
        }
    }

    public static class Ai {
        private String provider = "mlx";
        private String baseUrl = "http://127.0.0.1:8000/v1";
        private String apiKey = "mlx-local";
        private String model = "mlx-community/gemma-4-12B-it-qat-4bit";
        private String huggingfaceRepo = "mlx-community/gemma-4-12B-it-qat-4bit";
        private String modelCacheDir = "";
        private boolean autoDownloadModel = true;
        private boolean autoStartServer = true;
        private int serverPort = 8000;
        private Duration modelDownloadTimeout = Duration.ofMinutes(60);
        private Duration serverStartTimeout = Duration.ofMinutes(10);
        private String mlxPythonCommand = "python3";
        private String ensureModelScriptPath = "";
        private Duration timeout = Duration.ofSeconds(120);
        private int maxRetries = 2;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getHuggingfaceRepo() {
            return huggingfaceRepo;
        }

        public void setHuggingfaceRepo(String huggingfaceRepo) {
            this.huggingfaceRepo = huggingfaceRepo;
        }

        public String getModelCacheDir() {
            return modelCacheDir;
        }

        public void setModelCacheDir(String modelCacheDir) {
            this.modelCacheDir = modelCacheDir;
        }

        public boolean isAutoDownloadModel() {
            return autoDownloadModel;
        }

        public void setAutoDownloadModel(boolean autoDownloadModel) {
            this.autoDownloadModel = autoDownloadModel;
        }

        public boolean isAutoStartServer() {
            return autoStartServer;
        }

        public void setAutoStartServer(boolean autoStartServer) {
            this.autoStartServer = autoStartServer;
        }

        public int getServerPort() {
            return serverPort;
        }

        public void setServerPort(int serverPort) {
            this.serverPort = serverPort;
        }

        public Duration getModelDownloadTimeout() {
            return modelDownloadTimeout;
        }

        public void setModelDownloadTimeout(Duration modelDownloadTimeout) {
            this.modelDownloadTimeout = modelDownloadTimeout;
        }

        public Duration getServerStartTimeout() {
            return serverStartTimeout;
        }

        public void setServerStartTimeout(Duration serverStartTimeout) {
            this.serverStartTimeout = serverStartTimeout;
        }

        public String getMlxPythonCommand() {
            return mlxPythonCommand;
        }

        public void setMlxPythonCommand(String mlxPythonCommand) {
            this.mlxPythonCommand = mlxPythonCommand;
        }

        public String getEnsureModelScriptPath() {
            return ensureModelScriptPath;
        }

        public void setEnsureModelScriptPath(String ensureModelScriptPath) {
            this.ensureModelScriptPath = ensureModelScriptPath;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }

    public static class Ops {
        private String apiKey = "";
        private int logRingMaxLines = 500;
        private boolean logRingEnabled = true;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getLogRingMaxLines() {
            return logRingMaxLines;
        }

        public void setLogRingMaxLines(int logRingMaxLines) {
            this.logRingMaxLines = logRingMaxLines;
        }

        public boolean isLogRingEnabled() {
            return logRingEnabled;
        }

        public void setLogRingEnabled(boolean logRingEnabled) {
            this.logRingEnabled = logRingEnabled;
        }
    }
}
