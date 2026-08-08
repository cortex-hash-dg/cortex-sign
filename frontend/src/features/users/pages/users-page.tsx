import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2, UserRound } from 'lucide-react'
import { type FormEvent, useMemo, useState } from 'react'
import { toast } from 'sonner'

import { getApiErrorMessage } from '../../../services/api/api-error'
import { Button } from '../../../shared/ui/button'
import { DataTable, type DataTableColumn } from '../../../shared/ui/data-table'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { PageSection } from '../../../shared/ui/page-section'
import { StatusBadge } from '../../../shared/ui/status-badge'
import { useCurrentUser } from '../../auth/hooks/use-current-user'
import { listOrganizations } from '../../organizations/api/organizations-api'
import {
  createUser,
  deleteUser,
  listUsers,
  type User,
  type UserRole,
  updateUser,
} from '../api/users-api'

type UserFormState = {
  organizacaoId: string
  nome: string
  email: string
  senha: string
  perfil: UserRole
  ativo: boolean
}

const roleOptions: Array<{ label: string; value: UserRole }> = [
  { label: 'Super administrador', value: 'SUPER_ADMINISTRADOR' },
  { label: 'Administrador da organização', value: 'ADMINISTRADOR_ORGANIZACAO' },
  { label: 'Gestor', value: 'GESTOR' },
  { label: 'Operador', value: 'OPERADOR' },
  { label: 'Auditor', value: 'AUDITOR' },
]

const emptyForm: UserFormState = {
  organizacaoId: '',
  nome: '',
  email: '',
  senha: '',
  perfil: 'OPERADOR',
  ativo: true,
}

function formatRole(role: UserRole) {
  return roleOptions.find((option) => option.value === role)?.label ?? role
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

export function UsersPage() {
  const queryClient = useQueryClient()
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [form, setForm] = useState<UserFormState>(emptyForm)

  const currentUserQuery = useCurrentUser()
  const currentUser = currentUserQuery.data
  const isSuperAdmin = currentUser?.perfil === 'SUPER_ADMINISTRADOR'
  const availableRoleOptions = useMemo(
    () => isSuperAdmin
      ? roleOptions
      : roleOptions.filter((option) => option.value !== 'SUPER_ADMINISTRADOR'),
    [isSuperAdmin],
  )

  const usersQuery = useQuery({ queryKey: ['users'], queryFn: listUsers })
  const organizationsQuery = useQuery({
    queryKey: ['organizations'],
    queryFn: listOrganizations,
    enabled: isSuperAdmin,
  })

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (selectedUser) {
        return updateUser(selectedUser.id, {
          organizacaoId: form.organizacaoId || undefined,
          nome: form.nome.trim(),
          email: form.email.trim(),
          perfil: form.perfil,
          ativo: form.ativo,
        })
      }

      return createUser({
        organizacaoId: form.organizacaoId || undefined,
        nome: form.nome.trim(),
        email: form.email.trim(),
        senha: form.senha,
        perfil: form.perfil,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      toast.success(selectedUser ? 'Usuário atualizado.' : 'Usuário criado.')
      clearForm()
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      toast.success('Usuário excluído.')
      clearForm()
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const users = usersQuery.data ?? []
  const orderedUsers = useMemo(
    () => [...users].sort((first, second) => first.nome.localeCompare(second.nome)),
    [users],
  )

  const userColumns = useMemo<Array<DataTableColumn<User>>>(() => [
    {
      key: 'usuario',
      header: 'Usuário',
      sortable: true,
      searchValue: (user) => `${user.nome} ${user.email}`,
      sortValue: (user) => user.nome,
      cell: (user) => (
        <div className="flex items-center gap-3">
          <div className="grid size-9 place-items-center rounded-control bg-brand-50 text-brand-500">
            <UserRound aria-hidden="true" size={18} />
          </div>
          <div>
            <p className="font-medium text-ink">{user.nome}</p>
            <p className="mt-0.5 text-xs text-muted">{user.email}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'organizacao',
      header: 'Organização',
      sortable: true,
      searchValue: (user) => user.organizacaoNome,
      sortValue: (user) => user.organizacaoNome,
      cell: (user) => <span className="text-sm text-muted">{user.organizacaoNome}</span>,
    },
    {
      key: 'perfil',
      header: 'Perfil',
      sortable: true,
      searchValue: (user) => formatRole(user.perfil),
      sortValue: (user) => formatRole(user.perfil),
      cell: (user) => <span className="text-sm text-muted">{formatRole(user.perfil)}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      searchValue: (user) => (user.ativo ? 'Ativo' : 'Inativo'),
      sortValue: (user) => (user.ativo ? 'Ativo' : 'Inativo'),
      cell: (user) => (
        <StatusBadge label={user.ativo ? 'Ativo' : 'Inativo'} tone={user.ativo ? 'green' : 'slate'} />
      ),
    },
    {
      key: 'atualizado',
      header: 'Atualizado em',
      sortable: true,
      searchValue: (user) => formatDate(user.atualizadoEm),
      sortValue: (user) => user.atualizadoEm,
      cell: (user) => <span className="text-sm text-muted">{formatDate(user.atualizadoEm)}</span>,
    },
    {
      key: 'acoes',
      header: 'Ações',
      headerClassName: 'text-right',
      className: 'text-right',
      cell: (user) => (
        <div className="flex justify-end gap-2">
          <button
            className="grid size-8 place-items-center rounded-control text-muted transition-colors hover:bg-brand-50 hover:text-brand-500"
            type="button"
            onClick={() => editUser(user)}
            aria-label={`Editar ${user.nome}`}
          >
            <Pencil aria-hidden="true" size={16} />
          </button>
          <button
            className="grid size-8 place-items-center rounded-control text-muted transition-colors hover:bg-red-50 hover:text-danger"
            type="button"
            onClick={() => confirmDelete(user)}
            aria-label={`Excluir ${user.nome}`}
          >
            <Trash2 aria-hidden="true" size={16} />
          </button>
        </div>
      ),
    },
  ], [])

  function clearForm() {
    setSelectedUser(null)
    setForm(emptyForm)
  }

  function editUser(user: User) {
    setSelectedUser(user)
    setForm({
      organizacaoId: user.organizacaoId,
      nome: user.nome,
      email: user.email,
      senha: '',
      perfil: user.perfil,
      ativo: user.ativo,
    })
  }

  function submitForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    saveMutation.mutate()
  }

  function confirmDelete(user: User) {
    const shouldDelete = window.confirm(`Excluir o usuário ${user.nome}?`)

    if (shouldDelete) {
      deleteMutation.mutate(user.id)
    }
  }

  return (
    <div className="grid gap-4 2xl:gap-5">
      <section className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-sm font-medium text-ink sm:text-base">
            Gerencie usuários, perfis e vínculo com organizações.
          </p>
          <p className="mt-1 text-sm text-muted">
            Os perfis seguem as permissões já aplicadas no backend.
          </p>
        </div>

        <Button onClick={clearForm}>
          <Plus aria-hidden="true" size={17} />
          Novo usuário
        </Button>
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_24rem] 2xl:grid-cols-[minmax(0,1fr)_28rem]">
        <PageSection title="Usuários cadastrados">
          {usersQuery.isLoading && (
            <div className="p-4">
              <InlineMessage message="Carregando usuários..." />
            </div>
          )}

          {usersQuery.isError && (
            <div className="p-4">
              <InlineMessage tone="error" message={getApiErrorMessage(usersQuery.error)} />
            </div>
          )}

          {!usersQuery.isLoading && !usersQuery.isError && (
            <DataTable
              rows={orderedUsers}
              columns={userColumns}
              getRowKey={(user) => user.id}
              minWidth="820px"
              pageSize={10}
              searchPlaceholder="Buscar por usuário, e-mail, perfil ou organização"
              emptyMessage="Nenhum usuário cadastrado"
            />
          )}
        </PageSection>

        <PageSection title={selectedUser ? 'Editar usuário' : 'Novo usuário'}>
          <form className="grid gap-4 p-4" onSubmit={submitForm}>
            {isSuperAdmin ? (
              <label className="grid gap-2 text-sm font-medium text-ink">
                Organização
                <select
                  className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors focus:border-brand-500"
                  value={form.organizacaoId}
                  onChange={(event) => setForm((current) => ({ ...current, organizacaoId: event.target.value }))}
                >
                  <option value="">Sem organização / Super Admin</option>
                  {organizationsQuery.data?.map((organization) => (
                    <option key={organization.id} value={organization.id}>
                      {organization.nome}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <div className="rounded-control border border-line bg-surface-page/70 p-3 text-sm text-muted">
                <strong className="font-medium text-ink">Organização:</strong>{' '}
                {currentUser?.organizacaoNome ?? 'Organização do usuário logado'}
              </div>
            )}

            <label className="grid gap-2 text-sm font-medium text-ink">
              Nome
              <input
                className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand-500"
                value={form.nome}
                onChange={(event) => setForm((current) => ({ ...current, nome: event.target.value }))}
                placeholder="Nome completo"
                required
              />
            </label>

            <label className="grid gap-2 text-sm font-medium text-ink">
              E-mail
              <input
                className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand-500"
                value={form.email}
                onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                placeholder="nome@empresa.com.br"
                type="email"
                required
              />
            </label>

            {!selectedUser && (
              <label className="grid gap-2 text-sm font-medium text-ink">
                Senha inicial
                <input
                  className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand-500"
                  value={form.senha}
                  onChange={(event) => setForm((current) => ({ ...current, senha: event.target.value }))}
                  placeholder="Mínimo 8 caracteres"
                  type="password"
                  minLength={8}
                  required
                />
              </label>
            )}

            <label className="grid gap-2 text-sm font-medium text-ink">
              Perfil
              <select
                className="min-h-10 rounded-control border border-line bg-white px-3 text-sm text-ink transition-colors focus:border-brand-500"
                value={form.perfil}
                onChange={(event) => setForm((current) => ({ ...current, perfil: event.target.value as UserRole }))}
              >
                {availableRoleOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            {selectedUser && (
              <label className="flex items-center gap-2 text-sm font-medium text-ink">
                <input
                  className="size-4 rounded border-line accent-brand-500"
                  type="checkbox"
                  checked={form.ativo}
                  onChange={(event) => setForm((current) => ({ ...current, ativo: event.target.checked }))}
                />
                Usuário ativo
              </label>
            )}

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              {selectedUser && (
                <Button type="button" variant="secondary" onClick={clearForm}>
                  Cancelar
                </Button>
              )}
              <Button type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Salvando...' : selectedUser ? 'Salvar alterações' : 'Criar usuário'}
              </Button>
            </div>
          </form>
        </PageSection>
      </section>
    </div>
  )
}
