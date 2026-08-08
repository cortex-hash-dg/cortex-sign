import { zodResolver } from '@hookform/resolvers/zod'
import { Mail, ShieldCheck } from 'lucide-react'
import { useForm } from 'react-hook-form'

import { getApiErrorMessage } from '../../../services/api/api-error'
import { Button } from '../../../shared/ui/button'
import { useLogin } from '../hooks/use-login'
import {
  loginSchema,
  type LoginFormData,
} from '../schemas/login-schema'

export function LoginForm() {
  const loginMutation = useLogin()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
      rememberMe: false,
    },
  })

  function submitForm(data: LoginFormData) {
    loginMutation.mutate(data)
  }

  return (
    <form className="mt-5 grid gap-3 sm:mt-6 sm:gap-3.5 2xl:mt-8 2xl:gap-5" onSubmit={handleSubmit(submitForm)} noValidate>
      <div className="login-reveal-block grid gap-2 [--login-reveal-y:24px] [animation-delay:300ms]">
        <label className="text-sm font-medium text-ink" htmlFor="email">
          E-mail
        </label>

        <div className="relative">
          <Mail
            aria-hidden="true"
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted"
            size={18}
          />
          <input
            className="min-h-10 w-full rounded-control border border-line bg-white px-10 text-sm text-ink shadow-sm transition-colors placeholder:text-muted/70 hover:border-brand-500 focus:border-brand-500 sm:min-h-11 2xl:min-h-12"
            id="email"
            type="email"
            autoComplete="username"
            placeholder="Digite seu e-mail"
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'email-error' : undefined}
            {...register('email')}
          />
        </div>

        {errors.email && (
          <p className="text-sm text-danger" id="email-error" role="alert">
            {errors.email.message}
          </p>
        )}
      </div>

      <div className="login-reveal-block grid gap-2 [--login-reveal-y:-24px] [animation-delay:380ms]">
        <label className="text-sm font-medium text-ink" htmlFor="password">
          Senha
        </label>

        <div className="relative">
          <ShieldCheck
            aria-hidden="true"
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted"
            size={18}
          />
          <input
            className="min-h-10 w-full rounded-control border border-line bg-white px-10 text-sm text-ink shadow-sm transition-colors hover:border-brand-500 focus:border-brand-500 sm:min-h-11 2xl:min-h-12"
            id="password"
            type="password"
            autoComplete="current-password"
            placeholder="Digite sua senha"
            aria-invalid={Boolean(errors.password)}
            aria-describedby={
              errors.password ? 'password-error' : undefined
            }
            {...register('password')}
          />
        </div>

        {errors.password && (
          <p className="text-sm text-danger" id="password-error" role="alert">
            {errors.password.message}
          </p>
        )}
      </div>

      <label className="login-reveal-block flex items-center gap-2 text-sm text-muted [--login-reveal-y:-58px] [animation-delay:460ms]">
        <input
          className="size-4 rounded border-line accent-brand-500"
          type="checkbox"
          {...register('rememberMe')}
        />
        Lembrar de mim
      </label>

      {loginMutation.isError && (
        <p
          className="rounded-control border border-danger/20 bg-red-50 px-3 py-2 text-sm text-danger"
          role="alert"
        >
          {getApiErrorMessage(loginMutation.error)}
        </p>
      )}

      <Button className="login-reveal-block w-full [--login-reveal-y:-86px] [animation-delay:540ms]" type="submit" disabled={loginMutation.isPending}>
        {loginMutation.isPending ? 'Entrando...' : 'Entrar'}
      </Button>
    </form>
  )
}
