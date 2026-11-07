import { signal } from '@angular/core';
import { Observable } from 'rxjs';
import { errMsg } from './api.service';

export interface RunResult {
  ok: boolean;
  label: string;
  body: unknown;
}

/** Tiny helper: runs an observable and stores its outcome per key for display. */
export class Runner {
  readonly results = signal<Record<string, RunResult>>({});
  readonly busy = signal<string | null>(null);

  run<T>(key: string, label: string, obs: Observable<T>, onOk?: (v: T) => void) {
    this.busy.set(key);
    obs.subscribe({
      next: (v) => {
        this.busy.set(null);
        this.set(key, { ok: true, label, body: v });
        onOk?.(v);
      },
      error: (e) => {
        this.busy.set(null);
        this.set(key, { ok: false, label: `${label} → ${errMsg(e)}`, body: e?.error ?? null });
      },
    });
  }

  get(key: string): RunResult | undefined {
    return this.results()[key];
  }

  private set(key: string, r: RunResult) {
    this.results.update((m) => ({ ...m, [key]: r }));
  }
}
