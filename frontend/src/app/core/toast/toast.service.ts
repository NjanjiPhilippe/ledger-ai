import { Injectable, signal } from '@angular/core';

export type ToastLevel = 'success' | 'info' | 'warning' | 'error';

export interface Toast {
  readonly id: number;
  readonly level: ToastLevel;
  readonly title: string;
  readonly detail?: string;
}

const DURATION_MS: Record<ToastLevel, number> = {
  success: 4000,
  info: 4000,
  warning: 6000,
  error: 8000,
};
const MAX_VISIBLE = 5;

/**
 * Transient messages. Messages arrive already translated: the service knows nothing about languages.
 * A message identical to one on screen only restarts that one's timer, so three failing requests of the same
 * page do not stack three identical toasts.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly current = signal<readonly Toast[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private nextId = 1;

  readonly toasts = this.current.asReadonly();

  success(title: string, detail?: string): void {
    this.show('success', title, detail);
  }

  info(title: string, detail?: string): void {
    this.show('info', title, detail);
  }

  warning(title: string, detail?: string): void {
    this.show('warning', title, detail);
  }

  error(title: string, detail?: string): void {
    this.show('error', title, detail);
  }

  show(level: ToastLevel, title: string, detail?: string): void {
    const duplicate = this.current().find(
      (t) => t.level === level && t.title === title && t.detail === detail,
    );
    if (duplicate) {
      this.schedule(duplicate);
      return;
    }
    const toast: Toast = { id: this.nextId++, level, title, detail };
    const visible = [...this.current(), toast];
    const dropped = visible.slice(0, Math.max(0, visible.length - MAX_VISIBLE));
    dropped.forEach((t) => this.clearTimer(t.id));
    this.current.set(visible.slice(dropped.length));
    this.schedule(toast);
  }

  dismiss(id: number): void {
    this.clearTimer(id);
    this.current.update((toasts) => toasts.filter((t) => t.id !== id));
  }

  private schedule(toast: Toast): void {
    this.clearTimer(toast.id);
    this.timers.set(
      toast.id,
      setTimeout(() => this.dismiss(toast.id), DURATION_MS[toast.level]),
    );
  }

  private clearTimer(id: number): void {
    const timer = this.timers.get(id);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
  }
}
