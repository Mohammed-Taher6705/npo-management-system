// View for UserController (MVC).
// Strategy: each user's preferred channel (EMAIL, SMS, SOCIAL) decides how their notifications are formatted and sent.
import { useEffect, useState } from 'react'
import { api } from '../api'

const ROLES = ['DONOR', 'VOLUNTEER', 'BENEFICIARY', 'ADMIN']
const CHANNELS = ['EMAIL', 'SMS', 'SOCIAL']
const EMPTY_USER = {
  role: 'DONOR', name: '', email: '', phone: '', password: '', preferredChannel: 'EMAIL',
  donorType: 'INDIVIDUAL', availability: '', address: '', familySize: 1,
}

export default function UserPage() {
  const [users, setUsers] = useState([])
  const [form, setForm] = useState(EMPTY_USER)
  const [selected, setSelected] = useState(null)
  const [notes, setNotes] = useState([])
  const [message, setMessage] = useState('')

  function loadUsers() {
    api.get('/users').then(setUsers).catch(e => setMessage(e.message))
  }

  useEffect(() => {
    loadUsers()
  }, [])

  function register(e) {
    e.preventDefault()
    api.post('/users/register', form)
      .then(() => { setForm(EMPTY_USER); setMessage('User registered.'); loadUsers() })
      .catch(err => setMessage(err.message))
  }

  // Strategy: switching the channel switches the strategy used for the next notifications
  function changeChannel(user, channel) {
    const data = { name: user.name, email: user.email, phone: user.phone, preferredChannel: channel }
    api.put(`/users/${user.id}`, data)
      .then(() => { setMessage(user.name + ' now uses ' + channel + '.'); loadUsers() })
      .catch(err => setMessage(err.message))
  }

  function deleteUser(id) {
    api.del(`/users/${id}`)
      .then(() => { setSelected(null); setNotes([]); setMessage('User deleted.'); loadUsers() })
      .catch(err => setMessage(err.message))
  }

  function showNotifications(user) {
    setSelected(user)
    api.get(`/users/${user.id}/notifications`).then(setNotes).catch(e => setMessage(e.message))
  }

  const set = key => e => setForm({ ...form, [key]: e.target.value })

  return (
    <div>
      <h2>Users</h2>
      {message && <p><b>{message}</b></p>}

      <h3>Register user</h3>
      <form onSubmit={register}>
        <select value={form.role} onChange={set('role')}>
          {ROLES.map(r => <option key={r}>{r}</option>)}
        </select>{' '}
        <input placeholder="Name" value={form.name} onChange={set('name')} required />{' '}
        <input placeholder="Email" value={form.email} onChange={set('email')} required />{' '}
        <input placeholder="Phone" value={form.phone} onChange={set('phone')} required />{' '}
        <input placeholder="Password" type="password" value={form.password} onChange={set('password')} required />{' '}
        <label>
          Channel{' '}
          <select value={form.preferredChannel} onChange={set('preferredChannel')}>
            {CHANNELS.map(c => <option key={c}>{c}</option>)}
          </select>
        </label>{' '}

        {form.role === 'DONOR' && (
          <select value={form.donorType} onChange={set('donorType')}>
            <option>INDIVIDUAL</option>
            <option>ORGANIZATION</option>
          </select>
        )}
        {form.role === 'VOLUNTEER' && (
          <input placeholder="Availability (e.g. Weekends)" value={form.availability} onChange={set('availability')} required />
        )}
        {form.role === 'BENEFICIARY' && (
          <>
            <input placeholder="Address" value={form.address} onChange={set('address')} required />{' '}
            <input type="number" min="1" value={form.familySize} onChange={set('familySize')} required />
          </>
        )}{' '}
        <button>Register</button>
      </form>

      <h3>All users</h3>
      <table border="1" cellPadding="6">
        <thead>
          <tr><th>Name</th><th>Role</th><th>Email</th><th>Phone</th><th>Channel (Strategy)</th><th></th></tr>
        </thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.name}</td>
              <td>{u.role}</td>
              <td>{u.email}</td>
              <td>{u.phone}</td>
              <td>
                <select value={u.preferredChannel} onChange={e => changeChannel(u, e.target.value)}>
                  {CHANNELS.map(c => <option key={c}>{c}</option>)}
                </select>
              </td>
              <td>
                <button onClick={() => showNotifications(u)}>Notifications</button>{' '}
                <button onClick={() => deleteUser(u.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selected && (
        <div>
          <h3>Notifications for {selected.name}</h3>
          <p>
            EMAIL adds a greeting and signature, SMS cuts the text to 160 characters, SOCIAL adds an [NPO] tag.
            FAILED means the channel could not reach the user (e.g. bad email or phone).
          </p>
          {notes.length === 0 && <p>No notifications yet.</p>}
          <table border="1" cellPadding="6">
            <thead>
              <tr><th>Date</th><th>Status</th><th>Message</th></tr>
            </thead>
            <tbody>
              {notes.map((n, i) => (
                <tr key={i}>
                  <td>{String(n.date).slice(0, 10)}</td>
                  <td>{n.status}</td>
                  <td style={{ whiteSpace: 'pre-wrap' }}>{n.content}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}