# Mock data

## Users (keycloak-account-service)
Seeded automatically on startup by `AppInitialConfiguration` (Keycloak + DB, idempotent).
Disable mock users with `app.seed.mock-users=false`.

| username | password  | roles       |
|----------|-----------|-------------|
| adminx   | 123123123 | ADMIN, USER |
| user01   | 123123123 | USER        |
| user02   | 123123123 | USER        |
| user03   | 123123123 | USER        |

Login (Keycloak client `bookingClient`):
```bash
curl -d "grant_type=password&client_id=bookingClient&username=user01&password=123123123" \
  http://localhost:9999/realms/booking_system_realm/protocol/openid-connect/token
```

## Events & tickets (event-service)
```bash
docker exec -i sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<pwd>' -C \
  -d event-service -i /dev/stdin < scripts/mock-data/event-service.sql
```
6 events (`E0000000-0000-0000-0000-00000000000N`) and 11 tickets with fixed ids
(`A0000000-0000-0000-000N-00000000000M`), e.g.:

| ticket id                              | event                    | note                    |
|----------------------------------------|--------------------------|-------------------------|
| A0000000-0000-0000-0001-000000000002   | Đêm nhạc Mùa Thu — VIP   | on sale, flash price    |
| A0000000-0000-0000-0005-000000000001   | V.League — STANDARD      | only 5 left (sold-out test) |
| A0000000-0000-0000-0003-000000000002   | Kịch — VIP               | SOLD_OUT                |
| A0000000-0000-0000-0002-000000000002   | Indie Fest — STANDARD    | DRAFT, sale opens in 3 days |
