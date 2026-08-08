import { Navigate, Outlet } from 'react-router-dom'

import { getAuthSession } from '../lib/auth-storage'

export function GuestRoute() {
  const session = getAuthSession()

  if (session?.tokenAcesso) {
    return <Navigate to="/app/dashboard" replace />
  }

  return <Outlet />
}
