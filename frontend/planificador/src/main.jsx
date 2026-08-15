import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from './auth/AuthContext.jsx'
import EnvWatermark from './components/EnvWatermark.jsx'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <EnvWatermark />
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)
