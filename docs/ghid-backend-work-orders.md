# Ghid backend — Work Orders (foldere definite de utilizator)

Modulul Work Orders oferă fiecărui work order un **arbore de foldere definit de utilizator**, cu **reguli de extensii per folder**. La upload, clasificarea pornește cu reguli de extensie și euristici pe nume/MIME; fișierele nerezolvate pot intra în **clasificare async MLX** (când `APP_FEATURES_WORK_ORDER_AI=true`).

## Principiu de arhitectură

**Baza de date este singura sursă de adevăr pentru structură; discul este un blob store opac.**

- Cheile de stocare au forma `{firmId}/{workOrderId}/{fileId}.ext` și **nu se schimbă niciodată** după upload.
- Redenumirea/mutarea folderelor și mutarea manuală a fișierelor sunt **doar UPDATE-uri în DB** — zero I/O pe disc.
- Migrarea pe S3/MinIO = o nouă implementare `BlobStorage` (nu există `move`, deci nu e nevoie de rename pe object storage).

## Schemă (V6)

| Tabel | Rol |
|-------|-----|
| `firm_work_orders` | Metadata work order (nume, client, locație, status etc.). |
| `work_order_folders` | Arborele virtual: `parent_id` self-FK, `name`, `catch_all`, `sort_order`. Nume unic (case-insensitive) între frați; exact un `catch_all=true` per work order (index unic parțial). |
| `work_order_folder_rules` | `extension` (lowercase, fără punct) → `folder_id`. Unic pe `(work_order_id, extension)` — clasificare deterministă. |
| `work_order_files` | Metadata fișier: `folder_id` FK, `display_name`, `storage_key`, plus `classification_status` (`PENDING` / `CLASSIFIED` / `FAILED`), `classification_source` (`RULE` / `AI` / `MANUAL`), `classification_error`. |

Migrare V9 adaugă coloanele de clasificare și index parțial pe `PENDING`.

## Componente

| Componentă | Rol |
|------------|-----|
| `service/storage/BlobStorage` + `LocalBlobStorage` | `store`, `open`, `delete`, `deleteByPrefix` (ștergere work order/firmă într-un singur apel). |
| `service/workorders/WorkOrderService` | CRUD + status; la creare populează folderele din `DefaultFolderTemplate`; la ștergere șterge rândurile și apoi blob-urile pe prefix (after-commit). |
| `service/workorders/WorkOrderFolderService` | Arbore: creare (max. 3 niveluri), redenumire, mutare (validare cicluri/adâncime), ștergere (gol sau `moveFilesTo=catchAll`), înlocuire reguli. |
| `service/workorders/FileClassifier` | Reguli de extensie (`resolveByExtension`). |
| `service/workorders/classification/*` | Heuristici, `MlxFolderClassifier`, `WorkOrderFileClassifier`, worker async. |
| `service/workorders/WorkOrderFileService` | Upload (single/batch), listare, descărcare, redenumire, mutare manuală (`MANUAL`), ștergere; trigger after-commit pentru `PENDING`. |
| `service/workorders/DefaultFolderTemplate` | Structura implicită (`Documents` + `Misc` catch-all). Numele nu sunt referențiate nicăieri în cod — flagul `catch_all` contează, nu numele. |
| `api/workorders/*` | `WorkOrderController`, `FolderController`, `FileController` + DTO-uri și mappers. |

## Fluxul de upload

1. Validare nume/MIME/dimensiune.
2. `WorkOrderFileClassifier.classifyOnUpload` — extensie → euristică → catch-all (+ `PENDING` dacă AI activ).
3. Blob scris sub cheie opacă (`firmId/workOrderId/fileId.ext`) + SHA-256.
4. INSERT rând fișier cu `classification_*`; dedupe pe `display_name`.
5. Dacă `PENDING` → `FileClassificationService.processAsync` după commit; worker poll la `app.files.classification-poll-interval`.
6. La eșec DB → blob șters (compensare).

### Feature flag

| Env | Rol |
|-----|-----|
| `APP_FEATURES_WORK_ORDER_AI` | `false` = fără `PENDING`, doar catch-all pentru nerezolvate |
| `APP_FILES_CLASSIFICATION_POLL_INTERVAL` | Interval worker (implicit `2s`) |
| `APP_FILES_CLASSIFICATION_BATCH_SIZE` | Batch worker (implicit `10`) |

## Invarianti și erori

- O extensie aparține unui singur folder per work order → **409** la conflict.
- Nume frați unice per părinte; nume fișiere unice per folder → **409** (sau sufix automat la upload/mutare).
- Catch-all: nu poate fi șters, mutat sau să aibă reguli (**400**/**409**).
- Folder cu fișiere (inclusiv în subfoldere): **409** la `DELETE` fără `?moveFilesTo=catchAll`.
- Adâncime maximă: 3 niveluri; mutare într-un descendent propriu: **400**.
- Constrângerile DB sunt arbitrul final la curse concurente (pattern pre-check + catch `DataIntegrityViolationException` → 409).

## Extensibilitate

- **S3/MinIO**: implementare nouă `BlobStorage`; `deleteByPrefix` = list + batch delete.
- **Template-uri per firmă**: tabel `firm_folder_templates` copiat la creare în loc de `DefaultFolderTemplate`.
- **Reguli mai bogate** (MIME, pattern de nume): coloană `kind` pe `work_order_folder_rules`; `FileClassifier` e singurul consumator.
- **Reclasificare după schimbarea regulilor**: bulk UPDATE pe `folder_id` — fără I/O pe disc.
