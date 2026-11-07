import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, errMsg } from '../core/api.service';
import { OrderDto } from '../core/models';

const QR_SIZE = 29;

/**
 * Builds a QR-looking module grid (finder patterns + pseudo-random data seeded by the payload).
 * It is NOT a scannable QR code: payment is mocked, the user confirms with a button instead.
 */
function mockQrModules(payload: string): boolean[][] {
  let seed = 2166136261;
  for (let i = 0; i < payload.length; i++) seed = Math.imul(seed ^ payload.charCodeAt(i), 16777619);
  const rand = () => {
    seed ^= seed << 13;
    seed ^= seed >>> 17;
    seed ^= seed << 5;
    return (seed >>> 0) / 4294967296;
  };

  const grid = Array.from({ length: QR_SIZE }, () => Array<boolean>(QR_SIZE).fill(false));
  const inFinder = (r: number, c: number) =>
    (r < 8 && c < 8) || (r < 8 && c >= QR_SIZE - 8) || (r >= QR_SIZE - 8 && c < 8);

  for (let r = 0; r < QR_SIZE; r++) {
    for (let c = 0; c < QR_SIZE; c++) {
      if (inFinder(r, c)) continue;
      grid[r][c] = r === 6 || c === 6 ? (r + c) % 2 === 0 : rand() < 0.5; // timing lines + data
    }
  }
  for (const [top, left] of [[0, 0], [0, QR_SIZE - 7], [QR_SIZE - 7, 0]]) {
    for (let r = 0; r < 7; r++) {
      for (let c = 0; c < 7; c++) {
        const ring = r === 0 || r === 6 || c === 0 || c === 6;
        const core = r >= 2 && r <= 4 && c >= 2 && c <= 4;
        grid[top + r][left + c] = ring || core;
      }
    }
  }
  return grid;
}

@Component({
  selector: 'app-payment',
  imports: [RouterLink, DatePipe, DecimalPipe],
  template: `
    <div class="container narrow">
      <h1>Thanh toán đơn hàng</h1>

      @if (error()) { <div class="alert error">{{ error() }}</div> }

      @if (order(); as o) {
        <div class="card">
          <div class="row head">
            <div>
              <div class="muted">Mã đơn</div>
              <strong>{{ o.orderNumber }}</strong>
            </div>
            <span class="badge {{ o.status }}">{{ o.status }}</span>
          </div>
          <div class="row info">
            <div><div class="muted">Số lượng</div>{{ o.quantity }} × {{ o.unitPrice | number }}đ</div>
            <div><div class="muted">Tổng tiền</div><strong class="amount">{{ o.totalAmount | number }}đ</strong></div>
            <div><div class="muted">Hết hạn</div>{{ o.expireTime | date: 'HH:mm:ss dd/MM' }}</div>
          </div>
        </div>

        @if (o.status === 'PENDING') {
          <div class="card pay">
            <p class="muted">Quét mã QR bằng ứng dụng ngân hàng để thanh toán</p>
            <div class="qr-box">
              <svg [attr.viewBox]="'0 0 ' + (size + 8) + ' ' + (size + 8)" shape-rendering="crispEdges" role="img" aria-label="Mã QR thanh toán (mô phỏng)">
                <rect [attr.width]="size + 8" [attr.height]="size + 8" fill="#fff" />
                @for (row of qr(); track $index; let r = $index) {
                  @for (on of row; track $index; let c = $index) {
                    @if (on) { <rect [attr.x]="c + 4" [attr.y]="r + 4" width="1" height="1" fill="#111" /> }
                  }
                }
              </svg>
              <span class="mock-tag">MOCK</span>
            </div>
            <div class="countdown" [class.late]="remaining() <= 60">
              @if (remaining() > 0) { Còn {{ mmss() }} để thanh toán } @else { Đơn đã quá hạn thanh toán }
            </div>
            <div class="transfer muted">
              Nội dung chuyển khoản: <code>{{ o.orderNumber }}</code> · Số tiền: <code>{{ o.totalAmount | number }}đ</code>
            </div>

            <div class="mock-note">
              Đây là thanh toán <b>mô phỏng</b>: mã QR không quét được. Bấm nút bên dưới để giả lập ngân hàng báo đã nhận tiền.
            </div>
            <button class="btn" style="width:100%" [disabled]="busy() || remaining() <= 0" (click)="pay()">
              {{ busy() ? 'Đang xác nhận…' : 'Tôi đã thanh toán (mock)' }}
            </button>
            <button class="btn ghost danger-text" style="width:100%;margin-top:8px" [disabled]="busy()" (click)="cancel()">Huỷ đơn</button>
          </div>
        } @else if (o.status === 'SUCCESS') {
          <div class="card done">
            <div class="big">✓</div>
            <h2>Thanh toán thành công</h2>
            <p class="muted">Vé đã được xác nhận cho đơn {{ o.orderNumber }}.</p>
            <a class="btn" routerLink="/orders">Xem vé của tôi</a>
          </div>
        } @else {
          <div class="card done">
            <h2>Đơn không thể thanh toán</h2>
            <p class="muted">Trạng thái hiện tại: {{ o.status }}.</p>
            <a class="btn ghost" routerLink="/">Về trang sự kiện</a>
          </div>
        }
      } @else if (!error()) {
        <p class="muted">Đang tải đơn hàng…</p>
      }
    </div>
  `,
  styles: `
    .narrow { max-width: 520px; }
    .head { justify-content: space-between; align-items: center; }
    .info { justify-content: space-between; margin-top: 12px; gap: 16px; }
    .amount { font-size: 18px; color: var(--c-primary, inherit); }
    .pay { text-align: center; }
    .qr-box { position: relative; width: 240px; max-width: 100%; margin: 12px auto; border: 1px solid var(--c-border); border-radius: var(--radius-sm); padding: 8px; background: #fff; }
    .qr-box svg { display: block; width: 100%; height: auto; }
    .mock-tag { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); background: #fff; color: #111; font-weight: 700; font-size: 12px; padding: 2px 6px; border: 2px solid #111; border-radius: 4px; }
    .countdown { font-weight: 600; margin: 8px 0; }
    .countdown.late { color: var(--c-danger); }
    .transfer { font-size: 13px; margin-bottom: 12px; }
    .mock-note { background: #fef3c7; color: #92400e; border-radius: var(--radius-sm); padding: 8px 10px; font-size: 13px; margin-bottom: 12px; text-align: left; }
    .danger-text { color: var(--c-danger); }
    .done { text-align: center; }
    .big { font-size: 48px; color: var(--c-success); line-height: 1; }
    code { font-family: var(--mono); font-size: 12px; }
  `,
})
export class Payment implements OnInit {
  private api = inject(ApiService);
  /** Route param :id (withComponentInputBinding). */
  id = input.required<string>();

  readonly size = QR_SIZE;
  order = signal<OrderDto | null>(null);
  error = signal('');
  busy = signal(false);
  private now = signal(Date.now());

  qr = computed(() => {
    const o = this.order();
    return o ? mockQrModules(`MOCKPAY|${o.orderNumber}|${o.totalAmount}`) : [];
  });
  remaining = computed(() => {
    const o = this.order();
    return o ? Math.max(0, Math.floor((new Date(o.expireTime).getTime() - this.now()) / 1000)) : 0;
  });
  mmss = computed(() => {
    const s = this.remaining();
    return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
  });

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  ngOnInit() {
    this.load();
  }

  load() {
    this.error.set('');
    this.api.getOrder(this.id()).subscribe({
      next: (r) => this.order.set(r.data),
      error: (e) => this.error.set(errMsg(e)),
    });
  }

  pay() {
    this.busy.set(true);
    this.error.set('');
    this.api.mockPay(this.id()).subscribe({
      next: () => {
        this.busy.set(false);
        this.load(); // the hook returns no body: fetch the updated order (status SUCCESS)
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errMsg(e));
        this.load(); // status may have changed (e.g. expired)
      },
    });
  }

  cancel() {
    if (!confirm('Huỷ đơn hàng này?')) return;
    this.busy.set(true);
    this.api.cancelOrder(this.id()).subscribe({
      next: () => {
        this.busy.set(false);
        this.load();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errMsg(e));
      },
    });
  }
}
