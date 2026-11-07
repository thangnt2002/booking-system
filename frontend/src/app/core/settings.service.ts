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
 * gateway: /api/v1/{service}/...  -> api-gateway :8888 (token introspected by identity-service)
 * direct : /svc/{service}/...     -> each service port (see proxy.conf.json)
 */
@Injectable({ providedIn: 'root' })
export class SettingsService {
  readonly mode = signal<ApiMode>(load('fe.mode', 'gateway'));
  readonly keycloakRealm = signal<string>(load('fe.kcRealm', 'booking_system_realm'));
  readonly keycloakClientId = signal<string>(load('fe.kcClient', 'booking-fe'));

  readonly base = computed(() => (this.mode() === 'gateway' ? '/api/v1' : '/svc'));

  url(service: string, path: string): string {
    return `${this.base()}/${service}${path}`;
  }

  setMode(m: ApiMode) {
    this.mode.set(m);
    save('fe.mode', m);
  }

  setKeycloak(realm: string, clientId: string) {
    this.keycloakRealm.set(realm);
    this.keycloakClientId.set(clientId);
    save('fe.kcRealm', realm);
    save('fe.kcClient', clientId);
  }
}
