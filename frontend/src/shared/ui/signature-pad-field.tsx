import { Eraser, PenLine } from 'lucide-react'
import { type PointerEvent, useEffect, useRef, useState } from 'react'

type SignaturePadFieldProps = {
  enabled: boolean
  onEnabledChange: (enabled: boolean) => void
  value: string | null
  onChange: (value: string | null) => void
  label?: string
  description?: string
  showToggle?: boolean
}

export function SignaturePadField({
  enabled,
  onEnabledChange,
  value,
  onChange,
  label = 'Incluir assinatura manuscrita digital',
  description = 'Opcional. A validação principal acontece pelo link seguro; a manuscrita aparece no manifesto visual do PDF.',
  showToggle = true,
}: SignaturePadFieldProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const drawingRef = useRef(false)
  const [hasDrawing, setHasDrawing] = useState(Boolean(value))

  useEffect(() => {
    if (!enabled) {
      onChange(null)
      setHasDrawing(false)
      return
    }

    const canvas = canvasRef.current
    if (!canvas) {
      return
    }

    const resizeCanvas = () => {
      const rect = canvas.getBoundingClientRect()
      const ratio = window.devicePixelRatio || 1
      canvas.width = rect.width * ratio
      canvas.height = rect.height * ratio

      const context = canvas.getContext('2d')
      if (!context) {
        return
      }

      context.setTransform(ratio, 0, 0, ratio, 0, 0)
      context.lineCap = 'round'
      context.lineJoin = 'round'
      context.lineWidth = 2.6
      context.strokeStyle = '#082345'

      if (value) {
        const image = new Image()
        image.onload = () => {
          const imageRatio = image.width / image.height
          const maxWidth = rect.width - 24
          const maxHeight = rect.height - 24
          const drawWidth = Math.min(maxWidth, maxHeight * imageRatio)
          const drawHeight = drawWidth / imageRatio
          context.drawImage(image, (rect.width - drawWidth) / 2, (rect.height - drawHeight) / 2, drawWidth, drawHeight)
          setHasDrawing(true)
        }
        image.src = value
      }
    }

    resizeCanvas()
    window.addEventListener('resize', resizeCanvas)
    return () => window.removeEventListener('resize', resizeCanvas)
  }, [enabled, onChange, value])

  function getPoint(event: PointerEvent<HTMLCanvasElement>) {
    const canvas = event.currentTarget
    const rect = canvas.getBoundingClientRect()
    return {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top,
    }
  }

  function startDrawing(event: PointerEvent<HTMLCanvasElement>) {
    const canvas = event.currentTarget
    const context = canvas.getContext('2d')
    if (!context) {
      return
    }

    const point = getPoint(event)
    drawingRef.current = true
    canvas.setPointerCapture(event.pointerId)
    context.beginPath()
    context.moveTo(point.x, point.y)
  }

  function draw(event: PointerEvent<HTMLCanvasElement>) {
    if (!drawingRef.current) {
      return
    }

    const context = event.currentTarget.getContext('2d')
    if (!context) {
      return
    }

    const point = getPoint(event)
    context.lineTo(point.x, point.y)
    context.stroke()
    setHasDrawing(true)
  }

  function stopDrawing(event: PointerEvent<HTMLCanvasElement>) {
    if (!drawingRef.current) {
      return
    }

    drawingRef.current = false
    const canvas = event.currentTarget
    onChange(canvas.toDataURL('image/png'))
  }

  function clearSignature() {
    const canvas = canvasRef.current
    const context = canvas?.getContext('2d')
    if (!canvas || !context) {
      return
    }

    context.clearRect(0, 0, canvas.width, canvas.height)
    setHasDrawing(false)
    onChange(null)
  }

  return (
    <section className="rounded-card border border-line bg-surface-page/60 p-4">
      {showToggle ? (
        <label className="flex items-start gap-3 text-sm text-ink">
          <input
            className="mt-1 size-4 rounded border-line text-brand-500 focus:ring-brand-500"
            type="checkbox"
            checked={enabled}
            onChange={(event) => onEnabledChange(event.target.checked)}
          />
          <span>
            <span className="block font-semibold">{label}</span>
            <span className="mt-1 block text-xs leading-5 text-muted">{description}</span>
          </span>
        </label>
      ) : (
        <div className="text-sm text-ink">
          <span className="block font-semibold">{label}</span>
          <span className="mt-1 block text-xs leading-5 text-muted">{description}</span>
        </div>
      )}

      {enabled && (
        <div className="mt-4 grid gap-3">
          <div className="overflow-hidden rounded-control border border-line bg-white shadow-card">
            <canvas
              ref={canvasRef}
              className="block h-32 w-full touch-none cursor-crosshair"
              aria-label="Campo para desenhar assinatura manuscrita"
              onPointerDown={startDrawing}
              onPointerMove={draw}
              onPointerUp={stopDrawing}
              onPointerCancel={stopDrawing}
            />
          </div>

          <div className="flex flex-col gap-2 text-xs text-muted sm:flex-row sm:items-center sm:justify-between">
            <span className="inline-flex items-center gap-2">
              <PenLine aria-hidden="true" size={15} />
              Desenhe sua assinatura com mouse ou dedo.
            </span>
            <button
              className="inline-flex items-center justify-center gap-2 rounded-control border border-line bg-white px-3 py-2 font-medium text-ink transition-colors hover:border-brand-200 hover:text-brand-500 disabled:cursor-not-allowed disabled:opacity-55"
              type="button"
              onClick={clearSignature}
              disabled={!hasDrawing}
            >
              <Eraser aria-hidden="true" size={15} />
              Limpar
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
