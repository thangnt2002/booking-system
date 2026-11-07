import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  ApiResponse,
  EventDto,
  EventRequest,
  KcRoleDto,
  KcUserRequest,
  KcUserResponse,
  KeycloakToken,
  OrderDto,
  Page,
  TicketDto,
  TicketRequest,
} from './models';
import { SettingsService } from './settings.service';

const PUBLIC = { headers: new HttpHeaders({ 'X-Skip-Auth': '1' }) };
const FORM = { headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }) };


@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private cfg = inject(SettingsService);

  private u(service: string, path: string) {
    return this.cfg.url(service, path);
  }
  private account() {
    return this.cfg.url('keycloak-profile', '');
  }

  // ---------- Keycloak (OIDC endpoints of the realm) ----------
  private oidc(path: string) {
    return `/kc/realms/${this.cfg.keycloakRealm()}/protocol/openid-connect${path}`;
  }
  private form(values: Record<string, string>) {
    let p = new HttpParams().set('client_id', this.cfg.keycloakClientId());
    for (const [k, v] of Object.entries(values)) p = p.set(k, v);
    return p.toString();
  }

  /** Authorization Code + PKCE: trade the code from Keycloak's redirect for tokens (see OidcService). */
  exchangeCode(code: string, codeVerifier: string, redirectUri: string) {
    return this.http.post<KeycloakToken>(
      this.oidc('/token'),
      this.form({ grant_type: 'authorization_code', code, code_verifier: codeVerifier, redirect_uri: redirectUri }),
      FORM,
    );
  }
  refresh(refreshToken: string) {
    return this.http.post<KeycloakToken>(this.oidc('/token'), this.form({ grant_type: 'refresh_token', refresh_token: refreshToken }), FORM);
  }
  logout(refreshToken: string) {
    return this.http.post<void>(this.oidc('/logout'), this.form({ refresh_token: refreshToken }), FORM);
  }
  /** Sends the bearer token explicitly (the interceptor skips /kc URLs). */
  userinfo(accessToken: string) {
    return this.http.get<Record<string, unknown>>(this.oidc('/userinfo'), {
      headers: new HttpHeaders({ Authorization: `Bearer ${accessToken}` }),
    });
  }

  // ---------- keycloak-account-service: profiles / roles ----------
  /** Public self sign-up. */
  register(body: KcUserRequest) {
    return this.http.post<ApiResponse<KcUserResponse>>(`${this.account()}/profiles/registrations`, body, PUBLIC);
  }
  /** ADMIN creates an account. */
  createProfile(body: KcUserRequest) {
    return this.http.post<ApiResponse<KcUserResponse>>(`${this.account()}/profiles`, body);
  }
  myProfile() {
    return this.http.get<ApiResponse<KcUserResponse>>(`${this.account()}/profiles/me`);
  }
  updateProfile(body: KcUserRequest) {
    return this.http.put<ApiResponse<KcUserResponse>>(`${this.account()}/profiles`, body);
  }
  getProfile(id: string) {
    return this.http.get<ApiResponse<KcUserResponse>>(`${this.account()}/profiles/${id}`);
  }
  getProfiles() {
    return this.http.get<ApiResponse<KcUserResponse[]>>(`${this.account()}/profiles`);
  }
  getRoles() {
    return this.http.get<ApiResponse<KcRoleDto[]>>(`${this.account()}/roles`);
  }
  createRole(body: { name: string; description: string; permissions: string[] }) {
    return this.http.post<ApiResponse<KcRoleDto>>(`${this.account()}/roles`, body);
  }
  deleteRole(id: string) {
    return this.http.delete<ApiResponse<void>>(`${this.account()}/roles/${id}`);
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
    return this.http.get<ApiResponse<EventDto[]>>(this.u('event', '/events/list'));
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
  /**
   * Mock payment: calls the payment-callback hook (OrderInternalController -> OrderServiceImpl.confirmPayment)
   * directly, standing in for the payment provider. Returns no body; reload the order afterwards.
   */
  mockPay(id: string) {
    return this.http.post<ApiResponse<void>>(this.u('order', `/internal/orders/${id}/paid`), {});
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
