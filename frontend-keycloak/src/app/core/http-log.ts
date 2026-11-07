import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { AuthService } from './auth.service';

export interface LogEntry {
  id: number;
  method: string;
  url: string;
  status: number;
  ms: number;
  request: unknown;
  response: unknown;
  at: Date;
}

@Injectable({ providedIn: 'root' })
export class HttpLogService {
  readonly entries = signal<LogEntry[]>([]);
  private seq = 0;

  add(e: Omit<LogEntry, 'id'>) {
    this.entries.update((list) => [{ ...e, id: ++this.seq }, ...list].slice(0, 50));
  }

  clear() {
    this.entries.set([]);
  }
}

/** Attaches the bearer token (except for public login/register/keycloak calls) and logs every call. */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const log = inject(HttpLogService);
  const token = auth.tokens()?.accessToken;
  const skipAuth = req.url.startsWith('/kc/') || req.headers.has('X-Skip-Auth');

  let r = req.headers.has('X-Skip-Auth') ? req.clone({ headers: req.headers.delete('X-Skip-Auth') }) : req;
  if (token && !skipAuth) {
    r = r.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  const started = performance.now();
  const record = (status: number, response: unknown) =>
    log.add({
      method: r.method,
      url: r.urlWithParams,
      status,
      ms: Math.round(performance.now() - started),
      request: r.body,
      response,
      at: new Date(),
    });

  return next(r).pipe(
    tap({
      next: (ev) => {
        if (ev instanceof HttpResponse) record(ev.status, ev.body);
      },
      error: (err: HttpErrorResponse) => record(err.status, err.error ?? err.message),
    }),
  );
};
