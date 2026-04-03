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
- `db/migration/` - migrari Flyway (`V1`, `V2`, `V3`).

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
- Creare produs
- Listare produse per firma
- Setare stoc
- Ajustare stoc

#### `api/HealthController`
- `GET /health` pentru health-check simplu.

### 3) Service (logica de business)

#### `service/AuthService`
- `signup`: creeaza utilizator local cu email/parola.
- `login`: valideaza credentialele.
- `getMe`: returneaza datele userului autentificat.

#### `service/JwtTokenService`
- Genereaza access token JWT semnat.
- Pune in claims: `sub` (userId), `email`, `provider`, `exp`.

#### `service/FirmService`
- Creare firma.
- Listare firme ale userului.
- Verificare apartenenta userului la firma.

#### `service/InventoryService`
- Operatii pe produse/stoc.
- Verifica accesul userului la firma inainte de operatii.

### 4) Repository (acces DB)

Clase principale:
- `UserRepository`
- `FirmRepository`
- `FirmMemberRepository`
- `ProductRepository`

Acestea folosesc Spring Data JPA pentru query-uri si CRUD.

### 5) Model (entitati + enum-uri)

Entitati principale:
- `UserEntity`
- `FirmEntity`
- `FirmMemberEntity`
- `ProductEntity`

Enum-uri principale:
- `ProviderType` (`LOCAL`)
- `MemberRole`

## Cum functioneaza autentificarea

Autentificarea curenta este bazata pe **email/parola + JWT Bearer token**.

### Fluxul de autentificare

1. Utilizatorul face `POST /auth/signup` cu email + parola.
2. Parola este hash-uita cu BCrypt.
3. Se salveaza userul cu provider `LOCAL`.
4. Serverul returneaza un access token JWT.
5. Pentru endpoint-urile protejate, clientul trimite `Authorization: Bearer <token>`.
6. Spring Security valideaza token-ul si pune principalul in context.
7. Controller-ele citesc userId din `jwt.getSubject()`.

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

## Configurare locala

Fisierul `.env` din root trebuie sa contina cel putin:

```env
DB_URL=jdbc:postgresql://localhost:5432/inventoryapp
DB_USERNAME=mirceone
DB_PASSWORD=postgres
DB_MAINTENANCE_DB=postgres
APP_DB_CREATE_IF_MISSING=true
```

## Rulare

```bash
./mvnw spring-boot:run
```

## Teste

```bash
./mvnw test
```
