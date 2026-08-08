import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Archive,
  ArrowUpDown,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Download,
  FileText,
  Mail,
  Search,
  Send,
  XCircle,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'

import { useCurrentUser } from '../../auth/hooks/use-current-user'
import { getApiErrorMessage } from '../../../services/api/api-error'
import { cn } from '../../../shared/lib/cn'
import { Button } from '../../../shared/ui/button'
import { EmptyState } from '../../../shared/ui/empty-state'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { PageSection } from '../../../shared/ui/page-section'
import { StatusBadge } from '../../../shared/ui/status-badge'
import { listOrganizations } from '../../organizations/api/organizations-api'
import { SignNowModal } from '../components/sign-now-modal'
import {
  archiveDocument,
  cancelSignatureRequest,
  downloadDocument,
  getDocument,
  listDocuments,
  listSignatureRequests,
  listSigners,
  sendSignatureInvitation,
  type DocumentStatus,
  type DocumentSummary,
  type SignatureInvitationChannel,
  type SignatureRequest,
} from '../api/documents-api'
import {
  documentStatusLabels,
  documentStatusTone,
  formatDate,
  formatSize,
  signatureRequestStatusLabels,
  signatureRequestStatusTone,
} from '../lib/document-ui'

type StatusFilter = 'TODOS' | DocumentStatus
type SortField = 'titulo' | 'organizacaoNome' | 'status' | 'criadoPorUsuarioNome' | 'atualizadoEm'
type SortDirection = 'asc' | 'desc'

const PER_PAGE = 10

const statusFilters: Array<{ label: string; value: StatusFilter }> = [
  { label: 'Todos', value: 'TODOS' },
  { label: 'Rascunhos', value: 'RASCUNHO' },
  { label: 'Para assinar', value: 'ENVIADO_PARA_ASSINATURA' },
  { label: 'Assinados', value: 'ASSINADO' },
  { label: 'Rejeitados', value: 'REJEITADO' },
  { label: 'Arquivados', value: 'ARQUIVADO' },
]

function getDocumentAge(document: DocumentSummary) {
  const diff = Date.now() - new Date(document.atualizadoEm).getTime()
  const minutes = Math.max(1, Math.round(diff / 1000 / 60))

  if (minutes < 60) {
    return `há ${minutes} min`
  }

  const hours = Math.round(minutes / 60)
  if (hours < 24) {
    return `há ${hours} h`
  }

  return formatDate(document.atualizadoEm)
}

function formatExpiration(value?: string | null) {
  return value ? formatDate(value) : 'Sem validade'
}

function matchesSearch(document: DocumentSummary, search: string) {
  if (!search.trim()) {
    return true
  }

  const term = search.trim().toLowerCase()
  const searchable = [
    document.titulo,
    document.id,
    document.organizacaoNome,
    document.criadoPorUsuarioNome,
    document.status,
  ]
    .join(' ')
    .toLowerCase()

  return searchable.includes(term)
}

function getSortValue(document: DocumentSummary, sortField: SortField) {
  if (sortField === 'atualizadoEm') {
    return new Date(document.atualizadoEm).getTime()
  }

  return String(document[sortField]).toLowerCase()
}

function compareDocuments(first: DocumentSummary, second: DocumentSummary, sortField: SortField, direction: SortDirection) {
  const firstValue = getSortValue(first, sortField)
  const secondValue = getSortValue(second, sortField)
  const modifier = direction === 'asc' ? 1 : -1

  if (firstValue > secondValue) {
    return modifier
  }

  if (firstValue < secondValue) {
    return -modifier
  }

  return 0
}

function canCurrentUserSignRequest(request: SignatureRequest, currentUserEmail?: string) {
  return Boolean(
    currentUserEmail
      && request.status === 'PENDENTE'
      && request.signatarioEmail.toLowerCase() === currentUserEmail,
  )
}

export function DocumentsPage() {
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const [organizationFilter, setOrganizationFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('TODOS')
  const [search, setSearch] = useState(searchParams.get('q') ?? '')
  const [sortField, setSortField] = useState<SortField>('atualizadoEm')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [page, setPage] = useState(1)
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(null)
  const [requestToSign, setRequestToSign] = useState<SignatureRequest | null>(null)

  const currentUserQuery = useCurrentUser()
  const currentUserEmail = currentUserQuery.data?.email.toLowerCase()
  const canSendInvitationEmail = Boolean(currentUserQuery.data?.emailVerificado)

  const organizationsQuery = useQuery({ queryKey: ['organizations'], queryFn: listOrganizations })
  const documentsQuery = useQuery({
    queryKey: ['documents', organizationFilter],
    queryFn: () => listDocuments(organizationFilter || undefined),
  })
  const selectedDocumentQuery = useQuery({
    queryKey: ['document', selectedDocumentId],
    queryFn: () => getDocument(selectedDocumentId as string),
    enabled: Boolean(selectedDocumentId),
  })
  const signersQuery = useQuery({
    queryKey: ['document-signers', selectedDocumentId],
    queryFn: () => listSigners(selectedDocumentId as string),
    enabled: Boolean(selectedDocumentId),
  })
  const signatureRequestsQuery = useQuery({
    queryKey: ['document-signature-requests', selectedDocumentId],
    queryFn: () => listSignatureRequests(selectedDocumentId as string),
    enabled: Boolean(selectedDocumentId),
  })

  useEffect(() => {
    const querySearch = searchParams.get('q') ?? ''
    setSearch(querySearch)
    setPage(1)
  }, [searchParams])

  const documents = documentsQuery.data ?? []
  const filteredDocuments = useMemo(
    () =>
      [...documents]
        .filter((document) => statusFilter === 'TODOS' || document.status === statusFilter)
        .filter((document) => matchesSearch(document, search))
        .sort((first, second) => compareDocuments(first, second, sortField, sortDirection)),
    [documents, search, sortDirection, sortField, statusFilter],
  )
  const totalPages = Math.max(1, Math.ceil(filteredDocuments.length / PER_PAGE))
  const visibleDocuments = filteredDocuments.slice((page - 1) * PER_PAGE, page * PER_PAGE)
  const selectedDocument = selectedDocumentQuery.data ?? null
  const signers = signersQuery.data ?? []
  const signatureRequests = signatureRequestsQuery.data ?? []

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages)
    }
  }, [page, totalPages])

  const archiveDocumentMutation = useMutation({
    mutationFn: archiveDocument,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
      setSelectedDocumentId(null)
      toast.success('Documento arquivado.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const cancelRequestMutation = useMutation({
    mutationFn: cancelSignatureRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['document-signature-requests', selectedDocumentId] })
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
      toast.success('Solicitação cancelada.')
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

  const downloadMutation = useMutation({
    mutationFn: downloadDocument,
    onSuccess: (blob) => {
      if (!selectedDocument) {
        return
      }

      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = selectedDocument.arquivoAtual?.nomeOriginal ?? `${selectedDocument.titulo}.pdf`
      link.click()
      URL.revokeObjectURL(url)
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  function updateSearch(value: string) {
    setSearch(value)
    setPage(1)

    const nextParams = new URLSearchParams(searchParams)
    if (value.trim()) {
      nextParams.set('q', value)
    } else {
      nextParams.delete('q')
    }
    setSearchParams(nextParams, { replace: true })
  }

  function changeStatusFilter(nextStatus: StatusFilter) {
    setStatusFilter(nextStatus)
    setPage(1)
  }

  function changeOrganizationFilter(value: string) {
    setOrganizationFilter(value)
    setSelectedDocumentId(null)
    setPage(1)
  }

  function changeSort(nextSortField: SortField) {
    if (sortField === nextSortField) {
      setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'))
      return
    }

    setSortField(nextSortField)
    setSortDirection(nextSortField === 'atualizadoEm' ? 'desc' : 'asc')
  }

  function confirmArchive(documentId: string, title: string) {
    const shouldArchive = window.confirm(`Arquivar o documento ${title}?`)

    if (shouldArchive) {
      archiveDocumentMutation.mutate(documentId)
    }
  }

  function refreshAfterSign() {
    queryClient.invalidateQueries({ queryKey: ['documents'] })
    queryClient.invalidateQueries({ queryKey: ['document', selectedDocumentId] })
    queryClient.invalidateQueries({ queryKey: ['document-signature-requests', selectedDocumentId] })
    queryClient.invalidateQueries({ queryKey: ['audit-logs'] })
    setRequestToSign(null)
  }

  return (
    <div className="grid gap-4 2xl:gap-5">
      <section className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <p className="text-sm font-medium text-ink sm:text-base">Acompanhe os documentos por estado.</p>
          <p className="mt-1 text-sm text-muted">Tabela com busca, ordenação e ações rápidas. A criação fica em Assinar documento.</p>
        </div>

        <Link className="inline-flex min-h-11 items-center justify-center gap-2 rounded-control border border-brand-500 bg-brand-500 px-4 text-sm font-medium text-white transition-colors hover:bg-brand-600" to="/app/assinatura/novo">
          <Send aria-hidden="true" size={17} />
          Assinar documento
        </Link>
      </section>

      <section className="grid items-start gap-4 xl:grid-cols-[minmax(0,1fr)_25rem] 2xl:grid-cols-[minmax(0,1fr)_30rem]">
        <PageSection title="Documentos" className="flex min-h-[28rem] flex-col self-start xl:max-h-[calc(100svh-14.5rem)]">
          <div className="shrink-0 grid gap-3 border-b border-line p-4 md:grid-cols-[minmax(0,1fr)_14rem]">
            <label className="relative block">
              <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" size={16} aria-hidden="true" />
              <input
                className="min-h-10 w-full rounded-control border border-line bg-white pl-9 pr-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand-500"
                value={search}
                onChange={(event) => updateSearch(event.target.value)}
                placeholder="Buscar por documento, ID, status ou organização"
              />
            </label>
            <select
              className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors focus:border-brand-500"
              value={organizationFilter}
              onChange={(event) => changeOrganizationFilter(event.target.value)}
            >
              <option value="">Todas as organizações</option>
              {organizationsQuery.data?.map((organization) => (
                <option key={organization.id} value={organization.id}>{organization.nome}</option>
              ))}
            </select>
          </div>

          <div className="flex shrink-0 gap-2 overflow-x-auto border-b border-line px-4 py-3">
            {statusFilters.map((filter) => (
              <button
                className={cn(
                  'shrink-0 rounded-full border border-line bg-white px-3 py-1.5 text-xs font-medium text-muted transition-colors hover:border-brand-500 hover:text-brand-500',
                  statusFilter === filter.value && 'border-brand-500 bg-brand-50 text-brand-500',
                )}
                key={filter.value}
                type="button"
                onClick={() => changeStatusFilter(filter.value)}
              >
                {filter.label}
              </button>
            ))}
          </div>

          {documentsQuery.isLoading && <div className="p-4"><InlineMessage message="Carregando documentos..." /></div>}
          {documentsQuery.isError && <div className="p-4"><InlineMessage tone="error" message={getApiErrorMessage(documentsQuery.error)} /></div>}
          {!documentsQuery.isLoading && filteredDocuments.length === 0 && <div className="p-4"><EmptyState title="Nenhum documento encontrado" description="Ajuste os filtros ou crie um fluxo em Assinar documento." /></div>}

          {filteredDocuments.length > 0 && (
            <>
              <div className="min-h-0 flex-1 overflow-y-auto">
                <div className="sticky top-0 z-10 hidden border-b border-line bg-surface-page/95 px-4 py-3 text-xs font-medium text-muted backdrop-blur md:grid md:grid-cols-[minmax(14rem,1.7fr)_minmax(7rem,0.8fr)_minmax(8rem,0.8fr)_6.5rem] lg:grid-cols-[minmax(15rem,1.8fr)_minmax(8rem,0.8fr)_minmax(8rem,0.8fr)_minmax(6.5rem,0.7fr)_6.5rem]">
                  <button className="inline-flex items-center gap-1.5 text-left font-medium transition-colors hover:text-brand-500" type="button" onClick={() => changeSort('titulo')}>
                    Documento
                    <ArrowUpDown className={cn(sortField === 'titulo' ? 'text-brand-500' : 'text-muted/70')} size={13} aria-hidden="true" />
                  </button>
                  <button className="inline-flex items-center gap-1.5 text-left font-medium transition-colors hover:text-brand-500" type="button" onClick={() => changeSort('organizacaoNome')}>
                    Organização
                    <ArrowUpDown className={cn(sortField === 'organizacaoNome' ? 'text-brand-500' : 'text-muted/70')} size={13} aria-hidden="true" />
                  </button>
                  <button className="inline-flex items-center gap-1.5 text-left font-medium transition-colors hover:text-brand-500" type="button" onClick={() => changeSort('status')}>
                    Status
                    <ArrowUpDown className={cn(sortField === 'status' ? 'text-brand-500' : 'text-muted/70')} size={13} aria-hidden="true" />
                  </button>
                  <button className="hidden items-center gap-1.5 text-left font-medium transition-colors hover:text-brand-500 lg:inline-flex" type="button" onClick={() => changeSort('criadoPorUsuarioNome')}>
                    Criado por
                    <ArrowUpDown className={cn(sortField === 'criadoPorUsuarioNome' ? 'text-brand-500' : 'text-muted/70')} size={13} aria-hidden="true" />
                  </button>
                  <button className="inline-flex items-center gap-1.5 text-left font-medium transition-colors hover:text-brand-500" type="button" onClick={() => changeSort('atualizadoEm')}>
                    Atualizado
                    <ArrowUpDown className={cn(sortField === 'atualizadoEm' ? 'text-brand-500' : 'text-muted/70')} size={13} aria-hidden="true" />
                  </button>
                </div>

                <div className="grid gap-2 p-3 md:block md:p-0">
                  {visibleDocuments.map((document) => (
                    <button
                      className={cn(
                        'w-full cursor-pointer rounded-card border border-line bg-white p-3 text-left transition-colors hover:border-brand-500 hover:bg-surface-page/60 md:grid md:grid-cols-[minmax(14rem,1.7fr)_minmax(7rem,0.8fr)_minmax(8rem,0.8fr)_6.5rem] md:items-center md:gap-3 md:rounded-none md:border-x-0 md:border-t-0 md:px-4 md:py-3 lg:grid-cols-[minmax(15rem,1.8fr)_minmax(8rem,0.8fr)_minmax(8rem,0.8fr)_minmax(6.5rem,0.7fr)_6.5rem]',
                        selectedDocumentId === document.id && 'border-brand-500 bg-brand-50/60 md:bg-brand-50/60',
                      )}
                      key={document.id}
                      type="button"
                      onClick={() => setSelectedDocumentId(document.id)}
                    >
                      <div className="flex min-w-0 items-center gap-3">
                        <div className="grid size-9 shrink-0 place-items-center rounded-control bg-brand-50 text-brand-500"><FileText aria-hidden="true" size={18} /></div>
                        <div className="min-w-0">
                          <p className="truncate font-medium text-ink">{document.titulo}</p>
                          <p className="mt-0.5 truncate text-xs text-muted">ID: {document.id}</p>
                        </div>
                      </div>

                      <div className="mt-3 grid grid-cols-2 gap-2 text-sm text-muted md:mt-0 md:block md:min-w-0">
                        <span className="text-xs font-medium uppercase tracking-[0.08em] text-muted/80 md:hidden">Organização</span>
                        <span className="min-w-0 truncate md:block">{document.organizacaoNome}</span>
                      </div>

                      <div className="mt-2 grid grid-cols-2 items-center gap-2 md:mt-0 md:block">
                        <span className="text-xs font-medium uppercase tracking-[0.08em] text-muted/80 md:hidden">Status</span>
                        <StatusBadge label={documentStatusLabels[document.status]} tone={documentStatusTone(document.status)} />
                      </div>

                      <div className="mt-2 hidden min-w-0 text-sm text-muted lg:block">
                        <span className="truncate">{document.criadoPorUsuarioNome}</span>
                      </div>

                      <div className="mt-2 grid grid-cols-2 gap-2 text-sm text-muted md:mt-0 md:block">
                        <span className="text-xs font-medium uppercase tracking-[0.08em] text-muted/80 md:hidden">Atualizado</span>
                        <span>{getDocumentAge(document)}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>

              <div className="shrink-0 flex flex-col gap-3 border-t border-line px-4 py-3 text-sm text-muted sm:flex-row sm:items-center sm:justify-between">
                <span>
                  Mostrando {((page - 1) * PER_PAGE) + 1} a {Math.min(page * PER_PAGE, filteredDocuments.length)} de {filteredDocuments.length} documentos
                </span>
                <div className="flex items-center gap-2">
                  <button className="grid size-9 place-items-center rounded-control border border-line bg-white text-ink disabled:opacity-45" type="button" disabled={page === 1} onClick={() => setPage((current) => Math.max(1, current - 1))} aria-label="Página anterior">
                    <ChevronLeft size={16} aria-hidden="true" />
                  </button>
                  <span className="rounded-control bg-surface-page px-3 py-2 text-xs font-medium text-ink">{page} / {totalPages}</span>
                  <button className="grid size-9 place-items-center rounded-control border border-line bg-white text-ink disabled:opacity-45" type="button" disabled={page === totalPages} onClick={() => setPage((current) => Math.min(totalPages, current + 1))} aria-label="Próxima página">
                    <ChevronRight size={16} aria-hidden="true" />
                  </button>
                </div>
              </div>
            </>
          )}
        </PageSection>

        <PageSection title="Detalhes" className="flex flex-col self-start xl:max-h-[calc(100svh-14.5rem)]">
          {!selectedDocumentId && <div className="p-4"><EmptyState title="Selecione um documento" description="O resumo, signatários e solicitações aparecem aqui." /></div>}
          {selectedDocumentQuery.isLoading && <div className="p-4"><InlineMessage message="Carregando detalhes..." /></div>}
          {selectedDocumentQuery.isError && <div className="p-4"><InlineMessage tone="error" message={getApiErrorMessage(selectedDocumentQuery.error)} /></div>}

          {selectedDocument && (
            <div className="grid min-h-0 flex-1 gap-4 overflow-y-auto p-4">
              <div>
                <StatusBadge label={documentStatusLabels[selectedDocument.status]} tone={documentStatusTone(selectedDocument.status)} />
                <h2 className="mt-3 text-lg font-semibold leading-tight text-ink">{selectedDocument.titulo}</h2>
                <p className="mt-1 text-sm text-muted">{selectedDocument.organizacaoNome}</p>
              </div>

              <div className="grid gap-2 rounded-card border border-line bg-surface-page/60 p-3 text-sm text-muted">
                <p><strong className="font-medium text-ink">Arquivo:</strong> {selectedDocument.arquivoAtual?.nomeOriginal ?? '-'}</p>
                <p><strong className="font-medium text-ink">Tamanho:</strong> {formatSize(selectedDocument.arquivoAtual?.tamanhoBytes)}</p>
                <p className="break-all"><strong className="font-medium text-ink">Checksum:</strong> {selectedDocument.arquivoAtual?.checksumSha256 ?? '-'}</p>
                <p><strong className="font-medium text-ink">Criado em:</strong> {formatDate(selectedDocument.criadoEm)}</p>
              </div>

              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                <Button variant="secondary" disabled={downloadMutation.isPending} onClick={() => downloadMutation.mutate(selectedDocument.id)}>
                  <Download aria-hidden="true" size={16} />
                  Baixar
                </Button>
                {selectedDocument.status === 'RASCUNHO' ? (
                  <Link className="inline-flex min-h-11 items-center justify-center gap-2 rounded-control border border-brand-500 bg-brand-500 px-4 text-sm font-medium text-white transition-colors hover:bg-brand-600" to={`/app/assinatura/novo?documentoId=${selectedDocument.id}`}>
                    <Send aria-hidden="true" size={16} />
                    Continuar fluxo
                  </Link>
                ) : (
                  <Button variant="secondary" onClick={() => confirmArchive(selectedDocument.id, selectedDocument.titulo)}>
                    <Archive aria-hidden="true" size={16} />
                    Arquivar
                  </Button>
                )}
              </div>

              <div>
                <p className="text-sm font-semibold text-ink">Signatários</p>
                <div className="mt-2 grid gap-2">
                  {signers.length === 0 && <InlineMessage message="Nenhum signatário cadastrado." />}
                  {signers.map((signer) => (
                    <div className="rounded-control border border-line px-3 py-2 text-sm" key={signer.id}>
                      <p className="font-medium text-ink">{signer.nome}</p>
                      <p className="mt-0.5 text-muted">{signer.email}</p>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <p className="text-sm font-semibold text-ink">Solicitações</p>
                <div className="mt-2 grid gap-2">
                  {signatureRequests.length === 0 && <InlineMessage message="Nenhuma solicitação enviada." />}
                  {signatureRequests.map((request) => (
                    <article className="rounded-card border border-line p-3" key={request.id}>
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <StatusBadge label={signatureRequestStatusLabels[request.status]} tone={signatureRequestStatusTone(request.status)} />
                          <p className="mt-2 truncate font-medium text-ink">{request.signatarioNome}</p>
                          <p className="mt-0.5 truncate text-sm text-muted">{request.signatarioEmail}</p>
                          {canCurrentUserSignRequest(request, currentUserEmail) && (
                            <p className="mt-1 text-xs font-medium text-brand-500">Você é o signatário desta solicitação</p>
                          )}
                        </div>
                        {request.status === 'PENDENTE' && (
                          <button className="grid size-8 place-items-center rounded-control text-muted hover:bg-red-50 hover:text-danger" type="button" onClick={() => cancelRequestMutation.mutate(request.id)} aria-label="Cancelar solicitação">
                            <XCircle aria-hidden="true" size={16} />
                          </button>
                        )}
                      </div>
                      <div className="mt-3 grid gap-1 rounded-control bg-surface-page/70 p-3 text-sm text-muted">
                        <p><strong className="font-medium text-ink">Autenticação:</strong> link seguro com token único da solicitação.</p>
                        <p><strong className="font-medium text-ink">Expira:</strong> {formatExpiration(request.expiraEm)}</p>
                      </div>
                      {request.token && (
                        <div className="mt-2 flex flex-col gap-2 sm:flex-row">
                          {canCurrentUserSignRequest(request, currentUserEmail) && (
                            <button className="inline-flex min-h-9 items-center justify-center gap-2 rounded-control border border-brand-500 bg-brand-500 px-3 text-sm font-medium text-white transition-colors hover:bg-brand-600" type="button" onClick={() => setRequestToSign(request)}>
                              <CheckCircle2 aria-hidden="true" size={15} />
                              Assinar agora
                            </button>
                          )}
                          {!canCurrentUserSignRequest(request, currentUserEmail) && (
                            <button
                              className="inline-flex min-h-9 items-center justify-center gap-2 rounded-control border border-line px-3 text-sm font-medium text-brand-500 hover:border-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
                              type="button"
                              disabled={sendInvitationMutation.isPending || !canSendInvitationEmail}
                              onClick={() => sendInvitationMutation.mutate({ request, canal: 'EMAIL' })}
                              title={!canSendInvitationEmail ? 'Confirme seu e-mail em Configurações para enviar convites' : undefined}
                            >
                              <Mail aria-hidden="true" size={15} />
                              Enviar e-mail
                            </button>
                          )}
                          {!canCurrentUserSignRequest(request, currentUserEmail) && !canSendInvitationEmail && (
                            <Link className="inline-flex min-h-9 items-center justify-center rounded-control px-2 text-sm font-medium text-brand-500 hover:underline" to="/app/configuracoes">
                              Verificar e-mail
                            </Link>
                          )}
                        </div>
                      )}
                    </article>
                  ))}
                </div>
              </div>
            </div>
          )}
        </PageSection>
      </section>
      <SignNowModal
        request={requestToSign}
        onClose={() => setRequestToSign(null)}
        onSigned={refreshAfterSign}
      />
    </div>
  )
}
