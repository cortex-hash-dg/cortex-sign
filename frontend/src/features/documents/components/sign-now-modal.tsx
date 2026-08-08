import { useMutation, useQuery } from '@tanstack/react-query'
import { CheckCircle2, X } from 'lucide-react'
import { type FormEvent, useEffect, useState } from 'react'
import { toast } from 'sonner'

import { signWithToken } from '../../public-signature/api/public-signature-api'
import { getApiErrorMessage } from '../../../services/api/api-error'
import { Button } from '../../../shared/ui/button'
import { SignaturePadField } from '../../../shared/ui/signature-pad-field'
import { getSignatureSettings } from '../../settings/api/signature-settings-api'
import { StatusBadge } from '../../../shared/ui/status-badge'
import type { SignatureRequest } from '../api/documents-api'
import {
  formatDate,
  signatureRequestStatusLabels,
  signatureRequestStatusTone,
} from '../lib/document-ui'

type SignNowModalProps = {
  request: SignatureRequest | null
  onClose: () => void
  onSigned: (request: SignatureRequest) => void
}

export function SignNowModal({ request, onClose, onSigned }: SignNowModalProps) {
  const [assinaturaManuscritaAtiva, setAssinaturaManuscritaAtiva] = useState(false)
  const [assinaturaManuscritaBase64, setAssinaturaManuscritaBase64] = useState<string | null>(null)

  useEffect(() => {
    setAssinaturaManuscritaAtiva(false)
    setAssinaturaManuscritaBase64(null)
  }, [request?.id])

  const signatureSettingsQuery = useQuery({
    queryKey: ['signature-settings'],
    queryFn: getSignatureSettings,
    enabled: Boolean(request),
  })

  const assinaturaSalva = signatureSettingsQuery.data?.assinaturaManuscritaBase64

  const signMutation = useMutation({
    mutationFn: () => {
      if (!request?.token) {
        throw new Error('Solicitação sem token de assinatura.')
      }

      return signWithToken(request.token, {
        tipo: 'LINK_TOKEN',
        assinaturaManuscritaBase64: assinaturaManuscritaAtiva ? assinaturaManuscritaBase64 ?? undefined : undefined,
      })
    },
    onSuccess: (signedRequest) => {
      toast.success('Documento assinado.')
      onSigned(signedRequest)
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  if (!request) {
    return null
  }

  function submitSign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    signMutation.mutate()
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-brand-900/45 px-3 py-4 backdrop-blur-sm sm:px-4">
      <section className="w-full max-w-3xl overflow-hidden rounded-card border border-line bg-white shadow-[0_24px_70px_rgba(6,38,85,0.22)]">
        <div className="flex items-start justify-between gap-4 border-b border-line px-4 py-4 sm:px-5">
          <div className="min-w-0">
            <StatusBadge
              label={signatureRequestStatusLabels[request.status]}
              tone={signatureRequestStatusTone(request.status)}
            />
            <h2 className="mt-3 text-lg font-semibold text-ink sm:text-xl">Confirmar assinatura</h2>
            <p className="mt-1 max-w-2xl text-sm leading-6 text-muted">
              Confira os dados do documento e escolha se deseja incluir sua assinatura manuscrita no manifesto visual.
            </p>
          </div>
          <button
            className="grid size-9 shrink-0 place-items-center rounded-control text-muted transition-colors hover:bg-surface-page hover:text-ink"
            type="button"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            <X aria-hidden="true" size={18} />
          </button>
        </div>

        <form className="grid gap-4 p-4 sm:p-5" onSubmit={submitSign}>
          <div className="grid gap-4">
            <div className="grid gap-4">
              <div className="grid gap-3 rounded-card border border-line bg-surface-page/60 p-4">
                <p className="text-sm font-semibold text-ink">Dados do documento</p>
                <dl className="grid gap-2 text-sm text-muted">
                  <div className="grid gap-1 sm:grid-cols-[7rem_minmax(0,1fr)]">
                    <dt className="font-medium text-ink">Documento</dt>
                    <dd className="min-w-0 break-words">{request.documentoTitulo}</dd>
                  </div>
                  <div className="grid gap-1 sm:grid-cols-[7rem_minmax(0,1fr)]">
                    <dt className="font-medium text-ink">Signatário</dt>
                    <dd className="min-w-0 break-words">{request.signatarioNome}</dd>
                  </div>
                  <div className="grid gap-1 sm:grid-cols-[7rem_minmax(0,1fr)]">
                    <dt className="font-medium text-ink">E-mail</dt>
                    <dd className="min-w-0 break-words">{request.signatarioEmail}</dd>
                  </div>
                  <div className="grid gap-1 sm:grid-cols-[7rem_minmax(0,1fr)]">
                    <dt className="font-medium text-ink">Expira em</dt>
                    <dd>{formatDate(request.expiraEm)}</dd>
                  </div>
                </dl>
              </div>

              {assinaturaSalva && (
                <div className="flex flex-col gap-3 rounded-card border border-line bg-surface-page/60 p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm font-semibold text-ink">Assinatura salva</p>
                    <p className="mt-1 text-xs leading-5 text-muted">Use o carimbo configurado em Configurações &gt; Sua assinatura.</p>
                  </div>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      setAssinaturaManuscritaAtiva(true)
                      setAssinaturaManuscritaBase64(assinaturaSalva)
                    }}
                  >
                    Usar assinatura salva
                  </Button>
                </div>
              )}
            </div>
          </div>

          <SignaturePadField
            enabled={assinaturaManuscritaAtiva}
            onEnabledChange={setAssinaturaManuscritaAtiva}
            value={assinaturaManuscritaBase64}
            onChange={setAssinaturaManuscritaBase64}
          />

          {assinaturaManuscritaAtiva && !assinaturaManuscritaBase64 && (
            <p className="text-xs font-medium text-danger">Desenhe a assinatura ou desative a opção manuscrita.</p>
          )}

          <div className="flex flex-col-reverse gap-2 border-t border-line pt-4 sm:flex-row sm:justify-end">
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={signMutation.isPending || request.status !== 'PENDENTE' || (assinaturaManuscritaAtiva && !assinaturaManuscritaBase64)}>
              <CheckCircle2 aria-hidden="true" size={18} />
              {signMutation.isPending ? 'Assinando...' : 'Assinar documento'}
            </Button>
          </div>
        </form>
      </section>
    </div>
  )
}
