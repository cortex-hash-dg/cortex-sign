import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  CheckCircle2,
  Clock3,
  FileText,
  LockKeyhole,
  Mail,
  ShieldCheck,
  XCircle,
} from 'lucide-react'
import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { toast } from 'sonner'

import xsignLogo from '../../../assets/brand/xsign-logo-login.png'
import loginPanelImage from '../../../assets/login/xsign-login.png'
import { getApiErrorMessage } from '../../../services/api/api-error'
import { Button } from '../../../shared/ui/button'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { SignaturePadField } from '../../../shared/ui/signature-pad-field'
import { StatusBadge } from '../../../shared/ui/status-badge'
import type { SignatureRequest, SignatureRequestStatus } from '../../documents/api/documents-api'
import {
  getPublicSignature,
  requestExternalSignerAccess,
  rejectWithToken,
  signWithToken,
  validateExternalSignerAccess,
  type PublicSignature,
} from '../api/public-signature-api'

const requestStatusLabels: Record<SignatureRequestStatus, string> = {
  PENDENTE: 'Pendente',
  ASSINADA: 'Assinada',
  REJEITADA: 'Rejeitada',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada',
}

function requestStatusTone(status: SignatureRequestStatus) {
  if (status === 'ASSINADA') {
    return 'green' as const
  }

  if (status === 'PENDENTE') {
    return 'orange' as const
  }

  if (status === 'REJEITADA' || status === 'CANCELADA' || status === 'EXPIRADA') {
    return 'red' as const
  }

  return 'slate' as const
}

function formatDate(value?: string | null) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function isExpired(signature?: PublicSignature) {
  if (!signature?.expiraEm) {
    return false
  }

  return new Date(signature.expiraEm).getTime() < Date.now()
}

function canSign(signature?: PublicSignature) {
  return Boolean(signature && signature.status === 'PENDENTE' && !isExpired(signature))
}

function getResultTitle(result: SignatureRequest) {
  if (result.status === 'ASSINADA') {
    return 'Documento assinado com sucesso.'
  }

  if (result.status === 'REJEITADA') {
    return 'Assinatura rejeitada.'
  }

  return 'Solicitação atualizada.'
}

export function PublicSignaturePage() {
  const { token = '' } = useParams()
  const queryClient = useQueryClient()
  const [motivo, setMotivo] = useState('')
  const [showRejectForm, setShowRejectForm] = useState(false)
  const [assinaturaManuscritaAtiva, setAssinaturaManuscritaAtiva] = useState(false)
  const [assinaturaManuscritaBase64, setAssinaturaManuscritaBase64] = useState<string | null>(null)
  const [cpfAcesso, setCpfAcesso] = useState('')
  const [codigoAcesso, setCodigoAcesso] = useState('')
  const [tokenAcessoExterno, setTokenAcessoExterno] = useState<string | null>(null)
  const [result, setResult] = useState<SignatureRequest | null>(null)

  const externalAccessStorageKey = token ? `xsign:external-signer-access:${token}` : ''

  useEffect(() => {
    if (!externalAccessStorageKey) {
      setTokenAcessoExterno(null)
      return
    }

    setTokenAcessoExterno(localStorage.getItem(externalAccessStorageKey))
  }, [externalAccessStorageKey])

  const publicSignatureQuery = useQuery({
    queryKey: ['public-signature', token],
    queryFn: () => getPublicSignature(token),
    enabled: Boolean(token),
    retry: false,
  })

  const signature = publicSignatureQuery.data
  const signatureIsExpired = useMemo(() => isExpired(signature), [signature])
  const signatureCanBeSigned = useMemo(() => canSign(signature), [signature])
  const externalAccessRequired = Boolean(signature?.acessoExternoObrigatorio && !tokenAcessoExterno)

  const signMutation = useMutation({
    mutationFn: () => signWithToken(token, {
      tipo: 'LINK_TOKEN',
      assinaturaManuscritaBase64: assinaturaManuscritaAtiva ? assinaturaManuscritaBase64 ?? undefined : undefined,
      tokenAcessoExterno: signature?.acessoExternoObrigatorio ? tokenAcessoExterno ?? undefined : undefined,
    }),
    onSuccess: (response) => {
      setResult(response)
      queryClient.invalidateQueries({ queryKey: ['public-signature', token] })
      toast.success('Documento assinado.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const rejectMutation = useMutation({
    mutationFn: () => rejectWithToken(token, {
      motivo: motivo.trim(),
      tokenAcessoExterno: signature?.acessoExternoObrigatorio ? tokenAcessoExterno ?? undefined : undefined,
    }),
    onSuccess: (response) => {
      setResult(response)
      queryClient.invalidateQueries({ queryKey: ['public-signature', token] })
      toast.success('Rejeição registrada.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const requestAccessMutation = useMutation({
    mutationFn: () => requestExternalSignerAccess(token, {
      cpf: cpfAcesso.trim(),
      canal: 'EMAIL',
    }),
    onSuccess: (response) => {
      toast.success(response.mensagem)
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const validateAccessMutation = useMutation({
    mutationFn: () => validateExternalSignerAccess(token, {
      cpf: cpfAcesso.trim(),
      codigo: codigoAcesso.trim(),
    }),
    onSuccess: (response) => {
      setTokenAcessoExterno(response.tokenAcesso)
      if (externalAccessStorageKey) {
        localStorage.setItem(externalAccessStorageKey, response.tokenAcesso)
      }
      toast.success(response.mensagem)
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  function submitSign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    signMutation.mutate()
  }

  function submitReject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    rejectMutation.mutate()
  }

  function submitRequestAccess(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    requestAccessMutation.mutate()
  }

  function submitValidateAccess(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    validateAccessMutation.mutate()
  }

  return (
    <main className="min-h-screen bg-surface-page text-ink [min-height:100svh]">
      <section className="grid min-h-screen [min-height:100svh] lg:grid-cols-[minmax(0,1fr)_28rem] xl:grid-cols-[minmax(0,1fr)_32rem]">
        <div className="relative hidden overflow-hidden bg-brand-900 lg:block">
          <img src={loginPanelImage} alt="" className="absolute inset-0 h-full w-full object-cover" />
          <div className="absolute inset-0 bg-gradient-to-r from-brand-900/80 via-brand-900/42 to-brand-900/16" />

          <div className="relative z-10 flex h-full flex-col justify-between p-10 2xl:p-14">
            <Link className="inline-flex w-fit" to="/login" aria-label="Ir para login Xsign">
              <img src={xsignLogo} alt="Xsign" className="h-9 w-auto brightness-0 invert" />
            </Link>

            <div className="max-w-xl">
              <p className="mb-4 text-xs font-semibold uppercase tracking-[0.24em] text-blue-100/72">
                Assinatura eletrônica
              </p>
              <h1 className="text-4xl font-semibold leading-tight tracking-tight text-white 2xl:text-5xl">
                Valide sua intenção de assinar com segurança.
              </h1>
              <p className="mt-5 max-w-lg text-sm leading-6 text-blue-50/76">
                O Xsign registra token seguro, data, IP e evidências técnicas para manter rastreabilidade do aceite.
              </p>
            </div>

            <div className="grid max-w-xl gap-3 sm:grid-cols-3">
              <div className="rounded-card border border-white/12 bg-white/[0.08] p-4 text-blue-50/86 backdrop-blur-xl">
                <ShieldCheck size={20} aria-hidden="true" />
                <p className="mt-3 text-xs font-medium">Evidência auditável</p>
              </div>
              <div className="rounded-card border border-white/12 bg-white/[0.08] p-4 text-blue-50/86 backdrop-blur-xl">
                <Mail size={20} aria-hidden="true" />
                <p className="mt-3 text-xs font-medium">Link seguro</p>
              </div>
              <div className="rounded-card border border-white/12 bg-white/[0.08] p-4 text-blue-50/86 backdrop-blur-xl">
                <LockKeyhole size={20} aria-hidden="true" />
                <p className="mt-3 text-xs font-medium">Token protegido</p>
              </div>
            </div>
          </div>
        </div>

        <div className="flex min-h-screen items-center justify-center px-4 py-6 [min-height:100svh] sm:px-6 lg:px-8">
          <div className="w-full max-w-xl">
            <div className="mb-6 flex items-center justify-between gap-4 lg:hidden">
              <img src={xsignLogo} alt="Xsign" className="h-8 w-auto" />
              <Link className="text-sm font-medium text-brand-500" to="/login">Área interna</Link>
            </div>

            <section className="rounded-[22px] border border-line bg-white p-5 shadow-[0_24px_70px_rgba(6,38,85,0.10)] sm:p-7 2xl:p-8">
              {publicSignatureQuery.isLoading && (
                <InlineMessage message="Carregando solicitação de assinatura..." />
              )}

              {publicSignatureQuery.isError && (
                <div className="grid gap-4 text-center">
                  <div className="mx-auto grid size-14 place-items-center rounded-full bg-red-50 text-danger">
                    <XCircle aria-hidden="true" size={26} />
                  </div>
                  <div>
                    <h1 className="text-xl font-semibold text-ink">Link de assinatura indisponível</h1>
                    <p className="mt-2 text-sm leading-6 text-muted">{getApiErrorMessage(publicSignatureQuery.error)}</p>
                  </div>
                  <Link className="text-sm font-medium text-brand-500" to="/login">Ir para a área interna</Link>
                </div>
              )}

              {signature && !result && (
                <div className="grid gap-5">
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <StatusBadge label={signatureIsExpired ? 'Expirada' : requestStatusLabels[signature.status]} tone={signatureIsExpired ? 'red' : requestStatusTone(signature.status)} />
                      <h1 className="mt-4 text-2xl font-semibold leading-tight tracking-tight text-ink">Assinar documento</h1>
                      <p className="mt-2 text-sm leading-6 text-muted">Confira os dados abaixo e confirme sua intenção de assinar pelo link seguro recebido.</p>
                    </div>
                    <div className="grid size-12 shrink-0 place-items-center rounded-control bg-brand-50 text-brand-500">
                      <FileText aria-hidden="true" size={24} />
                    </div>
                  </div>

                  <div className="grid gap-3 rounded-card border border-line bg-surface-page/60 p-4 text-sm">
                    <div className="flex items-start gap-3">
                      <FileText className="mt-0.5 shrink-0 text-brand-500" size={18} aria-hidden="true" />
                      <div className="min-w-0">
                        <p className="text-xs font-medium uppercase tracking-[0.12em] text-muted">Documento</p>
                        <p className="mt-1 break-words font-semibold text-ink">{signature.documentoTitulo}</p>
                      </div>
                    </div>
                    <div className="flex items-start gap-3">
                      <Mail className="mt-0.5 shrink-0 text-brand-500" size={18} aria-hidden="true" />
                      <div className="min-w-0">
                        <p className="text-xs font-medium uppercase tracking-[0.12em] text-muted">Signatário</p>
                        <p className="mt-1 font-semibold text-ink">{signature.signatarioNome}</p>
                        <p className="mt-0.5 break-all text-muted">{signature.signatarioEmail}</p>
                      </div>
                    </div>
                    <div className="flex items-start gap-3">
                      <Clock3 className="mt-0.5 shrink-0 text-brand-500" size={18} aria-hidden="true" />
                      <div>
                        <p className="text-xs font-medium uppercase tracking-[0.12em] text-muted">Validade</p>
                        <p className="mt-1 font-semibold text-ink">{formatDate(signature.expiraEm)}</p>
                      </div>
                    </div>
                  </div>

                  {!signatureCanBeSigned && (
                    <InlineMessage
                      tone="error"
                      message={signatureIsExpired ? 'Esta solicitação expirou e não pode mais ser assinada.' : 'Esta solicitação não está pendente para assinatura.'}
                    />
                  )}

                  {signatureCanBeSigned && externalAccessRequired && (
                    <div className="overflow-hidden rounded-[20px] border border-brand-100 bg-gradient-to-br from-brand-50 via-white to-white shadow-[0_18px_46px_rgba(6,38,85,0.10)]">
                      <div className="border-b border-brand-100 bg-brand-900 px-5 py-5 text-white">
                        <div className="flex items-start gap-3">
                          <div className="grid size-11 shrink-0 place-items-center rounded-control bg-white/12 text-white ring-1 ring-white/18">
                            <LockKeyhole aria-hidden="true" size={22} />
                          </div>
                          <div>
                            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-100/80">Acesso externo</p>
                            <h2 className="mt-2 text-xl font-semibold leading-tight">Confirme sua identidade para continuar</h2>
                            <p className="mt-2 text-sm leading-6 text-blue-50/78">
                              Você verá somente os documentos associados ao CPF informado neste convite.
                            </p>
                          </div>
                        </div>
                      </div>

                      <form className="grid gap-3 p-5" onSubmit={submitRequestAccess}>
                        <label className="grid gap-2 text-sm font-medium text-ink">
                          CPF
                          <input
                            className="min-h-11 rounded-control border border-line bg-white px-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                            value={cpfAcesso}
                            onChange={(event) => setCpfAcesso(event.target.value)}
                            placeholder="Digite seu CPF"
                            maxLength={20}
                            required
                          />
                        </label>

                        <div className="rounded-control border border-brand-100 bg-brand-50 px-3 py-2 text-sm font-medium text-brand-900">
                          O código de acesso será enviado para o e-mail cadastrado no convite.
                        </div>

                        <Button type="submit" disabled={requestAccessMutation.isPending || !cpfAcesso.trim()}>
                          {requestAccessMutation.isPending ? 'Enviando...' : 'Enviar código de acesso'}
                        </Button>
                      </form>

                      <form className="grid gap-3 border-t border-line bg-white/70 p-5" onSubmit={submitValidateAccess}>
                        <label className="grid gap-2 text-sm font-medium text-ink">
                          Código recebido
                          <input
                            className="min-h-11 rounded-control border border-line bg-white px-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                            value={codigoAcesso}
                            onChange={(event) => setCodigoAcesso(event.target.value)}
                            placeholder="Digite o código"
                            maxLength={12}
                            required
                          />
                        </label>

                        <Button type="submit" disabled={validateAccessMutation.isPending || !cpfAcesso.trim() || !codigoAcesso.trim()}>
                          <LockKeyhole aria-hidden="true" size={17} />
                          {validateAccessMutation.isPending ? 'Validando...' : 'Liberar assinatura'}
                        </Button>
                      </form>
                    </div>
                  )}

                  {signatureCanBeSigned && !externalAccessRequired && (
                    <div className="grid gap-4">
                      <form className="grid gap-4" onSubmit={submitSign}>
                        <div className="rounded-card border border-blue-100 bg-blue-50/70 p-4 text-sm leading-6 text-muted">
                          <p className="font-semibold text-ink">Assinatura por link seguro</p>
                          <p className="mt-1">
                            Ao clicar em assinar, o Xsign registra o token do convite, data, hora, IP e evidências técnicas do aceite.
                          </p>
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

                        <Button type="submit" disabled={signMutation.isPending || (assinaturaManuscritaAtiva && !assinaturaManuscritaBase64)}>
                          <CheckCircle2 aria-hidden="true" size={18} />
                          {signMutation.isPending ? 'Assinando...' : 'Assinar documento'}
                        </Button>
                      </form>

                      <div className="border-t border-line pt-4">
                        {!showRejectForm ? (
                          <button
                            className="text-sm font-medium text-danger transition-colors hover:text-red-700"
                            type="button"
                            onClick={() => setShowRejectForm(true)}
                          >
                            Rejeitar assinatura
                          </button>
                        ) : (
                          <form className="grid gap-3" onSubmit={submitReject}>
                            <label className="grid gap-2 text-sm font-medium text-ink">
                              Motivo da rejeição
                              <textarea
                                className="min-h-24 resize-none rounded-control border border-line bg-white px-3 py-2 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                                value={motivo}
                                onChange={(event) => setMotivo(event.target.value)}
                                placeholder="Explique rapidamente por que não deseja assinar."
                                maxLength={500}
                                required
                              />
                            </label>
                            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                              <Button type="button" variant="secondary" onClick={() => setShowRejectForm(false)}>
                                Cancelar
                              </Button>
                              <Button type="submit" variant="danger" disabled={rejectMutation.isPending || !motivo.trim()}>
                                <XCircle aria-hidden="true" size={18} />
                                {rejectMutation.isPending ? 'Registrando...' : 'Confirmar rejeição'}
                              </Button>
                            </div>
                          </form>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {result && (
                <div className="grid gap-5 text-center">
                  <div className="mx-auto grid size-16 place-items-center rounded-full bg-green-50 text-success">
                    {result.status === 'REJEITADA' ? <XCircle aria-hidden="true" size={30} /> : <CheckCircle2 aria-hidden="true" size={30} />}
                  </div>
                  <div>
                    <StatusBadge label={requestStatusLabels[result.status]} tone={requestStatusTone(result.status)} />
                    <h1 className="mt-4 text-2xl font-semibold text-ink">{getResultTitle(result)}</h1>
                    <p className="mt-2 text-sm leading-6 text-muted">A solicitação foi atualizada e as evidências foram registradas pelo Cortex Sign.</p>
                  </div>

                  <div className="grid gap-2 rounded-card border border-line bg-surface-page/60 p-4 text-left text-sm text-muted">
                    <p><strong className="font-medium text-ink">Documento:</strong> {result.documentoTitulo}</p>
                    <p><strong className="font-medium text-ink">Signatário:</strong> {result.signatarioNome}</p>
                    <p><strong className="font-medium text-ink">Atualizado em:</strong> {formatDate(result.atualizadoEm)}</p>
                    {result.assinatura?.protocolo && <p><strong className="font-medium text-ink">Protocolo:</strong> {result.assinatura.protocolo}</p>}
                    {result.assinatura?.documentoHashSha256 && <p className="break-all"><strong className="font-medium text-ink">Hash do documento:</strong> {result.assinatura.documentoHashSha256}</p>}
                    {result.assinatura?.evidenciaHashSha256 && <p className="break-all"><strong className="font-medium text-ink">Hash da evidência:</strong> {result.assinatura.evidenciaHashSha256}</p>}
                  </div>

                  <button
                    className="text-sm font-medium text-brand-500"
                    type="button"
                    onClick={() => {
                      setResult(null)
                      publicSignatureQuery.refetch()
                    }}
                  >
                    Ver status atualizado
                  </button>
                </div>
              )}
            </section>
          </div>
        </div>
      </section>
    </main>
  )
}
