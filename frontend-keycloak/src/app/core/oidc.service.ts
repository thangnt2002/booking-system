import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { SettingsService } from './settings.service';

/** Per-login values kept across the redirect to Keycloak and back. */
interface PendingLogin {
  state: string;
  nonce: string;
  verifier: string;
  next: string;
}

const PENDING_KEY = 'fe.oidc.pending';
export const CALLBACK_PATH = '/auth/callback';

function randomString(bytes = 32): string {
  const buf = crypto.getRandomValues(new Uint8Array(bytes));
  return base64Url(buf);
}

function base64Url(bytes: Uint8Array): string {
  let s = '';
  bytes.forEach((b) => (s += String.fromCharCode(b)));
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function s256(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64Url(new Uint8Array(digest));
}

/**
 * OIDC Authorization Code + PKCE (S256) against Keycloak.
 * The user types the password on Keycloak's login page; this app only ever sees the resulting tokens.
 */
@Injectable({ providedIn: 'root' })
export class OidcService {
  private cfg = inject(SettingsService);
  private api = inject(ApiService);
  private auth = inject(AuthService);

  private redirectUri() {
    return `${location.origin}${CALLBACK_PATH}`;
  }

  /** Browser-facing realm URL (Keycloak's own origin, so its login page and SSO cookie work normally). */
  private realmUrl() {
    return `${this.cfg.keycloakUrl().replace(/\/+$/, '')}/realms/${encodeURIComponent(this.cfg.keycloakRealm())}`;
  }

  /** Redirects to Keycloak's login page. */
  async login(next = '/') {
    const pending: PendingLogin = { state: randomString(), nonce: randomString(), verifier: randomString(48), next };
    sessionStorage.setItem(PENDING_KEY, JSON.stringify(pending));

    const params = new URLSearchParams({
      client_id: this.cfg.keycloakClientId(),
      redirect_uri: this.redirectUri(),
      response_type: 'code',
      scope: 'openid',
      state: pending.state,
      nonce: pending.nonce,
      code_challenge: await s256(pending.verifier),
      code_challenge_method: 'S256',
    });
    location.assign(`${this.realmUrl()}/protocol/openid-connect/auth?${params}`);
  }

  /** Handles ?code=&state= on the callback route. Returns the in-app URL to continue to. */
  async handleCallback(query: URLSearchParams): Promise<string> {
    const raw = sessionStorage.getItem(PENDING_KEY);
    sessionStorage.removeItem(PENDING_KEY);

    const error = query.get('error');
    if (error) throw new Error(`${error}: ${query.get('error_description') ?? ''}`.trim());

    const pending: PendingLogin | null = raw ? JSON.parse(raw) : null;
    const code = query.get('code');
    if (!pending || !code) throw new Error('Không có phiên đăng nhập đang chờ (mở lại trang Đăng nhập).');
    if (query.get('state') !== pending.state) throw new Error('state không khớp — có thể là request giả mạo.');

    const r = await firstValueFrom(this.api.exchangeCode(code, pending.verifier, this.redirectUri()));

    const idClaims = AuthService.decode(r.id_token);
    if (idClaims?.['nonce'] !== pending.nonce) throw new Error('nonce trong id_token không khớp.');

    this.auth.set({ accessToken: r.access_token, refreshToken: r.refresh_token, idToken: r.id_token, source: 'keycloak' });
    return pending.next || '/';
  }

  /** RP-initiated logout: ends the Keycloak SSO session, then Keycloak redirects back to this app. */
  logout() {
    const idToken = this.auth.tokens()?.idToken;
    this.auth.set(null);
    if (!idToken) return false; // manual token: nothing to end at Keycloak
    const params = new URLSearchParams({
      client_id: this.cfg.keycloakClientId(),
      id_token_hint: idToken,
      post_logout_redirect_uri: location.origin,
    });
    location.assign(`${this.realmUrl()}/protocol/openid-connect/logout?${params}`);
    return true;
  }
}
