import { HttpErrorResponse } from '@angular/common/http';

export type ProblemKind =
  | 'network'
  | 'unauthorized'
  | 'forbidden'
  | 'notFound'
  | 'validation'
  | 'conflict'
  | 'gateway'
  | 'server'
  | 'unknown';

export interface FieldProblem {
  readonly field: string;
  readonly message: string;
}

/**
 * What a failed API call means for the UI. The backend answers with { status, message, timestamp } and, for
 * validation errors, an `errors` list of { field, message } (the error shapes are not part of the OpenAPI document).
 */
export interface ApiProblem {
  readonly kind: ProblemKind;
  readonly status: number;
  /** The backend's message (in English): useful detail, but only for kinds where it is meant for users. */
  readonly message: string | null;
  readonly fieldErrors: readonly FieldProblem[];
}

export function toApiProblem(error: HttpErrorResponse): ApiProblem {
  const body = isRecord(error.error) ? error.error : {};
  const message = typeof body['message'] === 'string' ? body['message'] : null;
  return {
    kind: kindOf(error.status),
    status: error.status,
    message,
    fieldErrors: parseFieldErrors(body['errors']),
  };
}

function kindOf(status: number): ProblemKind {
  switch (status) {
    case 0:
      return 'network';
    case 400:
      return 'validation';
    case 401:
      return 'unauthorized';
    case 403:
      return 'forbidden';
    case 404:
      return 'notFound';
    case 409:
      return 'conflict';
    case 502:
    case 503:
    case 504:
      return 'gateway';
    default:
      return status >= 500 ? 'server' : 'unknown';
  }
}

function parseFieldErrors(raw: unknown): FieldProblem[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.filter(isRecord).flatMap((entry) => {
    const field = entry['field'];
    const message = entry['message'];
    return typeof field === 'string' && typeof message === 'string' ? [{ field, message }] : [];
  });
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
