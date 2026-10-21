import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth/auth.guard';
import { Shell } from './core/layout/shell';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard-page').then((m) => m.DashboardPage),
      },
      {
        path: 'accounts',
        loadComponent: () =>
          import('./features/accounts/pages/accounts-page').then((m) => m.AccountsPage),
      },
      {
        path: 'entries/new',
        canActivate: [roleGuard('accountant')],
        loadComponent: () =>
          import('./features/entries/pages/new-entry-page').then((m) => m.NewEntryPage),
      },
      {
        path: 'forbidden',
        loadComponent: () => import('./core/pages/forbidden-page').then((m) => m.ForbiddenPage),
      },
      {
        path: '**',
        loadComponent: () => import('./core/pages/not-found-page').then((m) => m.NotFoundPage),
      },
    ],
  },
];
