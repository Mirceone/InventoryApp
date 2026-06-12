# Ghid frontend: roluri firmă (RBAC faza 1)

> **Ghid complet actualizat:** [`ghid-frontend-firme.md`](ghid-frontend-firme.md) — include roluri, status firmă (ACTIVE/PAUSED/CRITICAL), Settings și checklist.

Document pentru implementarea în UI a rolurilor la nivel de firmă. Backend-ul expune rolul utilizatorului curent per firmă, iar flow-ul de echipă este disponibil în [`ghid-frontend-invitations.md`](./ghid-frontend-invitations.md).

## Context

| Rol API (`role`) | Label UI (`roleDisplayLabel`) | Cine îl are azi |
|------------------|-------------------------------|-----------------|
| `OWNER` | `Admin` | Creatorul la `POST /firms` |
| `MEMBER` | `Angajat` | Angajat / membru echipă, inclusiv prin invitație |

- În DB și în JSON folosiți **`role`** (`OWNER` / `MEMBER`), nu `ADMIN`.
- `roleDisplayLabel` e doar pentru afișare (română din backend); poți ignora label-ul și mapa local dacă vrei i18n.

Alias-uri acceptate de backend pentru căutări viitoare (nu le trimiteți la create/update): `OWNER` ↔ `ADMIN`, `ADMINISTRATOR`; `MEMBER` ↔ `EMPLOYEE`, `ANGAJAT`.

## Verificare backend înainte de UI

1. Repornește Spring din repo-ul `InventoryApp` (nu o instanță veche).
2. Swagger → **Firms**:
   - `GET /firms` — răspuns cu `role`, `roleDisplayLabel`
   - `PATCH /firms/{firmId}` — rename (OWNER)
   - `DELETE /firms/{firmId}` — 204 fără body (OWNER)
3. Dacă lipsesc `PATCH`/`DELETE`, rename/delete vor eșua (500/405) — nu e bug de validare în frontend.

## Contract API — `FirmResponse`

Toate răspunsurile firmă (create, list, rename) au aceeași formă:

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

### Endpoints relevante

| Metodă | Path | Auth | Cine poate | Răspuns |
|--------|------|------|------------|---------|
| `POST` | `/firms` | JWT | orice user logat | `200` + `FirmResponse` (creator = `OWNER`) |
| `GET` | `/firms` | JWT | membri | `200` + `FirmResponse[]` (rol per firmă) |
| `PATCH` | `/firms/{firmId}` | JWT | **OWNER** (firmă ACTIVE) | `200` + `FirmResponse` |
| `PATCH` | `/firms/{firmId}/status` | JWT | **OWNER** | `200` + `FirmResponse` |
| `DELETE` | `/firms/{firmId}` | JWT | **OWNER** | **`204 No Content`** (fără body JSON) |

Body rename:

```json
{ "name": "Nume nou" }
```

Validare: `name` obligatoriu, max 255 caractere.

### Erori de afișat

| HTTP | Când | Mesaj tipic backend |
|------|------|---------------------|
| `400` | Nume gol / invalid | din `VALIDATION_ERROR` |
| `401` | Fără token / expirat | login din nou |
| `403` | Nu e membru sau nu e OWNER la PATCH/DELETE | `"Only the firm owner can perform this action"` sau `"Not a member of this firm"` |
| `404` | `firmId` inexistent | `"Firm not found"` |

**Important la DELETE:** nu parsa JSON la `204` — `response.json()` va eșua.

```typescript
async function deleteFirm(firmId: string): Promise<void> {
  const res = await api.delete(`/firms/${firmId}`);
  if (res.status === 204) return;
  // tratează 403, 404, etc. din body ApiError dacă există
}
```

## Tipuri TypeScript (recomandat)

```typescript
/** Valori canonice din API — singura sursă pentru logică UI */
export type FirmMemberRole = "OWNER" | "MEMBER";

export interface Firm {
  id: string;
  name: string;
  role: FirmMemberRole;
  roleDisplayLabel: string;
}

export interface UpdateFirmRequest {
  name: string;
}
```

Helper-e:

```typescript
export function isFirmOwner(firm: Pick<Firm, "role">): boolean {
  return firm.role === "OWNER";
}

/** Pentru badge-uri; poți folosi roleDisplayLabel din API */
export function firmRoleBadge(firm: Firm): string {
  return firm.roleDisplayLabel;
}
```

## Stare globală / context firmă activă

Când utilizatorul alege o firmă (selector, sidebar, settings):

1. Păstrează obiectul complet `Firm` (cu `role`), nu doar `id` + `name`.
2. La `GET /firms`, reîmprospătează lista și actualizează firma activă dacă `id`-ul există încă (altfel redirect la prima firmă sau ecran „fără firmă”).
3. După `PATCH` rename, înlocuiește firma activă cu răspunsul (include `role` neschimbat).

```typescript
// exemplu Zustand / context
type FirmStore = {
  firms: Firm[];
  activeFirm: Firm | null;
  setActiveFirm: (firm: Firm) => void;
  refreshFirms: () => Promise<void>;
};
```

## Reguli UI — ce afișezi după rol

### Faza 1 (comportament backend actual)

| Zonă UI | OWNER | MEMBER |
|---------|:-----:|:------:|
| Inventar (produse, categorii, stoc) | da | da |
| Dosare + documente | da | da |
| **Settings → redenumire firmă** | da | ascuns / dezactivat |
| **Settings → ștergere firmă** | da | ascuns / dezactivat |
| Badge rol lângă nume firmă | „Admin” | „Angajat” |

Logica recomandată:

```typescript
const canManageFirm = isFirmOwner(activeFirm);

// Settings
<Button disabled={!canManageFirm} onClick={openRename}>Redenumește</Button>
<Button disabled={!canManageFirm} variant="destructive" onClick={confirmDelete}>Șterge firmă</Button>
```

Dacă un MEMBER apelează PATCH/DELETE (bug UI sau API manual), backend răspunde **403** — afișează mesaj clar, nu „Unexpected server error”.

### Ce NU implementați încă

- Roluri `MANAGER`, `WAREHOUSE`, `VIEWER` — nu există în API
- Permission matrix mai granular pentru membri

## Fluxuri ecran

### 1. Listă / selector firme

- `GET /firms` la login sau după create.
- Afișează `name` + badge `roleDisplayLabel` (sau icon diferit pentru OWNER).
- La create (`POST /firms`), răspunsul are `role: "OWNER"` — setează firma nouă ca activă.

### 2. Settings (firmă activă)

```
┌─────────────────────────────────────┐
│ Firmă: Demo SRL    [Admin]          │
│                                     │
│ Nume firmă: [___________] [Salvează]│  ← doar OWNER
│                                     │
│ [Șterge firmă]                      │  ← doar OWNER, confirmare modal
└─────────────────────────────────────┘
```

- **Salvează:** `PATCH /firms/{activeFirm.id}` cu `{ name }`.
- **Ștergere:** modal cu confirmare (tastează numele firmei), apoi `DELETE`, apoi `refreshFirms()` și navigare dacă nu mai rămân firme.

### 3. Restul aplicației (inventar, work orders)

- **Nu** restricționa încă după rol — MEMBER are aceleași drepturi ca OWNER la inventar/documente.
- Nu ascunde upload-uri sau CRUD produse pentru MEMBER în faza 1.

## Client API (exemplu fetch)

```typescript
const API = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function listFirms(token: string): Promise<Firm[]> {
  const res = await fetch(`${API}/firms`, {
    headers: { Authorization: `Bearer ${token}` },
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
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ name }),
  });
  if (!res.ok) throw await parseApiError(res);
  return res.json();
}
```

## Compatibilitate versiuni vechi

Dacă `GET /firms` nu returnează `role` (backend vechi):

- Tratează lipsa ca `role: "OWNER"` doar dacă e singura firmă și user-ul a creat-o (fragil), **sau**
- Afișează banner: „Actualizează backend-ul pentru roluri firmă” și dezactivează rename/delete (pattern deja folosit la PATCH/DELETE lipsă).

Verificare robustă:

```typescript
function firmHasRoleFields(firm: unknown): firm is Firm {
  return (
    typeof firm === "object" &&
    firm !== null &&
    "role" in firm &&
    "roleDisplayLabel" in firm
  );
}
```

## Checklist implementare

- [ ] Tip `Firm` cu `role` + `roleDisplayLabel`
- [ ] `GET /firms` mapat corect; badge rol în selector
- [ ] Context firmă activă păstrează `role`
- [ ] Settings: rename/delete vizibile doar pentru `role === "OWNER"`
- [ ] `DELETE` fără parsare JSON la 204
- [ ] Mesaje 403/404 clare la rename/delete
- [ ] Swagger verificat: PATCH + DELETE pe `/firms/{firmId}`
- [ ] (Opțional) Banner dacă OpenAPI nu listează PATCH/DELETE

## Test manual rapid

1. User A: `POST /firms` → verifică `role: "OWNER"`, `roleDisplayLabel: "Admin"`.
2. `GET /firms` → același rol.
3. `PATCH` rename → OK; UI actualizează numele.
4. User B (alt cont): nu poate PATCH/DELETE firma lui A (403) — dacă nu există invite acceptat, B nu apare în lista de firme a lui A.
5. User A: `DELETE` firmă → 204; lista firme goală; redirect onboarding.

## Legături backend

- Roluri și permisiuni: `service/firms/access/` (`FirmAccessService`, `RolePermissions`, `MemberRoleCatalog`)
- Controller: `api/firms/FirmController.java`
- Echipă și invitații: vezi [`ghid-frontend-invitations.md`](./ghid-frontend-invitations.md)

## Întrebări frecvente

**Folosim `role` sau `roleDisplayLabel` pentru if-uri?**  
Întotdeauna **`role`** (`OWNER` / `MEMBER`). Label-ul e doar UI.

**Afișăm „Admin” în loc de „Owner”?**  
Da, folosește `roleDisplayLabel` din API sau mapare locală echivalentă.

**MEMBER poate șterge work orders?**  
Da, în faza 1 — restricțiile pe work orders/inventar vor veni cu roluri granulare (faza 3).

**Cum adaug un angajat?**  
Da, prin flow-ul de invitații și management membri din [`ghid-frontend-invitations.md`](./ghid-frontend-invitations.md).
