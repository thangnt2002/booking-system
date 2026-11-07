# Booking FE (Angular 21)

FE đơn giản để test toàn bộ API public của booking-system.

```bash
npm install
npm start            # http://localhost:4200, proxy theo proxy.conf.json
```

| Prefix FE                | Đích                                  |
|--------------------------|---------------------------------------|
| `/api/v1/{service}/...`  | api-gateway :8888 (chế độ Gateway)    |
| `/svc/{service}/...`     | gọi thẳng service (chế độ Direct)     |
| `/kc/...`                | Keycloak :9999                        |

Đổi chế độ, realm/client Keycloak, hoặc dán token thủ công ở trang **Cài đặt**.
Nút **API log** (góc phải dưới) hiển thị request/response của mọi lời gọi.

Trang: Sự kiện (list/page/search) · Chi tiết sự kiện + mua vé · Vé của tôi (orders/me, get, cancel) ·
Tài khoản (introspect/refresh/logout, profile, PUT user) · Quản trị (event, ticket, role, permission,
admin-data, profile, keycloak-account-service, notification).

Theme: biến CSS trong `src/styles.css` (`:root`).
