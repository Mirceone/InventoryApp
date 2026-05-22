package com.mirceone.inventoryapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppIntegrationProperties {

    private final Features features = new Features();
    private final Ollama ollama = new Ollama();
    private final Storage storage = new Storage();
    private final Ops ops = new Ops();
    private final Documents documents = new Documents();

    public Features getFeatures() {
        return features;
    }

    public Ollama getOllama() {
        return ollama;
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

    public static class Features {
        private boolean routesEnabled = true;
        private boolean dossierEnabled = true;
        private boolean dossierAiEnabled = true;
        private boolean opsEnabled = true;

        public boolean isRoutesEnabled() {
            return routesEnabled;
        }

        public void setRoutesEnabled(boolean routesEnabled) {
            this.routesEnabled = routesEnabled;
        }

        public boolean isDossierEnabled() {
            return dossierEnabled;
        }

        public void setDossierEnabled(boolean dossierEnabled) {
            this.dossierEnabled = dossierEnabled;
        }

        public boolean isDossierAiEnabled() {
            return dossierAiEnabled;
        }

        public void setDossierAiEnabled(boolean dossierAiEnabled) {
            this.dossierAiEnabled = dossierAiEnabled;
        }

        public boolean isOpsEnabled() {
            return opsEnabled;
        }

        public void setOpsEnabled(boolean opsEnabled) {
            this.opsEnabled = opsEnabled;
        }
    }

    public static class Ollama {
        private String baseUrl = "http://127.0.0.1:11434";
        private String model = "gemma4:e4b";
        private Duration chatTimeout = Duration.ofSeconds(120);
        private boolean sendDocumentContent = false;
        private int maxContentChars = 8_000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Duration getChatTimeout() {
            return chatTimeout;
        }

        public void setChatTimeout(Duration chatTimeout) {
            this.chatTimeout = chatTimeout;
        }

        public boolean isSendDocumentContent() {
            return sendDocumentContent;
        }

        public void setSendDocumentContent(boolean sendDocumentContent) {
            this.sendDocumentContent = sendDocumentContent;
        }

        public int getMaxContentChars() {
            return maxContentChars;
        }

        public void setMaxContentChars(int maxContentChars) {
            this.maxContentChars = maxContentChars;
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
        /** Maximum page size for GET /firms/{firmId}/documents. */
        private int pageMaxSize = 100;
        private int batchMaxFiles = 50;
        private long batchMaxTotalBytes = 3L * 1024 * 1024 * 1024;
        private Duration organizationPollInterval = Duration.ofSeconds(2);
        private boolean aiSubfoldersEnabled = true;
        private int organizationBatchSize = 20;

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

        public Duration getOrganizationPollInterval() {
            return organizationPollInterval;
        }

        public void setOrganizationPollInterval(Duration organizationPollInterval) {
            this.organizationPollInterval = organizationPollInterval;
        }

        public boolean isAiSubfoldersEnabled() {
            return aiSubfoldersEnabled;
        }

        public void setAiSubfoldersEnabled(boolean aiSubfoldersEnabled) {
            this.aiSubfoldersEnabled = aiSubfoldersEnabled;
        }

        public int getOrganizationBatchSize() {
            return organizationBatchSize;
        }

        public void setOrganizationBatchSize(int organizationBatchSize) {
            this.organizationBatchSize = organizationBatchSize;
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
