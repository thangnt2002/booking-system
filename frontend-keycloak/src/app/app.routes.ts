import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/home').then((m) => m.Home) },
  { path: 'events/:id', loadComponent: () => import('./pages/event-detail').then((m) => m.EventDetail) },
  { path: 'login', loadComponent: () => import('./pages/auth-pages').then((m) => m.Login) },
  { path: 'auth/callback', loadComponent: () => import('./pages/auth-pages').then((m) => m.AuthCallback) },
  { path: 'register', loadComponent: () => import('./pages/auth-pages').then((m) => m.Register) },
  { path: 'pay/:id', loadComponent: () => import('./pages/payment').then((m) => m.Payment) },
  { path: 'orders', loadComponent: () => import('./pages/orders').then((m) => m.Orders) },
  { path: 'account', loadComponent: () => import('./pages/account').then((m) => m.Account) },
  { path: 'admin', loadComponent: () => import('./pages/admin').then((m) => m.Admin) },
  { path: 'settings', loadComponent: () => import('./pages/settings').then((m) => m.Settings) },
  { path: '**', redirectTo: '' },
];
