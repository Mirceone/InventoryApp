# Ghid frontend — notifications inbox

> Vezi și [ghid-frontend-firme.md](./ghid-frontend-firme.md) pentru contextul firmelor și [ghid-frontend-invitations.md](./ghid-frontend-invitations.md) pentru fluxurile de echipă.

## Scope v1

Backend-ul expune un inbox de notificări **per user**, persistat în DB, pentru evenimente generale din aplicație.

În prima versiune sunt trimise notificări pentru:

- produs adăugat în inventar
- produs care trece sub pragul minim și apare în buy list
- status firmă actualizat (manual sau automat)

Email-ul este folosit **doar** pentru schimbări de status care duc firma în `CRITICAL`, iar email-ul merge către owner-ul curent al firmei. Frontend-ul nu trebuie să presupună email pentru celelalte notificări.

## API

Toate endpoint-urile cer `Authorization: Bearer ...`.

| Metodă | Rută | Descriere |
|--------|------|-----------|
| `GET` | `/notifications?unreadOnly=false&limit=50` | Listă inbox curent + `unreadCount` |
| `POST` | `/notifications/{notificationId}/read` | Marchează o notificare ca citită |
| `POST` | `/notifications/read-all` | Marchează toate notificările userului ca citite |

### `GET /notifications`

Query params:

- `unreadOnly` — opțional, implicit `false`
- `limit` — opțional, implicit `50`, max `100`

Răspuns:

```json
{
  "unreadCount": 3,
  "items": [
    {
      "id": "uuid",
      "firmId": "uuid",
      "type": "PRODUCT_LOW_STOCK",
      "level": "WARNING",
      "title": "Produs sub prag minim",
      "body": "Produsul \"Laptop\" a scazut sub pragul minim (2/4) si este vizibil in buy list pentru firma Demo SRL.",
      "metadata": {
        "event": "product_low_stock",
        "productId": "uuid",
        "productName": "Laptop",
        "sku": "SKU-1",
        "currentQuantity": "2",
        "effectiveMinThreshold": "4",
        "buyListVisible": "true"
      },
      "read": false,
      "readAt": null,
      "createdAt": "2026-05-25T15:00:00Z"
    }
  ]
}
```

### `POST /notifications/{id}/read`

Răspuns: `204 No Content`

### `POST /notifications/read-all`

Răspuns: `204 No Content`

## Tipuri și nivele

### `type`

| Tip | Când apare |
|-----|------------|
| `PRODUCT_CREATED` | După creare produs nou |
| `PRODUCT_LOW_STOCK` | O singură dată la crossing sub prag |
| `FIRM_STATUS_CHANGED` | La orice schimbare de status, manuală sau automată |

### `level`

| Level | Semnificație UI |
|-------|-----------------|
| `INFO` | eveniment informativ |
| `WARNING` | necesită atenție |
| `CRITICAL` | problemă importantă, accent vizual puternic |

## Metadata pe eveniment

Frontend-ul trebuie să folosească `type` pentru logică și `metadata` pentru deep-link/context. `title` și `body` sunt deja user-facing și pot fi afișate direct.

### `PRODUCT_CREATED`

```json
{
  "event": "product_created",
  "productId": "uuid",
  "productName": "Laptop",
  "sku": "SKU-1",
  "initialQuantity": "10"
}
```

Recomandare: click pe notificare -> deschide firma (`firmId`) și lista de produse; dacă ai detail page, poți focaliza produsul după `productId`.

### `PRODUCT_LOW_STOCK`

```json
{
  "event": "product_low_stock",
  "productId": "uuid",
  "productName": "Laptop",
  "sku": "SKU-1",
  "currentQuantity": "2",
  "effectiveMinThreshold": "4",
  "buyListVisible": "true"
}
```

Recomandare: click pe notificare -> firmă activă + ecran Buy List / inventory restock view.

Important:

- evenimentul se emite doar la **crossing** din `>= threshold` în `< threshold`
- dacă stocul rămâne sub prag și se mai modifică, backend-ul **nu** emite duplicate
- buy list-ul rămâne dinamic; notificarea spune că produsul este vizibil acolo, nu că există o intrare persistentă separată

### `FIRM_STATUS_CHANGED`

```json
{
  "event": "firm_status_changed",
  "previousStatus": "ACTIVE",
  "newStatus": "CRITICAL",
  "source": "SYSTEM",
  "message": "Ownership mismatch"
}
```

Recomandare: click pe notificare -> Settings / firm status page sau pagina unde afișezi istoricul `GET /firms/{firmId}/status/history`.

## UI recomandat

### Bell / badge global

- badge cu `unreadCount`
- la login: fetch inbox imediat după bootstrap-ul sesiunii
- după `POST /notifications/{id}/read` sau `POST /notifications/read-all`: actualizează local store-ul optimist sau refetch

### Inbox panel / pagină

Minim recomandat:

- listă ordonată descrescător după `createdAt`
- icon / culoare după `level`
- badge sau text `Citită` / `Necitită`
- buton `Mark all as read`
- filtru `Unread only`

### Mapping UI pe `level`

- `INFO` -> neutral / albastru / gri
- `WARNING` -> galben / portocaliu
- `CRITICAL` -> roșu

### Empty states

- fără notificări: „Nu există notificări încă.”
- fără necitite pe filtru: „Nu există notificări necitite.”

## TypeScript

```ts
export type NotificationType =
  | "PRODUCT_CREATED"
  | "PRODUCT_LOW_STOCK"
  | "FIRM_STATUS_CHANGED";

export type NotificationLevel = "INFO" | "WARNING" | "CRITICAL";

export interface AppNotification {
  id: string;
  firmId: string;
  type: NotificationType;
  level: NotificationLevel;
  title: string;
  body: string;
  metadata: Record<string, string>;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationInbox {
  unreadCount: number;
  items: AppNotification[];
}
```

## Exemplu client API

```ts
const API = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

function authHeaders(token: string): HeadersInit {
  return { Authorization: `Bearer ${token}` };
}

export async function listNotifications(
  token: string,
  options?: { unreadOnly?: boolean; limit?: number }
): Promise<NotificationInbox> {
  const params = new URLSearchParams();
  if (options?.unreadOnly) params.set("unreadOnly", "true");
  if (options?.limit) params.set("limit", String(options.limit));

  const qs = params.toString();
  const res = await fetch(`${API}/notifications${qs ? `?${qs}` : ""}`, {
    headers: authHeaders(token),
  });
  if (!res.ok) throw await parseApiError(res);
  return res.json();
}

export async function markNotificationRead(token: string, id: string): Promise<void> {
  const res = await fetch(`${API}/notifications/${id}/read`, {
    method: "POST",
    headers: authHeaders(token),
  });
  if (res.status === 204) return;
  if (!res.ok) throw await parseApiError(res);
}

export async function markAllNotificationsRead(token: string): Promise<void> {
  const res = await fetch(`${API}/notifications/read-all`, {
    method: "POST",
    headers: authHeaders(token),
  });
  if (res.status === 204) return;
  if (!res.ok) throw await parseApiError(res);
}
```

## Checklist

- [ ] tipuri TS pentru inbox și item
- [ ] store global pentru `unreadCount`
- [ ] bell / badge în header
- [ ] drawer/pagină cu listă și filtru `Unread only`
- [ ] `mark read` și `mark all as read`
- [ ] deep-link pe `firmId` + `metadata`
- [ ] mapping vizual pe `level`
