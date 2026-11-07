import { Component, input } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { RunResult } from '../core/runner';

@Component({
  selector: 'app-result',
  imports: [JsonPipe],
  template: `
    @if (r(); as res) {
      <div class="alert" [class.ok]="res.ok" [class.error]="!res.ok">{{ res.ok ? '✓ ' : '✗ ' }}{{ res.label }}</div>
      @if (res.body) { <pre class="json">{{ res.body | json }}</pre> }
    }
  `,
})
export class Result {
  r = input<RunResult | undefined>();
}
