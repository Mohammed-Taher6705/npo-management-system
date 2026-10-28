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
    <div>
      <h2>System health</h2>
      <p>{status}</p>
    </div>
  )
}
