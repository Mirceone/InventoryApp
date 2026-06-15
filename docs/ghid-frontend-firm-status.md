# Ghid frontend: status firmă

> **Ghid complet:** [`ghid-frontend-firme.md`](ghid-frontend-firme.md)

## Statusuri

| `status` | `statusDisplayLabel` | Operații pe firmă |
|----------|----------------------|-------------------|
| `ACTIVE` | Active | Permise (după rol) |
| `PAUSED` | Paused | **Blocate** (citire + scriere) |
| `CRITICAL` | Critical | **Blocate** + afișează `statusMessage` |

La `POST /firms`, firma nouă are `status: "ACTIVE"`.

## Răspuns `FirmResponse`

```json
{
  "id": "...",
  "name": "Demo SRL",
  "role": "OWNER",
  "roleDisplayLabel": "Admin",
  "status": "PAUSED",
  "statusDisplayLabel": "Paused",
  "statusMessage": null
}
```

## Schimbare status (OWNER)

`PATCH /firms/{firmId}/status`

```json
{
  "status": "PAUSED",
  "message": "Opțional — recomandat la CRITICAL"
}
```

- Funcționează chiar dacă firma e deja în pauză/critic (pentru recuperare la `ACTIVE`).
- `DELETE /firms/{firmId}` rămâne permis pentru OWNER pe firmă blocată.

## Erori la operații blocate

Orice endpoint pe `/firms/{firmId}/...` (produse, categorii, work orders, documente) returnează **403** cu mesaj:

- PAUSED: `Firm operations are paused`
- CRITICAL: `Firm is in critical state` sau `Firm is in critical state: {statusMessage}`

`GET /firms` **nu** e blocat — utilizatorul vede toate firmele și statusurile.

## TypeScript

```typescript
export type FirmStatus = "ACTIVE" | "PAUSED" | "CRITICAL";

export interface Firm {
  id: string;
  name: string;
  role: "OWNER" | "MEMBER";
  roleDisplayLabel: string;
  status: FirmStatus;
  statusDisplayLabel: string;
  statusMessage: string | null;
}

export function isFirmOperational(firm: Pick<Firm, "status">): boolean {
  return firm.status === "ACTIVE";
}
```

## UI recomandat

1. Badge status lângă numele firmei în selector (`statusDisplayLabel`).
2. La schimbare de status, frontend-ul poate citi și inbox-ul din [`ghid-frontend-notifications.md`](./ghid-frontend-notifications.md); backend-ul emite `FIRM_STATUS_CHANGED` pentru update-uri manuale și automate.
3. Dacă `!isFirmOperational(activeFirm)`:
   - Banner galben (PAUSED) sau roșu (CRITICAL) cu `statusMessage` dacă există.
   - Dezactivează navigarea inventar / work orders.
   - Păstrează acces Settings: schimbare status + ștergere firmă (OWNER).
4. Formular status în Settings: select `ACTIVE` | `PAUSED` | `CRITICAL`, câmp mesaj pentru CRITICAL.

## Checklist

- [ ] Tip `Firm` extins cu `status`, `statusDisplayLabel`, `statusMessage`
- [ ] Banner când firma nu e ACTIVE
- [ ] `PATCH /firms/{id}/status` pentru OWNER
- [ ] Tratare 403 cu mesaj clar (nu generic 500)
