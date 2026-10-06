import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
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
        loadComponent: () => import('./features/home/pages/home-page').then((m) => m.HomePage),
      },
      {
        path: 'accounts',
        loadComponent: () =>
          import('./features/accounts/pages/accounts-page').then((m) => m.AccountsPage),
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
