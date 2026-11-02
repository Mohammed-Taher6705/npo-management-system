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
    <div>
      <h2>Events</h2>
      {message && <p><b>{message}</b></p>}

      <h3>Create event</h3>
      <form onSubmit={createEvent}>
        <input placeholder="Title" value={form.title} onChange={set('title')} required />{' '}
        <select value={form.eventType} onChange={set('eventType')}>
          <option>FUNDRAISER</option>
          <option>WORKSHOP</option>
          <option>OUTREACH</option>
        </select>{' '}
        <input type="date" value={form.date} onChange={set('date')} required />{' '}
        <input placeholder="Location" value={form.location} onChange={set('location')} required />{' '}
        <input type="number" min="1" value={form.capacity} onChange={set('capacity')} required />{' '}
        <button>Create</button>
      </form>

      <h3>All events</h3>
      <table border="1" cellPadding="6">
        <thead>
          <tr><th>Title</th><th>Type</th><th>Date</th><th>Location</th><th>Seats</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          {events.map(ev => (
            <tr key={ev.id}>
              <td>{ev.title}</td>
              <td>{ev.eventType}</td>
              <td>{day(ev.date)}</td>              <td>{ev.location}</td>
              <td>{ev.registrations.length} / {ev.capacity}</td>
              <td>{ev.status}</td>
              <td>
                <button onClick={() => { setSelected(ev); setNotes([]) }}>Open</button>{' '}
                <button onClick={() => deleteEvent(ev.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selected && (
        <div>
          <h3>{selected.title} ({selected.status})</h3>

          <p>
            Change status (notifies attendees):{' '}
            {STATUSES.map(s => <button key={s} onClick={() => changeStatus(s)}>{s}</button>)}
          </p>

          <h4>Register a user (Decorator)</h4>
          <form onSubmit={register}>
            <select value={ticket.userId} onChange={e => setTicket({ ...ticket, userId: e.target.value })} required>
              <option value="">Choose user</option>
              {users.map(u => <option key={u.id} value={u.id}>{u.name} ({u.role})</option>)}
            </select>{' '}
            <label>
              <input type="checkbox" checked={ticket.vip} onChange={e => setTicket({ ...ticket, vip: e.target.checked })} /> VIP (+200)
            </label>{' '}
            <select value={ticket.mealType} onChange={e => setTicket({ ...ticket, mealType: e.target.value })}>
              <option value="">No meal</option>
              <option>Standard</option>
              <option>Vegetarian</option>
            </select>{' '}
            <button>Register</button>
          </form>
          <p>Ticket: {description} = {price}</p>
          <p>If the event is full, the user joins the waiting list.</p>

          <h4>Attendees</h4>
          <table border="1" cellPadding="6">
            <thead>
              <tr><th>Name</th><th>Role</th><th>Ticket</th><th>Price</th><th>Attended</th><th></th></tr>
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
                    <button onClick={() => markAttended(r.userId)}>Attended</button>{' '}
                    <button onClick={() => cancel(r.userId)}>Cancel</button>{' '}
                    <button onClick={() => showNotifications(r.userId)}>Notifications</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <h4>Waiting list</h4>
          <ol>
            {selected.waitingList.map(w => <li key={w.userId}>{w.name}</li>)}
          </ol>

          <h4>Notifications (Observer)</h4>
          <ul>
            {notes.map((n, i) => <li key={i}>[{n.status}] {n.content}</li>)}
          </ul>
        </div>
      )}
    </div>
  )
}