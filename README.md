# InventoryApp backend

REST API pentru aplicația de inventar (Spring Boot 3, PostgreSQL, JWT, Flyway).

## Cerințe

- **JDK 22** și **Maven 3**
- **PostgreSQL** rulând local (implicit: `jdbc:postgresql://localhost:5432/inventoryapp`)

## Pornire rapidă

```bash
mvn spring-boot:run
```

Variabile importante (valorile din `application.yml` au fallback-uri pentru dev):

| Variabilă | Rol |
|-----------|-----|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexiune PostgreSQL |
| `APP_JWT_SECRET` | Secret semnătură JWT (**obligatoriu în producție**; dacă lipsește local, poate fi generat la startup — vezi config) |
| `APP_SECURITY_CORS_ALLOWED_ORIGINS` | Origini frontend (JSON array sau liste Spring), ex.: `http://localhost:5173` |
| `APP_FRONTEND_URL` | Bază pentru link-uri din email (reset parolă) |
| `RESEND_API_KEY`, `RESEND_FROM` | Email tranzacțional (dacă sunt goale se folosește implementarea care doar loghează) |

Aplicația citește un fișier **`.env` din directorul curent**, dacă există (înainte de pornirea Spring — vezi `DotenvLoader` și `InventoryAppApplication`).

## Bază de date

- Migrările Flyway sunt în `src/main/resources/db/migration/`.
- Opțional: `APP_DB_CREATE_IF_MISSING=true` (implicit) poate crea baza de date lipsă înainte de Flyway (conexiune la serverul Postgres, nu la DB-ul țintă).

## Dosar electronic (documente firmă)

### Dosare (nume ales de utilizator)

1. **`POST /firms/{firmId}/dossiers`** — `{ "name": "Proiect Renovare" }` creează dosarul.
2. **`GET /firms/{firmId}/dossiers`** — listă dosare (`documentCount` inclus).
3. Upload și listare documente **doar în dosar**: prefix **`/firms/{firmId}/dossiers/{dossierId}/documents`**.
4. Vechiul path `/firms/{firmId}/documents` răspunde **410 Gone** — folosește dosarul.

### Fișiere în dosar

- Listare (pagină, filtru `?folder=`), `GET .../folders` (căi populate), `GET .../folder-structure` (taxonomie fixă + count), upload (`file`), **batch** (`POST .../batch` cu `files[]`), descărcare, ștergere.
- După upload: `processingStatus: PENDING`, staging `{firmId}/dossiers/{dossierId}/_pending/`.
- Worker mută în **exact 5 foldere root** — Ollama alege doar destinația, nu creează subfoldere.

**Taxonomie (5 directoare):** `Documente`, `Renders`, `Poze`, `Facturi`, `Misc`. Reguli: PDF/CAD/SketchUp/Office → Documente; imagini render → Renders; alte imagini → Poze; facturi → Facturi; rest → Misc (sau AI). Detalii: [`docs/ghid-backend-five-folders.md`](docs/ghid-backend-five-folders.md).
- **`DELETE /dossiers/{dossierId}`** — șterge dosarul și toate documentele (cascade + cleanup disc).

Documente migrate din versiuni anterioare sunt în dosarul **`Dosar general`** per firmă.

| Variabilă / proprietate | Rol |
|-------------------------|-----|
| `APP_FEATURES_DOSSIER` | `true` / `false` — dezactivează API-ul dosarului (403). |
| `APP_FEATURES_DOSSIER_AI` | `true` / `false` — clasificare Ollama (altfel doar reguli + `Misc`). |
| `APP_OLLAMA_BASE_URL`, `APP_OLLAMA_MODEL` | Server și model Ollama (implicit `http://127.0.0.1:11434`). |
| `APP_STORAGE_ROOT` | Director rădăcină pentru fișiere (implicit `~/.inventoryapp/uploads`). |
| `APP_STORAGE_MAX_FILE_SIZE_BYTES` | Limită mărime per fișier (implicit 1 GiB). |
| `APP_STORAGE_MAX_FILE_SIZE` / `APP_STORAGE_MAX_REQUEST_SIZE` | Limite multipart Spring (implicit 1 GiB / 3 GiB per request). |
| `APP_STORAGE_ALLOWED_MIME_PREFIXES` | Listă goală = orice MIME; altfel prefixe (ex. `application/pdf`, `image/`). |
| `APP_DOCUMENTS_BATCH_MAX_FILES`, `APP_DOCUMENTS_BATCH_MAX_TOTAL_BYTES` | Limite upload multiplu (implicit 50 fișiere / 3 GiB total per batch). |
| `APP_DOCUMENTS_ORGANIZATION_POLL_INTERVAL` | Interval worker organizare (implicit `2s`). |
| `APP_DOCUMENTS_AI_SUBFOLDERS` | Ignorat pentru subfoldere — AI alege doar una din cele 5 foldere root. |
| `APP_DOCUMENTS_PAGE_MAX_SIZE` | Plafon pentru `size` la listare. |
| `APP_SECURITY_RATE_LIMIT_DOCUMENTS_*` | Rată limită pentru POST `/documents` și `/documents/batch`. |

Răspuns **413** dacă fișierul depășește limita; **409** la download dacă documentul e încă `PENDING` sau `FAILED`.

## Firme, roluri și status

- `GET /firms` returnează per firmă: `role` (`OWNER` | `MEMBER`), `roleDisplayLabel`, `status` (`ACTIVE` | `PAUSED` | `CRITICAL`), `statusDisplayLabel`, `statusMessage`.
- La creare, status implicit: **ACTIVE**.
- `PATCH /firms/{firmId}/status` — schimbare status (OWNER); `message` opțional (recomandat la `CRITICAL`).
- Când status ≠ `ACTIVE`: toate operațiile pe firmă (inventar, dosare, documente, rename) sunt blocate cu **403**; `GET /firms`, `PATCH .../status` și `DELETE` firmă rămân pentru OWNER.
- Redenumire / ștergere firmă: doar `OWNER` (`PATCH` / `DELETE` pe `/firms/{firmId}`).
- **Ghid frontend (complet):** [`docs/ghid-frontend-firme.md`](docs/ghid-frontend-firme.md) — roluri, status, Settings, erori, checklist.

## Structură pachete (rezumat)

- `api/` — controlere REST, DTO-uri HTTP, `*WebMapper` (traducere între DTO și contracte de serviciu)
- `service.inventory`, `service.firms`, `service.auth` — logică aplicație și contracte interne (fără dependență de DTO-uri HTTP)
- `service.documents` — dosar electronic firmă (metadata + storage + organizare)
- `service.documents.ai` — clasificare foldere (reguli + Ollama)
- `service.email` — trimitere email (Resend / logging)
- `repository/`, `model/` — persistență JPA
- `security/` — JWT, CORS, rate limiting pe `/auth` și upload documente
- `config/`, `bootstrap/` — configurare și încărcare mediu

## Teste

```bash
mvn test
```

Testele de integrare folosesc profilul definit în `src/test/resources/application-test.yml`.

## API / documentație

Cu aplicația pornită: Swagger UI la `/swagger-ui.html`, OpenAPI la `/v3/api-docs`.

Actuator: health și info (vezi `management.endpoints` în `application.yml`).
