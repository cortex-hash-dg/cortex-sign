import { CheckCircle2, Palette } from 'lucide-react'
import { Button } from '../../shared/ui/button'

const foundations = [
  'Tailwind e tokens visuais',
  'Rotas da aplicação',
  'Cache e sincronização da API',
  'Cliente HTTP para /api',
  'Acessibilidade e movimento reduzido',
]

export function FoundationPage() {
  return (
    <main className="grid min-h-screen place-items-center px-4 py-10 sm:px-6">
      <section className="w-full max-w-2xl rounded-card border border-line bg-surface-card p-6 shadow-card sm:p-8">
        <div className="mb-6 flex size-12 items-center justify-center rounded-control bg-brand-50 text-brand-500">
          <Palette aria-hidden="true" size={24} />
        </div>

        <p className="mb-2 text-sm font-medium text-brand-500">Córtex Sign</p>
        <h1 className="text-3xl font-semibold tracking-tight text-ink">
          Fundação pronta para receber o login
        </h1>
        <p className="mt-3 max-w-xl text-sm leading-6 text-muted">
          Esta página temporária confirma a identidade visual e a infraestrutura
          inicial. Ela será substituída pela tela de login aprovada.
        </p>

        <ul className="my-8 grid gap-3 sm:grid-cols-2">
          {foundations.map((foundation) => (
            <li
              className="flex items-center gap-2 text-sm text-ink"
              key={foundation}
            >
              <CheckCircle2
                aria-hidden="true"
                className="shrink-0 text-success"
                size={18}
              />
              {foundation}
            </li>
          ))}
        </ul>

        <Button>Continuar para o login</Button>
      </section>
    </main>
  )
}
