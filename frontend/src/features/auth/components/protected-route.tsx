import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { getAuthSession } from '../lib/auth-storage'

export function ProtectedRoute() {
  const location = useLocation()
  const session = getAuthSession()

  if (!session?.tokenAcesso) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
