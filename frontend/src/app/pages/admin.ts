import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { EventDto, SELLING_TYPE, TICKET_STATUS, TICKET_TYPE, TicketRequest } from '../core/models';
import { Runner } from '../core/runner';
import { rememberTicket } from './event-detail';
import { Result } from './result';

type Tab = 'event' | 'ticket' | 'identity' | 'profile' | 'keycloak' | 'notification';

/** datetime-local value <-> ISO-8601 Instant */
const toIso = (v: string) => (v ? new Date(v).toISOString() : null);
const toLocal = (iso?: string) => {
  if (!iso) return '';
  const d = new Date(iso);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};

@Component({
  selector: 'app-admin',
  imports: [FormsModule, RouterLink, Result],
  template: `
    <div class="container">
      <h1>Quản trị</h1>
      <div class="tabs">
        @for (t of tabs; track t.id) {
          <button [class.active]="tab() === t.id" (click)="tab.set(t.id)">{{ t.label }}</button>
        }
      </div>

      @switch (tab()) {
        @case ('event') {
          <div class="grid-2">
            <div class="card">
              <h2>{{ editId ? 'Sửa sự kiện' : 'Tạo sự kiện' }}</h2>
              <div class="row">
                <div class="field"><label>Event ID (để trống = tạo mới)</label><input [(ngModel)]="editId" /></div>
                <button class="btn ghost sm" (click)="loadForEdit()" [disabled]="!editId">Tải</button>
              </div>
              <div class="field"><label>Tên *</label><input [(ngModel)]="ev.name" /></div>
              <div class="field"><label>Mô tả</label><textarea [(ngModel)]="ev.description"></textarea></div>
              <div class="row">
                <div class="field"><label>Bắt đầu</label><input type="datetime-local" [(ngModel)]="ev.startTime" /></div>
                <div class="field"><label>Kết thúc</label><input type="datetime-local" [(ngModel)]="ev.endTime" /></div>
              </div>
              <div class="field"><label>Địa điểm</label><input [(ngModel)]="ev.location" /></div>
              <div class="field"><label>Poster URL</label><input [(ngModel)]="ev.poster" /></div>
              <button class="btn" (click)="saveEvent()">{{ editId ? 'PUT /events/update/{id}' : 'POST /events/create' }}</button>
              <app-result [r]="run.get('event')" />
              @if (lastEventId()) {
                <p><a [routerLink]="['/events', lastEventId()]">Mở trang sự kiện →</a></p>
              }
            </div>
            <div class="card">
              <h2>Tra cứu sự kiện</h2>
              <div class="row">
                <input [(ngModel)]="lookupEventId" placeholder="Event ID" style="flex:2" />
                <input type="number" [(ngModel)]="lookupVersion" placeholder="version" style="flex:1" />
                <button class="btn sm" (click)="run.run('eventGet', 'GET /events/' + lookupEventId, api.getEvent(lookupEventId, lookupVersion))">GET</button>
              </div>
              <app-result [r]="run.get('eventGet')" />
            </div>
          </div>
        }

        @case ('ticket') {
          <div class="grid-2">
            <div class="card">
              <h2>Tạo vé</h2>
              <div class="field"><label>Event ID *</label><input [(ngModel)]="tk.eventId" /></div>
              <div class="row">
                <div class="field"><label>Loại</label><select [(ngModel)]="tk.type">@for (o of TYPES; track o) { <option>{{ o }}</option> }</select></div>
                <div class="field"><label>Trạng thái</label><select [(ngModel)]="tk.status">@for (o of STATUSES; track o) { <option>{{ o }}</option> }</select></div>
                <div class="field"><label>Hình thức bán</label><select [(ngModel)]="tk.sellingType">@for (o of SELLING; track o) { <option>{{ o }}</option> }</select></div>
              </div>
              <div class="row">
                <div class="field"><label>Stock ban đầu</label><input type="number" [(ngModel)]="tk.stockInitial" /></div>
                <div class="field"><label>Stock còn</label><input type="number" [(ngModel)]="tk.stockAvailable" /></div>
                <div class="field"><label>Reserved</label><input type="number" [(ngModel)]="tk.reservedStock" /></div>
              </div>
              <div class="row">
                <div class="field"><label>Giá gốc</label><input type="number" [(ngModel)]="tk.originalPrice" /></div>
                <div class="field"><label>Giá flash</label><input type="number" [(ngModel)]="tk.flashPrice" /></div>
              </div>
              <div class="row">
                <div class="field"><label>Mở bán</label><input type="datetime-local" [(ngModel)]="saleStart" /></div>
                <div class="field"><label>Đóng bán</label><input type="datetime-local" [(ngModel)]="saleEnd" /></div>
              </div>
              <button class="btn" (click)="createTicket()">POST /tickets/create</button>
              <app-result [r]="run.get('ticket')" />
            </div>
            <div class="card">
              <h2>Tra cứu vé</h2>
              <div class="row">
                <input [(ngModel)]="lookupTicketId" placeholder="Ticket ID" style="flex:2" />
                <input type="number" [(ngModel)]="lookupTicketVersion" placeholder="version" style="flex:1" />
                <button class="btn sm" (click)="run.run('ticketGet', 'GET /tickets/' + lookupTicketId, api.getTicket(lookupTicketId, lookupTicketVersion))">GET</button>
              </div>
              <app-result [r]="run.get('ticketGet')" />
            </div>
          </div>
        }

        @case ('identity') {
          <div class="grid-2">
            <div class="card">
              <h2>Permissions</h2>
              <div class="row">
                <input [(ngModel)]="perm.name" placeholder="name" style="flex:1" />
                <input [(ngModel)]="perm.description" placeholder="description" style="flex:2" />
                <button class="btn sm" (click)="run.run('perm', 'POST /permissions', api.createPermission(perm))">Tạo</button>
              </div>
              <div class="row" style="margin-top:8px">
                <button class="btn ghost sm" (click)="run.run('perm', 'GET /permissions', api.getPermissions())">GET all</button>
                <input [(ngModel)]="delPerm" placeholder="name cần xoá" style="flex:1" />
                <button class="btn danger sm" (click)="run.run('perm', 'DELETE /permissions/' + delPerm, api.deletePermission(delPerm))">Xoá</button>
              </div>
              <app-result [r]="run.get('perm')" />
            </div>
            <div class="card">
              <h2>Roles</h2>
              <div class="row">
                <input [(ngModel)]="role.name" placeholder="name" style="flex:1" />
                <input [(ngModel)]="role.description" placeholder="description" style="flex:1" />
              </div>
              <div class="row" style="margin-top:8px">
                <input [(ngModel)]="role.perms" placeholder="permissions (phẩy)" style="flex:1" />
                <button class="btn sm" (click)="createRole()">Tạo</button>
              </div>
              <div class="row" style="margin-top:8px">
                <button class="btn ghost sm" (click)="run.run('role', 'GET /roles', api.getRoles())">GET all</button>
                <input [(ngModel)]="delRole" placeholder="name cần xoá" style="flex:1" />
                <button class="btn danger sm" (click)="run.run('role', 'DELETE /roles/' + delRole, api.deleteRole(delRole))">Xoá</button>
              </div>
              <app-result [r]="run.get('role')" />
            </div>
            <div class="card">
              <h2>Admin & user</h2>
              <div class="row">
                <button class="btn ghost sm" (click)="run.run('adm', 'GET /admin/admin-data', api.adminData())">GET /admin/admin-data</button>
              </div>
              <div class="row" style="margin-top:8px">
                <input [(ngModel)]="userId" placeholder="User ID" style="flex:1" />
                <button class="btn sm" (click)="run.run('adm', 'GET /users/' + userId, api.getUser(userId))">GET /users/{{ '{' }}id{{ '}' }}</button>
              </div>
              <app-result [r]="run.get('adm')" />
            </div>
          </div>
        }

        @case ('profile') {
          <div class="card">
            <h2>Profile service</h2>
            <div class="row">
              <button class="btn ghost sm" (click)="run.run('prof', 'GET /profile/profiles', api.getProfiles())">GET all (ADMIN)</button>
              <input [(ngModel)]="profileId" placeholder="Profile/User ID" style="flex:1" />
              <button class="btn sm" (click)="run.run('prof', 'GET /profile/profiles/' + profileId, api.getProfile(profileId))">GET by id</button>
            </div>
            <app-result [r]="run.get('prof')" />
          </div>
        }

        @case ('keycloak') {
          <p class="muted">keycloak-account-service (:8082, context /keycloak-profile) — không có route trên gateway, luôn gọi trực tiếp qua <code>/svc/keycloak-profile</code>.</p>
          <div class="grid-2">
            <div class="card">
              <h2>Profiles</h2>
              <div class="row">
                <div class="field"><label>userId (cho PUT)</label><input [(ngModel)]="kc.userId" /></div>
                <div class="field"><label>username</label><input [(ngModel)]="kc.username" /></div>
                <div class="field"><label>password</label><input type="password" [(ngModel)]="kc.password" /></div>
              </div>
              <div class="row">
                <div class="field"><label>Email</label><input [(ngModel)]="kc.email" /></div>
                <div class="field"><label>Họ</label><input [(ngModel)]="kc.lastName" /></div>
                <div class="field"><label>Tên</label><input [(ngModel)]="kc.firstName" /></div>
              </div>
              <div class="row">
                <div class="field"><label>Ngày sinh</label><input type="date" [(ngModel)]="kc.dob" /></div>
                <div class="field"><label>Thành phố</label><input [(ngModel)]="kc.city" /></div>
                <div class="field"><label>Avatar</label><input [(ngModel)]="kc.avatar" /></div>
              </div>
              <div class="row">
                <button class="btn sm" (click)="run.run('kcp', 'POST /profiles', api.kcProfileCreate(clean(kc)))">POST</button>
                <button class="btn ghost sm" (click)="run.run('kcp', 'PUT /profiles', api.kcProfileUpdate(clean(kc)))">PUT</button>
                <button class="btn ghost sm" (click)="run.run('kcp', 'GET /profiles', api.kcProfileList())">GET all</button>
                <input [(ngModel)]="kcId" placeholder="UUID" style="flex:1" />
                <button class="btn ghost sm" (click)="run.run('kcp', 'GET /profiles/' + kcId, api.kcProfileGet(kcId))">GET id</button>
              </div>
              <app-result [r]="run.get('kcp')" />
            </div>
            <div class="card">
              <h2>Roles</h2>
              <div class="row">
                <input [(ngModel)]="kcRole.name" placeholder="name" style="flex:1" />
                <input [(ngModel)]="kcRole.description" placeholder="description" style="flex:1" />
                <button class="btn sm" (click)="run.run('kcr', 'POST /roles', api.kcRoleCreate({ name: kcRole.name, description: kcRole.description, permissions: [] }))">Tạo</button>
              </div>
              <div class="row" style="margin-top:8px">
                <button class="btn ghost sm" (click)="run.run('kcr', 'GET /roles', api.kcRoles())">GET all</button>
                <input [(ngModel)]="kcRoleDel" placeholder="UUID" style="flex:1" />
                <button class="btn danger sm" (click)="run.run('kcr', 'DELETE /roles/' + kcRoleDel, api.kcRoleDelete(kcRoleDel))">Xoá</button>
              </div>
              <app-result [r]="run.get('kcr')" />
            </div>
          </div>
        }

        @case ('notification') {
          <div class="card" style="max-width:720px">
            <h2>Gửi email</h2>
            <div class="row">
              <div class="field"><label>Email người nhận</label><input [(ngModel)]="mail.email" /></div>
              <div class="field"><label>Tên</label><input [(ngModel)]="mail.name" /></div>
            </div>
            <div class="field"><label>Tiêu đề</label><input [(ngModel)]="mail.subject" /></div>
            <div class="field"><label>Nội dung HTML</label><textarea [(ngModel)]="mail.html"></textarea></div>
            <div class="row">
              <button class="btn" (click)="sendMail()">POST /email/send</button>
              <button class="btn ghost" (click)="run.run('mail', 'GET /notification/ok', api.notificationOk())">GET /ok</button>
            </div>
            <app-result [r]="run.get('mail')" />
          </div>
        }
      }
    </div>
  `,
  styles: `code { font-family: var(--mono); font-size: 12px; }`,
})
export class Admin {
  api = inject(ApiService);
  run = new Runner();

  readonly TYPES = TICKET_TYPE;
  readonly STATUSES = TICKET_STATUS;
  readonly SELLING = SELLING_TYPE;
  readonly tabs: { id: Tab; label: string }[] = [
    { id: 'event', label: 'Sự kiện' },
    { id: 'ticket', label: 'Vé' },
    { id: 'identity', label: 'Identity (role/permission)' },
    { id: 'profile', label: 'Profile' },
    { id: 'keycloak', label: 'Keycloak account' },
    { id: 'notification', label: 'Notification' },
  ];
  tab = signal<Tab>('event');
  lastEventId = signal('');

  // event
  editId = '';
  ev = { name: '', description: '', startTime: '', endTime: '', location: '', poster: '' };
  lookupEventId = '';
  lookupVersion: number | null = null;

  // ticket
  tk: TicketRequest = {
    eventId: '', status: 'ACTIVE', type: 'STANDARD', sellingType: 'GENERAL',
    stockInitial: 100, stockAvailable: 100, reservedStock: 0, originalPrice: 500000, flashPrice: null,
  };
  saleStart = '';
  saleEnd = '';
  lookupTicketId = '';
  lookupTicketVersion: number | null = null;

  // identity
  perm = { name: '', description: '' };
  delPerm = '';
  role = { name: '', description: '', perms: '' };
  delRole = '';
  userId = '';
  profileId = '';

  // keycloak-account-service
  kc = { userId: '', username: '', password: '', email: '', firstName: '', lastName: '', dob: '', city: '', avatar: '' };
  kcId = '';
  kcRole = { name: '', description: '' };
  kcRoleDel = '';

  // notification
  mail = { email: '', name: '', subject: 'Test email', html: '<h1>Xin chào</h1><p>Email test từ booking FE</p>' };

  constructor() {
    const q = inject(ActivatedRoute).snapshot.queryParamMap;
    const tab = q.get('tab') as Tab | null;
    if (tab) this.tab.set(tab);
    const edit = q.get('edit');
    if (edit) {
      this.editId = edit;
      this.loadForEdit();
    }
  }

  clean<T extends object>(o: T): Partial<T> {
    return Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v != null)) as Partial<T>;
  }

  loadForEdit() {
    this.run.run('event', 'GET /events/' + this.editId, this.api.getEvent(this.editId), (r) => {
      const e = r.data as EventDto;
      this.ev = {
        name: e.name ?? '', description: e.description ?? '', location: e.location ?? '', poster: e.poster ?? '',
        startTime: toLocal(e.startTime), endTime: toLocal(e.endTime),
      };
    });
  }

  saveEvent() {
    const body = {
      ...this.ev,
      startTime: toIso(this.ev.startTime) ?? undefined,
      endTime: toIso(this.ev.endTime) ?? undefined,
    };
    const id = this.editId.trim();
    const obs = id ? this.api.updateEvent(id, body) : this.api.createEvent(body);
    this.run.run('event', id ? `PUT /events/update/${id}` : 'POST /events/create', obs, (r) => {
      this.lastEventId.set(r.data?.id ?? '');
      this.tk.eventId = r.data?.id ?? this.tk.eventId;
    });
  }

  createTicket() {
    const body: TicketRequest = { ...this.tk, saleStartTime: toIso(this.saleStart), saleEndTime: toIso(this.saleEnd) };
    this.run.run('ticket', 'POST /tickets/create', this.api.createTicket(body), (r) => {
      const id = (r.data as any)?.id;
      if (id) rememberTicket(this.tk.eventId, id);
    });
  }

  createRole() {
    const permissions = this.role.perms.split(',').map((s) => s.trim()).filter(Boolean);
    this.run.run('role', 'POST /roles', this.api.createRole({ name: this.role.name, description: this.role.description, permissions }));
  }

  sendMail() {
    this.run.run(
      'mail',
      'POST /notification/email/send',
      this.api.sendEmail({ subject: this.mail.subject, htmlContent: this.mail.html, to: [{ email: this.mail.email, name: this.mail.name }] }),
    );
  }
}
