// View for LookupController. Owner: Person B
// Manages the three lookup tables (CRUD): Skills, Item categories, Need categories.
import { useCallback, useEffect, useState } from 'react'

// ---- Config: change only this block if your backend differs ----
const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

// One entry per lookup table. `field` is the extra attribute from the class diagram.
const TYPES = {
  skills: {
    label: 'Skills',
    field: 'level',
    fieldLabel: 'Level',
    options: ['Beginner', 'Intermediate', 'Advanced'],
  },
  'item-categories': { label: 'Item categories', field: 'unit', fieldLabel: 'Unit' },
  'need-categories': { label: 'Need categories', field: 'description', fieldLabel: 'Description' },
}

// ---- API client (talks to LookupController) ----
async function request(path, options) {
  const res = await fetch(`${API}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    throw new Error((await res.text()) || `Request failed (${res.status})`)
  }
  return res.status === 204 ? null : res.json()
}

const api = {
  list: (type) => request(`/lookups/${type}`),
  create: (type, data) => request(`/lookups/${type}`, { method: 'POST', body: JSON.stringify(data) }),
  update: (type, id, data) =>
    request(`/lookups/${type}/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (type, id) => request(`/lookups/${type}/${id}`, { method: 'DELETE' }),
}

const getId = (item) => item.id ?? item._id // MongoDB ids are strings

// ---- View ----
export default function LookupPage() {
  const [type, setType] = useState('skills')
  const [items, setItems] = useState([])
  const [form, setForm] = useState(null) // null = hidden, otherwise { id?, name, extra }
  const [message, setMessage] = useState(null) // { kind: 'ok' | 'error', text }
  const [loading, setLoading] = useState(false)

  const config = TYPES[type]

  const showMessage = (text) => setMessage({ kind: 'ok', text })
  const showError = (text) => setMessage({ kind: 'error', text })

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await api.list(type))
    } catch (e) {
      setItems([])
      showError(`Could not load ${TYPES[type].label.toLowerCase()}: ${e.message}`)
    } finally {
      setLoading(false)
    }
  }, [type])

  useEffect(() => {
    setForm(null)
    setMessage(null)
    refresh()
  }, [refresh])

  const openForm = (item) =>
    setForm(
      item
        ? { id: getId(item), name: item.name ?? '', extra: item[config.field] ?? '' }
        : { name: '', extra: config.options ? config.options[0] : '' }
    )

  const save = async (e) => {
    e.preventDefault()
    const name = form.name.trim()
    if (!name) {
      showError('Enter a name.')
      return
    }
    const data = { name, [config.field]: form.extra.trim() }
    try {
      if (form.id) {
        await api.update(type, form.id, data)
        showMessage(`Saved changes to "${name}".`)
      } else {
        await api.create(type, data)
        showMessage(`Added "${name}".`)
      }
      setForm(null)
      refresh()
    } catch (err) {
      showError(`Could not save: ${err.message}`)
    }
  }

  const remove = async (item) => {
    if (!window.confirm(`Delete "${item.name}"? This cannot be undone.`)) return
    try {
      await api.remove(type, getId(item))
      showMessage(`Deleted "${item.name}".`)
      refresh()
    } catch (err) {
      showError(`Could not delete: ${err.message}`)
    }
  }

  return (
    <div style={s.page}>
      <h2 style={s.title}>Lookup tables</h2>

      <div style={s.tabs} role="tablist">
        {Object.entries(TYPES).map(([key, t]) => (
          <button
            key={key}
            role="tab"
            aria-selected={key === type}
            onClick={() => setType(key)}
            style={{ ...s.tab, ...(key === type ? s.tabActive : null) }}
          >
            {t.label}
          </button>
        ))}
      </div>

      {message && (
        <p role="status" style={{ ...s.message, ...(message.kind === 'error' ? s.error : s.ok) }}>
          {message.text}
        </p>
      )}

      <div style={s.toolbar}>
        <button style={s.primary} onClick={() => openForm(null)}>
          Add {config.label.toLowerCase().replace(/s$/, '')}
        </button>
        <button style={s.button} onClick={refresh} disabled={loading}>
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </div>

      {form && (
        <form onSubmit={save} style={s.form}>
          <label style={s.label}>
            Name
            <input
              style={s.input}
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              autoFocus
            />
          </label>
          <label style={s.label}>
            {config.fieldLabel}
            {config.options ? (
              <select
                style={s.input}
                value={form.extra}
                onChange={(e) => setForm({ ...form, extra: e.target.value })}
              >
                {config.options.map((o) => (
                  <option key={o}>{o}</option>
                ))}
              </select>
            ) : (
              <input
                style={s.input}
                value={form.extra}
                onChange={(e) => setForm({ ...form, extra: e.target.value })}
              />
            )}
          </label>
          <div style={s.toolbar}>
            <button type="submit" style={s.primary}>
              {form.id ? 'Save changes' : 'Add'}
            </button>
            <button type="button" style={s.button} onClick={() => setForm(null)}>
              Cancel
            </button>
          </div>
        </form>
      )}

      <table style={s.table}>
        <thead>
          <tr>
            <th style={s.th}>Name</th>
            <th style={s.th}>{config.fieldLabel}</th>
            <th style={s.th}></th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={getId(item)}>
              <td style={s.td}>{item.name}</td>
              <td style={s.td}>{item[config.field]}</td>
              <td style={{ ...s.td, textAlign: 'right' }}>
                <button style={s.button} onClick={() => openForm(item)}>
                  Edit
                </button>{' '}
                <button style={s.danger} onClick={() => remove(item)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
          {!loading && items.length === 0 && (
            <tr>
              <td style={s.td} colSpan={3}>
                No {config.label.toLowerCase()} yet. Use the add button to create the first one.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

// Minimal inline styles so the page works without any CSS framework.
const s = {
  page: { maxWidth: 760, margin: '0 auto', padding: 16, fontFamily: 'system-ui, sans-serif' },
  title: { margin: '0 0 12px' },
  tabs: { display: 'flex', gap: 4, borderBottom: '1px solid #ccc', marginBottom: 12 },
  tab: { padding: '8px 14px', border: 'none', background: 'none', cursor: 'pointer', fontSize: 15 },
  tabActive: { borderBottom: '3px solid #2563eb', fontWeight: 600 },
  toolbar: { display: 'flex', gap: 8, margin: '12px 0' },
  button: { padding: '6px 12px', cursor: 'pointer' },
  primary: { padding: '6px 12px', cursor: 'pointer', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4 },
  danger: { padding: '6px 12px', cursor: 'pointer', color: '#b91c1c' },
  form: { display: 'grid', gap: 10, padding: 12, border: '1px solid #ddd', borderRadius: 6, marginBottom: 12 },
  label: { display: 'grid', gap: 4, fontSize: 14 },
  input: { padding: 6, fontSize: 15 },
  message: { padding: '8px 12px', borderRadius: 4, margin: '0 0 8px' },
  ok: { background: '#dcfce7', color: '#166534' },
  error: { background: '#fee2e2', color: '#991b1b' },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: { textAlign: 'left', padding: 8, borderBottom: '2px solid #ccc' },
  td: { padding: 8, borderBottom: '1px solid #eee' },
}
