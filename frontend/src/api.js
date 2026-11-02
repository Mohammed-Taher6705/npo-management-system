const API_BASE = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')

export async function request(url, options = {}) {
  const res = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  if (!res.ok) {
    const raw = await res.text()
    let message = raw
    try { const data = JSON.parse(raw); message = data.message || data.detail || data.error || raw } catch { /* plain text response */ }
    throw new Error(message || `Request failed (${res.status})`)
  }
  return res.status === 204 ? null : res.json()
}

export const api = {
  get: (url, options) => request(url, options),
  post: (url, body) => request(url, { method: 'POST', body: JSON.stringify(body) }),
  put: (url, body) => request(url, { method: 'PUT', body: JSON.stringify(body) }),
  del: (url) => request(url, { method: 'DELETE' }),
}
