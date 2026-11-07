import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth.service';
import { ApiMode, SettingsService } from '../core/settings.service';

@Component({
  selector: 'app-settings',
  imports: [FormsModule],
  template: `
    <div class="container" style="max-width:760px">
      <h1>Cài đặt</h1>

      <div class="card">
        <h2>Chế độ gọi API</h2>
        <label class="opt">
          <input type="radio" name="mode" value="gateway" [ngModel]="cfg.mode()" (ngModelChange)="setMode($event)" />
          <span><strong>Gateway</strong> (mặc định) — <code>/api/v1/&#123;service&#125;/…</code> → api-gateway :8888, gateway xác thực JWT Keycloak rồi route tới service.</span>
        </label>
        <label class="opt">
          <input type="radio" name="mode" value="direct" [ngModel]="cfg.mode()" (ngModelChange)="setMode($event)" />
          <span><strong>Direct</strong> — <code>/svc/&#123;service&#125;/…</code> → gọi thẳng từng service (keycloak-account 8082, notification 8083, event 8084, order 8085). Chỉ dùng để debug khi gateway không chạy.</span>
        </label>
        <p class="muted">Cổng được map trong <code>frontend/proxy.conf.json</code>.</p>
      </div>

      <div class="card">
        <h2>Keycloak</h2>
        <div class="row">
          <div class="field"><label>Realm</label><input [(ngModel)]="realm" /></div>
          <div class="field"><label>Client ID (public, bật Standard flow + PKCE S256)</label><input [(ngModel)]="clientId" /></div>
        </div>
        <div class="field"><label>Keycloak URL (trang đăng nhập)</label><input [(ngModel)]="kcUrl" /></div>
        <button class="btn sm" (click)="cfg.setKeycloak(realm, clientId, kcUrl); saved.set('Đã lưu')">Lưu</button>
        <span class="muted"> {{ saved() }}</span>
      </div>

      <div class="card">
        <h2>Dán access token thủ công</h2>
        <textarea [(ngModel)]="manual" placeholder="eyJhbGciOi..."></textarea>
        <div class="row" style="margin-top:8px">
          <button class="btn sm" (click)="useManual()">Dùng token này</button>
          <button class="btn ghost sm" (click)="auth.set(null)">Xoá token</button>
        </div>
      </div>
    </div>
  `,
  styles: `
    .opt { display: flex; gap: 10px; align-items: flex-start; font-weight: 400; color: var(--c-text); font-size: 14px; margin-bottom: 10px; }
    .opt input { width: auto; margin-top: 4px; }
    code { font-family: var(--mono); font-size: 12px; background: var(--c-bg); padding: 1px 5px; border-radius: 4px; }
  `,
})
export class Settings {
  cfg = inject(SettingsService);
  auth = inject(AuthService);
  realm = this.cfg.keycloakRealm();
  clientId = this.cfg.keycloakClientId();
  kcUrl = this.cfg.keycloakUrl();
  manual = '';
  saved = signal('');

  setMode(m: ApiMode) {
    this.cfg.setMode(m);
  }

  useManual() {
    const t = this.manual.trim().replace(/^Bearer\s+/i, '');
    if (t) this.auth.set({ accessToken: t, source: 'manual' });
  }
}
