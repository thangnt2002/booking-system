import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  ApiResponse,
  AuthResponse,
  EventDto,
  EventRequest,
  OrderDto,
  Page,
  PermissionDto,
  RoleDto,
  TicketDto,
  TicketRequest,
  UserCreationRequest,
} from './models';
import { SettingsService } from './settings.service';

const PUBLIC = { headers: new HttpHeaders({ 'X-Skip-Auth': '1' }) };

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private cfg = inject(SettingsService);

  private u(service: string, path: string) {
    return this.cfg.url(service, path);
  }

  // ---------- identity: auth ----------
  login(username: string, password: string) {
    return this.http.post<ApiResponse<AuthResponse>>(this.u('identity', '/auth/login'), { username, password }, PUBLIC);
  }
  introspect(accessToken: string, refreshToken?: string) {
    return this.http.post<ApiResponse<{ valid?: boolean; isValid?: boolean }>>(
      this.u('identity', '/auth/introspect'),
      { accessToken, refreshToken },
    );
  }
  logout(accessToken: string, refreshToken?: string) {
    return this.http.post<ApiResponse<void>>(this.u('identity', '/auth/logout'), { accessToken, refreshToken });
  }
  refresh(accessToken: string, refreshToken?: string) {
    return this.http.post<ApiResponse<AuthResponse>>(this.u('identity', '/auth/refresh'), { accessToken, refreshToken });
  }

  /** Keycloak password grant (event/order services validate Keycloak JWTs). */
  keycloakLogin(username: string, password: string): Observable<{ access_token: string; refresh_token: string }> {
    const body = new HttpParams()
      .set('grant_type', 'password')
      .set('client_id', this.cfg.keycloakClientId())
      .set('username', username)
      .set('password', password);
    return this.http.post<{ access_token: string; refresh_token: string }>(
      `/kc/realms/${this.cfg.keycloakRealm()}/protocol/openid-connect/token`,
      body.toString(),
      { headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }) },
    );
  }

  // ---------- identity: users / roles / permissions / admin ----------
  register(req: UserCreationRequest) {
    return this.http.post<ApiResponse<unknown>>(this.u('identity', '/users/registrations'), req, PUBLIC);
  }
  getUser(id: string) {
    return this.http.get<ApiResponse<unknown>>(this.u('identity', `/users/${id}`));
  }
  updateUser(body: unknown) {
    return this.http.put<ApiResponse<unknown>>(this.u('identity', '/users'), body);
  }
  getRoles() {
    return this.http.get<ApiResponse<RoleDto[]>>(this.u('identity', '/roles'));
  }
  createRole(body: { name: string; description: string; permissions: string[] }) {
    return this.http.post<ApiResponse<RoleDto>>(this.u('identity', '/roles'), body);
  }
  deleteRole(id: string) {
    return this.http.delete<ApiResponse<void>>(this.u('identity', `/roles/${id}`));
  }
  getPermissions() {
    return this.http.get<ApiResponse<PermissionDto[]>>(this.u('identity', '/permissions'));
  }
  createPermission(body: PermissionDto) {
    return this.http.post<ApiResponse<PermissionDto>>(this.u('identity', '/permissions'), body);
  }
  deletePermission(id: string) {
    return this.http.delete<ApiResponse<void>>(this.u('identity', `/permissions/${id}`));
  }
  adminData() {
    return this.http.get<ApiResponse<unknown>>(this.u('identity', '/admin/admin-data'));
  }

  // ---------- profile ----------
  getProfile(id: string) {
    return this.http.get<ApiResponse<unknown>>(this.u('profile', `/profiles/${id}`));
  }
  getProfiles() {
    return this.http.get<ApiResponse<unknown[]>>(this.u('profile', '/profiles'));
  }

  // ---------- keycloak-account-service (direct only, not routed by gateway) ----------
  kcProfileCreate(body: unknown) {
    return this.http.post<ApiResponse<unknown>>('/svc/keycloak-profile/profiles', body);
  }
  kcProfileUpdate(body: unknown) {
    return this.http.put<ApiResponse<unknown>>('/svc/keycloak-profile/profiles', body);
  }
  kcProfileGet(id: string) {
    return this.http.get<ApiResponse<unknown>>(`/svc/keycloak-profile/profiles/${id}`);
  }
  kcProfileList() {
    return this.http.get<ApiResponse<unknown[]>>('/svc/keycloak-profile/profiles');
  }
  kcRoles() {
    return this.http.get<ApiResponse<unknown[]>>('/svc/keycloak-profile/roles');
  }
  kcRoleCreate(body: unknown) {
    return this.http.post<ApiResponse<unknown>>('/svc/keycloak-profile/roles', body);
  }
  kcRoleDelete(id: string) {
    return this.http.delete<ApiResponse<void>>(`/svc/keycloak-profile/roles/${id}`);
  }

  // ---------- notification ----------
  sendEmail(body: { subject: string; htmlContent: string; to: { email: string; name: string }[] }) {
    return this.http.post<ApiResponse<string>>(this.u('notification', '/email/send'), body, PUBLIC);
  }
  notificationOk() {
    return this.http.get<ApiResponse<string>>(this.u('notification', '/ok'));
  }

  // ---------- event ----------
  listEvents() {
    return this.http.get<ApiResponse<EventDto[]>>(this.u('event', '/events/list'), PUBLIC);
  }
  pageEvents(cursor = '', limit = 10, searchKey = '') {
    const params = new HttpParams().set('cursor', cursor).set('limit', limit).set('searchKey', searchKey);
    return this.http.get<ApiResponse<Page<EventDto>>>(this.u('event', '/events/page'), { params });
  }
  getEvent(id: string, version?: number | null) {
    let params = new HttpParams();
    if (version != null) params = params.set('version', version);
    return this.http.get<ApiResponse<EventDto>>(this.u('event', `/events/${id}`), { params });
  }
  createEvent(body: EventRequest) {
    return this.http.post<ApiResponse<EventDto>>(this.u('event', '/events/create'), body);
  }
  updateEvent(id: string, body: EventRequest) {
    return this.http.put<ApiResponse<EventDto>>(this.u('event', `/events/update/${id}`), body);
  }
  createTicket(body: TicketRequest) {
    return this.http.post<ApiResponse<TicketDto>>(this.u('event', '/tickets/create'), body);
  }
  getTicket(id: string, version?: number | null) {
    let params = new HttpParams();
    if (version != null) params = params.set('version', version);
    return this.http.get<ApiResponse<TicketDto>>(this.u('event', `/tickets/${id}`), { params });
  }

  // ---------- order ----------
  placeOrder(ticketId: string, quantity: number, idempotencyKey: string) {
    return this.http.post<ApiResponse<OrderDto>>(
      this.u('order', '/orders'),
      { ticketId, quantity },
      { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) },
    );
  }
  getOrder(id: string) {
    return this.http.get<ApiResponse<OrderDto>>(this.u('order', `/orders/${id}`));
  }
  cancelOrder(id: string) {
    return this.http.post<ApiResponse<void>>(this.u('order', `/orders/${id}/cancel`), {});
  }
  myOrders(cursor = '', limit = 10, search = '') {
    let params = new HttpParams().set('limit', limit);
    if (cursor) params = params.set('cursor', cursor);
    if (search) params = params.set('search', search);
    return this.http.get<ApiResponse<Page<OrderDto>>>(this.u('order', '/orders/me'), { params });
  }
}

/** Extracts a readable error message from an HttpErrorResponse. */
export function errMsg(e: any): string {
  const body = e?.error;
  if (body && typeof body === 'object') {
    return `${e.status} · ${body.message ?? body.error_description ?? body.error ?? JSON.stringify(body)}`;
  }
  return `${e?.status ?? ''} · ${e?.message ?? e}`;
}

export const unwrap = <T>() => map((r: ApiResponse<T>) => r.data);
