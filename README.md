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
| `APP_FRONTEND_URL` | Bază pentru link-uri din email (reset parolă, invitații) |
| `APP_INVITATIONS_TOKEN_TTL_SECONDS` | Expirare token invitație (implicit 604800 = 7 zile) |
| `APP_OPS_API_KEY` | API key pentru endpointurile `/ops/*` |
| `RESEND_API_KEY`, `RESEND_FROM` | Email tranzacțional (dacă sunt goale se folosește implementarea care doar loghează) |

Aplicația citește un fișier **`.env` din directorul curent**, dacă există (înainte de pornirea Spring — vezi `DotenvLoader` și `InventoryAppApplication`).

## Bază de date

- Migrările Flyway sunt în `src/main/resources/db/migration/`.
- Opțional: `APP_DB_CREATE_IF_MISSING=true` (implicit) poate crea baza de date lipsă înainte de Flyway (conexiune la serverul Postgres, nu la DB-ul țintă).

## Work Orders (fișiere firmă)

### Work Orders (nume ales de utilizator)

1. **`POST /firms/{firmId}/work-orders`** — creează work order-ul (metadata + status) și îi populează structura implicită de foldere (`Documents` + `Misc` catch-all).
2. **`GET /firms/{firmId}/work-orders`** — listă work orders (`fileCount` inclus).
3. Upload și listare fișiere **doar în work order**: prefix **`/firms/{firmId}/work-orders/{workOrderId}/files`**.
4. Vechiul path `/firms/{firmId}/documents` răspunde **410 Gone** — folosește work order-ul.

### Foldere (arbore definit de utilizator)

Structura de foldere trăiește **doar în baza de date**; pe disc fișierele sunt stocate sub chei opace (`{firmId}/{workOrderId}/{fileId}.ext`), deci redenumirea/mutarea folderelor nu atinge discul.

- `GET .../folders` — arborele complet (nume, `catchAll`, `fileCount`, reguli `extensions`, `children`).
- `POST .../folders` — `{ "parentId"?, "name", "extensions": ["pdf", "dwg"] }` (max. 3 niveluri).
- `PATCH .../folders/{folderId}` — redenumire și/sau mutare (`parentId: null` = mutare la rădăcină).
- `PUT .../folders/{folderId}/rules` — înlocuiește regulile de extensii (o extensie aparține unui singur folder per work order; **409** la conflict).
- `DELETE .../folders/{folderId}` — doar folder gol; cu `?moveFilesTo=catchAll` fișierele sunt mutate în folderul catch-all.
- Folderul **catch-all** (`Misc` implicit) nu poate fi șters/mutat și nu poate avea reguli.

### Fișiere în work order

- Upload (`POST .../files` cu `file`), **batch** (`POST .../files/batch` cu `files[]`) — clasificare **sincronă**: extensia decide folderul pe baza regulilor; fără regulă → catch-all. Răspunsul include `folderId` + `folderPath`.
- Nume duplicate în același folder primesc sufix automat (`plan (1).pdf`).
- Listare paginată (`GET .../files?folderId=`), descărcare (`GET .../files/{fileId}/content`), redenumire/mutare manuală (`PATCH .../files/{fileId}` cu `{ displayName?, folderId? }`), ștergere.
- **`DELETE /work-orders/{workOrderId}`** — șterge work order-ul, folderele și fișierele (cascade + cleanup disc pe prefix).

| Variabilă / proprietate | Rol |
|-------------------------|-----|
| `APP_FEATURES_WORK_ORDER` | `true` / `false` — dezactivează API-ul work order-urilor (403). |
| `APP_STORAGE_ROOT` | Director rădăcină pentru fișiere (implicit `~/.inventoryapp/uploads`). |
| `APP_STORAGE_MAX_FILE_SIZE_BYTES` | Limită mărime per fișier (implicit 1 GiB). |
| `APP_STORAGE_MAX_FILE_SIZE` / `APP_STORAGE_MAX_REQUEST_SIZE` | Limite multipart Spring (implicit 1 GiB / 3 GiB per request). |
| `APP_STORAGE_ALLOWED_MIME_PREFIXES` | Listă goală = orice MIME; altfel prefixe (ex. `application/pdf`, `image/`). |
| `APP_DOCUMENTS_BATCH_MAX_FILES`, `APP_DOCUMENTS_BATCH_MAX_TOTAL_BYTES` | Limite upload multiplu (implicit 50 fișiere / 3 GiB total per batch). |
| `APP_DOCUMENTS_PAGE_MAX_SIZE` | Plafon pentru `size` la listare. |
| `APP_SECURITY_RATE_LIMIT_DOCUMENTS_*` | Rată limită pentru POST `/files` și `/files/batch`. |

Răspuns **413** dacă fișierul depășește limita; **409** la conflicte de nume/extensii. Detalii: [`docs/ghid-backend-work-orders.md`](docs/ghid-backend-work-orders.md).

### Facturi (Invoices)

Domeniu separat de Files — upload dedicat, procesare asincronă cu **Microsoft MarkItDown** (markdown + status).

- `GET/POST .../work-orders/{workOrderId}/invoices` — listare paginată / upload single (`file`)
- `POST .../invoices/batch` — upload multiplu (`files[]`)
- `GET .../invoices/{invoiceId}` — detaliu + `markdownText` când `processingStatus=READY`
- `GET .../invoices/{invoiceId}/content` — descărcare fișier original
- `POST .../invoices/{invoiceId}/retry` — reprocesare pentru `FAILED`
- `DELETE .../invoices/{invoiceId}`

Statusuri: `PENDING` → `READY` (markdown extras) sau `FAILED` (cu `processingError`). MIME permise implicit: PDF și imagini.

Cerință locală: `pip install 'markitdown[all]'` și `markitdown` în PATH. Detalii: [`docs/ghid-backend-invoices.md`](docs/ghid-backend-invoices.md).

| Variabilă / proprietate | Rol |
|-------------------------|-----|
| `APP_INVOICES_MARKITDOWN_COMMAND` | Comandă CLI (implicit `markitdown`) |
| `APP_INVOICES_PROCESSING_POLL_INTERVAL` | Interval worker (implicit `2s`) |
| `APP_INVOICES_EXTRACTOR` | `markitdown` (prod) sau `stub` (teste) |

### AI (MLX)

Infrastructură internă pentru apeluri LLM — **fără endpoint de chat** către frontend. Serviciile de domeniu vor injecta `AiService` când un feature are nevoie de AI.

```
Spring Boot → AiService → OpenAI Java SDK → localhost:8000/v1 → MLX OpenAI Server
```

| Variabilă / proprietate | Rol |
|-------------------------|-----|
| `APP_AI_PROVIDER` | `mlx` (implicit) sau `stub` (teste/CI) |
| `APP_AI_BASE_URL` | URL OpenAI-compatible (implicit `http://127.0.0.1:8000/v1`) |
| `APP_AI_API_KEY` | Bearer token (implicit `mlx-local`) |
| `APP_AI_MODEL` | Model MLX (implicit `mlx-community/gemma-4-12B-it-qat-4bit`) |
| `APP_AI_HUGGINGFACE_REPO` | Repo Hugging Face pentru download weights |
| `APP_AI_AUTO_DOWNLOAD_MODEL` | Descarcă automat Gemma 4 12B la startup dacă lipsește |
| `APP_AI_AUTO_START_SERVER` | Pornește `mlx_vlm.server` local dacă nu rulează |
| `APP_AI_TIMEOUT` | Timeout apeluri (implicit `120s`) |

Setup Python: `python3 -m venv .venv-mlx && .venv-mlx/bin/pip install huggingface_hub mlx-vlm`

Detalii: [`docs/ghid-backend-ai.md`](docs/ghid-backend-ai.md).

## Firme, roluri și status

- `GET /firms` returnează per firmă: `role` (`OWNER` | `MEMBER`), `roleDisplayLabel`, `status` (`ACTIVE` | `PAUSED` | `CRITICAL`), `statusDisplayLabel`, `statusMessage`.
- La creare, status implicit: **ACTIVE**.
- `PATCH /firms/{firmId}/status` — schimbare status (OWNER); `message` opțional (recomandat la `CRITICAL`).
- `GET /firms/{firmId}/status/history` — istoric status (OWNER), cu sursă `MANUAL` / `SYSTEM`.
- Când status ≠ `ACTIVE`: toate operațiile pe firmă (inventar, work orders, documente, rename) sunt blocate cu **403**; `GET /firms`, `PATCH .../status` și `DELETE` firmă rămân pentru OWNER.
- Redenumire / ștergere firmă: doar `OWNER` (`PATCH` / `DELETE` pe `/firms/{firmId}`).
- Un monitor backend verifică periodic consistența `firms.owner_user_id` vs `firm_members.role`; dacă găsește drift, setează firma `CRITICAL`.
- **Ghid frontend (complet):** [`docs/ghid-frontend-firme.md`](docs/ghid-frontend-firme.md) — roluri, status, Settings, erori, checklist.

## Invitații membri (echipă)

- Doar **OWNER** poate invita (`POST /firms/{firmId}/invitations`) — rol invitat: **MEMBER** (Angajat).
- Email cu link: `{APP_FRONTEND_URL}/accept-invitation?token=...` (TTL: `APP_INVITATIONS_TOKEN_TTL_SECONDS`, implicit 7 zile).
- **Cont nou:** `GET /auth/invitations/{token}` (preview) → `POST /auth/accept-invitation` cu `displayName` + `password` → JWT.
- **Cont existent:** login obligatoriu, apoi `POST /auth/accept-invitation` cu `{ "token": "..." }` + Bearer.
- `GET /firms/{firmId}/members` — listă echipă.
- `PATCH /firms/{firmId}/members/{memberUserId}/role` — schimbare rol membru (în prezent folosit pentru reguli explicite; `OWNER` nu se setează aici).
- `DELETE /firms/{firmId}/members/{memberUserId}` — scoate membru din firmă.
- `POST /firms/{firmId}/ownership/transfer` — inițiază transferul de ownership și trimite owner-ului curent un cod de confirmare din 6 cifre pe email (`202 Accepted`).
- `POST /firms/{firmId}/ownership/transfer/confirm` — confirmă transferul cu codul primit pe email (`204 No Content`).
- `GET/DELETE /firms/{firmId}/invitations` — pending / revoke.
- La confirmare, backend-ul trimite emailuri finale atât fostului owner, cât și noului owner promovat.
- **Ghid frontend:** [`docs/ghid-frontend-invitations.md`](docs/ghid-frontend-invitations.md).

## Notificări

- `GET /notifications` — inbox per user cu `unreadCount` + listă notificări (`?unreadOnly`, `?limit`).
- `POST /notifications/{notificationId}/read` — marchează o notificare ca citită.
- `POST /notifications/read-all` — marchează toate notificările userului ca citite.
- Evenimentele v1:
  - `PRODUCT_CREATED`
  - `PRODUCT_LOW_STOCK` (doar la crossing sub prag; buy list-ul rămâne calculat dinamic)
  - `FIRM_STATUS_CHANGED` (manual sau `SYSTEM`)
- Email-ul este folosit conservator doar când o firmă intră în `CRITICAL`, către owner-ul curent.
- **Ghid frontend:** [`docs/ghid-frontend-notifications.md`](docs/ghid-frontend-notifications.md).

## Ops / diagnostic

- `GET /ops/logs` și `GET /ops/events` expun loguri și evenimente operaționale recente.
- Accesul se face cu header `X-Ops-Api-Key` și cheia din `APP_OPS_API_KEY`.
- Suprafața este read-only și destinată debugging-ului backend.

## Structură pachete (rezumat)

- `api/` — controlere REST, DTO-uri HTTP, `*WebMapper` (traducere între DTO și contracte de serviciu)
- `service.inventory`, `service.firms`, `service.auth` — logică aplicație și contracte interne (fără dependență de DTO-uri HTTP)
- `service.workorders` — work orders firmă (metadata, arbore de foldere, reguli extensii, fișiere)
- `service.ai` — `AiService` (OpenAI SDK → MLX local; fără chatbot expus)
- `service.storage` — `BlobStorage` (chei opace; local acum, S3/MinIO ulterior)
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
