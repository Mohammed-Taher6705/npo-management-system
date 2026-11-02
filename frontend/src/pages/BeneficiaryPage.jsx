// View for BeneficiaryController (MVC).
// Observer + client requirement: a HIGH priority need sends an URGENT alert to every admin.
// Need categories come from the NeedCategory lookup table.
import { useEffect, useState } from 'react'
import { api } from '../api'

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH']
const CHANNELS = ['EMAIL', 'SMS', 'SOCIAL']
const EMPTY_BEN = { name: '', email: '', phone: '', password: '', preferredChannel: 'EMAIL', address: '', familySize: 1 }

export default function BeneficiaryPage() {
  const [list, setList] = useState([])
  const [categories, setCategories] = useState([])
  const [admins, setAdmins] = useState([])
  const [form, setForm] = useState(EMPTY_BEN)
  const [selected, setSelected] = useState(null)
  const [need, setNeed] = useState({ categoryId: '', priority: 'LOW' })
  const [alerts, setAlerts] = useState([])
  const [message, setMessage] = useState('')

  function loadList() {
    api.get('/beneficiaries').then(setList).catch(e => setMessage(e.message))
  }

  useEffect(() => {
    loadList()
    api.get('/lookups/need-categories').then(setCategories).catch(e => setMessage(e.message))
    api.get('/users')
      .then(users => setAdmins(users.filter(u => u.role === 'ADMIN')))
      .catch(e => setMessage(e.message))
  }, [])

  // after every change: show the new data and refresh the list
  function done(b, text) {
    setSelected(b)
    setMessage(text)
    loadList()
  }

  function addBeneficiary(e) {
    e.preventDefault()
    api.post('/beneficiaries', form)
      .then(b => { setForm(EMPTY_BEN); done(b, 'Beneficiary added.') })
      .catch(err => setMessage(err.message))
  }

  function deleteBeneficiary(id) {
    api.del(`/beneficiaries/${id}`)
      .then(() => { setSelected(null); setMessage('Beneficiary and their needs deleted.'); loadList() })
      .catch(err => setMessage(err.message))
  }

  function addNeed(e) {
    e.preventDefault()
    api.post(`/beneficiaries/${selected.id}/needs`, need)
      .then(b => {
        let text = 'Need added.'
        if (need.priority === 'HIGH') {
          text = 'Need added. HIGH priority: all admins were alerted.'
        }
        setNeed({ categoryId: '', priority: 'LOW' })
        done(b, text)
      })
      .catch(err => setMessage(err.message))
  }

  function updateNeed(n, priority, status) {
    api.put(`/beneficiaries/${selected.id}/needs/${n.id}`, { priority, status })
      .then(b => {
        let text = 'Need updated.'
        if (n.priority !== 'HIGH' && priority === 'HIGH') {
          text = 'Need raised to HIGH: all admins were alerted.'
        }
        done(b, text)
      })
      .catch(err => setMessage(err.message))
  }

  function removeNeed(needId) {
    api.del(`/beneficiaries/${selected.id}/needs/${needId}`)
      .then(b => done(b, 'Need removed.'))
      .catch(err => setMessage(err.message))
  }

  function showAlerts(adminId) {
    if (!adminId) {
      setAlerts([])
      return
    }
    api.get(`/users/${adminId}/notifications`)
      .then(notes => setAlerts(notes.filter(n => n.content.includes('URGENT'))))
      .catch(e => setMessage(e.message))
  }

  const set = key => e => setForm({ ...form, [key]: e.target.value })

  return (
    <div>
      <h2>Beneficiaries</h2>
      {message && <p><b>{message}</b></p>}

      <h3>Add beneficiary</h3>
      <form onSubmit={addBeneficiary}>
        <input placeholder="Name" value={form.name} onChange={set('name')} required />{' '}
        <input placeholder="Email" value={form.email} onChange={set('email')} required />{' '}
        <input placeholder="Phone" value={form.phone} onChange={set('phone')} required />{' '}
        <input placeholder="Password" type="password" value={form.password} onChange={set('password')} required />{' '}
        <select value={form.preferredChannel} onChange={set('preferredChannel')}>
          {CHANNELS.map(c => <option key={c}>{c}</option>)}
        </select>{' '}
        <input placeholder="Address" value={form.address} onChange={set('address')} required />{' '}
        <label>
          Family size{' '}
          <input type="number" min="1" value={form.familySize} onChange={set('familySize')} required />
        </label>{' '}
        <button>Add</button>
      </form>

      <h3>All beneficiaries</h3>
      <table border="1" cellPadding="6">
        <thead>
          <tr><th>Name</th><th>Address</th><th>Family size</th><th>Open needs</th><th></th></tr>
        </thead>
        <tbody>
          {list.map(b => (
            <tr key={b.id}>
              <td>{b.name}</td>
              <td>{b.address}</td>
              <td>{b.familySize}</td>
              <td>{b.needs.filter(n => n.status === 'OPEN').length}</td>
              <td>
                <button onClick={() => setSelected(b)}>Open</button>{' '}
                <button onClick={() => deleteBeneficiary(b.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selected && (
        <div>
          <h3>Needs of {selected.name}</h3>

          <form onSubmit={addNeed}>
            <select value={need.categoryId} onChange={e => setNeed({ ...need, categoryId: e.target.value })} required>
              <option value="">Choose category</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>{' '}
            <select value={need.priority} onChange={e => setNeed({ ...need, priority: e.target.value })}>
              {PRIORITIES.map(p => <option key={p}>{p}</option>)}
            </select>{' '}
            <button>Add need</button>
          </form>
          {categories.length === 0 && <p>No need categories yet: add some on the Lookups page.</p>}

          <table border="1" cellPadding="6">
            <thead>
              <tr><th>Category</th><th>Priority</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {selected.needs.map(n => (
                <tr key={n.id} style={n.priority === 'HIGH' && n.status === 'OPEN' ? { color: '#b00020' } : {}}>
                  <td>{n.category}</td>
                  <td>
                    <select value={n.priority} onChange={e => updateNeed(n, e.target.value, n.status)}>
                      {PRIORITIES.map(p => <option key={p}>{p}</option>)}
                    </select>
                  </td>
                  <td>{n.status}</td>
                  <td>
                    {n.status === 'OPEN'
                      ? <button onClick={() => updateNeed(n, n.priority, 'FULFILLED')}>Mark fulfilled</button>
                      : <button onClick={() => updateNeed(n, n.priority, 'OPEN')}>Reopen</button>}{' '}
                    <button onClick={() => removeNeed(n.id)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h3>Urgent alerts received by admins (Observer)</h3>
      {admins.length === 0 && <p>No admins yet: register an ADMIN on the Users page to receive alerts.</p>}
      <select onChange={e => showAlerts(e.target.value)}>
        <option value="">Choose admin</option>
        {admins.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
      </select>
      <ul>
        {alerts.map((n, i) => <li key={i}>[{n.status}] {n.content}</li>)}
      </ul>
    </div>
  )
}