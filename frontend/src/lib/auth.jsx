import { createContext, useContext, useEffect, useState } from 'react'
import api from './api'

const AuthCtx = createContext(null)

// Demo affordance: opening any URL with #token=<jwt> signs that device in
// (e.g. hand a judge's phone a pre-seeded session via QR). The hash is stripped after use.
function initialToken() {
  const m = window.location.hash.match(/token=([^&]+)/)
  if (m) {
    localStorage.setItem('kathirha_token', m[1])
    history.replaceState(null, '', window.location.pathname + window.location.search)
    return m[1]
  }
  return localStorage.getItem('kathirha_token')
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(initialToken)
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let active = true
    async function load() {
      if (token) {
        try {
          const { data } = await api.get('/auth/me')
          if (active) setUser(data)
        } catch {
          localStorage.removeItem('kathirha_token')
          if (active) { setToken(null); setUser(null) }
        }
      }
      if (active) setReady(true)
    }
    load()
    return () => { active = false }
  }, [token])

  const login = (tok, usr) => {
    localStorage.setItem('kathirha_token', tok)
    setToken(tok)
    if (usr) setUser(usr)
  }
  const logout = () => {
    localStorage.removeItem('kathirha_token')
    setToken(null)
    setUser(null)
  }
  const refresh = async () => {
    try {
      const { data } = await api.get('/auth/me')
      setUser(data)
    } catch { /* ignore */ }
  }

  return (
    <AuthCtx.Provider value={{ token, user, ready, login, logout, refresh, setUser }}>
      {children}
    </AuthCtx.Provider>
  )
}

export const useAuth = () => useContext(AuthCtx)
