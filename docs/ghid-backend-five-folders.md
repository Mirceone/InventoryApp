# Ghid backend: 5 foldere root în dosar

## Taxonomie (singura listă permisă)

| `folder_path` | Conținut tipic |
|---------------|----------------|
| `Documente` | PDF, Office, CAD (.dwg/.dxf), SketchUp (.skp) |
| `Renders` | Imagini cu „render” în nume |
| `Poze` | Alte imagini |
| `Facturi` | `factur*`, `invoice*`, XML e-factură |
| `Misc` | Neîncadrat / răspuns AI invalid |

**Fără subfoldere.** Proiectul = dosarul utilizatorului (`firm_dossiers`), nu subfoldere inventate de AI.

## API

- `GET .../documents/folder-structure` — mereu **5** intrări, `documentCount` per folder canonic.
- `GET .../documents?folder=Poze` — filtrează exact `folder_path = Poze`.
- `GET .../folders` — căi canonice distincte (max 5).

## Clasificare

1. **Reguli** — dacă tipul fișierului e clar → cale finală (fără Ollama).
2. **Ollama** (dacă `APP_FEATURES_DOSSIER_AI=true`) — alege **exact** una din cele 5; prompt enumerativ.
3. Altfel → `Misc`.

Sinonime plate (ex. `Renderi`, `SketchUp`) sunt mapate la unul din cele 5 foldere; căi cu `/` necunoscute → `Misc`.

## Migrare

**V16__five_root_folders.sql** — normalizează datele existente la cele 5 căi. Migrările Flyway rămân istoric; codul aplicației nu mai menține mapări legacy.

## Frontend

- Sidebar plat: 5 noduri din `folder-structure`.
- Labels UI: Documente, Renders, Poze, Facturi, Misc.
- Folosește doar căile returnate de API (`Documente`, `Renders`, `Poze`, `Facturi`, `Misc`).

## Teste

```bash
./mvnw test -Dtest=DocumentFolderTaxonomyTest,DocumentFolderClassifierTest,DocumentServiceIntegrationTest,FlywayIntegrationTest
```
