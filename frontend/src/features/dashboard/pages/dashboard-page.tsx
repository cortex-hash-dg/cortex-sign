import { useQuery } from '@tanstack/react-query'
import {
  CalendarClock,
  CheckCircle2,
  ChevronDown,
  Clock3,
  FileText,
  Plus,
  Send,
  UsersRound,
} from 'lucide-react'
import { Link } from 'react-router-dom'

import { cn } from '../../../shared/lib/cn'
import { EmptyState } from '../../../shared/ui/empty-state'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { PageSection } from '../../../shared/ui/page-section'
import { StatusBadge } from '../../../shared/ui/status-badge'
import { listAuditLogs } from '../../audit/api/audit-api'
import { listDocuments, type DocumentStatus } from '../../documents/api/documents-api'
import { listUsers } from '../../users/api/users-api'

const documentStatusLabels: Record<DocumentStatus, string> = {
  RASCUNHO: 'Rascunho',
  ENVIADO_PARA_ASSINATURA: 'Aguardando assinatura',
  ASSINADO: 'Concluído',
  CANCELADO: 'Cancelado',
  REJEITADO: 'Rejeitado',
  ARQUIVADO: 'Arquivado',
}

function documentStatusTone(status: DocumentStatus) {
  if (status === 'ASSINADO') {
    return 'green' as const
  }

  if (status === 'ENVIADO_PARA_ASSINATURA') {
    return 'orange' as const
  }

  if (status === 'CANCELADO' || status === 'REJEITADO') {
    return 'red' as const
  }

  if (status === 'RASCUNHO') {
    return 'blue' as const
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

function formatShortDate(value?: string | null) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatAction(action: string) {
  return action
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/(^|\s)\S/g, (letter) => letter.toUpperCase())
}

export function DashboardPage() {
  const documentsQuery = useQuery({ queryKey: ['documents', 'dashboard'], queryFn: () => listDocuments() })
  const usersQuery = useQuery({ queryKey: ['users'], queryFn: listUsers })
  const auditLogsQuery = useQuery({ queryKey: ['audit-logs', 'dashboard'], queryFn: () => listAuditLogs() })

  const documents = documentsQuery.data ?? []
  const users = usersQuery.data ?? []
  const auditLogs = auditLogsQuery.data ?? []

  const activeDocuments = documents.filter((document) => document.status !== 'ARQUIVADO')
  const pendingDocuments = documents.filter((document) => document.status === 'ENVIADO_PARA_ASSINATURA')
  const signedDocuments = documents.filter((document) => document.status === 'ASSINADO')
  const drafts = documents.filter((document) => document.status === 'RASCUNHO')
  const latestDraft = drafts[0]
  const recentDocuments = [...documents]
    .sort((first, second) => new Date(second.atualizadoEm).getTime() - new Date(first.atualizadoEm).getTime())
    .slice(0, 5)
  const recentActivities = [...auditLogs]
    .sort((first, second) => new Date(second.criadoEm).getTime() - new Date(first.criadoEm).getTime())
    .slice(0, 5)

  const metricCards = [
    {
      label: 'Documentos',
      value: activeDocuments.length,
      detail: `${documents.length} registros totais`,
      icon: FileText,
      iconClassName: 'bg-blue-50 text-brand-500',
    },
    {
      label: 'Aguardando assinatura',
      value: pendingDocuments.length,
      detail: 'Fluxos pendentes',
      icon: Clock3,
      iconClassName: 'bg-orange-50 text-warning',
    },
    {
      label: 'Concluídos',
      value: signedDocuments.length,
      detail: 'Documentos assinados',
      icon: CheckCircle2,
      iconClassName: 'bg-green-50 text-success',
    },
    {
      label: 'Usuários',
      value: users.length,
      detail: 'Contas cadastradas',
      icon: UsersRound,
      iconClassName: 'bg-surface-page text-brand-900',
    },
  ]

  return (
    <div className="grid gap-4 2xl:gap-5">
      <section className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-sm font-medium text-ink sm:text-base">Acompanhe seus documentos e assinaturas.</p>
          <p className="mt-1 text-sm text-muted">Dados carregados direto da API local do Cortex Sign.</p>
        </div>

        <Link
          className="inline-flex min-h-10 items-center justify-center overflow-hidden rounded-control border border-brand-500 bg-brand-500 text-sm font-medium text-white shadow-[0_14px_32px_rgba(8,126,209,0.20)] transition-colors hover:border-brand-600 hover:bg-brand-600 sm:min-h-11"
          to="/app/assinatura/novo"
        >
          <span className="inline-flex min-h-10 items-center gap-2 px-3.5 sm:min-h-11 sm:px-4"><Plus aria-hidden="true" size={18} />Novo documento</span>
          <span className="grid min-h-10 w-10 place-items-center border-l border-white/18 sm:min-h-11 sm:w-11"><ChevronDown aria-hidden="true" size={18} /></span>
        </Link>
      </section>

      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4 2xl:gap-4">
        {metricCards.map((metric) => (
          <article className="rounded-card border border-line bg-white p-4 shadow-card 2xl:p-5" key={metric.label}>
            <div className="flex items-center gap-4">
              <div className={cn('grid size-10 shrink-0 place-items-center rounded-control 2xl:size-11', metric.iconClassName)}>
                <metric.icon aria-hidden="true" size={20} />
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm text-muted">{metric.label}</p>
                <p className="mt-1 text-[1.65rem] font-semibold leading-none tracking-tight text-ink 2xl:text-2xl">{metric.value}</p>
                <p className="mt-1 text-xs text-muted">{metric.detail}</p>
              </div>
            </div>
          </article>
        ))}
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_21rem] 2xl:grid-cols-[minmax(0,1fr)_28rem] 2xl:gap-5">
        <div className="grid gap-4 2xl:gap-5">
          <PageSection title="Continue de onde parou">
            <div className="p-4">
              {documentsQuery.isLoading && <InlineMessage message="Carregando documentos..." />}
              {!documentsQuery.isLoading && !latestDraft && (
                <EmptyState title="Nenhum rascunho aberto" description="Quando houver um documento em rascunho, ele aparece aqui para retomada rápida." />
              )}
              {latestDraft && (
                <div className="flex flex-col gap-3 rounded-card border border-line p-3.5 lg:flex-row lg:items-center lg:justify-between 2xl:p-4">
                  <div className="flex min-w-0 items-center gap-3 2xl:gap-4">
                    <div className="grid size-10 shrink-0 place-items-center rounded-control bg-brand-50 text-brand-500 2xl:size-12"><FileText aria-hidden="true" size={22} /></div>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="font-medium text-ink">{latestDraft.titulo}</p>
                        <StatusBadge label="Rascunho" tone="blue" />
                      </div>
                      <p className="mt-1 text-sm text-muted">Atualizado em {formatDate(latestDraft.atualizadoEm)}</p>
                    </div>
                  </div>
                  <Link className="inline-flex min-h-10 items-center justify-center gap-2 rounded-control border border-line bg-white px-3.5 text-sm font-medium text-ink transition-colors hover:border-brand-500 hover:text-brand-500" to="/app/assinatura/documentos">
                    Continuar
                    <ChevronDown aria-hidden="true" className="-rotate-90" size={16} />
                  </Link>
                </div>
              )}
            </div>
          </PageSection>

          <PageSection title="Documentos recentes" action={<Link className="text-sm font-medium text-brand-500" to="/app/assinatura/documentos">Ver todos</Link>}>
            {documentsQuery.isError && <div className="p-4"><InlineMessage tone="error" message="Não foi possível carregar documentos." /></div>}
            {!documentsQuery.isLoading && recentDocuments.length === 0 && <div className="p-4"><EmptyState title="Sem documentos recentes" description="Envie um PDF para preencher essa listagem." /></div>}
            {recentDocuments.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] border-collapse text-left">
                  <thead>
                    <tr className="border-b border-line bg-surface-page/70 text-xs font-medium text-muted">
                      <th className="px-4 py-3">Nome do documento</th>
                      <th className="px-4 py-3">Organização</th>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3">Atualizado em</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentDocuments.map((document) => (
                      <tr className="border-b border-line last:border-b-0 hover:bg-surface-page/60" key={document.id}>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <FileText className="shrink-0 text-brand-900" size={18} aria-hidden="true" />
                            <div className="min-w-0">
                              <p className="truncate font-medium text-ink">{document.titulo}</p>
                              <p className="mt-0.5 text-xs text-muted">ID: {document.id}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-sm text-muted">{document.organizacaoNome}</td>
                        <td className="px-4 py-3"><StatusBadge label={documentStatusLabels[document.status]} tone={documentStatusTone(document.status)} /></td>
                        <td className="px-4 py-3 text-sm text-muted">{formatShortDate(document.atualizadoEm)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </PageSection>
        </div>

        <PageSection title="Atividade recente" action={<Link className="text-sm font-medium text-brand-500" to="/app/auditoria">Ver todas</Link>}>
          <div className="grid gap-3 p-4">
            {auditLogsQuery.isLoading && <InlineMessage message="Carregando atividades..." />}
            {!auditLogsQuery.isLoading && recentActivities.length === 0 && <EmptyState title="Sem atividades" description="Eventos de auditoria aparecerão aqui." />}
            {recentActivities.map((activity) => (
              <article className="flex gap-3 rounded-card border border-line p-3" key={activity.id}>
                <div className="grid size-10 shrink-0 place-items-center rounded-full bg-brand-50 text-brand-500">
                  {activity.acao.toLowerCase().includes('assin') ? <CheckCircle2 size={18} aria-hidden="true" /> : activity.acao.toLowerCase().includes('env') ? <Send size={18} aria-hidden="true" /> : <CalendarClock size={18} aria-hidden="true" />}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-ink">{formatAction(activity.acao)}</p>
                  <p className="mt-0.5 text-sm text-muted">{activity.detalhes ?? activity.entidadeTipo ?? 'Evento registrado'}</p>
                  <p className="mt-1 text-xs text-muted">{formatShortDate(activity.criadoEm)}</p>
                </div>
              </article>
            ))}
          </div>
        </PageSection>
      </section>
    </div>
  )
}
