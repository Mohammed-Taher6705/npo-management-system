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
    <section className="lookup-page" aria-labelledby="volunteers-title">
      <p className="eyebrow">NPO Management</p>
      <h1 id="volunteers-title">Volunteers</h1>
      <p className="muted">Manage volunteer tasks, assignments, working hours, and unavailable dates.</p>

      {message && (
        <p className="notice success" role="status">{message}</p>
      )}

      <form className="lookup-form" onSubmit={createTask}>
        <h2>Create task</h2>
        <fieldset>
          <label>
            Title
            <input placeholder="Title" value={form.title} onChange={set('title')} required />
          </label>

          <label>
            Date
            <input type="date" value={form.date} onChange={set('date')} required />
          </label>

          <label>
            Location
            <input placeholder="Location" value={form.location} onChange={set('location')} required />
          </label>

          <div className="actions">
            <button className="primary" type="submit">Create</button>
          </div>
        </fieldset>
      </form>

      <h2>All tasks</h2>
      <div className="lookup-table">
        <table>
          <caption>Tasks</caption>
          <thead>
            <tr>
              <th scope="col">Title</th>
              <th scope="col">Date</th>
              <th scope="col">Location</th>
              <th scope="col">Volunteers</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {tasks.map(t => (
              <tr key={t.id}>
                <td>{t.title}</td>
                <td>{day(t.date)}</td>
                <td>{t.location}</td>
                <td>{t.assignees.length}</td>
                <td>
                  <div className="actions">
                    <button onClick={() => openTask(t)}>Open</button>
                    <button className="danger" onClick={() => deleteTask(t.id)}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
            {!tasks.length && (
              <tr>
                <td colSpan={5}>No tasks to display. Create a task to get started.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {selected && (
        <section className="lookup-form">
          <h2>{selected.title}</h2>

          <form onSubmit={editTask}>
            <h3>Change date / location</h3>
            <p className="muted">Updating the task date or location notifies assigned volunteers.</p>

            <fieldset>
              <label>
                Date
                <input type="date" value={edit.date} onChange={setE('date')} required />
              </label>

              <label>
                Location
                <input placeholder="Location" value={edit.location} onChange={setE('location')} required />
              </label>

              <div className="actions">
                <button className="primary" type="submit">Save</button>
              </div>
            </fieldset>
          </form>

          <h3>Assign volunteer</h3>
          <form onSubmit={assign}>
            <fieldset>
              <label>
                Volunteer
                <select value={assignId} onChange={e => setAssignId(e.target.value)} required>
                  <option value="">Choose volunteer</option>
                  {volunteers.map(v => (
                    <option key={v.id} value={v.id}>{v.name} ({v.availability})</option>
                  ))}
                </select>
              </label>

              <div className="actions">
                <button className="primary" type="submit">Assign</button>
              </div>
            </fieldset>
          </form>

          <h3>Assigned volunteers</h3>
          <div className="lookup-table">
            <table>
              <caption>Assigned volunteers</caption>
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Availability</th>
                  <th scope="col">Total hours</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {selected.assignees.map(a => (
                  <tr key={a.id}>
                    <td>{a.name}</td>
                    <td>{a.availability}</td>
                    <td>{a.totalHours}</td>
                    <td>
                      <button className="danger" onClick={() => unassign(a.id)}>Remove</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <h3>Log hours</h3>
          <form onSubmit={logHours}>
            <fieldset>
              <label>
                Volunteer
                <select value={hours.volunteerId} onChange={e => setHours({ ...hours, volunteerId: e.target.value })} required>
                  <option value="">Choose volunteer</option>
                  {selected.assignees.map(a => (
                    <option key={a.id} value={a.id}>{a.name}</option>
                  ))}
                </select>
              </label>

              <label>
                Hours
                <input
                  type="number"
                  min="0.5"
                  step="0.5"
                  value={hours.hours}
                  onChange={e => setHours({ ...hours, hours: e.target.value })}
                  required
                />
              </label>

              <div className="actions">
                <button className="primary" type="submit">Log</button>
              </div>
            </fieldset>
          </form>
        </section>
      )}

      <h2>Volunteer details</h2>
      <div className="lookup-toolbar">
        <label>
          Select volunteer
          <select value={volunteer} onChange={e => loadVolunteer(e.target.value)}>
            <option value="">Choose volunteer</option>
            {volunteers.map(v => (
              <option key={v.id} value={v.id}>{v.name}</option>
            ))}
          </select>
        </label>
      </div>

      {volunteer && (
        <section className="lookup-form">
          <h3>Hours history (total: {total})</h3>

          <div className="lookup-table">
            <table>
              <caption>Volunteer hours history</caption>
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Task</th>
                  <th scope="col">Hours</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {logs.map(l => (
                  <tr key={l.id}>
                    <td>{day(l.date)}</td>
                    <td>{l.taskTitle}</td>
                    <td>{l.hours}</td>
                    <td>
                      <button className="danger" onClick={() => deleteLog(l.id)}>Delete</button>
                    </td>
                  </tr>
                ))}
                {!logs.length && (
                  <tr>
                    <td colSpan={4}>No hours recorded yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <h3>Unavailable dates</h3>
          <form onSubmit={addOffDate}>
            <fieldset>
              <label>
                Date
                <input type="date" value={offDate} onChange={e => setOffDate(e.target.value)} required />
              </label>

              <div className="actions">
                <button className="primary" type="submit">Add</button>
              </div>
            </fieldset>
          </form>

          <div className="lookup-table">
            <table>
              <caption>Unavailable dates</caption>
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {offDates.map(d => (
                  <tr key={d}>
                    <td>{day(d)}</td>
                    <td>
                      <button className="danger" onClick={() => removeOffDate(d)}>Remove</button>
                    </td>
                  </tr>
                ))}
                {!offDates.length && (
                  <tr>
                    <td colSpan={2}>No unavailable dates recorded.</td>
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

