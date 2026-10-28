import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import HealthPage from './pages/HealthPage'
import UserPage from './pages/UserPage'
import EventPage from './pages/EventPage'
import DonationPage from './pages/DonationPage'
import VolunteerPage from './pages/VolunteerPage'
import BeneficiaryPage from './pages/BeneficiaryPage'
import LookupPage from './pages/LookupPage'

const pages = [
  ['/', 'Health', <HealthPage />],
  ['/users', 'Users', <UserPage />],
  ['/events', 'Events', <EventPage />],
  ['/donations', 'Donations', <DonationPage />],
  ['/volunteers', 'Volunteers', <VolunteerPage />],
  ['/beneficiaries', 'Beneficiaries', <BeneficiaryPage />],
  ['/lookups', 'Lookups', <LookupPage />],
]

export default function App() {
  return (
    <BrowserRouter>
      <nav className="nav">
        <strong>NPO System</strong>
        {pages.map(([path, label]) => (
          <NavLink key={path} to={path} end>{label}</NavLink>
        ))}
      </nav>
      <main className="content">
        <Routes>
          {pages.map(([path, , el]) => <Route key={path} path={path} element={el} />)}
        </Routes>
      </main>
    </BrowserRouter>
  )
}
