// View for UserController (MVC).
// Strategy: each user's preferred channel determines how notifications are formatted and sent.

import { useEffect, useState } from 'react'
import { api } from '../api'

const ROLES = ['DONOR', 'VOLUNTEER', 'BENEFICIARY', 'ADMIN']
const CHANNELS = ['EMAIL', 'SMS', 'SOCIAL']

const EMPTY_USER = {
  role: 'DONOR',
  name: '',
  email: '',
  phone: '',
  password: '',
  preferredChannel: 'EMAIL',
  donorType: 'INDIVIDUAL',
  availability: '',
  address: '',
  familySize: 1,
}

export default function UserPage() {
  const [users, setUsers] = useState([])
  const [form, setForm] = useState(EMPTY_USER)
  const [selected, setSelected] = useState(null)
  const [notes, setNotes] = useState([])
  const [message, setMessage] = useState('')
  const [messageKind, setMessageKind] = useState('success')

  function showMessage(text, kind = 'success') {
    setMessage(text)
    setMessageKind(kind)
  }

  function loadUsers() {
    api.get('/users')
      .then(setUsers)
      .catch(err => showMessage(err.message, 'error'))
  }

  useEffect(() => {
    loadUsers()
  }, [])

  function set(key) {
    return e => {
      const value = e.target.value
      setForm(previous => ({ ...previous, [key]: value }))
    }
  }

  function changeRole(e) {
    const role = e.target.value

    setForm(previous => ({
      ...previous,
      role,
    }))
  }

  function register(e) {
    e.preventDefault()

    const payload = {
      role: form.role,
      name: form.name.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      password: form.password,
      preferredChannel: form.preferredChannel,
    }

    // Add only the fields required by the selected role.
    if (form.role === 'DONOR') {
      payload.donorType = form.donorType
    }

    if (form.role === 'VOLUNTEER') {
      payload.availability = form.availability.trim()

      if (!payload.availability) {
        showMessage('Please enter volunteer availability.', 'error')
        return
      }
    }

    if (form.role === 'BENEFICIARY') {
      payload.address = form.address.trim()
      payload.familySize = Number(form.familySize)

      if (!payload.address) {
        showMessage('Please enter the beneficiary address.', 'error')
        return
      }

      if (!Number.isInteger(payload.familySize) || payload.familySize < 1) {
        showMessage('Family size must be a positive whole number.', 'error')
        return
      }
    }

    if (
      !payload.name ||
      !payload.email ||
      !payload.phone ||
      !payload.password
    ) {
      showMessage('Please fill in all required fields.', 'error')
      return
    }

    api.post('/users/register', payload)
      .then(() => {
        setForm({ ...EMPTY_USER })
        showMessage('User registered successfully.')
        loadUsers()
      })
      .catch(err => {
        showMessage(err.message || 'Registration failed.', 'error')
      })
  }

  // Strategy: changing the preferred channel changes the notification strategy.
  function changeChannel(user, channel) {
    const data = {
      name: user.name,
      email: user.email,
      phone: user.phone,
      preferredChannel: channel,
    }

    api.put(`/users/${user.id}`, data)
      .then(() => {
        showMessage(`${user.name} now uses ${channel}.`)
        loadUsers()
      })
      .catch(err => showMessage(err.message, 'error'))
  }

  function deleteUser(id) {
    api.del(`/users/${id}`)
      .then(() => {
        setSelected(null)
        setNotes([])
        showMessage('User deleted.')
        loadUsers()
      })
      .catch(err => showMessage(err.message, 'error'))
  }

  function showNotifications(user) {
    setSelected(user)
    setNotes([])

    api.get(`/users/${user.id}/notifications`)
      .then(setNotes)
      .catch(err => showMessage(err.message, 'error'))
  }

  return (
    <section className="lookup-page" aria-labelledby="users-title">
      <p className="eyebrow">NPO Management</p>
      <h1 id="users-title">Users</h1>
      <p className="muted">
        Manage organization users, registration details, and notification preferences.
      </p>

      {message && (
        <p
          className={`notice ${messageKind === 'error' ? 'error' : 'success'}`}
          role="status"
        >
          {message}
        </p>
      )}

      <form className="lookup-form" onSubmit={register}>
        <h2>Register user</h2>

        <fieldset>
          <label>
            Role
            <select value={form.role} onChange={changeRole}>
              {ROLES.map(role => (
                <option key={role} value={role}>{role}</option>
              ))}
            </select>
          </label>

          <label>
            Name
            <input
              placeholder="Name"
              value={form.name}
              onChange={set('name')}
              required
            />
          </label>

          <label>
            Email
            <input
              type="email"
              placeholder="Email"
              value={form.email}
              onChange={set('email')}
              required
            />
          </label>

          <label>
            Phone
            <input
              type="tel"
              placeholder="Phone"
              value={form.phone}
              onChange={set('phone')}
              required
            />
          </label>

          <label>
            Password
            <input
              type="password"
              placeholder="Password"
              value={form.password}
              onChange={set('password')}
              required
            />
          </label>

          <label>
            Preferred channel
            <select
              value={form.preferredChannel}
              onChange={set('preferredChannel')}
            >
              {CHANNELS.map(channel => (
                <option key={channel} value={channel}>{channel}</option>
              ))}
            </select>
          </label>

          {form.role === 'DONOR' && (
            <label>
              Donor type
              <select
                value={form.donorType}
                onChange={set('donorType')}
              >
                <option value="INDIVIDUAL">INDIVIDUAL</option>
                <option value="ORGANIZATION">ORGANIZATION</option>
              </select>
            </label>
          )}

          {form.role === 'VOLUNTEER' && (
            <label>
              Availability
              <input
                placeholder="Availability (e.g. Weekends)"
                value={form.availability}
                onChange={set('availability')}
                required
              />
            </label>
          )}

          {form.role === 'BENEFICIARY' && (
            <>
              <label>
                Address
                <input
                  placeholder="Address"
                  value={form.address}
                  onChange={set('address')}
                  required
                />
              </label>

              <label>
                Family size
                <input
                  type="number"
                  min="1"
                  step="1"
                  value={form.familySize}
                  onChange={set('familySize')}
                  required
                />
              </label>
            </>
          )}

          {form.role === 'ADMIN' && (
            <p className="muted">
              No additional information is required for an administrator.
            </p>
          )}

          <div className="actions">
            <button className="primary" type="submit">
              Register
            </button>
          </div>
        </fieldset>
      </form>

      <h2>All users</h2>

      <div className="lookup-table">
        <table>
          <caption>Registered users</caption>

          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Role</th>
              <th scope="col">Email</th>
              <th scope="col">Phone</th>
              <th scope="col">Channel (Strategy)</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>

          <tbody>
            {users.map(user => (
              <tr key={user.id}>
                <td>{user.name}</td>
                <td>{user.role}</td>
                <td>{user.email}</td>
                <td>{user.phone}</td>

                <td>
                  <select
                    value={user.preferredChannel}
                    onChange={e => changeChannel(user, e.target.value)}
                  >
                    {CHANNELS.map(channel => (
                      <option key={channel} value={channel}>
                        {channel}
                      </option>
                    ))}
                  </select>
                </td>

                <td>
                  <div className="actions">
                    <button
                      type="button"
                      onClick={() => showNotifications(user)}
                    >
                      Notifications
                    </button>

                    <button
                      type="button"
                      className="danger"
                      onClick={() => deleteUser(user.id)}
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}

            {!users.length && (
              <tr>
                <td colSpan={6}>
                  No users to display. Register a user to get started.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {selected && (
        <section className="lookup-form">
          <h2>Notifications for {selected.name}</h2>

          <p className="muted">
            EMAIL adds a greeting and signature, SMS cuts the text to
            160 characters, SOCIAL adds an [NPO] tag.
            FAILED means the channel could not reach the user
            (e.g. bad email or phone).
          </p>

          {notes.length === 0 && (
            <p className="muted">No notifications yet.</p>
          )}

          <div className="lookup-table">
            <table>
              <caption>Notification history</caption>

              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Status</th>
                  <th scope="col">Message</th>
                </tr>
              </thead>

              <tbody>
                {notes.map((note, index) => (
                  <tr key={index}>
                    <td>{String(note.date).slice(0, 10)}</td>
                    <td>{note.status}</td>
                    <td style={{ whiteSpace: 'pre-wrap' }}>
                      {note.content}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </section>
  )
}

