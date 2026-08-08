import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, Loader2, MailCheck, TriangleAlert } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'

import xsignLogo from '../../../assets/brand/xsign-logo-login.png'
import { getApiErrorMessage } from '../../../services/api/api-error'
import { confirmEmailVerification } from '../api/email-verification-api'

export function ConfirmEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')?.trim() ?? ''

  const confirmQuery = useQuery({
    queryKey: ['confirm-email', token],
    queryFn: () => confirmEmailVerification(token),
    enabled: token.length > 0,
    retry: false,
  })

  const isMissingToken = token.length === 0
  const isSuccess = confirmQuery.isSuccess
  const isError = isMissingToken || confirmQuery.isError

  return (
    <main className="grid min-h-screen place-items-center bg-surface-page px-4 py-8 text-ink [min-height:100dvh]">
      <section className="w-full max-w-lg overflow-hidden rounded-card border border-line bg-white shadow-card">
        <header className="border-b border-line px-5 py-7 text-center sm:px-8">
          <img src={xsignLogo} alt="Xsign" className="mx-auto w-40 max-w-full sm:w-48" />
          <div className="mx-auto mt-6 grid size-12 place-items-center rounded-full bg-blue-50 text-brand-500">
            {confirmQuery.isLoading && <Loader2 className="animate-spin" size={24} aria-hidden="true" />}
            {isSuccess && <CheckCircle2 size={24} aria-hidden="true" />}
            {isError && <TriangleAlert size={24} aria-hidden="true" />}
            {!confirmQuery.isLoading && !isSuccess && !isError && <MailCheck size={24} aria-hidden="true" />}
          </div>
          <h1 className="mt-4 text-2xl font-semibold tracking-tight text-ink">
            {isSuccess ? 'E-mail confirmado' : 'Confirmação de e-mail'}
          </h1>
          <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-muted">
            {confirmQuery.isLoading && 'Estamos validando seu link de confirmação.'}
            {isSuccess && (confirmQuery.data?.mensagem ?? 'Seu e-mail foi confirmado com sucesso.')}
            {isMissingToken && 'O link de confirmação está incompleto. Solicite um novo link nas configurações da conta.'}
            {confirmQuery.isError && getApiErrorMessage(confirmQuery.error)}
            {!confirmQuery.isLoading && !isSuccess && !isError && 'Abra o link recebido por e-mail para confirmar sua conta.'}
          </p>
        </header>

        <div className="grid gap-3 px-5 py-5 sm:px-8">
          <Link
            className="inline-flex min-h-11 items-center justify-center rounded-control border border-brand-500 bg-brand-500 px-4 text-sm font-medium text-white transition-colors duration-150 hover:border-brand-600 hover:bg-brand-600"
            to="/login"
          >
            Voltar para o login
          </Link>
          <Link className="text-center text-sm font-medium text-brand-500 transition-colors hover:text-brand-600" to="/app/configuracoes">
            Ir para configurações
          </Link>
        </div>
      </section>
    </main>
  )
}
