import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LoadingBar } from './core/loading/loading-bar';
import { ToastContainer } from './core/toast/toast-container';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoadingBar, ToastContainer],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-loading-bar />
    <router-outlet />
    <app-toast-container />
  `,
})
export class App {}
