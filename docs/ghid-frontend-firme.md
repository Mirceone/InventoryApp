# Ghid frontend: firme (roluri + status + Settings)

Document unic pentru colegul de la frontend — aliniat la API-ul actual din `InventoryApp` (Spring Boot). Acoperă **roluri** (`OWNER` / `MEMBER`), **status firmă** (`ACTIVE` / `PAUSED` / `CRITICAL`), rename, delete și blocarea operațiilor.

Ghiduri vechi (parțiale): [`ghid-frontend-firm-roles.md`](ghid-frontend-firm-roles.md), [`ghid-frontend-firm-status.md`](ghid-frontend-firm-status.md). Pentru notification center vezi [`ghid-frontend-notifications.md`](./ghid-frontend-notifications.md).

---

## 1. Verificare backend (înainte de UI)

1. Pornește backend din `/Users/mirceone/IdeaProjects/InventoryApp` (migrare **V17** pentru status).
2. Swagger → tag **Firms** — trebuie să existe:
   - `GET /firms`
   - `POST /firms`
   - `PATCH /firms/{firmId}` — rename
   - `PATCH /firms/{firmId}/status` — **nou**
   - `DELETE /firms/{firmId}`
3. `GET /firms` trebuie să returneze câmpurile: `role`, `roleDisplayLabel`, `status`, `statusDisplayLabel`, `statusMessage`.

Dacă lipsește `PATCH .../status` sau câmpurile de status, frontend-ul va primi erori la schimbarea stării sau la parsare.

---

## 2. Contract `FirmResponse` (complet)

Toate răspunsurile firmă (create, list, rename, update status) au **aceeași formă**:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Demo SRL",
  "role": "OWNER",
  "roleDisplayLabel": "Admin",
  "status": "ACTIVE",
  "statusDisplayLabel": "Active",
  "statusMessage": null
}
```

### Roluri

| `role` | `roleDisplayLabel` | Logică UI |
|--------|-------------------|-----------|
| `OWNER` | Admin | Poate rename, delete, schimbare status |
| `MEMBER` | Angajat | Nu poate gestiona firma; se adaugă prin invitație — vezi [ghid-frontend-invitations.md](./ghid-frontend-invitations.md) |

Folosiți **`role`** pentru `if`, nu `roleDisplayLabel`. Nu trimiteți `ADMIN` la API — e doar label UI.

### Status firmă

| `status` | `statusDisplayLabel` | Operații pe firmă |
|----------|----------------------|-------------------|
| `ACTIVE` | Active | Permise (după rol) |
| `PAUSED` | Paused | **Toate blocate** (citire + scriere inventar/dosare) |
| `CRITICAL` | Critical | **Toate blocate** + afișați `statusMessage` |

La `POST /firms`, status implicit: **`ACTIVE`**.

---

## 3. Endpoints Firms

| Metodă | Path | Cine | Când firmă PAUSED/CRITICAL |
|--------|------|------|----------------------------|
| `GET` | `/firms` | orice membru | **Permis** — vede lista + statusuri |
| `POST` | `/firms` | user logat | Firmă nouă = ACTIVE |
| `PATCH` | `/firms/{firmId}` | OWNER | **Blocat** (403) — rename |
| `PATCH` | `/firms/{firmId}/status` | OWNER | **Permis** — poate reactiva la ACTIVE |
| `DELETE` | `/firms/{firmId}` | OWNER | **Permis** — poate șterge firma blocată |

### Body-uri

**Create** `POST /firms`:
```json
{ "name": "Numele firmei" }
```

**Rename** `PATCH /firms/{firmId}`:
```json
{ "name": "Nume nou" }
```

**Status** `PATCH /firms/{firmId}/status`:
```json
{
  "status": "PAUSED",
  "message": "Opțional — recomandat la CRITICAL"
}
```

Valori `status`: `ACTIVE` | `PAUSED` | `CRITICAL`. `message` max 512 caractere.

**Delete** `DELETE /firms/{firmId}` → **`204 No Content`**, fără body JSON.

---

## 4. TypeScript (tipuri + helper-e)

```typescript
export type FirmMemberRole = "OWNER" | "MEMBER";
export type FirmStatus = "ACTIVE" | "PAUSED" | "CRITICAL";

export interface Firm {
  id: string;
  name: string;
  role: FirmMemberRole;
  roleDisplayLabel: string;
  status: FirmStatus;
  statusDisplayLabel: string;
  statusMessage: string | null;
}

export interface CreateFirmRequest {
  name: string;
}

export interface UpdateFirmRequest {
  name: string;
}

export interface UpdateFirmStatusRequest {
  status: FirmStatus;
  message?: string | null;
}

export function isFirmOwner(firm: Pick<Firm, "role">): boolean {
  return firm.role === "OWNER";
}

export function isFirmOperational(firm: Pick<Firm, "status">): boolean {
  return firm.status === "ACTIVE";
}

/** Mesaj banner când firma nu e operational */
export function firmBlockedMessage(firm: Firm): string {
  if (firm.status === "PAUSED") {
    return "Operațiunile firmei sunt în pauză.";
  }
  if (firm.status === "CRITICAL") {
    return firm.statusMessage
      ? `Firmă în stare critică: ${firm.statusMessage}`
      : "Firmă în stare critică. Contactați administratorul.";
  }
  return "";
}
```

### Context / store (recomandat)

```typescript
type FirmStore = {
  firms: Firm[];
  activeFirm: Firm | null;
  setActiveFirm: (firm: Firm) => void;
  refreshFirms: () => Promise<void>;
};
```

La `refreshFirms`, actualizați `activeFirm` dacă același `id` există încă (cu status/rol nou).

---

## 5. Client API (fetch)

```typescript
const API = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

function authHeaders(token: string, json = false): HeadersInit {
  const h: Record<string, string> = { Authorization: `Bearer ${token}` };
  if (json) h["Content-Type"] = "application/json";
  return h;
}

export async function listFirms(token: string): Promise<Firm[]> {
  const res = await fetch(`${API}/firms`, { headers: authHeaders(token) });
  if (!res.ok) throw await parseApiError(res);
  return res.json();
}

export async function createFirm(token: string, name: string): Promise<Firm> {
  const res = await fetch(`${API}/firms`, {
    method: "POST",
    headers: authHeaders(token, true),
    body: JSON.stringify({ name }),
  });
  if (!res.ok) throw await parseApiError(res);
  return res.json();
}

export async function renameFirm(
  token: string,
  firmId: string,
  name: string
): Promise<Firm> {
  const res = await fetch(`${API}/firms/${firmId}`, {
    method: "PATCH",
    headers: authHeaders(token, true),
    body: JSON.stringify({ name }),
  });
  if (!res.ok) throw await parseApiError(res);
  return res.json();
}

export async function updateFirmStatus(
  token: string,
  firmId: string,
  body: UpdateFirmStatusRequest
): Promise<Firm> {
  const res = await fetch(`${API}/firms/${firmId}/status`, {
    method: "PATCH",
    headers: authHeaders(token, true),
    body: JSON.stringify(body),
  });
  if (!res.ok) throw await parseApiError(res);
  return res.json();
}

export async function deleteFirm(token: string, firmId: string): Promise<void> {
  const res = await fetch(`${API}/firms/${firmId}`, {
    method: "DELETE",
    headers: authHeaders(token),
  });
  if (res.status === 204) return;
  if (!res.ok) throw await parseApiError(res);
}
```

---

## 6. Reguli UI — matrice decizii

### După rol (`role`)

| Zonă | OWNER | MEMBER |
|------|:-----:|:------:|
| Inventar, dosare, documente | da* | da* |
| Settings → rename | da* | nu |
| Settings → status | da* | nu |
| Settings → delete | da* | nu |

\*Doar dacă `status === "ACTIVE"` (vezi mai jos).

### După status (`status`)

| Zonă | ACTIVE | PAUSED | CRITICAL |
|------|:------:|:------:|:--------:|
| Navigare inventar / dosare | da | **nu** | **nu** |
| API `/firms/{id}/products`, dossiers, documents | da | **403** | **403** |
| Settings → rename | da (OWNER) | **nu** (403) | **nu** (403) |
| Settings → schimbare status | da (OWNER) | **da** (OWNER) | **da** (OWNER) |
| Settings → delete | da (OWNER) | da (OWNER) | da (OWNER) |
| `GET /firms` | da | da | da |

### Helper combinat (recomandat în layout)

```typescript
export function canUseFirmFeatures(firm: Firm | null): boolean {
  return firm != null && isFirmOperational(firm);
}

export function canManageFirmSettings(firm: Firm | null): boolean {
  return firm != null && isFirmOwner(firm);
}

export function canRenameFirm(firm: Firm | null): boolean {
  return canManageFirmSettings(firm) && isFirmOperational(firm);
}
```

---

## 7. Fluxuri ecran

### 7.1 Selector firme (sidebar / header)

- `GET /firms` la login și după create.
- Afișare: `name` + badge `roleDisplayLabel` + badge status (`statusDisplayLabel`).
- Icon/culoare status: verde ACTIVE, galben PAUSED, roșu CRITICAL.
- La selectare firmă PAUSED/CRITICAL: permite selectarea, dar app-ul intră în mod „read-only settings”.

### 7.2 Banner global (firmă activă non-ACTIVE)

Când `activeFirm && !isFirmOperational(activeFirm)`:

```
┌────────────────────────────────────────────────────────────┐
│ ⚠ Paused — operațiunile sunt oprite.  [Mergi la Setări] │
└────────────────────────────────────────────────────────────┘
```

Pentru `CRITICAL`, include `statusMessage` dacă există.

### 7.3 Layout principal — guard pe rută

```typescript
// În layout-ul care învelește /inventory, /dossiers, etc.
if (activeFirm && !isFirmOperational(activeFirm)) {
  return <FirmBlockedPage firm={activeFirm} />;
}
```

Nu apelați produse/dosare — veți primi oricum 403.

### 7.4 Settings firmă

```
┌──────────────────────────────────────────────────┐
│ Demo SRL    [Admin]  [Active ▼]                  │
├──────────────────────────────────────────────────┤
│ Status firmă (OWNER)                             │
│   [ Active | Paused | Critical ]                 │
│   Mesaj (dacă Critical): [________________]      │
│   [Salvează status]                              │
├──────────────────────────────────────────────────┤
│ Nume firmă (OWNER, doar ACTIVE)                  │
│   [___________] [Salvează]                       │
├──────────────────────────────────────────────────┤
│ [Șterge firmă]  (OWNER, orice status)            │
└──────────────────────────────────────────────────┘
```

- **Salvează status:** `PATCH /firms/{id}/status` → înlocuiți `activeFirm` cu răspunsul.
- **Rename:** dezactivat dacă `!isFirmOperational(activeFirm)`.
- **Delete:** confirmare modal → `DELETE` → `refreshFirms()` → redirect dacă lista e goală.

---

## 8. Erori HTTP de tratat

| HTTP | Situație | `message` tipic (body `ApiErrorResponse`) |
|------|----------|---------------------------------------------|
| `400` | Validare nume/status | `VALIDATION_ERROR` |
| `401` | Token lipsă/expirat | Re-login |
| `403` | Nu e OWNER la PATCH/DELETE/status | `Only the firm owner can perform this action` |
| `403` | Firmă PAUSED | `Firm operations are paused` |
| `403` | Firmă CRITICAL | `Firm is in critical state` sau cu detaliu |
| `403` | Nu e membru | `Not a member of this firm` |
| `404` | firmId invalid | `Firm not found` |

**Nu** afișați „Unexpected server error” pentru 403 — mapați mesajul din `message`.

```typescript
async function parseApiError(res: Response): Promise<Error> {
  try {
    const body = await res.json();
    return new Error(body.message ?? res.statusText);
  } catch {
    return new Error(res.statusText);
  }
}
```

---

## 9. Compatibilitate backend vechi

```typescript
export function firmHasFullContract(firm: unknown): firm is Firm {
  if (typeof firm !== "object" || firm === null) return false;
  const f = firm as Record<string, unknown>;
  return (
    "role" in f &&
    "status" in f &&
    "roleDisplayLabel" in f &&
    "statusDisplayLabel" in f
  );
}
```

Dacă `GET /firms` nu are `status`:

- Banner: „Backend-ul trebuie actualizat (status firmă).”
- Dezactivați inventar/dosare sau presupuneți `ACTIVE` doar ca fallback fragil.

Verificare Swagger: există `PATCH /firms/{firmId}/status`.

---

## 10. Endpoints firmă-scoped (inventar, dosare)

Aceste rute **respectă statusul** — nu e nevoie de header suplimentar:

- `GET/POST/PATCH/DELETE` `/firms/{firmId}/products...`
- `/firms/{firmId}/categories...`
- `/firms/{firmId}/dossiers...`
- `/firms/{firmId}/dossiers/{dossierId}/documents...`

Taxonomie dosare (5 foldere): [`ghid-backend-five-folders.md`](ghid-backend-five-folders.md).

---

## 11. Checklist implementare

### Tipuri & API
- [ ] `Firm` cu toate câmpurile (rol + status)
- [ ] `listFirms`, `createFirm`, `renameFirm`, `updateFirmStatus`, `deleteFirm`
- [ ] `DELETE` fără `response.json()` la 204

### UI
- [ ] Badge rol + badge status în selector firme
- [ ] `activeFirm` complet în context
- [ ] Banner când `!isFirmOperational(activeFirm)`
- [ ] Guard pe rute inventar/dosare
- [ ] Settings: status (OWNER), rename (OWNER + ACTIVE), delete (OWNER)

### Erori
- [ ] Mesaje 403 specifice (pauză / critic / not owner)
- [ ] Fără 500 generic la PATCH status lipsă — verificare Swagger

### Test manual
1. `POST /firms` → `status: ACTIVE`, `role: OWNER`
2. `PATCH .../status` → `PAUSED` → inventar returnează 403
3. `PATCH .../status` → `ACTIVE` → inventar funcționează
4. `PATCH .../status` → `CRITICAL` + message → banner cu mesaj
5. Rename pe firmă PAUSED → 403; pe ACTIVE → OK
6. DELETE pe firmă CRITICAL → 204

---

## 12. Ce nu e încă în API

- Invitații membri, listă echipă, schimbare rol, remove member și transfer ownership — **disponibile:** [ghid-frontend-invitations.md](./ghid-frontend-invitations.md)
- Istoric schimbări status — disponibil prin `GET /firms/{firmId}/status/history`
- Job automat conservator care setează `CRITICAL` la inconsistențe de ownership — disponibil server-side
- Notificări inbox per user — **disponibile:** [ghid-frontend-notifications.md](./ghid-frontend-notifications.md)

---

## 13. FAQ

**Prioritate la blocare: rol sau status?**  
Verificați ambele: mai întâi `isFirmOperational`, apoi `isFirmOwner` pentru acțiuni Settings.

**MEMBER poate schimba statusul?**  
Nu — doar OWNER.

**Putem lăsa userul pe pagina de produse cu firmă în pauză?**  
Nu recomandat — redirect sau pagină dedicată + link la Settings.

**Culoare badge status?**  
Folosiți `statusDisplayLabel` din API; culorile sunt decizie UI (galben/roșu).
