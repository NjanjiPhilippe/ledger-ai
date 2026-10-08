import { computed, Injectable, signal } from '@angular/core';

/** Counts the calls to our API that are in flight, so one global bar can show "something is loading". */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly pending = signal(0);

  readonly busy = computed(() => this.pending() > 0);

  start(): void {
    this.pending.update((n) => n + 1);
  }

  stop(): void {
    this.pending.update((n) => Math.max(0, n - 1));
  }
}
