/*
 * Mock events + tickets for event-service (DB: event-service). Idempotent: rows are keyed by fixed ids.
 * Times are relative to "now", so re-running after deleting the rows gives fresh sale windows.
 * Redis stock counters are warmed lazily from the DB (SET NX), so no Redis seeding is needed.
 *
 * Run:
 *   docker exec -i sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<pwd>' -C \
 *     -d event-service -i /dev/stdin < scripts/mock-data/event-service.sql
 */
SET NOCOUNT ON;

DECLARE @now   DATETIMEOFFSET = SYSDATETIMEOFFSET();
DECLARE @admin UNIQUEIDENTIFIER = '7841444C-9CA8-4BA6-A432-DADB79A85001'; -- adminx (Keycloak id)

DECLARE @events TABLE (id UNIQUEIDENTIFIER, name NVARCHAR(255), description NVARCHAR(MAX), location NVARCHAR(255),
                       poster VARCHAR(255), start_days INT, hours INT);
INSERT INTO @events VALUES
 ('E0000000-0000-0000-0000-000000000001', N'Đêm nhạc Mùa Thu Hà Nội',
  N'Live concert với các ca sĩ hàng đầu, dàn nhạc giao hưởng và hiệu ứng ánh sáng 3D.',
  N'Nhà hát Lớn Hà Nội, 1 Tràng Tiền, Hoàn Kiếm', 'https://picsum.photos/seed/booking-e1/1200/675', 14, 3),
 ('E0000000-0000-0000-0000-000000000002', N'Saigon Indie Fest 2026',
  N'Lễ hội âm nhạc indie ngoài trời 2 ngày, hơn 20 nghệ sĩ.',
  N'Công viên Gia Định, Phú Nhuận, TP.HCM', 'https://picsum.photos/seed/booking-e2/1200/675', 30, 10),
 ('E0000000-0000-0000-0000-000000000003', N'Kịch: Hồn Trương Ba, Da Hàng Thịt',
  N'Vở kịch kinh điển của Lưu Quang Vũ, dàn diễn viên Nhà hát Kịch Việt Nam.',
  N'Nhà hát Kịch Việt Nam, Hà Nội', 'https://picsum.photos/seed/booking-e3/1200/675', 7, 2),
 ('E0000000-0000-0000-0000-000000000004', N'Tech Summit Đà Nẵng',
  N'Hội thảo công nghệ: AI, Cloud, Microservices. Có workshop thực hành.',
  N'Ariyana Convention Centre, Đà Nẵng', 'https://picsum.photos/seed/booking-e4/1200/675', 21, 8),
 ('E0000000-0000-0000-0000-000000000005', N'V.League: Hà Nội FC vs Công An HN',
  N'Trận derby thủ đô vòng 10 V.League.',
  N'Sân vận động Hàng Đẫy, Hà Nội', 'https://picsum.photos/seed/booking-e5/1200/675', 5, 2),
 ('E0000000-0000-0000-0000-000000000006', N'Stand-up Comedy Night',
  N'Đêm hài độc thoại cùng các comedian trẻ.',
  N'Hard Rock Cafe, Quận 1, TP.HCM', 'https://picsum.photos/seed/booking-e6/1200/675', 10, 2);

INSERT INTO events (id, name, description, location, poster, start_time, end_time,
                    created_by, created_date, updated_by, updated_date, is_deleted)
SELECT e.id, e.name, e.description, e.location, e.poster,
       DATEADD(DAY, e.start_days, @now), DATEADD(HOUR, e.hours, DATEADD(DAY, e.start_days, @now)),
       @admin, @now, @admin, @now, 0
FROM @events e
WHERE NOT EXISTS (SELECT 1 FROM events x WHERE x.id = e.id);

/* sale_start_offset_h < 0 => already on sale; > 0 => opens later */
DECLARE @tickets TABLE (id UNIQUEIDENTIFIER, event_id UNIQUEIDENTIFIER, type VARCHAR(20), selling_type VARCHAR(20),
                        status VARCHAR(20), stock INT, available INT, original_price DECIMAL(19,2),
                        flash_price DECIMAL(19,2), sale_start_offset_h INT);
INSERT INTO @tickets VALUES
 -- Đêm nhạc Mùa Thu
 ('A0000000-0000-0000-0001-000000000001', 'E0000000-0000-0000-0000-000000000001', 'VVIP',     'GENERAL',  'ACTIVE',   20,  20, 3500000, 2990000, -24),
 ('A0000000-0000-0000-0001-000000000002', 'E0000000-0000-0000-0000-000000000001', 'VIP',      'GENERAL',  'ACTIVE',   100, 100, 1800000, 1500000, -24),
 ('A0000000-0000-0000-0001-000000000003', 'E0000000-0000-0000-0000-000000000001', 'STANDARD', 'GENERAL',  'ACTIVE',   500, 500,  650000,  650000, -24),
 -- Indie Fest
 ('A0000000-0000-0000-0002-000000000001', 'E0000000-0000-0000-0000-000000000002', 'VIP',      'PRE_SALE', 'ACTIVE',   200, 200, 1200000,  990000, -2),
 ('A0000000-0000-0000-0002-000000000002', 'E0000000-0000-0000-0000-000000000002', 'STANDARD', 'GENERAL',  'DRAFT',    2000, 2000, 450000,  450000, 72),
 -- Kịch
 ('A0000000-0000-0000-0003-000000000001', 'E0000000-0000-0000-0000-000000000003', 'CAT',      'GENERAL',  'ACTIVE',   150, 150,  300000,  250000, -48),
 ('A0000000-0000-0000-0003-000000000002', 'E0000000-0000-0000-0000-000000000003', 'VIP',      'GENERAL',  'SOLD_OUT', 30,  0,    600000,  600000, -48),
 -- Tech Summit
 ('A0000000-0000-0000-0004-000000000001', 'E0000000-0000-0000-0000-000000000004', 'STANDARD', 'GENERAL',  'ACTIVE',   300, 300,  990000,  790000, -24),
 -- Bóng đá: ít vé để test hết hàng / concurrency
 ('A0000000-0000-0000-0005-000000000001', 'E0000000-0000-0000-0000-000000000005', 'STANDARD', 'GENERAL',  'ACTIVE',   5,   5,    150000,  150000, -1),
 ('A0000000-0000-0000-0005-000000000002', 'E0000000-0000-0000-0000-000000000005', 'VIP',      'GENERAL',  'ACTIVE',   50,  50,   400000,  350000, -1),
 -- Comedy
 ('A0000000-0000-0000-0006-000000000001', 'E0000000-0000-0000-0000-000000000006', 'STANDARD', 'GENERAL',  'ACTIVE',   80,  80,   350000,  299000, -24);

INSERT INTO tickets (id, event_id, type, selling_type, status, stock_initial, stock_available, reserved_stock, sold,
                     original_price, flash_price, sale_start_time, sale_end_time,
                     created_by, created_date, updated_by, updated_date, is_deleted)
SELECT t.id, t.event_id, t.type, t.selling_type, t.status, t.stock, t.available, 0, t.stock - t.available,
       t.original_price, t.flash_price,
       DATEADD(HOUR, t.sale_start_offset_h, @now),
       e.start_time,                                   -- sale closes when the event starts
       @admin, @now, @admin, @now, 0
FROM @tickets t
JOIN events e ON e.id = t.event_id
WHERE NOT EXISTS (SELECT 1 FROM tickets x WHERE x.id = t.id);

SELECT e.name, t.id AS ticket_id, t.type, t.status, t.stock_available, t.flash_price
FROM tickets t JOIN events e ON e.id = t.event_id
WHERE t.id LIKE 'A0000000-%'
ORDER BY e.start_time, t.type;
