import { Component, inject, signal } from '@angular/core';
import { JsonPipe, SlicePipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { HttpLogService } from './core/http-log';
import { SettingsService } from './core/settings.service';
import { OidcService } from './core/oidc.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, JsonPipe, SlicePipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  auth = inject(AuthService);
  log = inject(HttpLogService);
  cfg = inject(SettingsService);
  private oidc = inject(OidcService);
  private router = inject(Router);

  showLog = signal(false);
  openEntry = signal<number | null>(null);

  logout() {
    // Keycloak tokens: redirect to Keycloak's end-session endpoint (ends the SSO session too).
    // Manual token: just clear it locally.
    if (!this.oidc.logout()) this.router.navigateByUrl('/');
  }

  toggle(id: number) {
    this.openEntry.update((v) => (v === id ? null : id));
  }
}
