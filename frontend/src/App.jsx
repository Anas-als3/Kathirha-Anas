import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './lib/auth.jsx'
import { Loader } from './components/ui.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Missions from './pages/Missions.jsx'
import Goals from './pages/Goals.jsx'
import Leaderboard from './pages/Leaderboard.jsx'
import Shop from './pages/Shop.jsx'
import Quiz from './pages/Quiz.jsx'
import Season from './pages/Season.jsx'
import WhatsApp from './pages/WhatsApp.jsx'
import Admin from './pages/Admin.jsx'
import ConnectBank from './pages/ConnectBank.jsx'
import CardSkins from './pages/CardSkins.jsx'

function Protected({ children }) {
  const { token, ready } = useAuth()
  if (!ready) return <Loader full />
  if (!token) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/missions" element={<Missions />} />
        <Route path="/goals" element={<Goals />} />
        <Route path="/leaderboard" element={<Leaderboard />} />
        <Route path="/shop" element={<Shop />} />
        <Route path="/quiz" element={<Quiz />} />
        <Route path="/season" element={<Season />} />
        <Route path="/whatsapp" element={<WhatsApp />} />
        <Route path="/cards" element={<CardSkins />} />
        <Route path="/admin" element={<Admin />} />
        <Route path="/bank/connect" element={<ConnectBank />} />
        <Route path="/bank/callback" element={<ConnectBank />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
