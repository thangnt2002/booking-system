import { Component, OnInit, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { KcUserRequest, KcUserResponse } from '../core/models';
import { Runner } from '../core/runner';
import { Result } from './result';

@Component({
  selector: 'app-account',
  imports: [FormsModule, JsonPipe, RouterLink, Result],
  styles: `.profile img { width: 64px; height: 64px; border-radius: 50%; object-fit: cover; }`,
  template: `
    <div class="container">
      <h1>Tài khoản</h1>
      @if (!auth.isLoggedIn()) {
        <div class="alert error">Chưa đăng nhập. <a routerLink="/login">Đăng nhập</a></div>
      }
      <div class="grid-2">
        <div class="card">
          <h2>Token Keycloak</h2>
          <p class="muted">Nguồn: {{ auth.tokens()?.source ?? '—' }} · sub: {{ auth.userId() || '—' }}</p>
          <p class="muted">Realm roles: {{ auth.roles().join(', ') || '—' }}</p>
          <pre class="json">{{ auth.claims() | json }}</pre>
          <div class="row" style="margin-top:12px">
            <button class="btn ghost sm" (click)="userinfo()">GET userinfo</button>
            <button class="btn ghost sm" (click)="refresh()" [disabled]="!auth.tokens()?.refreshToken">Refresh token</button>
            <button class="btn ghost sm" (click)="logoutRemote()" [disabled]="!auth.tokens()?.refreshToken">Logout (Keycloak)</button>
          </div>
          <app-result [r]="run.get('token')" />
        </div>

        <div>
          <div class="card">
            <h2>Hồ sơ (keycloak-account-service)</h2>
            @if (me(); as p) {
              <div class="row profile">
                <img [src]="p.avatar || 'https://i.pravatar.cc/150?u=' + p.username" alt="" />
                <div>
                  <strong>{{ p.lastName }} {{ p.firstName }}</strong> <span class="muted">&#64;{{ p.username }}</span>
                  <div class="muted">{{ p.email }} · {{ p.city || '—' }} · {{ p.dob || '—' }}</div>
                  <div class="muted">profile id: {{ p.id }}</div>
                </div>
              </div>
            }
            <div class="row" style="margin-top:12px">
              <button class="btn sm" (click)="loadMe()" [disabled]="!auth.isLoggedIn()">GET /profiles/me</button>
              <input [(ngModel)]="profileId" placeholder="Profile ID (UUID)" style="flex:1" />
              <button class="btn ghost sm" (click)="run.run('profile', 'GET /profiles/' + profileId, api.getProfile(profileId))">GET by id</button>
            </div>
            <app-result [r]="run.get('profile')" />
          </div>

          <div class="card">
            <h2>Cập nhật hồ sơ</h2>
            <p class="muted">PUT /keycloak-profile/profiles — để trống trường không đổi.</p>
            <div class="row">
              <div class="field"><label>userId (trống = chính mình)</label><input [(ngModel)]="upd.userId" /></div>
              <div class="field"><label>Email</label><input [(ngModel)]="upd.email" /></div>
            </div>
            <div class="row">
              <div class="field"><label>Họ</label><input [(ngModel)]="upd.lastName" /></div>
              <div class="field"><label>Tên</label><input [(ngModel)]="upd.firstName" /></div>
            </div>
            <div class="row">
              <div class="field"><label>Ngày sinh</label><input type="date" [(ngModel)]="upd.dob" /></div>
              <div class="field"><label>Thành phố</label><input [(ngModel)]="upd.city" /></div>
            </div>
            <div class="row">
              <div class="field"><label>Avatar URL</label><input [(ngModel)]="upd.avatar" /></div>
              <div class="field"><label>Mật khẩu mới</label><input type="password" [(ngModel)]="upd.password" /></div>
            </div>
            <button class="btn" (click)="update()">Cập nhật</button>
            <app-result [r]="run.get('update')" />
          </div>
        </div>
      </div>
    </div>
  `,
})
export class Account implements OnInit {
  api = inject(ApiService);
  auth = inject(AuthService);
  run = new Runner();
  me = signal<KcUserResponse | null>(null);
  profileId = '';
  upd: KcUserRequest = { userId: '', email: '', firstName: '', lastName: '', dob: '', city: '', avatar: '', password: '' };

  ngOnInit() {
    if (this.auth.isLoggedIn()) this.loadMe();
  }

  loadMe() {
    this.run.run('profile', 'GET /profiles/me', this.api.myProfile(), (r) => {
      this.me.set(r.data);
      this.profileId = r.data?.id ?? '';
    });
  }

  userinfo() {
    this.run.run('token', 'GET userinfo', this.api.userinfo(this.auth.tokens()?.accessToken ?? ''));
  }

  refresh() {
    const rt = this.auth.tokens()?.refreshToken ?? '';
    this.run.run('token', 'refresh_token grant', this.api.refresh(rt), (r) =>
      this.auth.set({
        accessToken: r.access_token,
        refreshToken: r.refresh_token,
        idToken: r.id_token ?? this.auth.tokens()?.idToken,
        source: 'keycloak',
      }),
    );
  }

  logoutRemote() {
    this.run.run('token', 'Keycloak logout (session đã đóng, token local vẫn giữ để thử)', this.api.logout(this.auth.tokens()?.refreshToken ?? ''));
  }

  update() {
    const body = Object.fromEntries(Object.entries(this.upd).filter(([, v]) => v)) as KcUserRequest;
    this.run.run('update', 'PUT /keycloak-profile/profiles', this.api.updateProfile(body), (r) => {
      if (!body.userId || body.userId === this.auth.userId()) this.me.set(r.data);
    });
  }
}
