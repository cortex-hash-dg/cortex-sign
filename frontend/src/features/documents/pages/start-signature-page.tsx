import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  CheckCircle2,
  Mail,
  Pencil,
  Plus,
  Send,
  Trash2,
  UploadCloud,
  UserRound,
} from 'lucide-react'
import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'

import { getApiErrorMessage } from '../../../services/api/api-error'
import { cn } from '../../../shared/lib/cn'
import { Button } from '../../../shared/ui/button'
import { EmptyState } from '../../../shared/ui/empty-state'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { PageSection } from '../../../shared/ui/page-section'
import { StatusBadge } from '../../../shared/ui/status-badge'
import { useCurrentUser } from '../../auth/hooks/use-current-user'
import { listOrganizations } from '../../organizations/api/organizations-api'
import { SignNowModal } from '../components/sign-now-modal'
import {
  createDocument,
  createSignatureRequest,
  createSigner,
  deleteSigner,
  getDocument,
  listSignatureRequests,
  listSigners,
  sendSignatureInvitation,
  type SignatureRequest,
  type SignatureInvitationChannel,
  type Signer,
  type SignerType,
  type DocumentDetail,
  updateSigner,
} from '../api/documents-api'
import {
  documentStatusLabels,
  documentStatusTone,
  formatDate,
  formatSize,
  signatureRequestStatusLabels,
  signatureRequestStatusTone,
} from '../lib/document-ui'

type Step = 1 | 2 | 3 | 4

type UploadFormState = {
  organizacaoId: string
  titulo: string
  arquivos: File[]
}

type SignerFormState = {
  nome: string
  email: string
  numeroDocumento: string
  telefone: string
  tipo: SignerType
  ordemAssinatura: number
}

const emptyUploadForm: UploadFormState = {
  organizacaoId: '',
  titulo: '',
  arquivos: [],
}

const emptySignerForm: SignerFormState = {
  nome: '',
  email: '',
  numeroDocumento: '',
  telefone: '',
  tipo: 'EXTERNO',
  ordemAssinatura: 1,
}

const steps: Array<{ id: Step; label: string; description: string }> = [
  { id: 1, label: 'Documento', description: 'Envie o PDF' },
  { id: 2, label: 'Signatários', description: 'Defina quem assina' },
  { id: 3, label: 'Confirmação', description: 'Revise e envie' },
  { id: 4, label: 'Finalizado', description: 'Links gerados' },
]

function canCurrentUserSignRequest(request: SignatureRequest, currentUserEmail?: string) {
  return Boolean(
    currentUserEmail
      && request.status === 'PENDENTE'
      && request.signatarioEmail.toLowerCase() === currentUserEmail,
  )
}

function resolverTituloUpload(titulo: string, file: File, totalFiles: number) {
  const tituloNormalizado = titulo.trim()

  if (totalFiles === 1) {
    return tituloNormalizado || undefined
  }

  const nomeArquivo = file.name.replace(/\.pdf$/i, '').trim()
  return tituloNormalizado ? `${tituloNormalizado} - ${nomeArquivo}` : nomeArquivo
}

export function StartSignaturePage() {
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const documentIdFromUrl = searchParams.get('documentoId')
  const [step, setStep] = useState<Step>(documentIdFromUrl ? 2 : 1)
  const [documentId, setDocumentId] = useState<string | null>(documentIdFromUrl)
  const [documentIds, setDocumentIds] = useState<string[]>(documentIdFromUrl ? [documentIdFromUrl] : [])
  const [flowDocuments, setFlowDocuments] = useState<DocumentDetail[]>([])
  const [uploadForm, setUploadForm] = useState<UploadFormState>(emptyUploadForm)
  const [fileInputVersion, setFileInputVersion] = useState(0)
  const [selectedSigner, setSelectedSigner] = useState<Signer | null>(null)
  const [signerForm, setSignerForm] = useState<SignerFormState>(emptySignerForm)
  const [validityDays, setValidityDays] = useState(7)
  const [hasValidity, setHasValidity] = useState(true)
  const [createdRequests, setCreatedRequests] = useState<SignatureRequest[]>([])
  const [requestToSign, setRequestToSign] = useState<SignatureRequest | null>(null)

  const currentUserQuery = useCurrentUser()
  const currentUserEmail = currentUserQuery.data?.email.toLowerCase()
  const canSendInvitationEmail = Boolean(currentUserQuery.data?.emailVerificado)
  const organizationsQuery = useQuery({ queryKey: ['organizations'], queryFn: listOrganizations })
  const documentQuery = useQuery({
    queryKey: ['document', documentId],
    queryFn: () => getDocument(documentId as string),
    enabled: Boolean(documentId),
  })
  const signersQuery = useQuery({
    queryKey: ['document-signers', documentId],
    queryFn: () => listSigners(documentId as string),
    enabled: Boolean(documentId),
  })
  const signatureRequestsQuery = useQuery({
    queryKey: ['document-signature-requests', documentId],
    queryFn: () => listSignatureRequests(documentId as string),
    enabled: Boolean(documentId),
  })

  const document = documentQuery.data
  const documentsInFlow = flowDocuments.length > 0 ? flowDocuments : document ? [document] : []
  const flowDocumentIds = documentIds.length > 0 ? documentIds : documentId ? [documentId] : []
  const hasMultipleDocuments = flowDocumentIds.length > 1
  const signers = signersQuery.data ?? []
  const signatureRequests = signatureRequestsQuery.data ?? []
  const signersWithRequestIds = useMemo(
    () => new Set(signatureRequests.map((request) => request.signatarioId)),
    [signatureRequests],
  )
  const signersToSend = signers.filter((signer) => !signersWithRequestIds.has(signer.id))

  useEffect(() => {
    if (documentIdFromUrl && documentIdFromUrl !== documentId) {
      setDocumentId(documentIdFromUrl)
      setDocumentIds([documentIdFromUrl])
      setFlowDocuments([])
      setStep(2)
    }
  }, [documentId, documentIdFromUrl])

  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (uploadForm.arquivos.length === 0) {
        throw new Error('Selecione ao menos um PDF para enviar.')
      }

      const createdDocuments: DocumentDetail[] = []

      for (const arquivo of uploadForm.arquivos) {
        const createdDocument = await createDocument({
          organizacaoId: uploadForm.organizacaoId || undefined,
          titulo: resolverTituloUpload(uploadForm.titulo, arquivo, uploadForm.arquivos.length),
          arquivo,
        })

        createdDocuments.push(createdDocument)
      }

      return createdDocuments
    },
    onSuccess: (createdDocuments) => {
      const [firstDocument] = createdDocuments
      const ids = createdDocuments.map((createdDocument) => createdDocument.id)

      setDocumentId(firstDocument.id)
      setDocumentIds(ids)
      setFlowDocuments(createdDocuments)
      setSearchParams({ documentoId: firstDocument.id })
      setUploadForm(emptyUploadForm)
      setFileInputVersion((current) => current + 1)
      setStep(2)
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
      toast.success(createdDocuments.length > 1 ? 'Documentos enviados. Agora adicione os signatários.' : 'Documento enviado. Agora adicione os signatários.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const saveSignerMutation = useMutation({
    mutationFn: () => {
      if (flowDocumentIds.length === 0) {
        throw new Error('Envie um documento antes de adicionar signatários.')
      }

      const payload = {
        nome: signerForm.nome.trim(),
        email: signerForm.email.trim(),
        numeroDocumento: signerForm.numeroDocumento.trim() || undefined,
        telefone: signerForm.telefone.trim() || undefined,
        tipo: signerForm.tipo,
        ordemAssinatura: signerForm.ordemAssinatura,
      }

      if (selectedSigner) {
        return Promise.all(
          flowDocumentIds.map(async (flowDocumentId) => {
            const documentSigners = flowDocumentId === documentId ? signers : await listSigners(flowDocumentId)
            const signerToUpdate = documentSigners.find(
              (signer) => signer.email.toLowerCase() === selectedSigner.email.toLowerCase(),
            )

            return signerToUpdate
              ? updateSigner(signerToUpdate.id, payload)
              : createSigner(flowDocumentId, payload)
          }),
        )
      }

      return Promise.all(flowDocumentIds.map((flowDocumentId) => createSigner(flowDocumentId, payload)))
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['document-signers', documentId] })
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
      clearSignerForm()
      toast.success(selectedSigner ? 'Signatário atualizado.' : 'Signatário adicionado.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })


  const addOwnSignerMutation = useMutation({
    mutationFn: () => {
      if (flowDocumentIds.length === 0) {
        throw new Error('Envie um documento antes de adicionar sua assinatura.')
      }

      const currentUser = currentUserQuery.data

      if (!currentUser) {
        throw new Error('Não foi possível identificar o usuário logado.')
      }

      const alreadyIncluded = signers.some(
        (signer) => signer.email.toLowerCase() === currentUser.email.toLowerCase(),
      )

      if (alreadyIncluded) {
        throw new Error('Você já está na lista de signatários.')
      }

      return Promise.all(
        flowDocumentIds.map(async (flowDocumentId) => {
          const documentSigners = flowDocumentId === documentId ? signers : await listSigners(flowDocumentId)
          const isAlreadyIncluded = documentSigners.some(
            (signer) => signer.email.toLowerCase() === currentUser.email.toLowerCase(),
          )

          if (isAlreadyIncluded) {
            return null
          }

          return createSigner(flowDocumentId, {
            nome: currentUser.nome,
            email: currentUser.email,
            numeroDocumento: currentUser.cpf ?? undefined,
            telefone: currentUser.telefone ?? undefined,
            tipo: 'INTERNO',
            ordemAssinatura: documentSigners.length + 1,
          })
        }),
      )
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['document-signers', documentId] })
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
      clearSignerForm()
      toast.success('Assinatura própria adicionada como signatário interno.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const deleteSignerMutation = useMutation({
    mutationFn: async (signerId: string) => {
      const signerToDelete = signers.find((signer) => signer.id === signerId)

      if (!signerToDelete || flowDocumentIds.length <= 1) {
        await deleteSigner(signerId)
        return
      }

      await Promise.all(
        flowDocumentIds.map(async (flowDocumentId) => {
          const documentSigners = flowDocumentId === documentId ? signers : await listSigners(flowDocumentId)
          const matchingSigner = documentSigners.find(
            (signer) => signer.email.toLowerCase() === signerToDelete.email.toLowerCase(),
          )

          if (matchingSigner) {
            await deleteSigner(matchingSigner.id)
          }
        }),
      )
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['document-signers', documentId] })
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
      clearSignerForm()
      toast.success('Signatário removido.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const sendRequestsMutation = useMutation({
    mutationFn: async () => {
      if (flowDocumentIds.length === 0) {
        throw new Error('Documento não encontrado.')
      }

      const createdRequests: SignatureRequest[] = []

      for (const flowDocumentId of flowDocumentIds) {
        const documentSigners = flowDocumentId === documentId ? signers : await listSigners(flowDocumentId)
        const documentRequests = flowDocumentId === documentId ? signatureRequests : await listSignatureRequests(flowDocumentId)
        const signerIdsWithRequests = new Set(documentRequests.map((request) => request.signatarioId))
        const documentSignersToSend = documentSigners.filter((signer) => !signerIdsWithRequests.has(signer.id))

        for (const signer of documentSignersToSend) {
          const createdRequest = await createSignatureRequest(flowDocumentId, hasValidity
            ? {
                signatarioId: signer.id,
                validadeDias: validityDays,
              }
            : {
                signatarioId: signer.id,
                semValidade: true,
              })

          createdRequests.push(createdRequest)
        }
      }

      return createdRequests
    },
    onSuccess: (requests) => {
      setCreatedRequests(requests)
      setStep(4)
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      queryClient.invalidateQueries({ queryKey: ['document', documentId] })
      queryClient.invalidateQueries({ queryKey: ['document-signature-requests', documentId] })
      flowDocumentIds.forEach((flowDocumentId) => {
        queryClient.invalidateQueries({ queryKey: ['document', flowDocumentId] })
        queryClient.invalidateQueries({ queryKey: ['document-signers', flowDocumentId] })
        queryClient.invalidateQueries({ queryKey: ['document-signature-requests', flowDocumentId] })
      })
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
      toast.success(requests.length > 0 ? 'Solicitações enviadas.' : 'Este documento já tinha solicitações criadas.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const sendInvitationMutation = useMutation({
    mutationFn: ({ request, canal }: { request: SignatureRequest; canal: SignatureInvitationChannel }) => {
      if (!canSendInvitationEmail) {
        throw new Error('Confirme seu e-mail em Configurações antes de enviar convites por e-mail.')
      }

      if (!request.token) {
        throw new Error('Solicitação sem link de assinatura.')
      }

      return sendSignatureInvitation(request.token, canal)
    },
    onSuccess: (response) => {
      toast.success(`${response.mensagem}: ${response.destino}`)
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  function submitUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    uploadMutation.mutate()
  }

  function submitSigner(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    saveSignerMutation.mutate()
  }

  function clearSignerForm() {
    setSelectedSigner(null)
    setSignerForm(emptySignerForm)
  }

  function editSigner(signer: Signer) {
    setSelectedSigner(signer)
    setSignerForm({
      nome: signer.nome,
      email: signer.email,
      numeroDocumento: signer.numeroDocumento ?? '',
      telefone: signer.telefone ?? '',
      tipo: signer.tipo,
      ordemAssinatura: signer.ordemAssinatura,
    })
  }

  function confirmDeleteSigner(signer: Signer) {
    const shouldDelete = window.confirm(`Remover o signatário ${signer.nome}?`)

    if (shouldDelete) {
      deleteSignerMutation.mutate(signer.id)
    }
  }

  function refreshAfterSign(signedRequest: SignatureRequest) {
    setCreatedRequests((currentRequests) =>
      currentRequests.map((request) =>
        request.id === signedRequest.id ? signedRequest : request,
      ),
    )
    queryClient.invalidateQueries({ queryKey: ['documents'] })
    queryClient.invalidateQueries({ queryKey: ['document', documentId] })
    queryClient.invalidateQueries({ queryKey: ['document-signature-requests', documentId] })
    queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
    setRequestToSign(null)
  }

  function startNewFlow() {
    setDocumentId(null)
    setDocumentIds([])
    setFlowDocuments([])
    setSearchParams({})
    setUploadForm(emptyUploadForm)
    setCreatedRequests([])
    setValidityDays(7)
    setHasValidity(true)
    clearSignerForm()
    setStep(1)
  }

  function canAccessStep(targetStep: Step) {
    if (targetStep === 1) {
      return true
    }

    if (targetStep === 2) {
      return flowDocumentIds.length > 0
    }

    if (targetStep === 3) {
      return Boolean(flowDocumentIds.length > 0 && signers.length > 0)
    }

    return Boolean(flowDocumentIds.length > 0 && (createdRequests.length > 0 || signatureRequests.length > 0))
  }

  function goToStep(targetStep: Step) {
    if (canAccessStep(targetStep)) {
      setStep(targetStep)
    }
  }

  return (
    <div className="grid gap-4 2xl:gap-5">
      <section className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <p className="text-sm font-medium text-ink sm:text-base">Crie um fluxo de assinatura em etapas.</p>
          <p className="mt-1 text-sm text-muted">Primeiro envie um ou mais PDFs, depois escolha os signatários e confirme o envio.</p>
        </div>

        <Button variant="secondary" onClick={startNewFlow}>
          <Plus aria-hidden="true" size={17} />
          Novo fluxo
        </Button>
      </section>

      <section className="grid gap-3 sm:grid-cols-4">
        {steps.map((item) => {
          const isAccessible = canAccessStep(item.id)

          return (
            <button
              className={cn(
                'rounded-card border border-line bg-white p-4 text-left shadow-card transition-colors',
                isAccessible && 'hover:border-brand-500',
                item.id === step && 'border-brand-500 bg-brand-50/60',
                item.id < step && 'border-green-100 bg-green-50/40',
                !isAccessible && 'cursor-not-allowed opacity-55',
              )}
              key={item.id}
              type="button"
              disabled={!isAccessible}
              onClick={() => goToStep(item.id)}
            >
              <div className="flex items-center gap-3">
                <span
                  className={cn(
                    'grid size-8 place-items-center rounded-full bg-surface-page text-sm font-semibold text-muted',
                    item.id === step && 'bg-brand-500 text-white',
                    item.id < step && 'bg-success text-white',
                  )}
                >
                  {item.id < step ? <CheckCircle2 aria-hidden="true" size={16} /> : item.id}
                </span>
                <div>
                  <p className="text-sm font-semibold text-ink">{item.label}</p>
                  <p className="mt-0.5 text-xs text-muted">{item.description}</p>
                </div>
              </div>
            </button>
          )
        })}
      </section>

      {documentQuery.isError && (
        <InlineMessage tone="error" message={getApiErrorMessage(documentQuery.error)} />
      )}

      {step === 1 && (
        <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
          <PageSection title="1. Enviar documento">
            <form className="grid gap-4 p-4" onSubmit={submitUpload}>
              <label className="grid gap-2 text-sm font-medium text-ink">
                Organização
                <select
                  className="min-h-11 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors focus:border-brand-500"
                  value={uploadForm.organizacaoId}
                  onChange={(event) => setUploadForm((current) => ({ ...current, organizacaoId: event.target.value }))}
                >
                  <option value="">Organização do usuário logado</option>
                  {organizationsQuery.data?.map((organization) => (
                    <option key={organization.id} value={organization.id}>{organization.nome}</option>
                  ))}
                </select>
              </label>

              <label className="grid gap-2 text-sm font-medium text-ink">
                Título do documento
                <input
                  className="min-h-11 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand-500"
                  value={uploadForm.titulo}
                  onChange={(event) => setUploadForm((current) => ({ ...current, titulo: event.target.value }))}
                  placeholder="Ex.: Contrato de prestação de serviços"
                />
                <span className="text-xs font-normal leading-5 text-muted">
                  Se enviar mais de um PDF, o Xsign usa este título como prefixo e identifica cada documento pelo nome do arquivo.
                </span>
              </label>

              <label className="grid gap-2 text-sm font-medium text-ink">
                PDFs
                <input
                  key={fileInputVersion}
                  className="min-h-11 rounded-control border border-line bg-white px-3 py-2 text-sm text-muted file:mr-3 file:rounded-control file:border-0 file:bg-brand-50 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-brand-500"
                  type="file"
                  accept="application/pdf,.pdf"
                  multiple
                  required
                  onChange={(event) => setUploadForm((current) => ({ ...current, arquivos: Array.from(event.target.files ?? []) }))}
                />
                {uploadForm.arquivos.length > 0 && (
                  <div className="grid gap-1 rounded-control border border-line bg-surface-page/70 p-3 text-xs font-normal text-muted">
                    <span className="font-medium text-ink">{uploadForm.arquivos.length} arquivo(s) selecionado(s)</span>
                    {uploadForm.arquivos.slice(0, 4).map((arquivo) => (
                      <span className="truncate" key={`${arquivo.name}-${arquivo.size}`}>{arquivo.name}</span>
                    ))}
                    {uploadForm.arquivos.length > 4 && <span>+ {uploadForm.arquivos.length - 4} arquivo(s)</span>}
                  </div>
                )}
              </label>

              <Button type="submit" disabled={uploadMutation.isPending}>
                <UploadCloud aria-hidden="true" size={18} />
                {uploadMutation.isPending ? 'Enviando...' : 'Enviar e continuar'}
              </Button>
            </form>
          </PageSection>

          <PageSection title="Como funciona">
            <div className="grid gap-3 p-4 text-sm leading-6 text-muted">
              <p>Depois do upload, cada PDF fica como um documento próprio em rascunho.</p>
              <p>Ao adicionar signatários, o Xsign replica a lista para todos os documentos deste fluxo.</p>
              <p>Na confirmação, cada documento recebe suas próprias solicitações, links, hashes e assinaturas.</p>
            </div>
          </PageSection>
        </section>
      )}

      {step === 2 && document && (
        <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_24rem] 2xl:grid-cols-[minmax(0,1fr)_28rem]">
          <PageSection title={selectedSigner ? 'Editar signatário' : 'Novo signatário'}>
            <div className="grid gap-2 border-b border-line p-4">
              <Button
                variant="secondary"
                disabled={addOwnSignerMutation.isPending || !currentUserQuery.data}
                onClick={() => addOwnSignerMutation.mutate()}
              >
                <UserRound aria-hidden="true" size={17} />
                Assinatura própria
              </Button>
              <p className="text-xs leading-5 text-muted">Adiciona o usuário logado como signatário interno para agilizar fluxos em que você também assina.</p>
            </div>
            <form className="grid gap-3 p-4" onSubmit={submitSigner}>
              <input className="min-h-10 rounded-control border border-line bg-white px-3 text-sm" value={signerForm.nome} onChange={(event) => setSignerForm((current) => ({ ...current, nome: event.target.value }))} placeholder="Nome" required />
              <input className="min-h-10 rounded-control border border-line bg-white px-3 text-sm" value={signerForm.email} onChange={(event) => setSignerForm((current) => ({ ...current, email: event.target.value }))} placeholder="E-mail" type="email" required />
              <input className="min-h-10 rounded-control border border-line bg-white px-3 text-sm" value={signerForm.numeroDocumento} onChange={(event) => setSignerForm((current) => ({ ...current, numeroDocumento: event.target.value }))} placeholder="CPF/CNPJ opcional" />
              <input className="min-h-10 rounded-control border border-line bg-white px-3 text-sm" value={signerForm.telefone} onChange={(event) => setSignerForm((current) => ({ ...current, telefone: event.target.value }))} placeholder="Telefone opcional" />
              <div className="grid grid-cols-2 gap-2">
                <select className="min-h-10 rounded-control border border-line bg-white px-3 text-sm" value={signerForm.tipo} onChange={(event) => setSignerForm((current) => ({ ...current, tipo: event.target.value as SignerType }))}>
                  <option value="EXTERNO">Externo</option>
                  <option value="INTERNO">Interno</option>
                </select>
                <input className="min-h-10 rounded-control border border-line bg-white px-3 text-sm" min={1} type="number" value={signerForm.ordemAssinatura} onChange={(event) => setSignerForm((current) => ({ ...current, ordemAssinatura: Number(event.target.value) }))} />
              </div>
              <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                {selectedSigner && <Button type="button" variant="secondary" onClick={clearSignerForm}>Cancelar</Button>}
                <Button type="submit" disabled={saveSignerMutation.isPending}>{saveSignerMutation.isPending ? 'Salvando...' : selectedSigner ? 'Salvar' : 'Adicionar'}</Button>
              </div>
            </form>
          </PageSection>

          <PageSection title="2. Signatários">
            <div className="grid gap-4 p-4">
              <div className="rounded-card border border-line bg-surface-page/60 p-4">
                <StatusBadge label={documentStatusLabels[document.status]} tone={documentStatusTone(document.status)} />
                <h2 className="mt-3 text-lg font-semibold text-ink">{hasMultipleDocuments ? `${flowDocumentIds.length} documentos no fluxo` : document.titulo}</h2>
                <p className="mt-1 text-sm text-muted">{document.organizacaoNome} · {hasMultipleDocuments ? 'signatários replicados para todos' : formatSize(document.arquivoAtual?.tamanhoBytes)}</p>
                {documentsInFlow.length > 1 && (
                  <div className="mt-3 grid gap-1 text-xs text-muted">
                    {documentsInFlow.slice(0, 5).map((flowDocument) => (
                      <span className="truncate" key={flowDocument.id}>{flowDocument.titulo}</span>
                    ))}
                    {documentsInFlow.length > 5 && <span>+ {documentsInFlow.length - 5} documentos</span>}
                  </div>
                )}
              </div>

              {signersQuery.isLoading && <InlineMessage message="Carregando signatários..." />}
              {!signersQuery.isLoading && signers.length === 0 && <EmptyState title="Nenhum signatário" description="Adicione ao menos uma pessoa para seguir para confirmação." />}
              <div className="grid gap-3">
                {signers.map((signer) => (
                  <article className="flex flex-col gap-3 rounded-card border border-line p-3 sm:flex-row sm:items-center sm:justify-between" key={signer.id}>
                    <div className="flex min-w-0 items-center gap-3">
                      <div className="grid size-9 shrink-0 place-items-center rounded-full bg-brand-50 text-brand-500"><UserRound aria-hidden="true" size={18} /></div>
                      <div className="min-w-0">
                        <p className="truncate font-medium text-ink">{signer.nome}</p>
                        <p className="mt-0.5 truncate text-sm text-muted">{signer.email}</p>
                        <p className="mt-1 text-xs text-muted">{signer.tipo === 'INTERNO' ? 'Interno' : 'Externo'} · ordem {signer.ordemAssinatura}</p>
                      </div>
                    </div>
                    <div className="flex gap-2 sm:justify-end">
                      <button className="grid size-8 place-items-center rounded-control text-muted hover:bg-brand-50 hover:text-brand-500" type="button" onClick={() => editSigner(signer)} aria-label={`Editar ${signer.nome}`}><Pencil aria-hidden="true" size={16} /></button>
                      <button className="grid size-8 place-items-center rounded-control text-muted hover:bg-red-50 hover:text-danger" type="button" onClick={() => confirmDeleteSigner(signer)} aria-label={`Remover ${signer.nome}`}><Trash2 aria-hidden="true" size={16} /></button>
                    </div>
                  </article>
                ))}
              </div>

              <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <Button variant="secondary" onClick={() => setStep(1)}>Voltar</Button>
                <Button disabled={signers.length === 0} onClick={() => setStep(3)}>Continuar para confirmação</Button>
              </div>
            </div>
          </PageSection>
        </section>
      )}

      {step === 3 && document && (
        <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
          <PageSection title="3. Confirmação do envio">
            <div className="grid gap-4 p-4">
              <div className="rounded-card border border-line bg-surface-page/60 p-4">
                <p className="text-xs font-medium uppercase tracking-[0.12em] text-muted">Documento</p>
                <h2 className="mt-2 text-xl font-semibold text-ink">{hasMultipleDocuments ? `${flowDocumentIds.length} documentos serão enviados` : document.titulo}</h2>
                <p className="mt-1 text-sm text-muted">{document.organizacaoNome}</p>
                {documentsInFlow.length > 1 && (
                  <div className="mt-3 grid gap-1 text-xs text-muted">
                    {documentsInFlow.map((flowDocument) => (
                      <span className="truncate" key={flowDocument.id}>{flowDocument.titulo}</span>
                    ))}
                  </div>
                )}
              </div>

              <div className="grid gap-2">
                <p className="text-sm font-semibold text-ink">Signatários que receberão solicitação</p>
                {signersToSend.length === 0 && <InlineMessage message="Todos os signatários já possuem solicitação criada." />}
                {signersToSend.map((signer) => (
                  <div className="flex items-center gap-3 rounded-control border border-line px-3 py-2 text-sm" key={signer.id}>
                    <Mail className="shrink-0 text-brand-500" size={16} aria-hidden="true" />
                    <span className="font-medium text-ink">{signer.nome}</span>
                    <span className="min-w-0 truncate text-muted">{signer.email}</span>
                  </div>
                ))}
              </div>

              <div className="grid gap-3 rounded-card border border-line bg-surface-page/60 p-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm font-semibold text-ink">Validade do link de assinatura</p>
                    <p className="mt-1 text-sm text-muted">Ative apenas quando quiser que o convite expire automaticamente.</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-medium text-muted">{hasValidity ? 'Com validade' : 'Sem validade'}</span>
                    <button
                      className={cn(
                        'relative inline-flex h-6 w-11 shrink-0 rounded-full border transition-colors',
                        hasValidity ? 'border-brand-500 bg-brand-500' : 'border-line bg-white',
                      )}
                      type="button"
                      aria-label="Alternar validade do link de assinatura"
                      aria-pressed={hasValidity}
                      onClick={() => setHasValidity((current) => !current)}
                    >
                      <span
                        className={cn(
                          'absolute top-0.5 size-5 rounded-full bg-white shadow-sm transition-transform',
                          hasValidity ? 'translate-x-5' : 'translate-x-0.5',
                        )}
                      />
                    </button>
                  </div>
                </div>

                {hasValidity ? (
                  <label className="grid max-w-xs gap-2 text-sm font-medium text-ink">
                    Dias até expirar
                    <input className="min-h-10 rounded-control border border-line bg-white px-3 text-sm" min={1} type="number" value={validityDays} onChange={(event) => setValidityDays(Number(event.target.value))} />
                  </label>
                ) : (
                  <InlineMessage message="As solicitações serão criadas sem data de expiração." />
                )}
              </div>

              <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <Button variant="secondary" onClick={() => setStep(2)}>Voltar</Button>
                <Button disabled={sendRequestsMutation.isPending} onClick={() => sendRequestsMutation.mutate()}>
                  <Send aria-hidden="true" size={17} />
                  {sendRequestsMutation.isPending ? 'Enviando...' : 'Enviar para assinatura'}
                </Button>
              </div>
            </div>
          </PageSection>

          <PageSection title="Resumo">
            <div className="grid gap-3 p-4 text-sm text-muted">
              <p><strong className="font-medium text-ink">Signatários:</strong> {signers.length}</p>
              <p><strong className="font-medium text-ink">Documentos:</strong> {flowDocumentIds.length}</p>
              <p><strong className="font-medium text-ink">Já enviados:</strong> {signatureRequests.length}</p>
              <p><strong className="font-medium text-ink">Novos envios:</strong> {hasMultipleDocuments ? `${signers.length * flowDocumentIds.length} no máximo` : signersToSend.length}</p>
            </div>
          </PageSection>
        </section>
      )}

      {step === 4 && document && (
        <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
          <PageSection title="4. Fluxo finalizado">
            <div className="grid gap-4 p-4">
              <div className="rounded-card border border-green-100 bg-green-50/60 p-4">
                <div className="flex items-start gap-3">
                  <div className="grid size-10 shrink-0 place-items-center rounded-full bg-success text-white"><CheckCircle2 aria-hidden="true" size={20} /></div>
                  <div>
                    <h2 className="text-lg font-semibold text-ink">Solicitações prontas para assinatura.</h2>
                    <p className="mt-1 text-sm text-muted">Compartilhe o link seguro. O signatário acessa, revisa e assina sem etapas repetitivas.</p>
                  </div>
                </div>
              </div>

              {(createdRequests.length > 0 ? createdRequests : signatureRequests).map((request) => (
                <article className="rounded-card border border-line p-3" key={request.id}>
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0">
                      <StatusBadge label={signatureRequestStatusLabels[request.status]} tone={signatureRequestStatusTone(request.status)} />
                      <p className="mt-3 font-medium text-ink">{request.signatarioNome}</p>
                      <p className="mt-0.5 truncate text-sm text-muted">{request.signatarioEmail}</p>
                    </div>
                    {request.token && (
                      <div className="flex flex-col gap-2 sm:flex-row">
                        {canCurrentUserSignRequest(request, currentUserEmail) && (
                          <button className="inline-flex min-h-11 items-center justify-center gap-2 rounded-control border border-brand-500 bg-brand-500 px-4 text-sm font-medium text-white transition-colors hover:bg-brand-600" type="button" onClick={() => setRequestToSign(request)}>
                            <CheckCircle2 aria-hidden="true" size={16} />
                            Assinar agora
                          </button>
                        )}
                        {!canCurrentUserSignRequest(request, currentUserEmail) && (
                          <Button
                            variant="secondary"
                            disabled={sendInvitationMutation.isPending || !canSendInvitationEmail}
                            onClick={() => sendInvitationMutation.mutate({ request, canal: 'EMAIL' })}
                            title={!canSendInvitationEmail ? 'Confirme seu e-mail em Configurações para enviar convites' : undefined}
                          >
                            <Mail aria-hidden="true" size={15} />
                            Enviar e-mail
                          </Button>
                        )}
                        {!canCurrentUserSignRequest(request, currentUserEmail) && !canSendInvitationEmail && (
                          <Link className="inline-flex min-h-11 items-center justify-center rounded-control px-3 text-sm font-medium text-brand-500 hover:underline" to="/app/configuracoes">
                            Verificar e-mail
                          </Link>
                        )}
                      </div>
                    )}
                  </div>
                  <div className="mt-3 grid gap-3 rounded-control bg-surface-page/70 p-3 text-sm text-muted">
                    <p><strong className="font-medium text-ink">Autenticação:</strong> link seguro com token único da solicitação.</p>
                    <p><strong className="font-medium text-ink">Expira em:</strong> {request.expiraEm ? formatDate(request.expiraEm) : 'Sem validade'}</p>
                    <p className="break-all"><strong className="font-medium text-ink">Token:</strong> {request.token ?? '-'}</p>
                  </div>
                </article>
              ))}
            </div>
          </PageSection>

          <PageSection title="Próximo passo">
            <div className="grid gap-3 p-4">
              <Link className="inline-flex min-h-11 items-center justify-center rounded-control border border-brand-500 bg-brand-500 px-4 text-sm font-medium text-white transition-colors hover:bg-brand-600" to="/app/assinatura/documentos">Ver documentos</Link>
              <Button variant="secondary" onClick={() => setStep(3)}>Voltar para confirmação</Button>
              <Button variant="secondary" onClick={startNewFlow}>Criar outro fluxo</Button>
            </div>
          </PageSection>
        </section>
      )}
      <SignNowModal
        request={requestToSign}
        onClose={() => setRequestToSign(null)}
        onSigned={refreshAfterSign}
      />
    </div>
  )
}
