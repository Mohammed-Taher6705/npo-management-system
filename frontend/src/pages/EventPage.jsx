// View for EventController (MVC).
// Decorator: the ticket options (VIP, meal) are sent to the backend, which wraps a BasicTicket.
// Observer: registering or changing the status notifies the attendees; their messages are shown below.
import { useEffect, useState } from 'react'
import { api } from '../api'

const day = d => new Date(d).toLocaleDateString('en-CA')
const STATUSES = ['UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELLED']
const EMPTY_EVENT = { title: '', eventType: 'FUNDRAISER', date: '', location: '', capacity: 10 }
const EMPTY_TICKET = { userId: '', vip: false, mealType: '' }

export default function EventPage() {
  const [events, setEvents] = useState([])
  const [users, setUsers] = useState([])
  const [form, setForm] = useState(EMPTY_EVENT)
  const [selected, setSelected] = useState(null)
  const [ticket, setTicket] = useState(EMPTY_TICKET)
  const [notes, setNotes] = useState([])
  const [message, setMessage] = useState('')

  function loadEvents() {
    api.get('/events').then(setEvents).catch(e => setMessage(e.message))
  }

  useEffect(() => {
    loadEvents()
    api.get('/users').then(setUsers).catch(e => setMessage(e.message))
  }, [])

  // after every change: show the new event data and refresh the list
  function done(event, text) {
    setSelected(event)
    setMessage(text)
    loadEvents()
  }

  function createEvent(e) {
    e.preventDefault()
    api.post('/events', form)
      .then(ev => { setForm(EMPTY_EVENT); done(ev, 'Event created.') })
      .catch(err => setMessage(err.message))
  }

  function register(e) {
    e.preventDefault()
    api.post(`/events/${selected.id}/register`, ticket)
      .then(ev => { setTicket(EMPTY_TICKET); done(ev, 'Registration saved.') })
      .catch(err => setMessage(err.message))
  }

  function changeStatus(status) {
    api.put(`/events/${selected.id}/status`, { status })
      .then(ev => done(ev, 'Status changed. Attendees were notified.'))
      .catch(err => setMessage(err.message))
  }

  function cancel(userId) {
    api.del(`/events/${selected.id}/register/${userId}`)
      .then(ev => done(ev, 'Registration cancelled.'))
      .catch(err => setMessage(err.message))
  }

  function markAttended(userId) {
    api.put(`/events/${selected.id}/attend/${userId}`, {})
      .then(ev => done(ev, 'Attendance marked.'))
      .catch(err => setMessage(err.message))
  }

  function deleteEvent(id) {
    api.del(`/events/${id}`)
      .then(() => { setSelected(null); setMessage('Event deleted.'); loadEvents() })
      .catch(err => setMessage(err.message))
  }

  function showNotifications(userId) {
    api.get(`/users/${userId}/notifications`).then(setNotes).catch(e => setMessage(e.message))
  }

  const set = key => e => setForm({ ...form, [key]: e.target.value })

  // same prices as the backend decorators, only for the preview
  let price = 100
  let description = 'Basic ticket'
  if (ticket.vip) { price += 200; description += ' + VIP' }
  if (ticket.mealType) { price += 50; description += ' + ' + ticket.mealType + ' meal' }

  return (
    <section className="lookup-page" aria-labelledby="events-title">
      <p className="eyebrow">NPO Management</p>
      <h1 id="events-title">Events</h1>
      <p className="muted">Manage events, ticket registrations, attendees, and notifications.</p>

      {message && (
        <p className="notice success" role="status">{message}</p>
      )}

      <form className="lookup-form" onSubmit={createEvent}>
        <h2>Create event</h2>
        <fieldset>
          <label>
            Title
            <input placeholder="Title" value={form.title} onChange={set('title')} required />
          </label>

          <label>
            Event type
            <select value={form.eventType} onChange={set('eventType')}>
              <option>FUNDRAISER</option>
              <option>WORKSHOP</option>
              <option>OUTREACH</option>
            </select>
          </label>

          <label>
            Date
            <input type="date" value={form.date} onChange={set('date')} required />
          </label>

          <label>
            Location
            <input placeholder="Location" value={form.location} onChange={set('location')} required />
          </label>

          <label>
            Capacity
            <input type="number" min="1" value={form.capacity} onChange={set('capacity')} required />
          </label>

          <div className="actions">
            <button className="primary" type="submit">Create</button>
          </div>
        </fieldset>
      </form>

      <h2>All events</h2>
      <div className="lookup-table">
        <table>
          <caption>Events</caption>
          <thead>
            <tr>
              <th scope="col">Title</th>
              <th scope="col">Type</th>
              <th scope="col">Date</th>
              <th scope="col">Location</th>
              <th scope="col">Seats</th>
              <th scope="col">Status</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {events.map(ev => (
              <tr key={ev.id}>
                <td>{ev.title}</td>
                <td>{ev.eventType}</td>
                <td>{day(ev.date)}</td>
                <td>{ev.location}</td>
                <td>{ev.registrations.length} / {ev.capacity}</td>
                <td>{ev.status}</td>
                <td>
                  <div className="actions">
                    <button onClick={() => { setSelected(ev); setNotes([]) }}>Open</button>
                    <button className="danger" onClick={() => deleteEvent(ev.id)}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
            {!events.length && (
              <tr>
                <td colSpan={7}>No events to display. Create an event to get started.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {selected && (
        <section className="lookup-form">
          <h2>{selected.title} ({selected.status})</h2>

          <h3>Change event status</h3>
          <p className="muted">Changing the status notifies registered attendees.</p>
          <div className="actions">
            {STATUSES.map(s => (
              <button key={s} onClick={() => changeStatus(s)}>{s}</button>
            ))}
          </div>

          <h3>Register a user (Decorator)</h3>
          <form onSubmit={register}>
            <fieldset>
              <label>
                User
                <select value={ticket.userId} onChange={e => setTicket({ ...ticket, userId: e.target.value })} required>
                  <option value="">Choose user</option>
                  {users.map(u => (
                    <option key={u.id} value={u.id}>{u.name} ({u.role})</option>
                  ))}
                </select>
              </label>

              <label className="vip-option">
  <input
    type="checkbox"
    checked={ticket.vip}
    onChange={e => setTicket({ ...ticket, vip: e.target.checked })}
  />
  VIP (+200)
</label>

              <label>
                Meal type
                <select value={ticket.mealType} onChange={e => setTicket({ ...ticket, mealType: e.target.value })}>
                  <option value="">No meal</option>
                  <option>Standard</option>
                  <option>Vegetarian</option>
                </select>
              </label>

              <div className="actions">
                <button className="primary" type="submit">Register</button>
              </div>
            </fieldset>
          </form>

          <div className="lookup-form">
            <h3>Ticket preview</h3>
            <p>{description} = {price}</p>
            <p className="muted">If the event is full, the user joins the waiting list.</p>
          </div>

          <h3>Attendees</h3>
          <div className="lookup-table">
            <table>
              <caption>Event attendees</caption>
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Role</th>
                  <th scope="col">Ticket</th>
                  <th scope="col">Price</th>
                  <th scope="col">Attended</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {selected.registrations.map(r => (
                  <tr key={r.userId}>
                    <td>{r.name}</td>
                    <td>{r.role}</td>
                    <td>{r.ticket}</td>
                    <td>{r.price}</td>
                    <td>{r.attended ? 'Yes' : 'No'}</td>
                    <td>
                      <div className="actions">
                        <button onClick={() => markAttended(r.userId)}>Attended</button>
                        <button className="danger" onClick={() => cancel(r.userId)}>Cancel</button>
                        <button onClick={() => showNotifications(r.userId)}>Notifications</button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!selected.registrations.length && (
                  <tr>
                    <td colSpan={6}>No attendees registered yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <h3>Waiting list</h3>
          <div className="lookup-table">
            <table>
              <caption>Waiting list</caption>
              <thead>
                <tr>
                  <th scope="col">Name</th>
                </tr>
              </thead>
              <tbody>
                {selected.waitingList.map(w => (
                  <tr key={w.userId}>
                    <td>{w.name}</td>
                  </tr>
                ))}
                {!selected.waitingList.length && (
                  <tr>
                    <td>No users on the waiting list.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <h3>Notifications (Observer)</h3>
          <div className="lookup-table">
            <table>
              <caption>Attendee notifications</caption>
              <thead>
                <tr>
                  <th scope="col">Status</th>
                  <th scope="col">Message</th>
                </tr>
              </thead>
              <tbody>
                {notes.map((n, i) => (
                  <tr key={i}>
                    <td>{n.status}</td>
                    <td style={{ whiteSpace: 'pre-wrap' }}>{n.content}</td>
                  </tr>
                ))}
                {!notes.length && (
                  <tr>
                    <td colSpan={2}>No notifications to display.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </section>
  )
}

