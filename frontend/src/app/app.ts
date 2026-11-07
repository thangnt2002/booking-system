import { Component, inject, signal } from '@angular/core';
import { JsonPipe, SlicePipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { HttpLogService } from './core/http-log';
import { SettingsService } from './core/settings.service';
import { ApiService } from './core/api.service';

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
  private api = inject(ApiService);
  private router = inject(Router);

  showLog = signal(false);
  openEntry = signal<number | null>(null);

  logout() {
    const t = this.auth.tokens();
    if (t?.source === 'identity') {
      // best effort: invalidate on identity-service, then clear locally either way
      this.api.logout(t.accessToken, t.refreshToken).subscribe({ complete: () => {}, error: () => {} });
    }
    this.auth.set(null);
    this.router.navigateByUrl('/');
  }

  toggle(id: number) {
    this.openEntry.update((v) => (v === id ? null : id));
  }
}
