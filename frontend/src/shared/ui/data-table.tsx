import { ChevronDown, ChevronLeft, ChevronRight, ChevronsUpDown, Search } from 'lucide-react'
import { type ReactNode, useEffect, useMemo, useState } from 'react'

import { cn } from '../lib/cn'

type SortDirection = 'asc' | 'desc'
type SortValue = string | number | Date | null | undefined

export type DataTableColumn<TData> = {
  key: string
  header: ReactNode
  cell: (row: TData) => ReactNode
  searchValue?: (row: TData) => SortValue
  sortValue?: (row: TData) => SortValue
  sortable?: boolean
  className?: string
  headerClassName?: string
}

type DataTableProps<TData> = {
  rows: TData[]
  columns: Array<DataTableColumn<TData>>
  getRowKey: (row: TData) => string
  minWidth?: string
  pageSize?: number
  searchPlaceholder?: string
  emptyMessage?: string
}

function normalizeSortValue(value: SortValue) {
  if (value instanceof Date) {
    return value.getTime()
  }

  if (typeof value === 'string') {
    return value.toLowerCase()
  }

  return value ?? ''
}

export function DataTable<TData>({
  rows,
  columns,
  getRowKey,
  minWidth = '760px',
  pageSize = 10,
  searchPlaceholder = 'Buscar na tabela',
  emptyMessage = 'Nenhum registro encontrado',
}: DataTableProps<TData>) {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [sort, setSort] = useState<{ key: string; direction: SortDirection } | null>(null)

  const filteredRows = useMemo(() => {
    const term = search.trim().toLowerCase()

    if (!term) {
      return rows
    }

    return rows.filter((row) => columns.some((column) => {
      const value = column.searchValue?.(row)

      return String(value ?? '').toLowerCase().includes(term)
    }))
  }, [columns, rows, search])

  const sortedRows = useMemo(() => {
    if (!sort) {
      return filteredRows
    }

    const column = columns.find((item) => item.key === sort.key)

    if (!column) {
      return filteredRows
    }

    return [...filteredRows].sort((leftRow, rightRow) => {
      const getValue = column.sortValue ?? column.searchValue
      const leftValue = normalizeSortValue(getValue?.(leftRow))
      const rightValue = normalizeSortValue(getValue?.(rightRow))

      if (leftValue < rightValue) {
        return sort.direction === 'asc' ? -1 : 1
      }

      if (leftValue > rightValue) {
        return sort.direction === 'asc' ? 1 : -1
      }

      return 0
    })
  }, [columns, filteredRows, sort])

  const totalPages = Math.max(1, Math.ceil(sortedRows.length / pageSize))
  const pageRows = sortedRows.slice((page - 1) * pageSize, page * pageSize)
  const firstItem = sortedRows.length === 0 ? 0 : ((page - 1) * pageSize) + 1
  const lastItem = Math.min(page * pageSize, sortedRows.length)

  useEffect(() => {
    setPage(1)
  }, [rows, search, sort])

  function toggleSort(column: DataTableColumn<TData>) {
    if (!column.sortable) {
      return
    }

    setSort((current) => {
      if (current?.key !== column.key) {
        return { key: column.key, direction: 'asc' }
      }

      if (current.direction === 'asc') {
        return { key: column.key, direction: 'desc' }
      }

      return null
    })
  }

  return (
    <div className="min-w-0">
      <div className="border-b border-line p-4">
        <label className="relative block max-w-xl">
          <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" size={16} aria-hidden="true" />
          <input
            className="min-h-10 w-full rounded-control border border-line bg-white pl-9 pr-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand-500"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder={searchPlaceholder}
          />
        </label>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left" style={{ minWidth }}>
          <thead>
            <tr className="border-b border-line bg-surface-page/70 text-xs font-medium text-muted">
              {columns.map((column) => {
                const isSorted = sort?.key === column.key

                return (
                  <th
                    className={cn('px-4 py-3', column.headerClassName)}
                    key={column.key}
                  >
                    {column.sortable ? (
                      <button
                        className="inline-flex items-center gap-1.5 text-left transition-colors hover:text-ink"
                        type="button"
                        onClick={() => toggleSort(column)}
                      >
                        <span>{column.header}</span>
                        {isSorted ? (
                          <ChevronDown
                            className={cn(sort.direction === 'asc' && 'rotate-180')}
                            size={14}
                            aria-hidden="true"
                          />
                        ) : (
                          <ChevronsUpDown size={14} aria-hidden="true" />
                        )}
                      </button>
                    ) : (
                      column.header
                    )}
                  </th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {pageRows.length === 0 && (
              <tr>
                <td className="px-4 py-10 text-center text-sm text-muted" colSpan={columns.length}>
                  {emptyMessage}
                </td>
              </tr>
            )}
            {pageRows.map((row) => (
              <tr
                className="border-b border-line last:border-b-0 hover:bg-surface-page/60"
                key={getRowKey(row)}
              >
                {columns.map((column) => (
                  <td className={cn('px-4 py-3', column.className)} key={column.key}>
                    {column.cell(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-col gap-3 border-t border-line px-4 py-3 text-sm text-muted sm:flex-row sm:items-center sm:justify-between">
        <span>
          Mostrando {firstItem} a {lastItem} de {sortedRows.length} registros
        </span>
        <div className="flex items-center gap-2">
          <button
            className="grid size-9 place-items-center rounded-control border border-line bg-white text-ink disabled:opacity-45"
            type="button"
            disabled={page === 1}
            onClick={() => setPage((current) => Math.max(1, current - 1))}
            aria-label="Página anterior"
          >
            <ChevronLeft size={16} aria-hidden="true" />
          </button>
          <span className="rounded-control bg-surface-page px-3 py-2 text-xs font-medium text-ink">{page} / {totalPages}</span>
          <button
            className="grid size-9 place-items-center rounded-control border border-line bg-white text-ink disabled:opacity-45"
            type="button"
            disabled={page === totalPages}
            onClick={() => setPage((current) => Math.min(totalPages, current + 1))}
            aria-label="Próxima página"
          >
            <ChevronRight size={16} aria-hidden="true" />
          </button>
        </div>
      </div>
    </div>
  )
}
