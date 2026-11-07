import { Component, inject, input, signal, effect, untracked } from '@angular/core';
import { DatePipe, DecimalPipe, JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, errMsg } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { EventDto, OrderDto, TicketDto } from '../core/models';

/** Ticket IDs remembered locally per event, since the BE has no "tickets of event" endpoint. */
export function rememberedTickets(eventId: string): string[] {
  try {
    return JSON.parse(localStorage.getItem('fe.tickets.' + eventId) || '[]');
  } catch {
    return [];
  }
}
export function rememberTicket(eventId: string, ticketId: string) {
  const list = rememberedTickets(eventId);
  if (!list.includes(ticketId)) list.unshift(ticketId);
  try {
    localStorage.setItem('fe.tickets.' + eventId, JSON.stringify(list.slice(0, 20)));
  } catch {
    /* ignore */
  }
}

interface LoadedTicket {
  id: string;
  ticket?: TicketDto;
  error?: string;
  qty: number;
}

@Component({
  selector: 'app-event-detail',
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink, JsonPipe],
  template: `
    <div class="container">
      <a routerLink="/" class="muted">← Quay lại</a>
      @if (error()) { <div class="alert error">{{ error() }}</div> }

      @if (event(); as e) {
        <div class="banner" [style.background-image]="e.poster ? 'url(' + e.poster + ')' : null"></div>
        <div class="grid-2" style="margin-top:20px">
          <div class="card">
            <h1>{{ e.name }}</h1>
            <p class="date">📅 {{ e.startTime | date: 'HH:mm, dd/MM/yyyy' }} – {{ e.endTime | date: 'HH:mm, dd/MM/yyyy' }}</p>
            <p>📍 {{ e.location || '—' }}</p>
            <p style="white-space:pre-line">{{ e.description }}</p>
            <p class="muted">id: {{ e.id }} · version: {{ e.version }}</p>
            <div class="row">
              <input style="max-width:140px" type="number" [(ngModel)]="eventVersion" placeholder="version" />
              <button class="btn ghost sm" (click)="loadEvent()">GET /events/{{ '{' }}id{{ '}' }}?version=</button>
              <a class="btn ghost sm" [routerLink]="['/admin']" [queryParams]="{ tab: 'event', edit: e.id }">Sửa sự kiện</a>
            </div>
          </div>

          <div class="card">
            <h2>Chọn vé</h2>
            <p class="muted">
              BE chưa có API liệt kê vé theo sự kiện, nên nhập Ticket ID (UUID) để tải.
              Vé tạo từ trang Quản trị sẽ được nhớ tự động nếu BE trả về id.
            </p>
            <div class="row">
              <input [(ngModel)]="ticketIdInput" placeholder="Ticket ID" style="flex:1" />
              <button class="btn sm" (click)="addTicket(ticketIdInput)">Tải vé</button>
            </div>

            @for (t of tickets(); track t.id) {
              <div class="ticket">
                @if (t.ticket; as tk) {
                  <div class="row">
                    <strong>{{ tk.type }}</strong>
                    <span class="badge {{ tk.status }}">{{ tk.status }}</span>
                    <span class="badge">{{ tk.sellingType }}</span>
                    <div class="spacer"></div>
                    <span class="price">
                      @if (tk.flashPrice) {
                        <s class="muted">{{ tk.originalPrice | number }}đ</s> {{ tk.flashPrice | number }}đ
                      } @else {
                        {{ tk.originalPrice | number }}đ
                      }
                    </span>
                  </div>
                  <div class="muted">
                    Còn {{ tk.stockAvailable }}/{{ tk.stockInitial }} · Mở bán {{ tk.saleStartTime | date: 'dd/MM HH:mm' }}
                    → {{ tk.saleEndTime | date: 'dd/MM HH:mm' }} · v{{ tk.version }}
                  </div>
                  <div class="muted mono">{{ t.id }}</div>
                  <div class="row" style="margin-top:8px">
                    <input type="number" min="1" style="width:90px" [(ngModel)]="t.qty" />
                    <button class="btn sm" (click)="buy(t)" [disabled]="buying()">Mua vé</button>
                    <button class="btn ghost sm" (click)="reloadTicket(t.id)">↻</button>
                  </div>
                } @else {
                  <div class="alert error">{{ t.id }} — {{ t.error || 'đang tải…' }}</div>
                }
              </div>
            }

            @if (orderMsg()) { <div class="alert" [class.ok]="!!order()" [class.error]="!order()">{{ orderMsg() }}</div> }
            @if (order(); as o) {
              <pre class="json">{{ o | json }}</pre>
              <a class="btn sm" [routerLink]="['/orders']">Xem đơn hàng</a>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .banner { margin-top: 12px; aspect-ratio: 21/7; border-radius: var(--radius);
      background: linear-gradient(135deg, var(--c-header), var(--c-primary)) center/cover; }
    .date { color: var(--c-primary); font-weight: 600; }
    .ticket { border: 1px solid var(--c-border); border-radius: var(--radius-sm); padding: 12px; margin-top: 12px; }
    .price { font-weight: 700; color: var(--c-primary); }
    .mono { font-family: var(--mono); font-size: 11px; }
  `,
})
export class EventDetail {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);

  id = input.required<string>();
  event = signal<EventDto | null>(null);
  error = signal('');
  tickets = signal<LoadedTicket[]>([]);
  buying = signal(false);
  order = signal<OrderDto | null>(null);
  orderMsg = signal('');
  eventVersion: number | null = null;
  ticketIdInput = '';

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => {
        this.loadEvent(id);
        this.tickets.set([]);
        rememberedTickets(id).forEach((t) => this.addTicket(t));
      });
    });
  }

  loadEvent(id = this.id()) {
    this.error.set('');
    this.api.getEvent(id, this.eventVersion).subscribe({
      next: (r) => this.event.set(r.data),
      error: (e) => this.error.set(errMsg(e) + (this.auth.isLoggedIn() ? '' : ' — hãy đăng nhập')),
    });
  }

  addTicket(raw: string) {
    const id = raw.trim();
    if (!id) return;
    if (!this.tickets().some((t) => t.id === id)) {
      this.tickets.update((l) => [...l, { id, qty: 1 }]);
    }
    this.reloadTicket(id);
    this.ticketIdInput = '';
  }

  reloadTicket(id: string) {
    this.api.getTicket(id).subscribe({
      next: (r) => {
        this.patch(id, { ticket: r.data, error: undefined });
        rememberTicket(this.id(), id);
      },
      error: (e) => this.patch(id, { error: errMsg(e) }),
    });
  }

  buy(t: LoadedTicket) {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { next: this.router.url } });
      return;
    }
    this.buying.set(true);
    this.order.set(null);
    this.orderMsg.set('');
    this.api.placeOrder(t.id, Number(t.qty) || 1, crypto.randomUUID()).subscribe({
      next: (r) => {
        this.buying.set(false);
        this.order.set(r.data);
        this.orderMsg.set(`Đặt vé thành công — đơn ${r.data?.orderNumber ?? ''} đang chờ thanh toán.`);
        this.reloadTicket(t.id);
      },
      error: (e) => {
        this.buying.set(false);
        this.orderMsg.set(errMsg(e));
      },
    });
  }

  private patch(id: string, p: Partial<LoadedTicket>) {
    this.tickets.update((l) => l.map((t) => (t.id === id ? { ...t, ...p } : t)));
  }
}
