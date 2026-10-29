// View for DonationController.
// Lists donations, records a new money or in-kind donation, changes status, deletes, shows a summary.
import { useCallback, useEffect, useState } from 'react'

// ---- Config ----
const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
const STATUSES = ['PENDING', 'RECEIVED', 'CANCELLED']

// ---- API client (talks to DonationController) ----
async function request(path, options) {
  const res = await fetch(`${API}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    let text = await res.text()
    try {
      text = JSON.parse(text).message || text // Spring error body
    } catch {
      /* plain text */
    }
    throw new Error(text || `Request failed (${res.status})`)
  }
  return res.status === 204 ? null : res.json()
}

const api = {
  list: () => request('/donations'),
  get: (id) => request(`/donations/${id}`),
  pay: (id, data) => request(`/donations/${id}/pay`, { method: 'POST', body: JSON.stringify(data) }),
  create: (data) => request('/donations', { method: 'POST', body: JSON.stringify(data) }),
  setStatus: (id, status) =>
    request(`/donations/${id}`, { method: 'PUT', body: JSON.stringify({ status }) }),
  remove: (id) => request(`/donations/${id}`, { method: 'DELETE' }),
}

// ---- Helpers ----
const isInKind = (d) => String(d.type || '').toLowerCase().includes('kind')

const donorLabel = (d) =>
  typeof d.donor === 'object' && d.donor !== null
    ? d.donor.name || d.donor.id || ''
    : d.donor || d.donorId || ''

const detailsOf = (d) =>
  isInKind(d)
    ? `${d.quantity} × ${d.itemName} (est. ${d.estimatedValue})`
    : `${d.amount} ${d.currency ?? ''}${d.paymentMethod ? ` (${d.paymentMethod})` : ''}`

const dateOf = (d) => (d.date ? String(d.date).slice(0, 10) : '')

// Payment method choices (Strategy pattern on the server). '' means pay later.
const EMPTY_PAYMENT = { paymentMethod: '', receivedBy: '', checkNo: '', bankName: '', cardHolder: '' }

// Returns { data } with the fields the chosen method needs, or { error }.
function paymentPayload(p) {
  switch (p.paymentMethod) {
    case 'cash':
      return p.receivedBy.trim()
        ? { data: { paymentMethod: 'cash', receivedBy: p.receivedBy.trim() } }
        : { error: 'Enter who received the cash.' }
    case 'check':
      if (!/^\d+$/.test(p.checkNo.trim())) return { error: 'Check number must be digits only.' }
      if (!p.bankName.trim()) return { error: 'Enter the bank name.' }
      return { data: { paymentMethod: 'check', checkNo: p.checkNo.trim(), bankName: p.bankName.trim() } }
    case 'online':
      return p.cardHolder.trim()
        ? { data: { paymentMethod: 'online', cardHolder: p.cardHolder.trim() } }
        : { error: 'Enter the card holder name.' }
    default:
      return { data: {} } // pay later
  }
}

function PaymentFields({ value, onChange, allowLater }) {
  const set = (field) => (e) => onChange({ ...value, [field]: e.target.value })
  return (
    <>
      <label style={s.label}>
        Payment method
        <select style={s.input} value={value.paymentMethod} onChange={set('paymentMethod')}>
          {allowLater && <option value="">Pay later</option>}
          <option value="cash">Cash</option>
          <option value="check">Check</option>
          <option value="online">Online (card)</option>
        </select>
      </label>
      {value.paymentMethod === 'cash' && (
        <label style={s.label}>
          Received by
          <input style={s.input} value={value.receivedBy} onChange={set('receivedBy')} />
        </label>
      )}
      {value.paymentMethod === 'check' && (
        <>
          <label style={s.label}>
            Check number
            <input style={s.input} value={value.checkNo} onChange={set('checkNo')} />
          </label>
          <label style={s.label}>
            Bank name
            <input style={s.input} value={value.bankName} onChange={set('bankName')} />
          </label>
        </>
      )}
      {value.paymentMethod === 'online' && (
        <label style={s.label}>
          Card holder name
          <input style={s.input} value={value.cardHolder} onChange={set('cardHolder')} />
        </label>
      )}
    </>
  )
}

const EMPTY_FORM = {
  type: 'money',
  donorId: '',
  amount: '',
  currency: 'EGP',
  itemName: '',
  quantity: '1',
  estimatedValue: '',
  ...EMPTY_PAYMENT,
}

// ---- View ----
export default function DonationPage() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(null) // null = hidden
  const [selected, setSelected] = useState(null) // donation shown in the summary panel
  const [payForm, setPayForm] = useState(null) // { id, ...payment fields } while taking payment
  const [message, setMessage] = useState(null)
  const [loading, setLoading] = useState(false)

  const showMessage = (text) => setMessage({ kind: 'ok', text })
  const showError = (text) => setMessage({ kind: 'error', text })

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await api.list())
    } catch (e) {
      setItems([])
      showError(`Could not load donations: ${e.message}`)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const save = async (e) => {
    e.preventDefault()
    if (!form.donorId.trim()) return showError('Enter the donor id.')

    let data
    if (form.type === 'money') {
      if (!(Number(form.amount) > 0)) return showError('Enter an amount greater than 0.')
      if (!form.currency.trim()) return showError('Enter a currency.')
      const pay = paymentPayload(form)
      if (pay.error) return showError(pay.error)
      data = {
        type: 'money',
        donorId: form.donorId.trim(),
        amount: Number(form.amount),
        currency: form.currency.trim(),
        ...pay.data,
      }
    } else {
      if (!form.itemName.trim()) return showError('Enter the item name.')
      if (!(Number(form.quantity) >= 1)) return showError('Quantity must be at least 1.')
      if (form.estimatedValue === '' || Number(form.estimatedValue) < 0)
        return showError('Enter the estimated value (0 or more).')
      data = {
        type: 'in-kind',
        donorId: form.donorId.trim(),
        itemName: form.itemName.trim(),
        quantity: Number(form.quantity),
        estimatedValue: Number(form.estimatedValue),
      }
    }

    try {
      await api.create(data)
      showMessage('Donation recorded.')
      setForm(null)
      refresh()
    } catch (err) {
      showError(`Could not record donation: ${err.message}`)
    }
  }

  const payNow = async (e) => {
    e.preventDefault()
    const pay = paymentPayload(payForm)
    if (pay.error) return showError(pay.error)
    try {
      await api.pay(payForm.id, pay.data)
      showMessage('Payment received.')
      setPayForm(null)
      refresh()
    } catch (err) {
      showError(`Could not take payment: ${err.message}`)
    }
  }

  const changeStatus = async (d, status) => {
    try {
      await api.setStatus(d.id, status)
      showMessage(`Status changed to ${status}.`)
      refresh()
    } catch (err) {
      showError(`Could not change status: ${err.message}`)
    }
  }

  const remove = async (d) => {
    if (!window.confirm('Delete this donation? This cannot be undone.')) return
    try {
      await api.remove(d.id)
      showMessage('Donation deleted.')
      if (selected?.id === d.id) setSelected(null)
      refresh()
    } catch (err) {
      showError(`Could not delete: ${err.message}`)
    }
  }

  const showDonationSummary = async (d) => {
    try {
      setSelected(await api.get(d.id))
    } catch (err) {
      showError(`Could not load donation: ${err.message}`)
    }
  }

  // Overview: count and money totals per currency
  const totals = {}
  items.filter((d) => !isInKind(d)).forEach((d) => {
    totals[d.currency] = (totals[d.currency] || 0) + Number(d.amount || 0)
  })
  const totalsText = Object.entries(totals).map(([c, v]) => `${v} ${c}`).join(', ')

  return (
    <div style={s.page}>
      <h2 style={s.title}>Donations</h2>

      {message && (
        <p role="status" style={{ ...s.message, ...(message.kind === 'error' ? s.error : s.ok) }}>
          {message.text}
        </p>
      )}

      <p style={s.summary}>
        {items.length} donation{items.length === 1 ? '' : 's'}
        {totalsText && ` · money received so far: ${totalsText}`}
      </p>

      <div style={s.toolbar}>
        <button style={s.primary} onClick={() => setForm({ ...EMPTY_FORM })}>
          Record donation
        </button>
        <button style={s.button} onClick={refresh} disabled={loading}>
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </div>

      {form && (
        <form onSubmit={save} style={s.form}>
          <label style={s.label}>
            Donation type
            <select style={s.input} value={form.type} onChange={set('type')}>
              <option value="money">Money</option>
              <option value="in-kind">In-kind (items)</option>
            </select>
          </label>
          <label style={s.label}>
            Donor id
            <input style={s.input} value={form.donorId} onChange={set('donorId')} autoFocus />
          </label>

          {form.type === 'money' ? (
            <>
              <label style={s.label}>
                Amount
                <input style={s.input} type="number" min="0" step="0.01" value={form.amount} onChange={set('amount')} />
              </label>
              <label style={s.label}>
                Currency
                <input style={s.input} value={form.currency} onChange={set('currency')} />
              </label>
              <PaymentFields value={form} onChange={setForm} allowLater />
            </>
          ) : (
            <>
              <label style={s.label}>
                Item name
                <input style={s.input} value={form.itemName} onChange={set('itemName')} />
              </label>
              <label style={s.label}>
                Quantity
                <input style={s.input} type="number" min="1" step="1" value={form.quantity} onChange={set('quantity')} />
              </label>
              <label style={s.label}>
                Estimated value
                <input style={s.input} type="number" min="0" step="0.01" value={form.estimatedValue} onChange={set('estimatedValue')} />
              </label>
            </>
          )}

          <div style={s.toolbar}>
            <button type="submit" style={s.primary}>Record donation</button>
            <button type="button" style={s.button} onClick={() => setForm(null)}>Cancel</button>
          </div>
        </form>
      )}

      {payForm && (
        <form onSubmit={payNow} style={s.form}>
          <strong>Take payment</strong>
          <PaymentFields value={payForm} onChange={setPayForm} />
          <div style={s.toolbar}>
            <button type="submit" style={s.primary}>Take payment</button>
            <button type="button" style={s.button} onClick={() => setPayForm(null)}>Cancel</button>
          </div>
        </form>
      )}

      {selected && (
        <div style={s.panel}>
          <strong>Donation summary</strong>
          <dl style={s.dl}>
            <dt>Type</dt><dd>{isInKind(selected) ? 'In-kind' : 'Money'}</dd>
            <dt>Donor</dt><dd>{donorLabel(selected)}</dd>
            <dt>Details</dt><dd>{detailsOf(selected)}</dd>
            {selected.transactionId && (<><dt>Transaction</dt><dd>{selected.transactionId}</dd></>)}
            <dt>Status</dt><dd>{selected.status}</dd>
            <dt>Date</dt><dd>{dateOf(selected)}</dd>
            <dt>Id</dt><dd>{selected.id}</dd>
          </dl>
          <button style={s.button} onClick={() => setSelected(null)}>Close</button>
        </div>
      )}

      <table style={s.table}>
        <thead>
          <tr>
            <th style={s.th}>Date</th>
            <th style={s.th}>Type</th>
            <th style={s.th}>Donor</th>
            <th style={s.th}>Details</th>
            <th style={s.th}>Status</th>
            <th style={s.th}></th>
          </tr>
        </thead>
        <tbody>
          {items.map((d) => (
            <tr key={d.id}>
              <td style={s.td}>{dateOf(d)}</td>
              <td style={s.td}>{isInKind(d) ? 'In-kind' : 'Money'}</td>
              <td style={s.td}>{donorLabel(d)}</td>
              <td style={s.td}>{detailsOf(d)}</td>
              <td style={s.td}>
                <select
                  value={d.status}
                  onChange={(e) => changeStatus(d, e.target.value)}
                  aria-label="Donation status"
                >
                  {[...new Set([d.status, ...STATUSES])].filter(Boolean).map((st) => (
                    <option key={st}>{st}</option>
                  ))}
                </select>
              </td>
              <td style={{ ...s.td, textAlign: 'right', whiteSpace: 'nowrap' }}>
                {!isInKind(d) && d.status === 'PENDING' && (
                  <>
                    <button
                      style={s.button}
                      onClick={() => setPayForm({ id: d.id, ...EMPTY_PAYMENT, paymentMethod: 'cash' })}
                    >
                      Take payment
                    </button>{' '}
                  </>
                )}
                <button style={s.button} onClick={() => showDonationSummary(d)}>View</button>{' '}
                <button style={s.danger} onClick={() => remove(d)}>Delete</button>
              </td>
            </tr>
          ))}
          {!loading && items.length === 0 && (
            <tr>
              <td style={s.td} colSpan={6}>
                No donations yet. Use “Record donation” to add the first one.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

// Minimal inline styles (same look as LookupPage) so it works without a CSS framework.
const s = {
  page: { maxWidth: 900, margin: '0 auto', padding: 16, fontFamily: 'system-ui, sans-serif' },
  title: { margin: '0 0 12px' },
  summary: { margin: '0 0 8px', color: '#444' },
  toolbar: { display: 'flex', gap: 8, margin: '12px 0' },
  button: { padding: '6px 12px', cursor: 'pointer' },
  primary: { padding: '6px 12px', cursor: 'pointer', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4 },
  danger: { padding: '6px 12px', cursor: 'pointer', color: '#b91c1c' },
  form: { display: 'grid', gap: 10, padding: 12, border: '1px solid #ddd', borderRadius: 6, marginBottom: 12 },
  label: { display: 'grid', gap: 4, fontSize: 14 },
  input: { padding: 6, fontSize: 15 },
  panel: { padding: 12, border: '1px solid #bfdbfe', background: '#eff6ff', borderRadius: 6, marginBottom: 12 },
  dl: { display: 'grid', gridTemplateColumns: 'max-content 1fr', gap: '4px 16px', margin: '8px 0' },
  message: { padding: '8px 12px', borderRadius: 4, margin: '0 0 8px' },
  ok: { background: '#dcfce7', color: '#166534' },
  error: { background: '#fee2e2', color: '#991b1b' },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: { textAlign: 'left', padding: 8, borderBottom: '2px solid #ccc' },
  td: { padding: 8, borderBottom: '1px solid #eee' },
}
