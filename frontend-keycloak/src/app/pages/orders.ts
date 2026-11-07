import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errMsg } from '../core/api.service';
import { OrderDto } from '../core/models';

@Component({
  selector: 'app-orders',
  imports: [FormsModule, RouterLink, DatePipe, DecimalPipe, JsonPipe],
  template: `
    <div class="container">
      <h1>Vé của tôi</h1>

      <div class="card">
        <form class="row" (ngSubmit)="load(true)">
          <div class="field" style="flex:2"><label>Tìm (search)</label><input [(ngModel)]="search" name="s" placeholder="orderNumber…" /></div>
          <div class="field" style="max-width:120px"><label>Limit</label><input type="number" [(ngModel)]="limit" name="l" /></div>
          <button class="btn">GET /orders/me</button>
        </form>
        @if (error()) { <div class="alert error">{{ error() }}</div> }
        @if (msg()) { <div class="alert ok">{{ msg() }}</div> }

        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Mã đơn</th><th>Trạng thái</th><th>SL</th><th>Tổng</th><th>Ngày đặt</th><th>Hết hạn</th><th></th></tr>
            </thead>
            <tbody>
              @for (o of orders(); track o.id) {
                <tr>
                  <td><strong>{{ o.orderNumber }}</strong><div class="muted mono">{{ o.id }}</div></td>
                  <td><span class="badge {{ o.status }}">{{ o.status }}</span></td>
                  <td>{{ o.quantity }}</td>
                  <td>{{ o.totalAmount | number }}đ</td>
                  <td>{{ o.orderDate | date: 'dd/MM HH:mm:ss' }}</td>
                  <td>{{ o.expireTime | date: 'dd/MM HH:mm:ss' }}</td>
                  <td class="row">
                    <button class="btn ghost sm" (click)="detail(o.id)">Chi tiết</button>
                    @if (o.status === 'PENDING') {
                      <a class="btn sm" [routerLink]="['/pay', o.id]">Thanh toán</a>
                      <button class="btn danger sm" (click)="cancel(o.id)">Huỷ</button>
                    }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="7" class="muted">Chưa có đơn hàng.</td></tr>
              }
            </tbody>
          </table>
        </div>
        @if (hasNext()) {
          <div style="text-align:center;margin-top:16px"><button class="btn ghost" (click)="load(false)">Tải thêm</button></div>
        }
      </div>

      <div class="card">
        <h2>Tra cứu / huỷ theo Order ID</h2>
        <div class="row">
          <input [(ngModel)]="lookupId" placeholder="Order ID (UUID)" style="flex:1" />
          <button class="btn sm" (click)="detail(lookupId)">GET /orders/{{ '{' }}id{{ '}' }}</button>
          <button class="btn danger sm" (click)="cancel(lookupId)">POST /orders/{{ '{' }}id{{ '}' }}/cancel</button>
        </div>
        @if (selected(); as s) { <pre class="json">{{ s | json }}</pre> }
      </div>
    </div>
  `,
  styles: `.mono { font-family: var(--mono); font-size: 11px; }`,
})
export class Orders {
  private api = inject(ApiService);
  orders = signal<OrderDto[]>([]);
  selected = signal<OrderDto | null>(null);
  error = signal('');
  msg = signal('');
  hasNext = signal(false);
  search = '';
  limit = 10;
  lookupId = '';
  private cursor = '';

  constructor() {
    this.load(true);
  }

  load(reset: boolean) {
    if (reset) this.cursor = '';
    this.error.set('');
    this.api.myOrders(this.cursor, this.limit, this.search).subscribe({
      next: (r) => {
        const p = r.data;
        this.orders.update((l) => (reset ? p?.data ?? [] : [...l, ...(p?.data ?? [])]));
        this.cursor = p?.cursor ?? '';
        this.hasNext.set(!!p?.hasNext);
      },
      error: (e) => this.error.set(errMsg(e)),
    });
  }

  detail(id: string) {
    if (!id?.trim()) return;
    this.lookupId = id;
    this.error.set('');
    this.api.getOrder(id.trim()).subscribe({
      next: (r) => this.selected.set(r.data),
      error: (e) => this.error.set(errMsg(e)),
    });
  }

  cancel(id: string) {
    if (!id?.trim() || !confirm('Huỷ đơn hàng này?')) return;
    this.error.set('');
    this.msg.set('');
    this.api.cancelOrder(id.trim()).subscribe({
      next: () => {
        this.msg.set('Đã huỷ đơn ' + id);
        this.load(true);
      },
      error: (e) => this.error.set(errMsg(e)),
    });
  }
}
