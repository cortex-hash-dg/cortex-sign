import { httpClient } from '../../../services/api/http-client'

export type CertificateValidation = {
  certificadoId: string
  documentoId: string
  assinaturaId: string
  status: string
  provedor: string
  protocolo: string | null
  assinadoEm: string | null
  hashDocumento: string
  assinaturaDigital: string
  certificadoCriadoEm: string
}

export type DocumentValidation = {
  certificadoId: string | null
  certificadoEncontrado: boolean
  documentoPossuiCertificado: boolean
  hashDocumentoConfere: boolean
  hashArquivoAssinadoConfere: boolean
  hashArquivoEnviadoSha256: string
  hashDocumentoRegistrado: string | null
  hashDocumentoExtraido: string | null
  hashArquivoAssinadoRegistrado: string | null
  assinaturaDigital: string | null
  mensagem: string
  certificado: CertificateValidation | null
}

export async function verifyCertificate(id: string) {
  const response = await httpClient.get<CertificateValidation>(`/certificados/verificar/${id}`)
  return response.data
}

export async function verifySignedDocument(file: File, certificateId?: string) {
  const formData = new FormData()
  formData.append('arquivo', file)

  if (certificateId) {
    formData.append('certificadoId', certificateId)
  }

  const response = await httpClient.post<DocumentValidation>('/certificados/verificar/documento', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  return response.data
}
