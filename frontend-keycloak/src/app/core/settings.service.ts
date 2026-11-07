import { Injectable, computed, signal } from '@angular/core';

export type ApiMode = 'gateway' | 'direct';

function load<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function save(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* ignore */
  }
}

/**
 * gateway: /api/v1/{service}/...  -> api-gateway :8888 (default; gateway verifies the Keycloak JWT)
 * direct : /svc/{service}/...     -> each service port (see proxy.conf.json)
 */
@Injectable({ providedIn: 'root' })
export class SettingsService {
  readonly mode = signal<ApiMode>(load('fe.kc.mode', 'gateway'));
  readonly keycloakRealm = signal<string>(load('fe.kcRealm', 'booking_system_realm'));
  readonly keycloakClientId = signal<string>(load('fe.kcClient', 'bookingClient'));
  /** Keycloak's own origin: the browser is redirected here for the login page (token calls go via the /kc proxy). */
  readonly keycloakUrl = signal<string>(load('fe.kcUrl', 'http://localhost:9999'));

  readonly base = computed(() => (this.mode() === 'gateway' ? '/api/v1' : '/svc'));

  url(service: string, path: string): string {
    return `${this.base()}/${service}${path}`;
  }

  setMode(m: ApiMode) {
    this.mode.set(m);
    save('fe.kc.mode', m);
  }

  setKeycloak(realm: string, clientId: string, url: string) {
    this.keycloakRealm.set(realm);
    this.keycloakClientId.set(clientId);
    this.keycloakUrl.set(url);
    save('fe.kcRealm', realm);
    save('fe.kcClient', clientId);
    save('fe.kcUrl', url);
  }
}
