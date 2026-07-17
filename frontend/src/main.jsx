import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './lib/auth.jsx'
import App from './App.jsx'
import './index.css'

// Catches render-time exceptions so a single page crash never blanks the whole app.
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }
  static getDerivedStateFromError() {
    return { hasError: true }
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen items-center justify-center p-6">
          <div className="card max-w-sm text-center">
            <div className="text-lg font-extrabold text-navy-800">حدث خطأ غير متوقع</div>
            <p className="mt-1 text-sm text-slate-500">أعد تحميل الصفحة للمتابعة</p>
            <button onClick={() => window.location.reload()} className="btn-primary mt-4">إعادة التحميل</button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ErrorBoundary>
          <App />
        </ErrorBoundary>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
)
