// View for VolunteerController (MVC).
// Observer: assigning a volunteer, or changing a task's date/location, notifies the assigned volunteers.
// Client requirement: a volunteer cannot be assigned on a date they marked as unavailable.
import { useEffect, useState } from 'react'
import { api } from '../api'

const EMPTY_TASK = { title: '', date: '', location: '' }

// backend dates come as UTC text; show them as the local day "2026-12-01"
const day = d => new Date(d).toLocaleDateString('en-CA')

export default function VolunteerPage() {
  const [tasks, setTasks] = useState([])
  const [volunteers, setVolunteers] = useState([])
  const [form, setForm] = useState(EMPTY_TASK)
  const [selected, setSelected] = useState(null)
  const [edit, setEdit] = useState(EMPTY_TASK)
  const [assignId, setAssignId] = useState('')
  const [hours, setHours] = useState({ volunteerId: '', hours: 1 })
  const [volunteer, setVolunteer] = useState('')
  const [logs, setLogs] = useState([])
  const [offDates, setOffDates] = useState([])
  const [offDate, setOffDate] = useState('')
  const [message, setMessage] = useState('')

  function loadTasks() {
    api.get('/tasks').then(setTasks).catch(e => setMessage(e.message))
  }

  useEffect(() => {
    loadTasks()
    api.get('/users')
      .then(users => setVolunteers(users.filter(u => u.role === 'VOLUNTEER')))
      .catch(e => setMessage(e.message))
  }, [])

  function openTask(t) {
    setSelected(t)
    setEdit({ title: t.title, date: day(t.date), location: t.location })
  }

  // after every task change: show the new data and refresh the list
  function done(task, text) {
    openTask(task)
    setMessage(text)
    loadTasks()
  }

  // ----- tasks -----
  function createTask(e) {
    e.preventDefault()
    api.post('/tasks', form)
      .then(t => { setForm(EMPTY_TASK); done(t, 'Task created.') })
      .catch(err => setMessage(err.message))
  }

  function editTask(e) {
    e.preventDefault()
    api.put(`/tasks/${selected.id}`, edit)
      .then(t => done(t, 'Task updated. Assigned volunteers were notified.'))
      .catch(err => setMessage(err.message))
  }

  function deleteTask(id) {
    api.del(`/tasks/${id}`)
      .then(() => { setSelected(null); setMessage('Task deleted. Assigned volunteers were notified.'); loadTasks() })
      .catch(err => setMessage(err.message))
  }

  function assign(e) {
    e.preventDefault()
    api.post(`/tasks/${selected.id}/assign`, { volunteerId: assignId })
      .then(t => { setAssignId(''); done(t, 'Volunteer assigned and notified.') })
      .catch(err => setMessage(err.message))
  }

  function unassign(volunteerId) {
    api.del(`/tasks/${selected.id}/assign/${volunteerId}`)
      .then(t => done(t, 'Volunteer removed from task.'))
      .catch(err => setMessage(err.message))
  }

  function logHours(e) {
    e.preventDefault()
    api.post('/tasks/hours', { taskId: selected.id, volunteerId: hours.volunteerId, hours: hours.hours })
      .then(() => {
        setMessage('Hours logged.')
        api.get(`/tasks/${selected.id}`).then(openTask)
        if (hours.volunteerId === volunteer) {
          loadVolunteer(volunteer)
        }
      })
      .catch(err => setMessage(err.message))
  }

  // ----- one volunteer: hours history + unavailable dates -----
  function loadVolunteer(id) {
    setVolunteer(id)
    if (!id) {
      setLogs([])
      setOffDates([])
      return
    }
    api.get(`/tasks/hours/${id}`).then(setLogs).catch(e => setMessage(e.message))
    api.get(`/tasks/unavailable/${id}`).then(setOffDates).catch(e => setMessage(e.message))
  }

  function deleteLog(logId) {
    api.del(`/tasks/hours/log/${logId}`)
      .then(() => { setMessage('Hours entry deleted.'); loadVolunteer(volunteer) })
      .catch(err => setMessage(err.message))
  }

  function addOffDate(e) {
    e.preventDefault()
    api.post(`/tasks/unavailable/${volunteer}`, { date: offDate })
      .then(list => { setOffDates(list); setOffDate(''); setMessage('Unavailable date added.') })
      .catch(err => setMessage(err.message))
  }

  function removeOffDate(d) {
    api.del(`/tasks/unavailable/${volunteer}/${day(d)}`)
      .then(list => { setOffDates(list); setMessage('Unavailable date removed.') })
      .catch(err => setMessage(err.message))
  }

  const set = key => e => setForm({ ...form, [key]: e.target.value })
  const setE = key => e => setEdit({ ...edit, [key]: e.target.value })

  let total = 0
  for (const l of logs) {
    total = total + l.hours
  }

  return (
    <div>
      <h2>Volunteers</h2>
      {message && <p><b>{message}</b></p>}

      <h3>Create task</h3>
      <form onSubmit={createTask}>
        <input placeholder="Title" value={form.title} onChange={set('title')} required />{' '}
        <input type="date" value={form.date} onChange={set('date')} required />{' '}
        <input placeholder="Location" value={form.location} onChange={set('location')} required />{' '}
        <button>Create</button>
      </form>

      <h3>All tasks</h3>
      <table border="1" cellPadding="6">
        <thead>
          <tr><th>Title</th><th>Date</th><th>Location</th><th>Volunteers</th><th></th></tr>
        </thead>
        <tbody>
          {tasks.map(t => (
            <tr key={t.id}>
              <td>{t.title}</td>
              <td>{day(t.date)}</td>
              <td>{t.location}</td>
              <td>{t.assignees.length}</td>
              <td>
                <button onClick={() => openTask(t)}>Open</button>{' '}
                <button onClick={() => deleteTask(t.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selected && (
        <div>
          <h3>{selected.title}</h3>

          <h4>Change date / location (notifies volunteers)</h4>
          <form onSubmit={editTask}>
            <input type="date" value={edit.date} onChange={setE('date')} required />{' '}
            <input placeholder="Location" value={edit.location} onChange={setE('location')} required />{' '}
            <button>Save</button>
          </form>

          <h4>Assign volunteer</h4>
          <form onSubmit={assign}>
            <select value={assignId} onChange={e => setAssignId(e.target.value)} required>
              <option value="">Choose volunteer</option>
              {volunteers.map(v => <option key={v.id} value={v.id}>{v.name} ({v.availability})</option>)}
            </select>{' '}
            <button>Assign</button>
          </form>

          <h4>Assigned volunteers</h4>
          <table border="1" cellPadding="6">
            <thead>
              <tr><th>Name</th><th>Availability</th><th>Total hours</th><th></th></tr>
            </thead>
            <tbody>
              {selected.assignees.map(a => (
                <tr key={a.id}>
                  <td>{a.name}</td>
                  <td>{a.availability}</td>
                  <td>{a.totalHours}</td>
                  <td><button onClick={() => unassign(a.id)}>Remove</button></td>
                </tr>
              ))}
            </tbody>
          </table>

          <h4>Log hours</h4>
          <form onSubmit={logHours}>
            <select value={hours.volunteerId} onChange={e => setHours({ ...hours, volunteerId: e.target.value })} required>
              <option value="">Choose volunteer</option>
              {selected.assignees.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
            </select>{' '}
            <input type="number" min="0.5" step="0.5" value={hours.hours}
              onChange={e => setHours({ ...hours, hours: e.target.value })} required />{' '}
            <button>Log</button>
          </form>
        </div>
      )}

      <h3>Volunteer details</h3>
      <select value={volunteer} onChange={e => loadVolunteer(e.target.value)}>
        <option value="">Choose volunteer</option>
        {volunteers.map(v => <option key={v.id} value={v.id}>{v.name}</option>)}
      </select>

      {volunteer && (
        <div>
          <h4>Hours history (total: {total})</h4>
          <table border="1" cellPadding="6">
            <thead>
              <tr><th>Date</th><th>Task</th><th>Hours</th><th></th></tr>
            </thead>
            <tbody>
              {logs.map(l => (
                <tr key={l.id}>
                  <td>{day(l.date)}</td>
                  <td>{l.taskTitle}</td>
                  <td>{l.hours}</td>
                  <td><button onClick={() => deleteLog(l.id)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>

          <h4>Unavailable dates</h4>
          <form onSubmit={addOffDate}>
            <input type="date" value={offDate} onChange={e => setOffDate(e.target.value)} required />{' '}
            <button>Add</button>
          </form>
          <ul>
            {offDates.map(d => (
              <li key={d}>
                {day(d)} <button onClick={() => removeOffDate(d)}>Remove</button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}