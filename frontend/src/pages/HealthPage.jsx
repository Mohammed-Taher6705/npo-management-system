import { useEffect, useState } from 'react'
import { api } from '../api'

// Setup check: confirms React -> Spring Boot -> MongoDB all work.
export default function HealthPage() {
  const [status, setStatus] = useState('checking...')

  useEffect(() => {
    api.get('/health')
      .then((d) => setStatus(`Backend: ${d.backend} | MongoDB: ${d.mongodb}`))
      .catch((e) => setStatus('Error: ' + e.message))
  }, [])

  return (
    <section className="lookup-page" aria-labelledby="health-title">
      <p className="eyebrow">NPO Management</p>

      <h1 id="health-title">System health</h1>

      <p className="muted">
        Check the connection between React, Spring Boot, and MongoDB.
      </p>

      <div className="lookup-form">
        <h2>Connection status</h2>
        <p role="status">{status}</p>
      </div>
    </section>
  )
}

