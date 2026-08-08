import { useQuery } from '@tanstack/react-query'
import { Activity, Database } from 'lucide-react'
import { useMemo, useState } from 'react'

import { getApiErrorMessage } from '../../../services/api/api-error'
import { DataTable, type DataTableColumn } from '../../../shared/ui/data-table'
import { EmptyState } from '../../../shared/ui/empty-state'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { PageSection } from '../../../shared/ui/page-section'
import { StatusBadge } from '../../../shared/ui/status-badge'
import { listOrganizations } from '../../organizations/api/organizations-api'
import { listAuditLogs, type AuditLog } from '../api/audit-api'

function formatDate(value?: string | null) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatAction(action: string) {
  return action
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/(^|\s)\S/g, (letter) => letter.toUpperCase())
}

function getActionTone(action: string) {
  const normalized = action.toLowerCase()

  if (normalized.includes('erro') || normalized.includes('excl') || normalized.includes('cancel') || normalized.includes('rejeit')) {
    return 'red' as const
  }

  if (normalized.includes('assin') || normalized.includes('concl')) {
    return 'green' as const
  }

  if (normalized.includes('cria') || normalized.includes('upload') || normalized.includes('env')) {
    return 'blue' as const
  }

  return 'slate' as const
}

function getAuditColumns(): Array<DataTableColumn<AuditLog>> {
  return [
    {
      key: 'evento',
      header: 'Evento',
      sortable: true,
      searchValue: (log) => formatAction(log.acao),
      sortValue: (log) => log.acao,
      cell: (log) => <StatusBadge label={formatAction(log.acao)} tone={getActionTone(log.acao)} />,
    },
    {
      key: 'organizacao',
      header: 'Organização',
      className: 'text-sm text-muted',
      sortable: true,
      searchValue: (log) => log.organizacaoNome,
      cell: (log) => log.organizacaoNome ?? '-',
    },
    {
      key: 'usuario',
      header: 'Usuário',
      className: 'text-sm text-muted',
      sortable: true,
      searchValue: (log) => log.usuarioNome,
      cell: (log) => log.usuarioNome ?? '-',
    },
    {
      key: 'entidade',
      header: 'Entidade',
      className: 'text-sm text-muted',
      sortable: true,
      searchValue: (log) => [log.entidadeTipo, log.entidadeId].filter(Boolean).join(' '),
      sortValue: (log) => log.entidadeTipo,
      cell: (log) => (
        <div>
          <p className="font-medium text-ink">{log.entidadeTipo ?? '-'}</p>
          <p className="mt-0.5 max-w-48 truncate text-xs">{log.entidadeId ?? '-'}</p>
        </div>
      ),
    },
    {
      key: 'detalhes',
      header: 'Detalhes',
      className: 'text-sm text-muted',
      searchValue: (log) => log.detalhes,
      cell: (log) => <p className="max-w-80 truncate">{log.detalhes ?? '-'}</p>,
    },
    {
      key: 'data',
      header: 'Data',
      className: 'text-sm text-muted',
      sortable: true,
      searchValue: (log) => formatDate(log.criadoEm),
      sortValue: (log) => log.criadoEm ? new Date(log.criadoEm) : null,
      cell: (log) => formatDate(log.criadoEm),
    },
  ]
}

export function AuditPage() {
  const [organizationFilter, setOrganizationFilter] = useState('')

  const organizationsQuery = useQuery({ queryKey: ['organizations'], queryFn: listOrganizations })
  const auditLogsQuery = useQuery({
    queryKey: ['audit-logs', organizationFilter],
    queryFn: () => listAuditLogs(organizationFilter || undefined),
  })

  const logs = auditLogsQuery.data ?? []
  const auditColumns = useMemo(() => getAuditColumns(), [])

  const todayLogs = useMemo(() => {
    const today = new Date().toDateString()
    return logs.filter((log) => new Date(log.criadoEm).toDateString() === today).length
  }, [logs])

  const documentLogs = logs.filter((log) => log.entidadeTipo?.toLowerCase().includes('documento')).length
  const signatureLogs = logs.filter((log) => log.entidadeTipo?.toLowerCase().includes('assin')).length

  return (
    <div className="grid gap-4 2xl:gap-5">
      <section className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <p className="text-sm font-medium text-ink sm:text-base">Acompanhe eventos registrados pela API.</p>
          <p className="mt-1 text-sm text-muted">Auditoria ajuda a validar ações de documentos, signatários e assinaturas.</p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <select
            className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink shadow-card transition-colors focus:border-brand-500"
            value={organizationFilter}
            onChange={(event) => setOrganizationFilter(event.target.value)}
          >
            <option value="">Todas as organizações</option>
            {organizationsQuery.data?.map((organization) => (
              <option key={organization.id} value={organization.id}>{organization.nome}</option>
            ))}
          </select>
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-3 2xl:gap-4">
        <article className="rounded-card border border-line bg-white p-4 shadow-card">
          <div className="flex items-center gap-3">
            <div className="grid size-10 place-items-center rounded-control bg-brand-50 text-brand-500"><Activity size={19} aria-hidden="true" /></div>
            <div>
              <p className="text-sm text-muted">Eventos totais</p>
              <p className="mt-1 text-2xl font-semibold text-ink">{logs.length}</p>
            </div>
          </div>
        </article>
        <article className="rounded-card border border-line bg-white p-4 shadow-card">
          <div className="flex items-center gap-3">
            <div className="grid size-10 place-items-center rounded-control bg-green-50 text-success"><Database size={19} aria-hidden="true" /></div>
            <div>
              <p className="text-sm text-muted">Hoje</p>
              <p className="mt-1 text-2xl font-semibold text-ink">{todayLogs}</p>
            </div>
          </div>
        </article>
        <article className="rounded-card border border-line bg-white p-4 shadow-card">
          <div className="flex items-center gap-3">
            <div className="grid size-10 place-items-center rounded-control bg-orange-50 text-warning"><Activity size={19} aria-hidden="true" /></div>
            <div>
              <p className="text-sm text-muted">Documentos / assinaturas</p>
              <p className="mt-1 text-2xl font-semibold text-ink">{documentLogs + signatureLogs}</p>
            </div>
          </div>
        </article>
      </section>

      <PageSection title="Eventos recentes">
        {auditLogsQuery.isLoading && <div className="p-4"><InlineMessage message="Carregando auditoria..." /></div>}
        {auditLogsQuery.isError && <div className="p-4"><InlineMessage tone="error" message={getApiErrorMessage(auditLogsQuery.error)} /></div>}
        {!auditLogsQuery.isLoading && logs.length === 0 && (
          <div className="p-4"><EmptyState title="Nenhum evento encontrado" description="Quando a API registrar ações, elas aparecerão aqui." /></div>
        )}
        {logs.length > 0 && (
          <DataTable
            columns={auditColumns}
            emptyMessage="Nenhum evento encontrado para essa busca"
            getRowKey={(log) => log.id}
            minWidth="860px"
            rows={logs}
            searchPlaceholder="Buscar evento, usuário, entidade ou ID"
          />
        )}
      </PageSection>
    </div>
  )
}
