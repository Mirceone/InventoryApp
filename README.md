# InventoryApp

## Descriere generala

InventoryApp este un backend Spring Boot pentru gestionarea firmelor si a stocurilor de produse.
Proiectul este structurat pe straturi (API -> Service -> Repository -> Model) si foloseste PostgreSQL + Flyway pentru persistenta.

## Structura proiectului

### `src/main/java/com/mirceone/inventoryapp`
Contine codul Java principal al aplicatiei.

- `InventoryAppApplication` - punctul de intrare in aplicatie.
- `bootstrap/` - logica de bootstrap rulata inainte de startup-ul complet Spring.
- `api/` - controllere REST si DTO-uri de request/response.
- `service/` - logica de business.
- `repository/` - acces la baza de date prin Spring Data JPA.
- `model/` - entitati JPA si enum-uri.
- `security/` - configurare securitate si JWT.

### `src/main/resources`
Contine configurari si migrari SQL.

- `application.yml` - configuratia aplicatiei (datasource, Flyway, JWT etc.).
- `db/migration/` - migrari Flyway (`V1` … `V7`).

## Rolul directoarelor si claselor principale

### 1) Bootstrap

#### `bootstrap/DBCreator`
- Citeste setarile DB din `.env` (sau variabile de mediu).
- Verifica daca baza de date exista.
- Creeaza baza daca lipseste (inainte de init-ul Spring/Flyway).

### 2) API (controllers + DTO)

#### `api/auth/AuthController`
Expune endpoint-uri pentru autentificare:
- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /auth/logout-all`
- `GET /auth/me`

DTO-uri relevante:
- `SignupRequest`
- `LoginRequest`
- `AuthResponse`
- `MeResponse`

#### `api/firms/FirmController`
- Creare firma
- Listare firme pentru utilizatorul curent

#### `api/inventory/ProductController`
- Creare produs (categorie optionala — implicit `Misc`; `imgUrl` optional; prag reaprovizionare optional)
- Listare produse per firma
- `GET .../products/buy-list` — produse cu stoc sub pragul efectiv (lista de cumparaturi / restock)
- `PATCH .../products/{productId}` — actualizare partiala (nume, SKU, categorie, imagine, prag reaprovizionare)
- Setare stoc
- Ajustare stoc

#### `api/inventory/CategoryController`
- `GET .../categories` — categorii pentru firma (include intotdeauna categoria implicita `Misc`)

#### `api/HealthController`
- `GET /health` pentru health-check simplu.

### 3) Service (logica de business)

#### `service/AuthService`
- `signup`: creeaza utilizator local cu email/parola.
- `login`: valideaza credentialele.
- `refresh`: roteste refresh token-ul si emite un token pair nou.
- `logout`: revoca refresh token-ul primit.
- `getMe`: returneaza datele userului autentificat.

#### `service/JwtTokenService`
- Genereaza access token JWT semnat.
- Pune in claims: `sub` (userId), `email`, `provider`, `exp`.

#### `service/RefreshTokenService`
- Genereaza refresh token random si stocheaza doar hash-ul in DB.
- Valideaza, revoca si roteste refresh token-uri.
- Aplica hardening:
  - limita sesiuni active per user,
  - cleanup token-uri expirate,
  - `logout-all-sessions` (revocare toate sesiunile active pentru user).

#### `security/AuthRateLimitFilter` + `security/AuthRateLimiter`
- Aplica rate limiting de baza pe endpoint-urile sensibile:
  - `POST /auth/login`
  - `POST /auth/refresh`
- Cand limita este depasita, API raspunde cu `429` si codul `RATE_LIMIT_EXCEEDED`.

#### `service/FirmService`
- Creare firma.
- La creare firma se creeaza automat categoria implicita `Misc` pentru produse fara categorie specificata.
- Listare firme ale userului.
- Verificare apartenenta userului la firma.

#### `service/CategoryService`
- Asigura existenta categoriei `Misc` pentru o firma noua.
- Listare categorii pentru membrii firmei.

#### `service/InventoryService`
- Operatii pe produse/stoc.
- Verifica accesul userului la firma inainte de operatii.
- Lista de reaprovizionare derivata: produse cu `reorder_enabled` si stoc curent sub pragul efectiv (explicit per produs sau default aplicatie).
- Inregistreaza audit trail pentru modificarile de stoc (`SET`, `ADJUST`) cu actor, cantitate anterioara, cantitate noua si delta.

### 4) Repository (acces DB)

Clase principale:
- `UserRepository`
- `FirmRepository`
- `FirmMemberRepository`
- `ProductRepository`
- `CategoryRepository`

Acestea folosesc Spring Data JPA pentru query-uri si CRUD.

### 5) Model (entitati + enum-uri)

Entitati principale:
- `UserEntity`
- `FirmEntity`
- `FirmMemberEntity`
- `ProductEntity`
- `CategoryEntity`
- `RefreshTokenEntity`
- `StockChangeEventEntity`

Enum-uri principale:
- `ProviderType` (`LOCAL`)
- `MemberRole`

## Cum functioneaza autentificarea

Autentificarea curenta este bazata pe **email/parola + JWT Bearer token + refresh token**.

### Fluxul de autentificare

1. Utilizatorul face `POST /auth/signup` cu email + parola.
2. Parola este hash-uita cu BCrypt.
3. Se salveaza userul cu provider `LOCAL`.
4. Serverul returneaza token pair (`accessToken` + `refreshToken`).
5. Pentru endpoint-urile protejate, clientul trimite `Authorization: Bearer <accessToken>`.
6. Cand access token-ul expira, clientul foloseste `POST /auth/refresh` pentru un token pair nou.
7. La `POST /auth/logout`, refresh token-ul este revocat.
8. Spring Security valideaza access token-ul si pune principalul in context.
9. Controller-ele citesc userId din `jwt.getSubject()`.

## OAuth sau nu?

### Pe scurt
**Nu exista in acest moment OAuth complet implementat pentru login social.**

### Detaliu important
- Aplicatia foloseste `oauth2-resource-server` pentru validarea token-urilor JWT pe endpoint-urile protejate.
- Asta inseamna ca partea de "resource server" este activa (validare token), dar nu exista in backend un flow complet de OAuth login (redirect/callback/authorization code exchange) pentru provideri externi.

## Flyway si baza de date

- Flyway ruleaza migrarile din `src/main/resources/db/migration` la startup.
- `spring.jpa.hibernate.ddl-auto: none` inseamna ca schema nu este generata automat de Hibernate.
- Schema este controlata strict prin migrari versionate.
- Migrarea `V4` adauga tabela `refresh_tokens`.
- Migrarea `V5` adauga tabela `stock_change_events`.
- Migrarea `V6` adauga pe `products` coloanele `reorder_enabled` si `reorder_threshold` (prag minim pentru reaprovizionare).
- Migrarea `V7` adauga tabela `categories` (per firma, nume unic), `products.category_id` si `products.img_url`.

## Configurare locala

Fisierul `.env` din root trebuie sa contina cel putin:

```env
DB_URL=jdbc:postgresql://localhost:5432/example
DB_USERNAME=user_example
DB_PASSWORD=pass_example
DB_MAINTENANCE_DB=postgres
APP_DB_CREATE_IF_MISSING=true
APP_JWT_REFRESH_TOKEN_TTL_SECONDS=1209600
APP_JWT_MAX_ACTIVE_REFRESH_TOKENS_PER_USER=5
APP_SECURITY_RATE_LIMIT_AUTH_MAX_REQUESTS=20
APP_SECURITY_RATE_LIMIT_AUTH_WINDOW_SECONDS=60
APP_DEFAULT_REORDER_THRESHOLD=4
```

## Rulare

```bash
./mvnw spring-boot:run
```

## API docs (Swagger)

Dupa pornirea aplicatiei, documentatia OpenAPI este disponibila la:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Teste

```bash
./mvnw test
```

### Teste de integrare (PostgreSQL local)

Nu folosesc Docker. Trebuie un server PostgreSQL accesibil si o **baza separata** pentru teste (aceleasi migrari Flyway ca in productie). O data, creaza baza (exemplu):

```sql
CREATE DATABASE inventoryapp_test;
```

URL-ul implicit din `src/test/resources/application-test.yml` este `jdbc:postgresql://localhost:5432/inventoryapp_test`. Poti suprascrie cu `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`.

La fiecare pornire a contextului Spring cu profilul `test`, Flyway executa `clean()` apoi `migrate()` — schema de test este recreata din migrari (nu se foloseste baza de productie).

### Rezumat implementari teste

Suita de teste acopera atat logica de business (service), cat si contractele HTTP (controller, MockMvc), inclusiv scenarii negative.

- **Auth service**
  - signup, login, refresh, logout
  - cazuri de eroare: email duplicat, parola invalida, refresh invalid/revocat
- **Refresh token service**
  - creare token, consum/rotatie, revocare
- **Inventory service**
  - creare produs, setare stoc, ajustare stoc, lista reaprovizionare (`buy-list`), prag minim
  - cazuri de eroare: produs inexistent, stoc negativ
  - audit trail la modificari de stoc
- **Auth controller (MockMvc)**
  - `signup`, `login`, `refresh`, `logout`, `me`
  - validare format raspuns si payload-uri invalide (`VALIDATION_ERROR`, `BUSINESS_ERROR`)
- **Firm controller (MockMvc)**
  - creare/listare firme
  - payload invalid + acces fara token (`401`)
- **Product controller (MockMvc)**
  - creare/listare produse, `buy-list`, `PATCH` produs, setare/ajustare stoc
  - payload invalid, eroare de business si acces fara token (`401`)
- **Integration tests (DB-backed, PostgreSQL local)**
  - Flyway migration checks (`V1..V7`, tabele esentiale)
  - end-to-end pentru flow-uri `AuthService` (signup/refresh/logout/logout-all)
  - end-to-end pentru `InventoryService` + audit trail in `stock_change_events`
  - profil Spring `test` + baza dedicata (ex. `inventoryapp_test`), aceleasi migrari ca in productie; la fiecare pornire a contextului de test, Flyway ruleaza `clean` apoi `migrate` (vezi `application-test.yml` si `IntegrationTestFlywayConfig`)

La momentul actual, suita ruleaza cu succes (`BUILD SUCCESS`) si include **48 de teste**.
