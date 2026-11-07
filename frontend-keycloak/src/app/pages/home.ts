import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errMsg } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { EventDto } from '../core/models';

@Component({
  selector: 'app-home',
  imports: [RouterLink, DatePipe, FormsModule],
  template: `
    <section class="hero">
      <div class="container">
        <h1>Khám phá sự kiện nổi bật</h1>
        <p>Âm nhạc · Sân khấu · Thể thao · Hội thảo</p>
        <form class="search" (ngSubmit)="search()">
          <input [(ngModel)]="searchKey" name="q" placeholder="Tìm sự kiện (GET /events/page?searchKey=)" />
          <button class="btn">Tìm kiếm</button>
        </form>
      </div>
    </section>

    <div class="container">
      <div class="row" style="margin-bottom:16px">
        <h2 style="margin:0">Sự kiện</h2>
        <span class="muted">{{ source() }}</span>
        <div class="spacer"></div>
        <button class="btn ghost sm" (click)="loadList()">GET /events/list (public)</button>
        <button class="btn ghost sm" (click)="loadPage(true)" [disabled]="!auth.isLoggedIn()">
          GET /events/page (cần login)
        </button>
      </div>

      @if (error()) { <div class="alert error">{{ error() }}</div> }
      @if (loading()) { <p class="muted">Đang tải…</p> }

      <div class="grid">
        @for (e of events(); track e.id) {
          <a class="event-card" [routerLink]="['/events', e.id]">
            <div class="poster" [style.background-image]="e.poster ? 'url(' + e.poster + ')' : null">
              @if (!e.poster) { <span>{{ e.name.charAt(0) }}</span> }
            </div>
            <div class="body">
              <h3>{{ e.name }}</h3>
              <div class="date">📅 {{ e.startTime | date: 'dd/MM/yyyy HH:mm' }}</div>
              <div class="muted">📍 {{ e.location || '—' }}</div>
            </div>
          </a>
        } @empty {
          @if (!loading()) { <p class="muted">Không có sự kiện nào.</p> }
        }
      </div>

      @if (hasNext()) {
        <div style="text-align:center;margin-top:24px">
          <button class="btn ghost" (click)="loadPage(false)">Xem thêm</button>
        </div>
      }
    </div>
  `,
  styles: `
    .hero { background: linear-gradient(135deg, var(--c-header) 0%, #2a1630 60%, var(--c-primary) 140%); color: #fff; }
    .hero .container { padding: 48px 16px; }
    .hero p { color: rgba(255,255,255,.75); margin: 0 0 20px; }
    .search { display: flex; gap: 8px; max-width: 560px; }
    .search input { border: 0; padding: 12px 16px; }
    .event-card {
      display: block; background: var(--c-surface); border-radius: var(--radius); overflow: hidden;
      box-shadow: var(--shadow); color: var(--c-text); transition: transform .15s, box-shadow .15s;
    }
    .event-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-hover); }
    .poster {
      aspect-ratio: 16/9; background: linear-gradient(135deg, var(--c-primary), #6d28d9) center/cover;
      display: flex; align-items: center; justify-content: center; color: #fff; font-size: 42px; font-weight: 700;
    }
    .body { padding: 14px 16px 16px; }
    .body h3 { margin-bottom: 6px; }
    .date { color: var(--c-primary); font-weight: 600; font-size: 14px; margin-bottom: 4px; }
  `,
})
export class Home {
  private api = inject(ApiService);
  auth = inject(AuthService);

  events = signal<EventDto[]>([]);
  loading = signal(false);
  error = signal('');
  hasNext = signal(false);
  source = signal('');
  searchKey = '';
  private cursor = '';

  constructor() {
    this.loadList();
  }

  loadList() {
    this.run('GET /events/list', true);
    this.api.listEvents().subscribe({
      next: (r) => {
        this.events.set(r.data ?? []);
        this.hasNext.set(false);
        this.loading.set(false);
      },
      error: (e) => this.fail(e),
    });
  }

  search() {
    if (!this.auth.isLoggedIn()) {
      const q = this.searchKey.toLowerCase();
      this.api.listEvents().subscribe({
        next: (r) => {
          this.source.set('lọc client-side từ /events/list (chưa đăng nhập)');
          this.events.set((r.data ?? []).filter((e) => e.name?.toLowerCase().includes(q)));
        },
        error: (e) => this.fail(e),
      });
      return;
    }
    this.loadPage(true);
  }

  loadPage(reset: boolean) {
    if (reset) this.cursor = '';
    this.run('GET /events/page', reset);
    this.api.pageEvents(this.cursor, 8, this.searchKey).subscribe({
      next: (r) => {
        const p = r.data;
        this.events.update((list) => (reset ? p?.data ?? [] : [...list, ...(p?.data ?? [])]));
        this.cursor = p?.cursor ?? '';
        this.hasNext.set(!!p?.hasNext);
        this.loading.set(false);
      },
      error: (e) => this.fail(e),
    });
  }

  private run(src: string, reset: boolean) {
    this.source.set(src);
    this.error.set('');
    this.loading.set(true);
    if (reset) this.events.set([]);
  }

  private fail(e: unknown) {
    this.loading.set(false);
    this.error.set(errMsg(e));
  }
}
