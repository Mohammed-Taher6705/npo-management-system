import { useEffect, useRef, useState } from 'react'
import { api } from '../api'

const TYPES = {
  skills: { label: 'Skills', singular: 'skill', field: 'level', extra: 'Level', options: ['Beginner', 'Intermediate', 'Advanced'] },
  'item-categories': { label: 'Item categories', singular: 'item category', field: 'unit', extra: 'Unit' },
  'need-categories': { label: 'Need categories', singular: 'need category', field: 'description', extra: 'Description' },
}

// MVC View: validation and presentation live here; persistence goes through LookupController.
export default function LookupPage() {
  const [type, setType] = useState('skills')
  const [items, setItems] = useState([])
  const [form, setForm] = useState(null)
  const [message, setMessage] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [query, setQuery] = useState('')
  const [revision, setRevision] = useState(0)
  const locked = useRef(false)
  const config = TYPES[type]

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setItems([])
    api.get(`/lookups/${type}`, { signal: controller.signal })
      .then(data => { if (!controller.signal.aborted) setItems(data) })
      .catch(error => { if (!controller.signal.aborted) setMessage({ error: true, text: `Could not load records: ${error.message}` }) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [type, revision])

  function selectType(next) {
    setType(next)
    setForm(null)
    setMessage(null)
    setQuery('')
  }

  async function mutate(action, success) {
    if (locked.current) return
    locked.current = true
    setBusy(true)
    setMessage(null)
    try {
      await action()
      setForm(null)
      setMessage({ text: success })
      setRevision(value => value + 1)
    } catch (error) {
      setMessage({ error: true, text: error.message })
    } finally {
      locked.current = false
      setBusy(false)
    }
  }

  function save(event) {
    event.preventDefault()
    const name = form.name.trim()
    const extra = form.extra.trim()
    if (!name || !extra) {
      setMessage({ error: true, text: `Enter a name and ${config.extra.toLowerCase()}.` })
      return
    }
    const data = { name, [config.field]: extra }
    const url = `/lookups/${type}`
    mutate(() => form.id ? api.put(`${url}/${form.id}`, data) : api.post(url, data), 'Record saved successfully.')
  }

  const visible = items.filter(item => `${item.name || ''} ${item[config.field] || ''}`.toLowerCase().includes(query.toLowerCase()))
  return (
    <section className="lookup-page" aria-labelledby="lookup-title">
      <p className="eyebrow">NPO Management</p>
      <h1 id="lookup-title">Lookup tables</h1>
      <p className="muted">Manage the skills and categories used across your organization.</p>
      <div className="lookup-types" role="group" aria-label="Lookup type">
        {Object.entries(TYPES).map(([key, value]) => <button key={key} aria-pressed={type === key} disabled={busy} onClick={() => selectType(key)}>{value.label}</button>)}
      </div>
      {message && <p className={`notice ${message.error ? 'error' : 'success'}`} role={message.error ? 'alert' : 'status'}>{message.text}</p>}
      <div className="lookup-toolbar">
        <label>Search {config.label.toLowerCase()}<input type="search" value={query} onChange={e => setQuery(e.target.value)} /></label>
        <button className="primary" disabled={busy || loading} onClick={() => { setMessage(null); setForm({ name: '', extra: config.options?.[0] || '' }) }}>Add {config.singular}</button>
        <button disabled={busy || loading} onClick={() => { setMessage(null); setRevision(value => value + 1) }}>Refresh</button>
      </div>
      {form && <form className="lookup-form" onSubmit={save}>
        <h2>{form.id ? 'Edit' : 'Add'} {config.singular}</h2>
        <fieldset disabled={busy}>
          <label>Name<input required maxLength={120} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
          <label>{config.extra}{config.options ? <select value={form.extra} onChange={e => setForm({ ...form, extra: e.target.value })}>
            {!config.options.includes(form.extra) && <option value={form.extra}>{form.extra || 'Choose a level'}</option>}
            {config.options.map(value => <option key={value}>{value}</option>)}
          </select> : <input required maxLength={500} value={form.extra} onChange={e => setForm({ ...form, extra: e.target.value })} />}</label>
          <div className="actions"><button className="primary" type="submit">{busy ? 'Saving...' : 'Save'}</button><button type="button" onClick={() => setForm(null)}>Cancel</button></div>
        </fieldset>
      </form>}
      <div className="lookup-table" aria-busy={loading}>
        <table><caption>{config.label}</caption><thead><tr><th scope="col">Name</th><th scope="col">{config.extra}</th><th scope="col">Actions</th></tr></thead>
          <tbody>{loading ? <tr><td colSpan={3}>Loading records...</td></tr> : visible.map(item => <tr key={item.id}>
            <td>{item.name}</td><td>{item[config.field]}</td><td><div className="actions">
              <button disabled={busy} aria-label={`Edit ${item.name}`} onClick={() => { setMessage(null); setForm({ id: item.id, name: item.name || '', extra: item[config.field] || '' }) }}>Edit</button>
              <button className="danger" disabled={busy} aria-label={`Delete ${item.name}`} onClick={() => {
                if (window.confirm(`Delete "${item.name}"? This cannot be undone.`)) mutate(() => api.del(`/lookups/${type}/${item.id}`), 'Record deleted successfully.')
              }}>Delete</button>
            </div></td>
          </tr>)}{!loading && !visible.length && <tr><td colSpan={3}>{query ? 'No matching records.' : 'No records to display. Add a record or refresh to try again.'}</td></tr>}</tbody>
        </table>
      </div>
    </section>
  )
}
