import axios from 'axios'

const api = axios.create({
  baseURL: (import.meta.env.VITE_API_URL ?? '') + '/api',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('kathirha_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    // Spring returns 403 (not 401) for expired/anonymous tokens on protected APIs;
    // /admin 403s are real permission errors handled by the Admin page itself.
    const sessionExpired = status === 401 || (status === 403 && !err.config?.url?.includes('/admin'))
    if (sessionExpired) {
      localStorage.removeItem('kathirha_token')
      if (window.location.pathname !== '/login') window.location.assign('/login')
    }
    return Promise.reject(err)
  }
)

export default api
