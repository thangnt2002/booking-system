# Booking FE · Keycloak (Angular 21)

Bản FE dùng **Keycloak + keycloak-account-service** (bản `../frontend` dùng identity-service + profile-service).

```bash
npm install
npm start            # http://localhost:4201 (bản identity chạy :4200, có thể chạy song song)
```

| Prefix FE                    | Đích                                         |
|------------------------------|----------------------------------------------|
| `/svc/{service}/...`         | gọi thẳng service — **mặc định**             |
| `/svc/keycloak-profile/...`  | keycloak-account-service :8082 (luôn direct) |
| `/api/v1/{service}/...`      | api-gateway :8888 (tuỳ chọn)                 |
| `/kc/...`                    | Keycloak :9999                               |

## Keycloak
Dùng client `bookingClient` có sẵn trong realm `booking_system_realm` (public, bật Direct access grants).

## Tài khoản & dữ liệu mock
Xem `../scripts/mock-data/README.md`: `adminx` (ADMIN), `user01..03` — mật khẩu `123123123`.
Trang chi tiết của sự kiện mock tự hiển thị vé mock.

## Luồng
- Đăng ký: `POST /keycloak-profile/profiles/registrations` (public, tạo user Keycloak + DB, role USER)
- Đăng nhập / refresh / logout / userinfo: endpoint OIDC của Keycloak
- Hồ sơ: `GET /profiles/me`, `PUT /profiles`; Quản trị: `POST/GET /profiles` (ADMIN), roles
- Sự kiện, vé, đơn hàng, notification: như bản identity
