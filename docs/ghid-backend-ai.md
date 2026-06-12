# Ghid backend — AI (MLX)

Infrastructură AI pentru apeluri LLM din servicii de domeniu. **Nu există endpoint de chat** expus către frontend.

## Arhitectură

```
Serviciu domeniu (viitor)
        ↓
    AiService
        ↓
 OpenAI Java SDK (OpenAIClient)
        ↓
 http://127.0.0.1:8000/v1  (configurabil)
        ↓
 MLX OpenAI-compatible server
        ↓
 Model Gemma 4 12B (implicit: mlx-community/gemma-4-12B-it-qat-4bit)
```

## Configurare (`application.yml`)

```yaml
app:
  ai:
    provider: mlx
    base-url: http://127.0.0.1:8000/v1
    api-key: mlx-local
    model: mlx-community/gemma-4-12B-it-qat-4bit
    huggingface-repo: mlx-community/gemma-4-12B-it-qat-4bit
    auto-download-model: true
    auto-start-server: true
    server-port: 8000
    model-download-timeout: 60m
    server-start-timeout: 10m
    timeout: 120s
    max-retries: 2
```

### Variabile de mediu

| Variabilă | Implicit | Rol |
|-----------|----------|-----|
| `APP_AI_PROVIDER` | `mlx` | `mlx` = SDK real; `stub` = răspunsuri fixe (teste/CI) |
| `APP_AI_BASE_URL` | `http://127.0.0.1:8000/v1` | URL bază OpenAI-compatible |
| `APP_AI_API_KEY` | `mlx-local` | Cheie Bearer pentru MLX |
| `APP_AI_MODEL` | `mlx-community/gemma-4-12B-it-qat-4bit` | Model trimis la `/v1/chat/completions` |
| `APP_AI_HUGGINGFACE_REPO` | la fel ca model | Repo Hugging Face pentru download |
| `APP_AI_MODEL_CACHE_DIR` | `.mlx-models/<repo-slug>` | Director local pentru weights |
| `APP_AI_AUTO_DOWNLOAD_MODEL` | `true` | Descarcă automat dacă lipsesc weights |
| `APP_AI_AUTO_START_SERVER` | `true` | Pornește `mlx_vlm.server` dacă nu răspunde |
| `APP_AI_SERVER_PORT` | `8000` | Port server MLX |
| `APP_AI_MODEL_DOWNLOAD_TIMEOUT` | `60m` | Timeout download (~11 GB) |
| `APP_AI_SERVER_START_TIMEOUT` | `10m` | Așteptare încărcare model în RAM |
| `APP_AI_MLX_PYTHON_COMMAND` | `python3` | Python (sau `.venv-mlx/bin/python3`) |
| `APP_AI_TIMEOUT` | `120s` | Timeout HTTP SDK |
| `APP_AI_MAX_RETRIES` | `2` | Reîncercări SDK |

## Pornire locală (MLX)

### 1. Dependențe Python (o singură dată)

```bash
python3 -m venv .venv-mlx
.venv-mlx/bin/pip install huggingface_hub mlx-vlm
```

### 2. Pornește Spring Boot

La `ApplicationReadyEvent`, backend-ul:

1. Verifică dacă weights există în `.mlx-models/` (sau `APP_AI_MODEL_CACHE_DIR`)
2. Dacă lipsesc și `auto-download-model=true` → rulează `scripts/ensure_mlx_model.py` (download Hugging Face)
3. Dacă serverul MLX nu răspunde și `auto-start-server=true` → pornește `mlx_vlm.server` cu modelul local
4. Probe `GET /v1/models`

Log așteptat: `AI MLX ready (baseUrl=..., model=..., weights=...)`

### 3. Verificare manuală

```bash
curl -H "Authorization: Bearer mlx-local" http://127.0.0.1:8000/v1/models
```

### Pornire manuală server (fără auto-start)

```bash
.venv-mlx/bin/python -m mlx_vlm.server \
  --port 8000 \
  --model .mlx-models/mlx-community-gemma-4-12B-it-qat-4bit
```

## Fără MLX (teste / CI)

În `application-test.yml` este setat `app.ai.provider: stub`.

Pentru dev fără server MLX:

```bash
export APP_AI_PROVIDER=stub
mvn spring-boot:run
```

## API intern (`AiService`)

Pachet: `com.mirceone.inventoryapp.service.ai`

| Metodă | Rol |
|--------|-----|
| `chat(List<AiChatMessage>)` | Completare chat non-streaming |
| `chatJson(String userPrompt)` | Completare cu `response_format: json_object` |

Implementări:

- `OpenAiSdkAiService` — activ când `app.ai.provider=mlx` (implicit)
- `StubAiService` — activ când `app.ai.provider=stub`

## Legare într-un feature viitor

```java
@Service
class ExampleService {
    private final AiService ai;

    ExampleService(AiService ai) {
        this.ai = ai;
    }

    void run() {
        String json = ai.chatJson("...");
        // parse & persist
    }
}
```

Clasificarea fișierelor work-order folosește în prezent **doar reguli de extensie** (`FileClassifier`). Facturile folosesc MarkItDown + OCR, nu LLM.

## Mapare env vechi Ollama (referință)

| Vechi | Nou |
|-------|-----|
| `APP_OLLAMA_BASE_URL` | `APP_AI_BASE_URL` |
| `APP_OLLAMA_MODEL` | `APP_AI_MODEL` |
| `APP_OLLAMA_CHAT_TIMEOUT` | `APP_AI_TIMEOUT` |
