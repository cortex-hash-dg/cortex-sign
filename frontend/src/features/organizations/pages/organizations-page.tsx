import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Pencil, Plus, Trash2 } from 'lucide-react'
import { type FormEvent, useMemo, useState } from 'react'
import { toast } from 'sonner'

import { getApiErrorMessage } from '../../../services/api/api-error'
import { Button } from '../../../shared/ui/button'
import { DataTable, type DataTableColumn } from '../../../shared/ui/data-table'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { PageSection } from '../../../shared/ui/page-section'
import { StatusBadge } from '../../../shared/ui/status-badge'
import { useCurrentUser } from '../../auth/hooks/use-current-user'
import { listUsers } from '../../users/api/users-api'
import {
  addOrganizationMember,
  listOrganizationMembers,
  removeOrganizationMember,
  updateOrganizationMember,
  type OrganizationMember,
  type OrganizationMemberRole,
  type OrganizationMemberStatus,
} from '../api/organization-members-api'
import {
  createOrganization,
  deleteOrganization,
  listOrganizations,
  type Organization,
  updateOrganization,
} from '../api/organizations-api'

type OrganizationFormState = {
  nome: string
  numeroDocumento: string
}

type MemberFormState = {
  usuarioId: string
  papel: OrganizationMemberRole
  status: OrganizationMemberStatus
}

const emptyForm: OrganizationFormState = {
  nome: '',
  numeroDocumento: '',
}

const emptyMemberForm: MemberFormState = {
  usuarioId: '',
  papel: 'OPERADOR',
  status: 'ATIVO',
}

const memberRoleOptions: Array<{ label: string; value: OrganizationMemberRole }> = [
  { label: 'Proprietário', value: 'PROPRIETARIO' },
  { label: 'Administrador', value: 'ADMINISTRADOR' },
  { label: 'Gestor', value: 'GESTOR' },
  { label: 'Operador', value: 'OPERADOR' },
  { label: 'Auditor', value: 'AUDITOR' },
]

const memberStatusOptions: Array<{ label: string; value: OrganizationMemberStatus }> = [
  { label: 'Ativo', value: 'ATIVO' },
  { label: 'Convidado', value: 'CONVIDADO' },
  { label: 'Suspenso', value: 'SUSPENSO' },
  { label: 'Removido', value: 'REMOVIDO' },
]

function formatMemberRole(role: OrganizationMemberRole) {
  return memberRoleOptions.find((option) => option.value === role)?.label ?? role
}

function formatMemberStatus(status: OrganizationMemberStatus) {
  return memberStatusOptions.find((option) => option.value === status)?.label ?? status
}

function getMemberStatusTone(status: OrganizationMemberStatus) {
  if (status === 'ATIVO') {
    return 'green' as const
  }

  if (status === 'SUSPENSO' || status === 'REMOVIDO') {
    return 'red' as const
  }

  return 'orange' as const
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function OrganizationsPage() {
  const queryClient = useQueryClient()
  const currentUserQuery = useCurrentUser()
  const isSuperAdmin = currentUserQuery.data?.perfil === 'SUPER_ADMINISTRADOR'
  const [selectedOrganization, setSelectedOrganization] = useState<Organization | null>(null)
  const [selectedMember, setSelectedMember] = useState<OrganizationMember | null>(null)
  const [form, setForm] = useState<OrganizationFormState>(emptyForm)
  const [memberForm, setMemberForm] = useState<MemberFormState>(emptyMemberForm)

  const organizationsQuery = useQuery({
    queryKey: ['organizations'],
    queryFn: listOrganizations,
    enabled: isSuperAdmin,
  })
  const usersQuery = useQuery({ queryKey: ['users'], queryFn: listUsers, enabled: isSuperAdmin })
  const membersQuery = useQuery({
    queryKey: ['organization-members', selectedOrganization?.id],
    queryFn: () => listOrganizationMembers(selectedOrganization!.id),
    enabled: isSuperAdmin && Boolean(selectedOrganization?.id),
  })

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = {
        nome: form.nome.trim(),
        numeroDocumento: form.numeroDocumento.trim() || undefined,
      }

      if (selectedOrganization) {
        return updateOrganization(selectedOrganization.id, payload)
      }

      return createOrganization(payload)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      toast.success(selectedOrganization ? 'Organização atualizada.' : 'Organização criada.')
      clearForm()
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteOrganization,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      toast.success('Organização excluída.')
      clearForm()
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const saveMemberMutation = useMutation({
    mutationFn: async () => {
      if (!selectedOrganization) {
        throw new Error('Selecione uma organização antes de gerenciar membros.')
      }

      if (selectedMember) {
        return updateOrganizationMember(selectedOrganization.id, selectedMember.id, {
          papel: memberForm.papel,
          status: memberForm.status,
        })
      }

      return addOrganizationMember(selectedOrganization.id, {
        usuarioId: memberForm.usuarioId,
        papel: memberForm.papel,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization-members', selectedOrganization?.id] })
      queryClient.invalidateQueries({ queryKey: ['my-organizations'] })
      toast.success(selectedMember ? 'Membro atualizado.' : 'Membro adicionado.')
      clearMemberForm()
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const removeMemberMutation = useMutation({
    mutationFn: async (member: OrganizationMember) => {
      if (!selectedOrganization) {
        throw new Error('Selecione uma organização antes de remover membros.')
      }

      return removeOrganizationMember(selectedOrganization.id, member.id)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization-members', selectedOrganization?.id] })
      queryClient.invalidateQueries({ queryKey: ['my-organizations'] })
      toast.success('Membro removido.')
      clearMemberForm()
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const organizations = organizationsQuery.data ?? []
  const orderedOrganizations = useMemo(
    () => [...organizations].sort((first, second) => first.nome.localeCompare(second.nome)),
    [organizations],
  )
  const members = membersQuery.data ?? []

  const organizationColumns = useMemo<Array<DataTableColumn<Organization>>>(() => [
    {
      key: 'organizacao',
      header: 'Organização',
      sortable: true,
      searchValue: (organization) => `${organization.nome} ${organization.numeroDocumento ?? ''} ${organization.id}`,
      sortValue: (organization) => organization.nome,
      cell: (organization) => (
        <div className="flex items-center gap-3">
          <div className="grid size-9 place-items-center rounded-control bg-brand-50 text-brand-500">
            <Building2 aria-hidden="true" size={18} />
          </div>
          <div>
            <p className="font-medium text-ink">{organization.nome}</p>
            <p className="mt-0.5 text-xs text-muted">ID: {organization.id}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'documento',
      header: 'Documento',
      sortable: true,
      searchValue: (organization) => organization.numeroDocumento ?? '',
      sortValue: (organization) => organization.numeroDocumento ?? '',
      cell: (organization) => <span className="text-sm text-muted">{organization.numeroDocumento || '-'}</span>,
    },
    {
      key: 'atualizado',
      header: 'Atualizada em',
      sortable: true,
      searchValue: (organization) => formatDate(organization.atualizadoEm),
      sortValue: (organization) => organization.atualizadoEm,
      cell: (organization) => <span className="text-sm text-muted">{formatDate(organization.atualizadoEm)}</span>,
    },
    {
      key: 'acoes',
      header: 'Ações',
      headerClassName: 'text-right',
      className: 'text-right',
      cell: (organization) => (
        <div className="flex justify-end gap-2">
          <button
            className="grid size-8 place-items-center rounded-control text-muted transition-colors hover:bg-brand-50 hover:text-brand-500"
            type="button"
            onClick={() => editOrganization(organization)}
            aria-label={`Editar ${organization.nome}`}
          >
            <Pencil aria-hidden="true" size={16} />
          </button>
          <button
            className="grid size-8 place-items-center rounded-control text-muted transition-colors hover:bg-red-50 hover:text-danger"
            type="button"
            onClick={() => confirmDelete(organization)}
            aria-label={`Excluir ${organization.nome}`}
          >
            <Trash2 aria-hidden="true" size={16} />
          </button>
        </div>
      ),
    },
  ], [])

  const memberColumns = useMemo<Array<DataTableColumn<OrganizationMember>>>(() => [
    {
      key: 'membro',
      header: 'Membro',
      sortable: true,
      searchValue: (member) => `${member.usuarioNome} ${member.usuarioEmail}`,
      sortValue: (member) => member.usuarioNome,
      cell: (member) => (
        <div>
          <p className="font-medium text-ink">{member.usuarioNome}</p>
          <p className="mt-0.5 text-xs text-muted">{member.usuarioEmail}</p>
        </div>
      ),
    },
    {
      key: 'papel',
      header: 'Papel',
      sortable: true,
      searchValue: (member) => formatMemberRole(member.papel),
      sortValue: (member) => formatMemberRole(member.papel),
      cell: (member) => <span className="text-sm text-muted">{formatMemberRole(member.papel)}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      searchValue: (member) => formatMemberStatus(member.status),
      sortValue: (member) => formatMemberStatus(member.status),
      cell: (member) => (
        <StatusBadge
          label={formatMemberStatus(member.status)}
          tone={getMemberStatusTone(member.status)}
        />
      ),
    },
    {
      key: 'acoes',
      header: 'Ações',
      headerClassName: 'text-right',
      className: 'text-right',
      cell: (member) => (
        <div className="flex justify-end gap-2">
          <button
            className="grid size-8 place-items-center rounded-control text-muted transition-colors hover:bg-brand-50 hover:text-brand-500"
            type="button"
            onClick={() => editMember(member)}
            aria-label={`Editar vínculo de ${member.usuarioNome}`}
          >
            <Pencil aria-hidden="true" size={16} />
          </button>
          <button
            className="grid size-8 place-items-center rounded-control text-muted transition-colors hover:bg-red-50 hover:text-danger"
            type="button"
            onClick={() => confirmRemoveMember(member)}
            aria-label={`Remover ${member.usuarioNome}`}
          >
            <Trash2 aria-hidden="true" size={16} />
          </button>
        </div>
      ),
    },
  ], [])

  function clearForm() {
    setSelectedOrganization(null)
    clearMemberForm()
    setForm(emptyForm)
  }

  function clearMemberForm() {
    setSelectedMember(null)
    setMemberForm(emptyMemberForm)
  }

  function editOrganization(organization: Organization) {
    setSelectedOrganization(organization)
    setForm({
      nome: organization.nome,
      numeroDocumento: organization.numeroDocumento ?? '',
    })
    clearMemberForm()
  }

  function editMember(member: OrganizationMember) {
    setSelectedMember(member)
    setMemberForm({
      usuarioId: member.usuarioId,
      papel: member.papel,
      status: member.status,
    })
  }

  function submitForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    saveMutation.mutate()
  }

  function submitMemberForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    saveMemberMutation.mutate()
  }

  function confirmDelete(organization: Organization) {
    const shouldDelete = window.confirm(`Excluir a organização ${organization.nome}?`)

    if (shouldDelete) {
      deleteMutation.mutate(organization.id)
    }
  }

  function confirmRemoveMember(member: OrganizationMember) {
    const shouldRemove = window.confirm(`Remover ${member.usuarioNome} desta organização?`)

    if (shouldRemove) {
      removeMemberMutation.mutate(member)
    }
  }

  if (currentUserQuery.isLoading) {
    return (
      <PageSection title="Organizações">
        <div className="p-4">
          <InlineMessage message="Carregando permissões..." />
        </div>
      </PageSection>
    )
  }

  if (!isSuperAdmin) {
    return (
      <PageSection title="Acesso restrito">
        <div className="p-4">
          <InlineMessage
            message="Organizações são gerenciadas apenas pelo Super Admin. Sua conta continua acessando somente os dados da sua organização."
          />
        </div>
      </PageSection>
    )
  }

  return (
    <div className="grid gap-4 2xl:gap-5">
      <section className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-sm font-medium text-ink sm:text-base">
            Gerencie organizações vinculadas à plataforma.
          </p>
          <p className="mt-1 text-sm text-muted">
            Use este cadastro para empresas, órgãos ou clientes que assinam documentos.
          </p>
        </div>

        <Button onClick={clearForm}>
          <Plus aria-hidden="true" size={17} />
          Nova organização
        </Button>
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem] 2xl:grid-cols-[minmax(0,1fr)_26rem]">
        <PageSection title="Organizações cadastradas">
          {organizationsQuery.isLoading && (
            <div className="p-4">
              <InlineMessage message="Carregando organizações..." />
            </div>
          )}

          {organizationsQuery.isError && (
            <div className="p-4">
              <InlineMessage tone="error" message={getApiErrorMessage(organizationsQuery.error)} />
            </div>
          )}

          {!organizationsQuery.isLoading && !organizationsQuery.isError && (
            <DataTable
              rows={orderedOrganizations}
              columns={organizationColumns}
              getRowKey={(organization) => organization.id}
              minWidth="680px"
              pageSize={10}
              searchPlaceholder="Buscar por organização, documento ou ID"
              emptyMessage="Nenhuma organização cadastrada"
            />
          )}
        </PageSection>

        <PageSection title={selectedOrganization ? 'Editar organização' : 'Nova organização'}>
          <form className="grid gap-4 p-4" onSubmit={submitForm}>
            <label className="grid gap-2 text-sm font-medium text-ink">
              Nome
              <input
                className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand-500"
                value={form.nome}
                onChange={(event) => setForm((current) => ({ ...current, nome: event.target.value }))}
                placeholder="Nome da organização"
                required
              />
            </label>

            <label className="grid gap-2 text-sm font-medium text-ink">
              CPF/CNPJ ou identificação
              <input
                className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand-500"
                value={form.numeroDocumento}
                onChange={(event) => setForm((current) => ({ ...current, numeroDocumento: event.target.value }))}
                placeholder="Ex.: 12345678000199"
              />
            </label>

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              {selectedOrganization && (
                <Button type="button" variant="secondary" onClick={clearForm}>
                  Cancelar
                </Button>
              )}
              <Button type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Salvando...' : selectedOrganization ? 'Salvar alterações' : 'Criar organização'}
              </Button>
            </div>
          </form>
        </PageSection>
      </section>

      {selectedOrganization && (
        <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem] 2xl:grid-cols-[minmax(0,1fr)_26rem]">
          <PageSection title={`Membros de ${selectedOrganization.nome}`}>
            {membersQuery.isLoading && (
              <div className="p-4">
                <InlineMessage message="Carregando membros..." />
              </div>
            )}

            {membersQuery.isError && (
              <div className="p-4">
                <InlineMessage tone="error" message={getApiErrorMessage(membersQuery.error)} />
              </div>
            )}

            {!membersQuery.isLoading && !membersQuery.isError && (
              <DataTable
                rows={members}
                columns={memberColumns}
                getRowKey={(member) => member.id}
                minWidth="680px"
                pageSize={10}
                searchPlaceholder="Buscar por membro, e-mail, papel ou status"
                emptyMessage="Nenhum membro vinculado"
              />
            )}
          </PageSection>

          <PageSection title={selectedMember ? 'Editar membro' : 'Adicionar membro'}>
            <form className="grid gap-4 p-4" onSubmit={submitMemberForm}>
              <label className="grid gap-2 text-sm font-medium text-ink">
                Usuário
                <select
                  className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors focus:border-brand-500 disabled:bg-surface-page"
                  value={memberForm.usuarioId}
                  onChange={(event) => setMemberForm((current) => ({ ...current, usuarioId: event.target.value }))}
                  disabled={Boolean(selectedMember)}
                  required
                >
                  <option value="">Selecione um usuário</option>
                  {usersQuery.data?.map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.nome} - {user.email}
                    </option>
                  ))}
                </select>
              </label>

              <label className="grid gap-2 text-sm font-medium text-ink">
                Papel na organização
                <select
                  className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors focus:border-brand-500"
                  value={memberForm.papel}
                  onChange={(event) => setMemberForm((current) => ({
                    ...current,
                    papel: event.target.value as OrganizationMemberRole,
                  }))}
                >
                  {memberRoleOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              {selectedMember && (
                <label className="grid gap-2 text-sm font-medium text-ink">
                  Status
                  <select
                    className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors focus:border-brand-500"
                    value={memberForm.status}
                    onChange={(event) => setMemberForm((current) => ({
                      ...current,
                      status: event.target.value as OrganizationMemberStatus,
                    }))}
                  >
                    {memberStatusOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
              )}

              <div className="rounded-control border border-line bg-surface-page/70 p-3 text-xs leading-relaxed text-muted">
                O usuário é a conta pessoal. O papel acima vale apenas dentro desta organização.
              </div>

              <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                {selectedMember && (
                  <Button type="button" variant="secondary" onClick={clearMemberForm}>
                    Cancelar
                  </Button>
                )}
                <Button type="submit" disabled={saveMemberMutation.isPending}>
                  {saveMemberMutation.isPending
                    ? 'Salvando...'
                    : selectedMember
                      ? 'Salvar vínculo'
                      : 'Adicionar membro'}
                </Button>
              </div>
            </form>
          </PageSection>
        </section>
      )}
    </div>
  )
}
