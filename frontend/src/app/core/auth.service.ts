import { Injectable, computed, signal } from '@angular/core';

export interface Tokens {
  accessToken: string;
  refreshToken?: string;
  source: 'identity' | 'keycloak' | 'manual';
}

function decode(token: string | undefined): Record<string, any> | null {
  if (!token) return null;
  try {
    const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(part.padEnd(part.length + ((4 - (part.length % 4)) % 4), '='))
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly tokens = signal<Tokens | null>(this.read());

  readonly claims = computed(() => decode(this.tokens()?.accessToken));
  readonly isLoggedIn = computed(() => !!this.tokens()?.accessToken);
  readonly userId = computed(() => (this.claims()?.['sub'] as string) ?? '');
  readonly displayName = computed(() => {
    const c = this.claims();
    return (c?.['preferred_username'] || c?.['name'] || c?.['sub'] || '') as string;
  });
  readonly roles = computed<string[]>(() => {
    const c = this.claims();
    if (!c) return [];
    const scope = typeof c['scope'] === 'string' ? (c['scope'] as string).split(' ') : [];
    const realm = (c['realm_access']?.roles as string[]) ?? [];
    return [...scope, ...realm];
  });
  readonly isAdmin = computed(() =>
    this.roles().some((r) => r === 'ROLE_ADMIN' || r === 'ADMIN' || r.toUpperCase() === 'ADMIN'),
  );

  set(tokens: Tokens | null) {
    this.tokens.set(tokens);
    try {
      if (tokens) localStorage.setItem('fe.tokens', JSON.stringify(tokens));
      else localStorage.removeItem('fe.tokens');
    } catch {
      /* ignore */
    }
  }

  private read(): Tokens | null {
    try {
      const raw = localStorage.getItem('fe.tokens');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
