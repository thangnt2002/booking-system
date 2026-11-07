import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, errMsg } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { UserCreationRequest } from '../core/models';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="container narrow">
      <div class="card">
        <h1>Đăng nhập</h1>
        <div class="tabs">
          <button [class.active]="provider() === 'identity'" (click)="provider.set('identity')">Identity service</button>
          <button [class.active]="provider() === 'keycloak'" (click)="provider.set('keycloak')">Keycloak</button>
        </div>
        <p class="muted">
          @if (provider() === 'identity') {
            POST /identity/auth/login — token được gateway introspect.
          } @else {
            Password grant tới Keycloak (realm/client trong Cài đặt) — event/order service xác thực JWT Keycloak.
          }
        </p>
        <form (ngSubmit)="submit()">
          <div class="field"><label>Username</label><input [(ngModel)]="username" name="u" required /></div>
          <div class="field"><label>Mật khẩu</label><input type="password" [(ngModel)]="password" name="p" required /></div>
          @if (error()) { <div class="alert error">{{ error() }}</div> }
          <button class="btn" style="width:100%" [disabled]="busy()">Đăng nhập</button>
        </form>
        <p class="muted" style="margin-top:12px">Chưa có tài khoản? <a routerLink="/register">Đăng ký</a> · Hoặc dán token trong <a routerLink="/settings">Cài đặt</a></p>
      </div>
    </div>
  `,
  styles: `.narrow { max-width: 460px; }`,
})
export class Login {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  provider = signal<'identity' | 'keycloak'>('identity');
  busy = signal(false);
  error = signal('');
  username = '';
  password = '';

  submit() {
    this.busy.set(true);
    this.error.set('');
    const done = () => {
      this.busy.set(false);
      this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('next') || '/');
    };
    const fail = (e: unknown) => {
      this.busy.set(false);
      this.error.set(errMsg(e));
    };
    if (this.provider() === 'identity') {
      this.api.login(this.username, this.password).subscribe({
        next: (r) => {
          this.auth.set({ accessToken: r.data.accessToken, refreshToken: r.data.refreshToken, source: 'identity' });
          done();
        },
        error: fail,
      });
    } else {
      this.api.keycloakLogin(this.username, this.password).subscribe({
        next: (r) => {
          this.auth.set({ accessToken: r.access_token, refreshToken: r.refresh_token, source: 'keycloak' });
          done();
        },
        error: fail,
      });
    }
  }
}

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="container narrow">
      <div class="card">
        <h1>Đăng ký</h1>
        <p class="muted">POST /identity/users/registrations (dob phải ≥ 18 tuổi)</p>
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
  m: UserCreationRequest = { username: '', password: '', firstName: '', lastName: '', email: '', dob: '', city: '', avatar: '' };

  submit() {
    this.busy.set(true);
    this.error.set('');
    this.ok.set('');
    const body = Object.fromEntries(Object.entries(this.m).filter(([, v]) => v !== '')) as UserCreationRequest;
    this.api.register(body).subscribe({
      next: (r: any) => {
        this.busy.set(false);
        this.ok.set(`Tạo thành công user id ${r.data?.id}. Bạn có thể đăng nhập.`);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errMsg(e));
      },
    });
  }
}
