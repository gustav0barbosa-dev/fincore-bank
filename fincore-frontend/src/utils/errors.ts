interface ApiErrorBody {
  message?: unknown;
  errors?: Record<string, unknown>;
}

/**
 * Extrai a mensagem mais útil de um erro do backend.
 * Validações retornam { message, errors: { campo: "detalhe" } };
 * erros de negócio retornam { message }.
 */
export function apiErrorMessage(error: unknown, fallback: string): string {
  const body = (error as { response?: { data?: ApiErrorBody } })?.response?.data;
  if (!body) return fallback;

  if (body.errors && typeof body.errors === 'object') {
    const first = Object.values(body.errors).find(
      value => typeof value === 'string' && value.trim().length > 0
    );
    if (typeof first === 'string') return first;
  }

  return typeof body.message === 'string' && body.message.trim()
    ? body.message
    : fallback;
}
