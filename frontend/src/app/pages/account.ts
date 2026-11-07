import { Component, inject } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Runner } from '../core/runner';
import { Result } from './result';

@Component({
  selector: 'app-account',
  imports: [FormsModule, JsonPipe, RouterLink, Result],
  template: `
    <div class="container">
      <h1>Tài khoản</h1>
      @if (!auth.isLoggedIn()) {
        <div class="alert error">Chưa đăng nhập. <a routerLink="/login">Đăng nhập</a></div>
      }
      <div class="grid-2">
        <div>
          <div class="card">
            <h2>Token hiện tại</h2>
            <p class="muted">Nguồn: {{ auth.tokens()?.source ?? '—' }} · user id (sub): {{ auth.userId() || '—' }}</p>
            <p class="muted">Roles/scope: {{ auth.roles().join(', ') || '—' }}</p>
            <pre class="json">{{ auth.claims() | json }}</pre>
            <div class="row" style="margin-top:12px">
              <button class="btn ghost sm" (click)="introspect()">POST /auth/introspect</button>
              <button class="btn ghost sm" (click)="refresh()">POST /auth/refresh</button>
              <button class="btn ghost sm" (click)="logoutRemote()">POST /auth/logout</button>
            </div>
            <app-result [r]="run.get('token')" />
          </div>
        </div>

        <div>
          <div class="card">
            <h2>Hồ sơ</h2>
            <div class="row">
              <input [(ngModel)]="profileId" placeholder="userId / profileId" style="flex:1" />
              <button class="btn sm" (click)="run.run('profile', 'GET /profile/profiles/' + profileId, api.getProfile(profileId))">GET profile</button>
              <button class="btn ghost sm" (click)="run.run('profile', 'GET /identity/users/' + profileId, api.getUser(profileId))">GET user</button>
            </div>
            <app-result [r]="run.get('profile')" />
          </div>

          <div class="card">
            <h2>Cập nhật user</h2>
            <p class="muted">PUT /identity/users — để trống trường không đổi; roles cách nhau bằng dấu phẩy.</p>
            <div class="field"><label>User ID</label><input [(ngModel)]="upd.id" /></div>
            <div class="row">
              <div class="field"><label>Họ</label><input [(ngModel)]="upd.lastName" /></div>
              <div class="field"><label>Tên</label><input [(ngModel)]="upd.firstName" /></div>
            </div>
            <div class="row">
              <div class="field"><label>Mật khẩu mới</label><input type="password" [(ngModel)]="upd.password" /></div>
              <div class="field"><label>Ngày sinh</label><input type="date" [(ngModel)]="upd.dob" /></div>
            </div>
            <div class="field"><label>Roles</label><input [(ngModel)]="upd.roles" placeholder="USER,ADMIN" /></div>
            <button class="btn" (click)="updateUser()">Cập nhật</button>
            <app-result [r]="run.get('update')" />
          </div>
        </div>
      </div>
    </div>
  `,
})
export class Account {
  api = inject(ApiService);
  auth = inject(AuthService);
  run = new Runner();
  profileId = this.auth.userId();
  upd = { id: this.auth.userId(), firstName: '', lastName: '', password: '', dob: '', roles: '' };

  introspect() {
    const t = this.auth.tokens();
    this.run.run('token', 'introspect', this.api.introspect(t?.accessToken ?? '', t?.refreshToken));
  }

  refresh() {
    const t = this.auth.tokens();
    this.run.run('token', 'refresh', this.api.refresh(t?.accessToken ?? '', t?.refreshToken), (r) => {
      if (r?.data?.accessToken) {
        this.auth.set({ accessToken: r.data.accessToken, refreshToken: r.data.refreshToken, source: 'identity' });
      }
    });
  }

  logoutRemote() {
    const t = this.auth.tokens();
    this.run.run('token', 'logout (token bị thu hồi, vẫn giữ local để thử lại)', this.api.logout(t?.accessToken ?? '', t?.refreshToken));
  }

  updateUser() {
    const body: Record<string, unknown> = { id: this.upd.id };
    for (const k of ['firstName', 'lastName', 'password', 'dob'] as const) if (this.upd[k]) body[k] = this.upd[k];
    if (this.upd.roles.trim()) body['roles'] = this.upd.roles.split(',').map((s) => s.trim()).filter(Boolean);
    this.run.run('update', 'PUT /identity/users', this.api.updateUser(body));
  }
}
