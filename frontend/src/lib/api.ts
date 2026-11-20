const TOKEN_KEY = 'hostelos.token'

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export const tokenStore = {
  get: () => { try { return localStorage.getItem(TOKEN_KEY) } catch { return null } },
  set: (t: string) => { try { localStorage.setItem(TOKEN_KEY, t) } catch { /* ignore */ } },
  clear: () => { try { localStorage.removeItem(TOKEN_KEY) } catch { /* ignore */ } },
}

/** Thin fetch wrapper: adds the JWT, parses JSON, and turns {"message": "..."} errors into ApiError. */
export async function api<T>(path: string, opts: { method?: string; body?: unknown } = {}): Promise<T> {
  const token = tokenStore.get()
  const res = await fetch(path, {
    method: opts.method ?? (opts.body !== undefined ? 'POST' : 'GET'),
    headers: {
      ...(opts.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  })
  const text = await res.text()
  const data = text ? safeParse(text) : undefined
  if (!res.ok) {
    if (res.status === 401 && token) {
      tokenStore.clear()
      window.dispatchEvent(new Event('hostelos:unauthorized'))
    }
    throw new ApiError(res.status, (data as { message?: string } | undefined)?.message ?? `Request failed (${res.status})`)
  }
  return data as T
}

function safeParse(text: string) {
  try { return JSON.parse(text) } catch { return undefined }
}
