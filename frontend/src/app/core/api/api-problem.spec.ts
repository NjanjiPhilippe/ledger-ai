import { HttpErrorResponse } from '@angular/common/http';
import { toApiProblem } from './api-problem';

const failure = (status: number, body: unknown = null) =>
  new HttpErrorResponse({ status, error: body, url: 'http://api.test/x' });

describe('toApiProblem', () => {
  it.each([
    [0, 'network'],
    [400, 'validation'],
    [401, 'unauthorized'],
    [403, 'forbidden'],
    [404, 'notFound'],
    [409, 'conflict'],
    [502, 'gateway'],
    [503, 'gateway'],
    [500, 'server'],
    [418, 'unknown'],
  ])('maps HTTP %i to "%s"', (status, kind) => {
    expect(toApiProblem(failure(status)).kind).toBe(kind);
  });

  it('reads the message of the backend ApiError', () => {
    const problem = toApiProblem(failure(409, { status: 409, message: 'Entry already posted' }));

    expect(problem.message).toBe('Entry already posted');
    expect(problem.fieldErrors).toEqual([]);
  });

  it('reads the field errors of a validation error', () => {
    const problem = toApiProblem(
      failure(400, {
        message: 'Validation failed',
        errors: [
          { field: 'lines[0].amount', message: 'Amount must be greater than zero' },
          { field: 'description', message: 'Description is required' },
        ],
      }),
    );

    expect(problem.fieldErrors).toEqual([
      { field: 'lines[0].amount', message: 'Amount must be greater than zero' },
      { field: 'description', message: 'Description is required' },
    ]);
  });

  it.each([null, 'plain text', 42, [], { errors: 'oops' }, { errors: [null, { field: 1 }] }])(
    'survives a body it does not understand: %j',
    (body) => {
      const problem = toApiProblem(failure(400, body));

      expect(problem.message).toBeNull();
      expect(problem.fieldErrors).toEqual([]);
    },
  );
});
