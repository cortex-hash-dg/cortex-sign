type ModulePlaceholderPageProps = {
  title: string
  description: string
}

export function ModulePlaceholderPage({
  title,
  description,
}: ModulePlaceholderPageProps) {
  return (
    <section className="rounded-card border border-line bg-white p-6 shadow-card">
      <p className="text-sm font-medium text-brand-500">Em construção</p>
      <h2 className="mt-2 text-2xl font-semibold tracking-tight text-ink">
        {title}
      </h2>
      <p className="mt-3 max-w-2xl text-sm leading-6 text-muted">
        {description}
      </p>
    </section>
  )
}
