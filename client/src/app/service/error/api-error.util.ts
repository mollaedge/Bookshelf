import { HttpErrorResponse } from '@angular/common/http';

type ErrorPayload = {
  error?: unknown;
  message?: unknown;
  details?: unknown;
};

function asText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function firstFromArray(value: unknown): string {
  if (!Array.isArray(value) || value.length === 0) {
    return '';
  }

  for (const item of value) {
    const text = asText(item);
    if (text) {
      return text;
    }
  }

  return '';
}

/** Extracts a display-safe API error message from HttpErrorResponse. */
export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return 'Service is temporarily unavailable. Please try again later.';
  }

  if (typeof error.error === 'string') {
    const text = error.error.trim();
    if (text) {
      return text;
    }
  }

  if (error.error && typeof error.error === 'object') {
    const payload = error.error as ErrorPayload;
    const nestedError = asText(payload.error);
    const nestedMessage = asText(payload.message);
    const detailsMessage = firstFromArray(payload.details);

    if (nestedError) return nestedError;
    if (nestedMessage) return nestedMessage;
    if (detailsMessage) return detailsMessage;
  }

  return fallback;
}
