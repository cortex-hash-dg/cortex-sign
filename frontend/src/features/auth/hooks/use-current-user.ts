import { useQuery } from '@tanstack/react-query'

import { getCurrentUser } from '../api/get-current-user'
import { getAuthSession } from '../lib/auth-storage'

export function useCurrentUser() {
  return useQuery({
    queryKey: ['auth', 'me'],
    queryFn: getCurrentUser,
    enabled: Boolean(getAuthSession()?.tokenAcesso),
  })
}
