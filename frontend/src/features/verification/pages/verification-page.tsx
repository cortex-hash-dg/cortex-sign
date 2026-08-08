import { useMutation } from '@tanstack/react-query'
import { Camera, CheckCircle2, FileCheck2, FileSearch, Fingerprint, LogIn, QrCode, Search, ShieldCheck, Upload, X, XCircle } from 'lucide-react'
import { type ChangeEvent, type FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'

import { getApiErrorMessage } from '../../../services/api/api-error'
import { Button } from '../../../shared/ui/button'
import { EmptyState } from '../../../shared/ui/empty-state'
import { InlineMessage } from '../../../shared/ui/inline-message'
import { PageSection } from '../../../shared/ui/page-section'
import { StatusBadge } from '../../../shared/ui/status-badge'
import { verifyCertificate, verifySignedDocument, type CertificateValidation, type DocumentValidation } from '../api/verification-api'

type BarcodeDetectorConstructor = new (options?: { formats?: string[] }) => {
  detect: (image: ImageBitmapSource) => Promise<Array<{ rawValue: string }>>
}

declare global {
  interface Window {
    BarcodeDetector?: BarcodeDetectorConstructor
  }
}

function normalizeCertificateInput(value: string) {
  const trimmed = value.trim()
  const match = trimmed.match(/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/)
  return match?.[0] ?? ''
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

function shortHash(value?: string | null) {
  if (!value) {
    return '-'
  }

  return value.length > 22 ? `${value.slice(0, 11)}...${value.slice(-11)}` : value
}

function getValidationTone(result?: DocumentValidation | null) {
  if (!result) {
    return 'slate' as const
  }

  if (result.certificadoEncontrado && result.documentoPossuiCertificado && (result.hashDocumentoConfere || result.hashArquivoAssinadoConfere)) {
    return 'green' as const
  }

  return 'orange' as const
}

export function VerificationPage() {
  const { certificadoId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const isInsideSystem = location.pathname.startsWith('/app/')
  const [certificateInput, setCertificateInput] = useState(certificadoId ?? '')
  const [certificateResult, setCertificateResult] = useState<CertificateValidation | null>(null)
  const [documentResult, setDocumentResult] = useState<DocumentValidation | null>(null)
  const [isCameraOpen, setIsCameraOpen] = useState(false)
  const [isCameraReady, setIsCameraReady] = useState(false)
  const [cameraError, setCameraError] = useState<string | null>(null)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const cameraStreamRef = useRef<MediaStream | null>(null)
  const scanFrameRef = useRef<number | null>(null)
  const qrDetectorRef = useRef<InstanceType<BarcodeDetectorConstructor> | null>(null)

  const normalizedCertificateId = useMemo(() => normalizeCertificateInput(certificateInput), [certificateInput])

  const certificateMutation = useMutation({
    mutationFn: (id: string) => verifyCertificate(id),
    onSuccess: (response) => {
      setCertificateResult(response)
      setDocumentResult(null)
      toast.success('Certificado localizado.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  const documentMutation = useMutation({
    mutationFn: ({ file, certificateId }: { file: File; certificateId?: string }) => verifySignedDocument(file, certificateId),
    onSuccess: (response) => {
      setDocumentResult(response)
      if (response.certificado) {
        setCertificateInput(response.certificado.certificadoId)
      }
      setCertificateResult(null)
      toast.success('Documento analisado.')
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  })

  useEffect(() => {
    if (certificadoId) {
      certificateMutation.mutate(certificadoId)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [certificadoId])

  useEffect(() => {
    if (!isCameraOpen) {
      stopCameraStream()
      return
    }

    let cancelled = false

    async function startCamera() {
      setCameraError(null)
      setIsCameraReady(false)

      if (!window.BarcodeDetector) {
        setCameraError('Este navegador não suporta leitura automática de QR Code pela câmera.')
        toast.error('Este navegador não suporta leitura automática de QR Code pela câmera.')
        setIsCameraOpen(false)
        return
      }

      if (!navigator.mediaDevices?.getUserMedia) {
        setCameraError('Não foi possível acessar a câmera neste dispositivo.')
        toast.error('Não foi possível acessar a câmera neste dispositivo.')
        setIsCameraOpen(false)
        return
      }

      try {
        qrDetectorRef.current = new window.BarcodeDetector({ formats: ['qr_code'] })
        const stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: {
            facingMode: { ideal: 'environment' },
            width: { ideal: 1280 },
            height: { ideal: 720 },
          },
        })

        if (cancelled) {
          stream.getTracks().forEach((track) => track.stop())
          return
        }

        cameraStreamRef.current = stream

        if (videoRef.current) {
          videoRef.current.srcObject = stream
          await videoRef.current.play()
          setIsCameraReady(true)
          scanCameraFrame()
        }
      } catch {
        setCameraError('Permita o acesso à câmera para ler o QR Code do documento.')
        toast.error('Não foi possível abrir a câmera.')
        setIsCameraOpen(false)
      }
    }

    startCamera()

    return () => {
      cancelled = true
      stopCameraStream()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isCameraOpen])

  function stopCameraStream() {
    if (scanFrameRef.current) {
      window.cancelAnimationFrame(scanFrameRef.current)
      scanFrameRef.current = null
    }

    cameraStreamRef.current?.getTracks().forEach((track) => track.stop())
    cameraStreamRef.current = null
    qrDetectorRef.current = null
    setIsCameraReady(false)
  }

  function submitCertificate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!normalizedCertificateId) {
      toast.error('Informe um ID ou link de certificado válido.')
      return
    }

    certificateMutation.mutate(normalizedCertificateId)
  }

  async function readQrCode(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (!file) {
      return
    }

    if (!window.BarcodeDetector) {
      toast.error('Este navegador não suporta leitura de QR Code por imagem. Cole o link ou ID no campo.')
      return
    }

    try {
      const bitmap = await createImageBitmap(file)
      const detector = new window.BarcodeDetector({ formats: ['qr_code'] })
      const codes = await detector.detect(bitmap)
      const rawValue = codes[0]?.rawValue

      if (!rawValue) {
        toast.error('Nenhum QR Code foi encontrado nessa imagem.')
        return
      }

      const id = normalizeCertificateInput(rawValue)
      if (!id) {
        toast.error('O QR Code não contém um certificado Xsign válido.')
        return
      }

      setCertificateInput(id)
      certificateMutation.mutate(id)
    } catch {
      toast.error('Não foi possível ler o QR Code enviado.')
    }
  }

  async function scanCameraFrame() {
    const video = videoRef.current
    const detector = qrDetectorRef.current

    if (!video || !detector || !cameraStreamRef.current) {
      return
    }

    try {
      if (video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA) {
        const codes = await detector.detect(video)
        const rawValue = codes[0]?.rawValue

        if (rawValue) {
          const id = normalizeCertificateInput(rawValue)

          if (!id) {
            toast.error('O QR Code não contém um certificado Xsign válido.')
            setIsCameraOpen(false)
            return
          }

          setCertificateInput(id)
          setIsCameraOpen(false)
          certificateMutation.mutate(id)
          toast.success('QR Code lido pela câmera.')
          return
        }
      }
    } catch {
      // Continua tentando no próximo quadro. Alguns navegadores falham enquanto o vídeo inicializa.
    }

    scanFrameRef.current = window.requestAnimationFrame(scanCameraFrame)
  }

  function uploadDocument(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (!file) {
      return
    }

    documentMutation.mutate({
      file,
      certificateId: normalizedCertificateId || undefined,
    })
  }

  return (
    <div className="mx-auto grid w-full max-w-6xl gap-4 2xl:gap-5">
      <section className="overflow-hidden rounded-card border border-line bg-white shadow-card">
        <div className="grid gap-4 p-4 sm:p-5 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
          <div className="flex min-w-0 gap-4">
            <div className="grid size-12 shrink-0 place-items-center rounded-control bg-brand-50 text-brand-500 sm:size-14">
              <ShieldCheck aria-hidden="true" size={26} />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-500">Validador Xsign</p>
              <h1 className="mt-2 text-xl font-semibold tracking-tight text-ink sm:text-2xl">
                Verifique a autenticidade de documentos assinados.
              </h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">
                Consulte pelo QR Code, pelo ID do certificado ou envie o PDF assinado para conferir hashes e evidências.
              </p>
            </div>
          </div>

          {!isInsideSystem && (
            <Button
              className="w-full sm:w-auto"
              type="button"
              variant="secondary"
              onClick={() => navigate('/login')}
            >
              <LogIn aria-hidden="true" size={17} />
              Voltar para o sistema
            </Button>
          )}
        </div>
      </section>

      <section className="grid items-start gap-4 lg:grid-cols-2 2xl:gap-5">
        <PageSection title="Consultar por QR Code ou ID">
          <div className="grid gap-4 p-4 sm:p-5">
            <form className="grid gap-3" onSubmit={submitCertificate}>
              <label className="grid gap-2 text-sm font-medium text-ink">
                Link ou ID do certificado
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" size={17} aria-hidden="true" />
                  <input
                    className="min-h-11 w-full rounded-control border border-line bg-white pl-10 pr-3 text-sm text-ink shadow-card transition-colors placeholder:text-muted/70 focus:border-brand-500"
                    value={certificateInput}
                    onChange={(event) => setCertificateInput(event.target.value)}
                    placeholder="Cole o link do QR Code ou o UUID do certificado"
                  />
                </div>
              </label>

              <div className="grid gap-2 sm:grid-cols-3">
                <Button className="w-full" type="submit" disabled={certificateMutation.isPending || !normalizedCertificateId}>
                  <FileSearch aria-hidden="true" size={17} />
                  {certificateMutation.isPending ? 'Verificando...' : 'Verificar certificado'}
                </Button>

                <Button className="w-full" type="button" variant="secondary" onClick={() => setIsCameraOpen(true)}>
                  <Camera aria-hidden="true" size={17} />
                  Ler pela câmera
                </Button>

                <label className="inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-control border border-line bg-white px-4 text-sm font-medium text-ink shadow-card transition-colors hover:border-brand-500 hover:text-brand-500">
                  <QrCode aria-hidden="true" size={17} />
                  Ler imagem
                  <input className="sr-only" type="file" accept="image/*" onChange={readQrCode} />
                </label>
              </div>
            </form>

            {certificateMutation.isError && <InlineMessage tone="error" message={getApiErrorMessage(certificateMutation.error)} />}

            {certificateResult ? (
              <CertificateResultCard certificate={certificateResult} />
            ) : (
              <EmptyState title="Nenhum certificado consultado" description="Cole o ID, leia o QR Code ou envie o PDF assinado para carregar a validação." />
            )}
          </div>
        </PageSection>

        <PageSection title="Enviar PDF assinado">
          <div className="grid gap-4 p-4 sm:p-5">
            <label className="group grid min-h-[12rem] cursor-pointer place-items-center rounded-card border border-dashed border-brand-500/25 bg-brand-50/55 px-4 py-8 text-center transition-colors hover:border-brand-500 hover:bg-brand-50">
              <span className="grid size-12 place-items-center rounded-control bg-white text-brand-500 shadow-card transition-transform group-hover:-translate-y-0.5">
                <Upload size={24} aria-hidden="true" />
              </span>
              <span className="mt-4 text-sm font-semibold text-ink">Selecionar PDF assinado</span>
              <span className="mt-1 max-w-xs text-xs leading-5 text-muted">
                O arquivo será analisado para localizar o certificado e comparar os hashes.
              </span>
              <input className="sr-only" type="file" accept="application/pdf,.pdf" onChange={uploadDocument} />
            </label>

            {documentMutation.isPending && <InlineMessage message="Analisando PDF e conferindo evidências..." />}
            {documentMutation.isError && <InlineMessage tone="error" message={getApiErrorMessage(documentMutation.error)} />}
            {documentResult && <DocumentResultCard result={documentResult} />}
          </div>
        </PageSection>
      </section>

      {isCameraOpen && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-brand-900/55 px-3 py-4 backdrop-blur-sm">
          <section className="w-full max-w-xl overflow-hidden rounded-card border border-line bg-white shadow-[0_24px_80px_rgba(6,38,85,0.24)]">
            <div className="flex items-start justify-between gap-4 border-b border-line px-4 py-3.5">
              <div>
                <p className="text-sm font-semibold text-ink">Ler QR Code do documento</p>
                <p className="mt-1 text-xs leading-5 text-muted">Aponte a câmera para o QR Code do manifesto de assinatura.</p>
              </div>
              <button
                className="grid size-9 shrink-0 place-items-center rounded-control text-muted transition-colors hover:bg-surface-page hover:text-ink"
                type="button"
                onClick={() => setIsCameraOpen(false)}
                aria-label="Fechar câmera"
              >
                <X aria-hidden="true" size={18} />
              </button>
            </div>

            <div className="grid gap-3 p-4">
              <div className="relative overflow-hidden rounded-card border border-line bg-brand-900">
                <video
                  ref={videoRef}
                  className="aspect-[4/3] w-full object-cover"
                  muted
                  playsInline
                  autoPlay
                />
                <div className="pointer-events-none absolute inset-0 grid place-items-center">
                  <div className="h-44 w-44 rounded-card border-2 border-white/90 shadow-[0_0_0_999px_rgba(6,38,85,0.32)] sm:h-56 sm:w-56" />
                </div>
                {!isCameraReady && (
                  <div className="absolute inset-0 grid place-items-center bg-brand-900/70 px-6 text-center text-sm font-medium text-white">
                    Abrindo câmera...
                  </div>
                )}
              </div>

              {cameraError ? (
                <InlineMessage tone="error" message={cameraError} />
              ) : (
                <p className="text-center text-xs leading-5 text-muted">
                  A leitura acontece automaticamente quando o QR Code estiver dentro do enquadramento.
                </p>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

function CertificateResultCard({ certificate }: { certificate: CertificateValidation }) {
  return (
    <article className="overflow-hidden rounded-card border border-green-200 bg-white shadow-card">
      <div className="h-1 bg-success" />
      <div className="p-4 sm:p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <StatusBadge label={certificate.status} tone="green" />
            <h2 className="mt-3 text-base font-semibold text-ink">Certificado encontrado</h2>
            <p className="mt-1 text-sm text-muted">Documento assinado em {formatDate(certificate.assinadoEm)}.</p>
          </div>
          <div className="grid size-11 place-items-center rounded-control bg-green-50 text-success ring-1 ring-green-100">
            <CheckCircle2 aria-hidden="true" size={22} />
          </div>
        </div>

        <div className="mt-4 grid gap-2 text-sm text-muted">
          <InfoLine label="Certificado" value={certificate.certificadoId} />
          <InfoLine label="Protocolo" value={certificate.protocolo ?? '-'} />
        </div>

        <details className="mt-4 rounded-card border border-line bg-surface-page/55">
          <summary className="cursor-pointer px-3 py-2.5 text-sm font-medium text-ink">
            Ver detalhes técnicos
          </summary>
          <div className="grid gap-2 border-t border-line p-3 text-sm text-muted">
            <InfoLine label="Documento" value={certificate.documentoId} />
            <InfoLine label="Assinatura" value={certificate.assinaturaId} />
            <InfoLine label="Hash documento" value={certificate.hashDocumento} breakAll />
            <InfoLine label="Assinatura digital" value={certificate.assinaturaDigital} breakAll />
          </div>
        </details>
      </div>
    </article>
  )
}

function DocumentResultCard({ result }: { result: DocumentValidation }) {
  const valid = result.certificadoEncontrado && result.documentoPossuiCertificado && (result.hashDocumentoConfere || result.hashArquivoAssinadoConfere)

  return (
    <article className={`overflow-hidden rounded-card border bg-white shadow-card ${valid ? 'border-green-200' : 'border-orange-200'}`}>
      <div className={valid ? 'h-1 bg-success' : 'h-1 bg-warning'} />
      <div className="p-4 sm:p-5">
        <div className="flex items-start gap-3">
          <div className={`grid size-11 shrink-0 place-items-center rounded-control ${valid ? 'bg-green-50 text-success ring-1 ring-green-100' : 'bg-orange-50 text-warning ring-1 ring-orange-100'}`}>
            {valid ? <FileCheck2 aria-hidden="true" size={20} /> : <XCircle aria-hidden="true" size={20} />}
          </div>
          <div className="min-w-0">
            <StatusBadge label={valid ? 'Validado' : 'Atenção'} tone={getValidationTone(result)} />
            <p className="mt-2 text-sm font-semibold text-ink">{result.mensagem}</p>
          </div>
        </div>

        <div className="mt-4 grid gap-2 text-sm text-muted">
          <InfoLine label="Certificado" value={result.certificadoId ?? '-'} />
          <InfoLine
            label="Integridade"
            value={valid ? 'Hashes conferem com o registro Xsign' : 'Verifique os detalhes técnicos'}
          />
        </div>

        <details className="mt-4 rounded-card border border-line bg-surface-page/55">
          <summary className="cursor-pointer px-3 py-2.5 text-sm font-medium text-ink">
            Ver hashes comparados
          </summary>
          <div className="grid gap-2 border-t border-line p-3 text-sm text-muted">
            <InfoLine label="Arquivo enviado" value={result.hashArquivoEnviadoSha256} breakAll />
            <InfoLine label="Documento registrado" value={result.hashDocumentoRegistrado ?? '-'} breakAll />
            <InfoLine label="Extraído do PDF" value={result.hashDocumentoExtraido ?? '-'} breakAll />
            <InfoLine label="PDF assinado" value={result.hashArquivoAssinadoRegistrado ?? 'Não disponível para certificados antigos'} breakAll />
          </div>
        </details>
      </div>
    </article>
  )
}

function InfoLine({ label, value, breakAll = false }: { label: string; value: string; breakAll?: boolean }) {
  return (
    <p className="grid gap-1 rounded-control border border-line/70 bg-surface-page/70 px-3 py-2.5 sm:grid-cols-[10rem_minmax(0,1fr)]">
      <span className="inline-flex items-center gap-2 font-medium text-ink">
        <Fingerprint aria-hidden="true" className="text-brand-500" size={14} />
        {label}
      </span>
      <span className={breakAll ? 'break-all font-mono text-xs leading-5' : 'truncate'} title={value}>{breakAll ? value : shortHash(value)}</span>
    </p>
  )
}
