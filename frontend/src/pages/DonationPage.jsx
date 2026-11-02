// View for DonationController.
// Lists donations, records a new money or in-kind donation, changes status, deletes, shows a summary.
import { useCallback, useEffect, useState } from 'react'
import { request } from '../api'

const STATUSES = ['PENDING', 'COMPLETED', 'FAILED', 'RECEIVED', 'CANCELLED']

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

function PaymentFields({ value, onChange }) {
  const set = (field) => (e) => onChange({ ...value, [field]: e.target.value })

  return (
    <>
      <label>
        Payment method
        <select value={value.paymentMethod} onChange={set('paymentMethod')}>
          <option value="cash">Cash</option>
          <option value="check">Check</option>
          <option value="online">Online (card)</option>
        </select>
      </label>

      {value.paymentMethod === 'cash' && (
        <label>
          Received by
          <input value={value.receivedBy} onChange={set('receivedBy')} />
        </label>
      )}

      {value.paymentMethod === 'check' && (
        <>
          <label>
            Check number
            <input value={value.checkNo} onChange={set('checkNo')} />
          </label>
          <label>
            Bank name
            <input value={value.bankName} onChange={set('bankName')} />
          </label>
        </>
      )}

      {value.paymentMethod === 'online' && (
        <label>
          Card holder name
          <input value={value.cardHolder} onChange={set('cardHolder')} />
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
  const [donors, setDonors] = useState([])
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

  // Load donors so the user can select a donor by name instead of entering an ID.
  useEffect(() => {
    request('/users')
      .then((users) => {
        setDonors(users.filter((user) => user.role === 'DONOR'))
      })
      .catch((e) => {
        showError(`Could not load donors: ${e.message}`)
      })
  }, [])

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const save = async (e) => {
    e.preventDefault()
    if (!form.donorId.trim()) return showError('Select a donor.')

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
    <section className="lookup-page" aria-labelledby="donations-title">
      <p className="eyebrow">NPO Management</p>
      <h1 id="donations-title">Donations</h1>
      <p className="muted">Record and manage money and in-kind donations.</p>

      {message && (
        <p
          role="status"
          className={`notice ${message.kind === 'error' ? 'error' : 'success'}`}
        >
          {message.text}
        </p>
      )}

      <p className="muted">
        {items.length} donation{items.length === 1 ? '' : 's'}
        {totalsText && ` · money received so far: ${totalsText}`}
      </p>

      <div className="lookup-toolbar">
        <button className="primary" onClick={() => setForm({ ...EMPTY_FORM })}>
          Record donation
        </button>
        <button onClick={refresh} disabled={loading}>
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </div>

      {form && (
        <form className="lookup-form" onSubmit={save}>
          <h2>Record donation</h2>

          <label>
            Donation type
            <select value={form.type} onChange={set('type')}>
              <option value="money">Money</option>
              <option value="in-kind">In-kind (items)</option>
            </select>
          </label>

          <label>
            Donor
            <select
              value={form.donorId}
              onChange={set('donorId')}
              required
              autoFocus
            >
              <option value="">Select a donor</option>
              {donors.map((donor) => (
                <option key={donor.id} value={donor.id}>
                  {donor.name} ({donor.email})
                </option>
              ))}
            </select>
          </label>

          {donors.length === 0 && (
            <p className="muted">
              No donors found. Register a user with the DONOR role first.
            </p>
          )}

          {form.type === 'money' ? (
            <>
              <label>
                Amount
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.amount}
                  onChange={set('amount')}
                />
              </label>

              <label>
                Currency
                <input value={form.currency} onChange={set('currency')} />
              </label>

              <PaymentFields value={form} onChange={setForm} allowLater />
            </>
          ) : (
            <>
              <label>
                Item name
                <input value={form.itemName} onChange={set('itemName')} />
              </label>

              <label>
                Quantity
                <input
                  type="number"
                  min="1"
                  step="1"
                  value={form.quantity}
                  onChange={set('quantity')}
                />
              </label>

              <label>
                Estimated value
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.estimatedValue}
                  onChange={set('estimatedValue')}
                />
              </label>
            </>
          )}

          <div className="actions">
            <button type="submit" className="primary">Record donation</button>
            <button type="button" onClick={() => setForm(null)}>Cancel</button>
          </div>
        </form>
      )}

      {payForm && (
        <form className="lookup-form" onSubmit={payNow}>
          <h2>Take payment</h2>

          <PaymentFields value={payForm} onChange={setPayForm} />

          <div className="actions">
            <button type="submit" className="primary">Take payment</button>
            <button type="button" onClick={() => setPayForm(null)}>Cancel</button>
          </div>
        </form>
      )}

      {selected && (
        <section className="lookup-form">
          <h2>Donation summary</h2>

          <dl className="donation-summary">
            <dt>Type</dt>
            <dd>{isInKind(selected) ? 'In-kind' : 'Money'}</dd>

            <dt>Donor</dt>
            <dd>{donorLabel(selected)}</dd>

            <dt>Details</dt>
            <dd>{detailsOf(selected)}</dd>

            {selected.transactionId && (
              <>
                <dt>Transaction</dt>
                <dd>{selected.transactionId}</dd>
              </>
            )}

            <dt>Status</dt>
            <dd>{selected.status}</dd>

            <dt>Date</dt>
            <dd>{dateOf(selected)}</dd>

            <dt>Id</dt>
            <dd>{selected.id}</dd>
          </dl>

          <button onClick={() => setSelected(null)}>Close</button>
        </section>
      )}

      <h2>All donations</h2>

      <div className="lookup-table">
        <table>
          <thead>
            <tr>
              <th scope="col">Date</th>
              <th scope="col">Type</th>
              <th scope="col">Donor</th>
              <th scope="col">Details</th>
              <th scope="col">Status</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>

          <tbody>
            {items.map((d) => (
              <tr key={d.id}>
                <td>{dateOf(d)}</td>
                <td>{isInKind(d) ? 'In-kind' : 'Money'}</td>
                <td>{donorLabel(d)}</td>
                <td>{detailsOf(d)}</td>

                <td>
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

                <td>
                  <div className="actions">
                    {!isInKind(d) && d.status === 'PENDING' && (
                      <button
                        onClick={() => setPayForm({ id: d.id, ...EMPTY_PAYMENT, paymentMethod: 'cash' })}
                      >
                        Take payment
                      </button>
                    )}

                    <button onClick={() => showDonationSummary(d)}>View</button>
                    <button className="danger" onClick={() => remove(d)}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}

            {!loading && items.length === 0 && (
              <tr>
                <td colSpan={6}>
                  No donations yet. Use “Record donation” to add the first one.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}

