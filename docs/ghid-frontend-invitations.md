# Ghid frontend — invitații membri firmă

> Vezi și [ghid-frontend-firme.md](./ghid-frontend-firme.md) pentru roluri, status firmă și setări.

## Flux

1. **Admin (OWNER)** trimite invitație: email + rol `MEMBER`.
2. Invitatul primește email cu link: `{APP_FRONTEND_URL}/accept-invitation?token=...`
3. **Cont nou**: formular username (`displayName`) + parolă → `POST /auth/accept-invitation` → JWT.
4. **Cont existent**: redirect login → după login, `POST /auth/accept-invitation` doar cu `{ token }` + header `Authorization`.

## API

### Admin — firmă (JWT obligatoriu)

| Metodă | Rută | Descriere |
|--------|------|-----------|
| `GET` | `/firms/{firmId}/members` | Listă echipă |
| `PATCH` | `/firms/{firmId}/members/{memberUserId}/role` | Update rol membru |
| `DELETE` | `/firms/{firmId}/members/{memberUserId}` | Scoate membru |
| `POST` | `/firms/{firmId}/ownership/transfer` | Inițiază transfer ownership (trimite cod pe email) |
| `POST` | `/firms/{firmId}/ownership/transfer/confirm` | Confirmă transfer ownership cu codul primit |
| `POST` | `/firms/{firmId}/invitations` | Trimite invitație (`201`) |
| `GET` | `/firms/{firmId}/invitations` | Invitații `PENDING` |
| `DELETE` | `/firms/{firmId}/invitations/{invitationId}` | Revocă invitație (`204`) |

**Body invite:**

```json
{ "email": "angajat@firma.ro", "role": "MEMBER" }
```

Doar `MEMBER` este permis. Răspuns **nu** conține token-ul din link (doar în email).

**Răspuns invitație:**

```json
{
  "id": "uuid",
  "email": "angajat@firma.ro",
  "role": "MEMBER",
  "roleDisplayLabel": "Angajat",
  "status": "PENDING",
  "expiresAt": "2026-05-29T12:00:00Z",
  "createdAt": "2026-05-22T12:00:00Z"
}
```

**Membru în listă:**

```json
{
  "userId": "uuid",
  "email": "angajat@firma.ro",
  "displayName": "mircea_angajat",
  "role": "MEMBER",
  "roleDisplayLabel": "Angajat",
  "joinedAt": "2026-05-22T12:00:00Z"
}
```

**Update rol membru:**

```json
{ "role": "MEMBER" }
```

`OWNER` nu se setează prin acest endpoint; pentru asta se folosește transferul de ownership.

**Inițiere transfer ownership:**

```json
{ "newOwnerUserId": "uuid" }
```

Răspuns: `202 Accepted`. Backend-ul trimite owner-ului curent un email cu un cod de confirmare din 6 cifre.

**Confirmare transfer ownership:**

```json
{
  "newOwnerUserId": "uuid",
  "confirmationCode": "123456"
}
```

Răspuns: `204 No Content`. Abia după confirmare owner-ul curent devine `MEMBER`, iar noul owner devine `OWNER`.

### Public — acceptare

| Metodă | Rută | Auth |
|--------|------|------|
| `GET` | `/auth/invitations/{token}` | Nu |
| `POST` | `/auth/accept-invitation` | Opțional (obligatoriu dacă `accountExists`) |

**Preview (`GET`):**

```json
{
  "firmName": "Demo SRL",
  "email": "angajat@firma.ro",
  "maskedEmail": "a***@firma.ro",
  "role": "MEMBER",
  "roleDisplayLabel": "Angajat",
  "expiresAt": "2026-05-29T12:00:00Z",
  "accountExists": false
}
```

**Accept cont nou (`POST`):**

```json
{
  "token": "...",
  "displayName": "mircea_angajat",
  "password": "minim8caractere"
}
```

**Accept cont existent (`POST`):** header `Authorization: Bearer ...`, body:

```json
{ "token": "..." }
```

Răspuns: același `AuthResponse` ca la login (access + refresh token).

## Erori uzuale

| HTTP | Mesaj / cauză |
|------|----------------|
| `401` | Token invitație invalid/expirat sau login necesar |
| `403` | User logat nu e același cu emailul invitației |
| `409` | Deja membru sau invitație pending duplicat |
| `403` | Firmă în pauză/critică la creare invitație |

## UI recomandat

### Pagină Settings → Echipă (OWNER)

- Tabel membri (`GET /members`)
- Acțiuni per membru: Change role, Remove
- Acțiune firmă: Transfer ownership către alt membru
- La click pe transfer:
  1. `POST /ownership/transfer`
  2. UI cere codul de 6 cifre primit pe email
  3. `POST /ownership/transfer/confirm`
- Form invitație: email + rol fix „Angajat”
- Tabel invitații pending + buton Revoke

### Pagină `/accept-invitation`

1. Citește `token` din query.
2. `GET /auth/invitations/{token}`.
3. Dacă `accountExists`:
   - Salvează `token` (sessionStorage).
   - Redirect `/login?returnUrl=/accept-invitation?token=...`
   - După login: `POST /auth/accept-invitation` cu Bearer.
4. Altfel: form `displayName` + `password` + confirmare parolă → `POST` fără Bearer → redirect dashboard.

## TypeScript

```ts
export type InviteMemberRequest = { email: string; role: "MEMBER" };

export type InvitationPreview = {
  firmName: string;
  email: string;
  maskedEmail: string;
  role: "MEMBER";
  roleDisplayLabel: string;
  expiresAt: string;
  accountExists: boolean;
};

export type AcceptInvitationRequest = {
  token: string;
  displayName?: string;
  password?: string;
};

export type TransferOwnershipRequest = {
  newOwnerUserId: string;
};

export type ConfirmOwnershipTransferRequest = {
  newOwnerUserId: string;
  confirmationCode: string; // exact 6 cifre
};
```

## Config backend

- `app.frontend-url` — baza linkului din email
- `app.invitations.token-ttl-seconds` — default 7 zile (`604800`)
- `app.firms.ownership-transfer-confirmation-ttl-seconds` — default 15 minute (`900`)
