import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, IdCard, Mail, PenLine, Phone, Save, Send, UserRound } from 'lucide-react'
import { type FormEvent, useEffect, useState } from 'react'
import { toast } from 'sonner'

import { getApiErrorMessage } from '../../../services/api/api-error'
import { Button } from '../../../shared/ui/button'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { PageSection } from '../../../shared/ui/page-section'
import { SignaturePadField } from '../../../shared/ui/signature-pad-field'
import { requestEmailVerification } from '../api/email-verification-api'
import { getProfileSettings, updateProfileSettings } from '../api/profile-settings-api'
import { getSignatureSettings, updateSignatureSettings } from '../api/signature-settings-api'

export function SettingsPage() {
  const queryClient = useQueryClient()
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [cpf, setCpf] = useState('')
  const [telefone, setTelefone] = useState('')
  const [nomeSocial, setNomeSocial] = useState('')
  const [dataNascimento, setDataNascimento] = useState('')
  const [nomeAssinatura, setNomeAssinatura] = useState('')
  const [assinaturaBase64, setAssinaturaBase64] = useState<string | null>(null)

  const profileQuery = useQuery({ queryKey: ['profile-settings'], queryFn: getProfileSettings })
  const signatureQuery = useQuery({ queryKey: ['signature-settings'], queryFn: getSignatureSettings })

  useEffect(() => {
    if (profileQuery.data) {
      setNome(profileQuery.data.nome ?? '')
      setEmail(profileQuery.data.email ?? '')
      setCpf(profileQuery.data.cpf ?? '')
      setTelefone(profileQuery.data.telefone ?? '')
      setNomeSocial(profileQuery.data.nomeSocial ?? '')
      setDataNascimento(profileQuery.data.dataNascimento ?? '')
    }
  }, [profileQuery.data])

  useEffect(() => {
    if (signatureQuery.data) {
      setNomeAssinatura(signatureQuery.data.nomeAssinatura ?? '')
      setAssinaturaBase64(signatureQuery.data.assinaturaManuscritaBase64)
    }
  }, [signatureQuery.data])

  const updateProfileMutation = useMutation({
    mutationFn: () => updateProfileSettings({
      nome: nome.trim(),
      email: email.trim(),
      cpf: cpf.trim() || null,
      telefone: telefone.trim() || null,
      nomeSocial: nomeSocial.trim() || null,
      dataNascimento: dataNascimento || null,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile-settings'] })
      queryClient.invalidateQueries({ queryKey: ['auth', 'me'] })
      toast.success('Cadastro atualizado.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const updateMutation = useMutation({
    mutationFn: () => updateSignatureSettings({
      nomeAssinatura: nomeAssinatura.trim() || null,
      assinaturaManuscritaBase64: assinaturaBase64,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['signature-settings'] })
      toast.success('Assinatura salva.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const requestEmailVerificationMutation = useMutation({
    mutationFn: requestEmailVerification,
    onSuccess: (response) => {
      toast.success(response.mensagem)
      queryClient.invalidateQueries({ queryKey: ['profile-settings'] })
      queryClient.invalidateQueries({ queryKey: ['auth', 'me'] })
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  function submitProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    updateProfileMutation.mutate()
  }

  function submitSignature(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    updateMutation.mutate()
  }

  return (
    <div className="grid gap-4 2xl:gap-5">
      <section>
        <p className="text-sm font-medium text-ink sm:text-base">Complete seu cadastro e sua assinatura.</p>
        <p className="mt-1 text-sm text-muted">
          Esses dados pertencem ao seu usuário e ajudam no recebimento seguro de links por e-mail.
        </p>
      </section>

      <section className="grid gap-4">
        <PageSection title="Dados cadastrais">
          <form className="grid gap-4 p-4" onSubmit={submitProfile}>
            {profileQuery.isLoading && <InlineMessage message="Carregando seu cadastro..." />}
            {profileQuery.isError && <InlineMessage tone="error" message={getApiErrorMessage(profileQuery.error)} />}

            <div className="grid gap-4 md:grid-cols-2">
              <label className="grid gap-2 text-sm font-medium text-ink">
                Nome completo
                <div className="relative">
                  <UserRound className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" size={17} aria-hidden="true" />
                  <input
                    className="min-h-11 w-full rounded-control border border-line bg-white pl-10 pr-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                    value={nome}
                    onChange={(event) => setNome(event.target.value)}
                    placeholder="Digite seu nome completo"
                    maxLength={150}
                    required
                  />
                </div>
              </label>

              <label className="grid gap-2 text-sm font-medium text-ink">
                Nome social
                <input
                  className="min-h-11 rounded-control border border-line bg-white px-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                  value={nomeSocial}
                  onChange={(event) => setNomeSocial(event.target.value)}
                  placeholder="Opcional"
                  maxLength={150}
                />
              </label>

              <label className="grid gap-2 text-sm font-medium text-ink">
                E-mail
                <div className="relative">
                  <Mail className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" size={17} aria-hidden="true" />
                  <input
                    className="min-h-11 w-full rounded-control border border-line bg-white pl-10 pr-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="Digite seu e-mail"
                    maxLength={150}
                    required
                  />
                </div>
              </label>

              <label className="grid gap-2 text-sm font-medium text-ink">
                  Telefone
                <div className="relative">
                  <Phone className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" size={17} aria-hidden="true" />
                  <input
                    className="min-h-11 w-full rounded-control border border-line bg-white pl-10 pr-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                    value={telefone}
                    onChange={(event) => setTelefone(event.target.value)}
                    placeholder="Ex.: 85999999999"
                    maxLength={30}
                  />
                </div>
              </label>

              <label className="grid gap-2 text-sm font-medium text-ink">
                CPF
                <div className="relative">
                  <IdCard className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" size={17} aria-hidden="true" />
                  <input
                    className="min-h-11 w-full rounded-control border border-line bg-white pl-10 pr-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                    value={cpf}
                    onChange={(event) => setCpf(event.target.value)}
                    placeholder="Digite seu CPF"
                    maxLength={20}
                  />
                </div>
              </label>

              <label className="grid gap-2 text-sm font-medium text-ink">
                Data de nascimento
                <input
                  className="min-h-11 rounded-control border border-line bg-white px-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                  type="date"
                  value={dataNascimento}
                  onChange={(event) => setDataNascimento(event.target.value)}
                />
              </label>
            </div>

            <div className="grid gap-3 rounded-card border border-line bg-surface-page/70 p-3 text-xs leading-5 text-muted md:grid-cols-[minmax(0,1fr)_auto] md:items-center">
              <div className="grid gap-2 sm:grid-cols-2">
                <span className="inline-flex items-center gap-2">
                  <span className={profileQuery.data?.emailVerificado ? 'text-success' : 'text-warning'}>
                    E-mail:
                  </span>
                  {profileQuery.data?.emailVerificado ? 'verificado' : 'pendente de verificação'}
                </span>
                <span>
                  Telefone: {profileQuery.data?.telefoneVerificado ? 'verificado' : 'pendente de verificação'}
                </span>
              </div>

              {!profileQuery.data?.emailVerificado && (
                <Button
                  className="min-h-9 px-3 text-xs"
                  type="button"
                  variant="secondary"
                  disabled={requestEmailVerificationMutation.isPending || profileQuery.isLoading}
                  onClick={() => requestEmailVerificationMutation.mutate()}
                >
                  <Send aria-hidden="true" size={14} />
                  {requestEmailVerificationMutation.isPending ? 'Enviando...' : 'Enviar confirmação'}
                </Button>
              )}
            </div>

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="submit" disabled={updateProfileMutation.isPending}>
                <Save aria-hidden="true" size={17} />
                {updateProfileMutation.isPending ? 'Salvando...' : 'Salvar cadastro'}
              </Button>
            </div>
          </form>
        </PageSection>

      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem] 2xl:gap-5">
        <PageSection title="Sua assinatura">
          <form className="grid gap-4 p-4" onSubmit={submitSignature}>
            {signatureQuery.isLoading && <InlineMessage message="Carregando assinatura salva..." />}
            {signatureQuery.isError && <InlineMessage tone="error" message={getApiErrorMessage(signatureQuery.error)} />}

            <label className="grid gap-2 text-sm font-medium text-ink">
              Nome exibido no carimbo
              <input
                className="min-h-11 rounded-control border border-line bg-white px-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                value={nomeAssinatura}
                onChange={(event) => setNomeAssinatura(event.target.value)}
                placeholder="Seu nome como aparecerá no documento"
                maxLength={150}
              />
            </label>

            <SignaturePadField
              enabled
              onEnabledChange={() => undefined}
              value={assinaturaBase64}
              onChange={setAssinaturaBase64}
              showToggle={false}
              label="Desenhe sua assinatura"
              description="Ela ficará salva como seu carimbo manuscrito digital para uso opcional na assinatura dos documentos."
            />

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="secondary" onClick={() => setAssinaturaBase64(null)}>
                Limpar assinatura
              </Button>
              <Button type="submit" disabled={updateMutation.isPending}>
                <Save aria-hidden="true" size={17} />
                {updateMutation.isPending ? 'Salvando...' : 'Salvar assinatura'}
              </Button>
            </div>
          </form>
        </PageSection>

        <PageSection title="Prévia do carimbo">
          <div className="p-4">
            <article className="rounded-card border border-line bg-white p-4 shadow-card">
              <div className="mb-4 flex items-center gap-2 text-sm text-muted">
                <PenLine aria-hidden="true" size={17} />
                Como aparecerá no relatório de assinaturas
              </div>
              <div className="grid gap-3 rounded-control border border-line bg-surface-page/70 p-4">
                <div className="h-24 rounded-control bg-white p-3">
                  {assinaturaBase64 ? (
                    <img src={assinaturaBase64} alt="Prévia da assinatura" className="mx-auto h-full max-w-full object-contain" />
                  ) : (
                    <p className="grid h-full place-items-center text-sm text-muted">Sem assinatura desenhada</p>
                  )}
                </div>
                <div className="border-t border-line pt-3">
                  <p className="text-sm font-semibold text-ink">{nomeAssinatura || 'Seu nome'}</p>
                  <p className="mt-1 text-xs text-muted">Assinado digitalmente na Xsign por {nomeAssinatura || 'Seu nome'}</p>
                </div>
              </div>

              {signatureQuery.data?.possuiAssinaturaManuscrita && (
                <p className="mt-4 inline-flex items-center gap-2 rounded-control bg-green-50 px-3 py-2 text-xs font-medium text-success">
                  <CheckCircle2 aria-hidden="true" size={15} />
                  Assinatura manuscrita salva
                </p>
              )}
            </article>
          </div>
        </PageSection>
      </section>
    </div>
  )
}
