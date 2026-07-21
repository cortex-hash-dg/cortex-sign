import { createBrowserRouter } from 'react-router-dom'
import { FoundationPage } from '../views/foundation-page'

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <FoundationPage />,
  },
])
