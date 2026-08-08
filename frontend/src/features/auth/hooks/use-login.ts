import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { login } from '../api/login'
import { saveAuthSession } from '../lib/auth-storage'
import type { LoginFormData } from '../schemas/login-schema'

export function useLogin() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (formData: LoginFormData) =>
      login({
        email: formData.email,
        senha: formData.password,
      }),

    onSuccess: (response, formData) => {
      saveAuthSession(response, formData.rememberMe)
      toast.success('Login realizado com sucesso')
      navigate('/app/dashboard', { replace: true })
    },
  })
}
