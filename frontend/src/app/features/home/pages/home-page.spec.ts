import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { MeResponse } from '../../../core/api/api-types';
import { translocoTesting } from '../../../../testing/transloco';
import { AuthService } from '../../../core/auth/auth.service';
import { MeApi } from '../data-access/me.api';
import { HomePage } from './home-page';

describe('HomePage', () => {
  async function open(me: Observable<MeResponse>, roles: string[]) {
    TestBed.configureTestingModule({
      imports: [HomePage, translocoTesting()],
      providers: [
        { provide: MeApi, useValue: { get: () => me } },
        {
          provide: AuthService,
          useValue: { username: signal('accountant'), appRoles: signal(roles) },
        },
      ],
    });
    const fixture = TestBed.createComponent(HomePage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const text = (element: HTMLElement, testId: string) =>
    element.querySelector(`[data-testid="${testId}"]`)?.textContent?.replace(/\s+/g, ' ').trim();

  it('greets the user and shows the identifiers returned by /me and the roles', async () => {
    const element = await open(of({ userId: 'user-1', tenantId: 'tenant-1' }), ['accountant']);

    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Welcome, accountant');
    expect(text(element, 'user-id')).toBe('user-1');
    expect(text(element, 'tenant-id')).toBe('tenant-1');
    expect(text(element, 'roles')).toBe('Accountant');
  });

  it('says so when no application role is assigned', async () => {
    const element = await open(of({ userId: 'u', tenantId: 't' }), []);

    expect(text(element, 'roles')).toBe('No role assigned');
  });

  it('stays usable when /me fails: the interceptor already showed the problem', async () => {
    const element = await open(
      throwError(() => new Error('down')),
      ['viewer'],
    );

    expect(element.querySelector('h1')).not.toBeNull();
    expect(text(element, 'user-id')).toBe('');
  });
});
