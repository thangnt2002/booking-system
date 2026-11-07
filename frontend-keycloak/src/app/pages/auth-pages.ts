import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, errMsg } from '../core/api.service';
import { OidcService } from '../core/oidc.service';
import { KcUserRequest } from '../core/models';
import { SettingsService } from '../core/settings.service';

@Component({
  selector: 'app-login',
  imports: [RouterLink],
  template: `
    <div class="container narrow">
      <div class="card">
        <h1>Đăng nhập</h1>
        <p class="muted">
          Authorization Code + PKCE · realm <b>{{ cfg.keycloakRealm() }}</b> · client <b>{{ cfg.keycloakClientId() }}</b>
          (<a routerLink="/settings">đổi</a>)
        </p>
        <p>Bạn sẽ được chuyển sang trang đăng nhập của Keycloak, sau đó quay lại đây.</p>
        @if (error()) { <div class="alert error">{{ error() }}</div> }
        <button class="btn" style="width:100%" [disabled]="busy()" (click)="start()">Đăng nhập bằng Keycloak</button>
        <p class="muted" style="margin-top:12px">
          Chưa có tài khoản? <a routerLink="/register">Đăng ký</a> · Hoặc dán token trong <a routerLink="/settings">Cài đặt</a>
        </p>
      </div>
    </div>
  `,
  styles: `.narrow { max-width: 460px; }`,
})
export class Login {
  private oidc = inject(OidcService);
  private route = inject(ActivatedRoute);
  cfg = inject(SettingsService);

  busy = signal(false);
  error = signal('');

  start() {
    this.busy.set(true);
    this.error.set('');
    this.oidc.login(this.route.snapshot.queryParamMap.get('next') || '/').catch((e) => {
      this.busy.set(false);
      this.error.set(String(e?.message ?? e));
    });
  }
}

/** Keycloak redirects back here with ?code=&state= (redirect_uri = {origin}/auth/callback). */
@Component({
  selector: 'app-auth-callback',
  imports: [RouterLink],
  template: `
    <div class="container narrow">
      <div class="card">
        @if (error()) {
          <h1>Đăng nhập thất bại</h1>
          <div class="alert error">{{ error() }}</div>
          <a class="btn" routerLink="/login">Thử lại</a>
        } @else {
          <p class="muted">Đang hoàn tất đăng nhập…</p>
        }
      </div>
    </div>
  `,
  styles: `.narrow { max-width: 460px; }`,
})
export class AuthCallback {
  private oidc = inject(OidcService);
  private router = inject(Router);
  error = signal('');

  constructor() {
    this.oidc
      .handleCallback(new URLSearchParams(location.search))
      .then((next) => this.router.navigateByUrl(next, { replaceUrl: true }))
      .catch((e) => this.error.set(e?.status ? errMsg(e) : String(e?.message ?? e)));
  }
}

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="container narrow">
      <div class="card">
        <h1>Đăng ký</h1>
        <p class="muted">POST /keycloak-profile/profiles/registrations (public) — tạo user trong Keycloak + DB, role USER.</p>
        <form (ngSubmit)="submit()">
          <div class="row">
            <div class="field"><label>Username *</label><input [(ngModel)]="m.username" name="username" required /></div>
            <div class="field"><label>Mật khẩu *</label><input type="password" [(ngModel)]="m.password" name="password" required /></div>
          </div>
          <div class="row">
            <div class="field"><label>Họ</label><input [(ngModel)]="m.lastName" name="lastName" /></div>
            <div class="field"><label>Tên</label><input [(ngModel)]="m.firstName" name="firstName" /></div>
          </div>
          <div class="row">
            <div class="field"><label>Email</label><input type="email" [(ngModel)]="m.email" name="email" /></div>
            <div class="field"><label>Ngày sinh</label><input type="date" [(ngModel)]="m.dob" name="dob" /></div>
          </div>
          <div class="row">
            <div class="field"><label>Thành phố</label><input [(ngModel)]="m.city" name="city" /></div>
            <div class="field"><label>Avatar URL</label><input [(ngModel)]="m.avatar" name="avatar" /></div>
          </div>
          @if (error()) { <div class="alert error">{{ error() }}</div> }
          @if (ok()) { <div class="alert ok">{{ ok() }}</div> }
          <button class="btn" style="width:100%" [disabled]="busy()">Tạo tài khoản</button>
        </form>
        <p class="muted" style="margin-top:12px">Đã có tài khoản? <a routerLink="/login">Đăng nhập</a></p>
      </div>
    </div>
  `,
  styles: `.narrow { max-width: 620px; }`,
})
export class Register {
  private api = inject(ApiService);
  busy = signal(false);
  error = signal('');
  ok = signal('');
  m: KcUserRequest = { username: '', password: '', firstName: '', lastName: '', email: '', dob: '', city: '', avatar: '' };

  submit() {
    this.busy.set(true);
    this.error.set('');
    this.ok.set('');
    const body = Object.fromEntries(Object.entries(this.m).filter(([, v]) => v !== '')) as KcUserRequest;
    this.api.register(body).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.ok.set(`Tạo thành công, profile id ${r.data?.id}. Bạn có thể đăng nhập.`);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errMsg(e));
      },
    });
  }
}
