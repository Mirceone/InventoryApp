# Ghid backend — Work Orders (foldere definite de utilizator)

Modulul Work Orders oferă fiecărui work order un **arbore de foldere definit de utilizator**, cu **reguli de extensii per folder**. Fișierele urcate sunt clasificate **sincron** la upload: extensia decide folderul; dacă nicio regulă nu se potrivește, fișierul ajunge în folderul **catch-all**.

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
| `work_order_files` | Metadata fișier: `folder_id` FK (fără cascade — un folder cu fișiere nu poate fi șters), `display_name` unic (case-insensitive) per folder, `storage_key` unic. |

Nu există `processing_status`, `folder_path` sau worker asincron — clasificarea e sincronă și deterministă.

## Componente

| Componentă | Rol |
|------------|-----|
| `service/storage/BlobStorage` + `LocalBlobStorage` | `store`, `open`, `delete`, `deleteByPrefix` (ștergere work order/firmă într-un singur apel). |
| `service/workorders/WorkOrderService` | CRUD + status; la creare populează folderele din `DefaultFolderTemplate`; la ștergere șterge rândurile și apoi blob-urile pe prefix (after-commit). |
| `service/workorders/WorkOrderFolderService` | Arbore: creare (max. 3 niveluri), redenumire, mutare (validare cicluri/adâncime), ștergere (gol sau `moveFilesTo=catchAll`), înlocuire reguli. |
| `service/workorders/FileClassifier` | `resolveFolderId(workOrderId, extension)` — regulă sau catch-all. |
| `service/workorders/WorkOrderFileService` | Upload (single/batch), listare paginată, descărcare, redenumire, mutare manuală, ștergere; dedupe nume (`plan (1).pdf`). |
| `service/workorders/DefaultFolderTemplate` | Structura implicită (`Documents` + `Misc` catch-all). Numele nu sunt referențiate nicăieri în cod — flagul `catch_all` contează, nu numele. |
| `api/workorders/*` | `WorkOrderController`, `FolderController`, `FileController` + DTO-uri și mappers. |

## Fluxul de upload

1. Validare nume/MIME/dimensiune.
2. `FileClassifier.resolveFolderId(workOrderId, extensie)` — regulă sau catch-all.
3. Blob scris sub cheie opacă (`firmId/workOrderId/fileId.ext`) + SHA-256.
4. INSERT rând fișier cu dedupe pe `display_name` (retry pe conflict de unicitate).
5. La eșec DB → blob-ul e șters (compensare). Orfanii rămași după crash sunt inofensivi.

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
