import axios from 'axios'

interface ApiErrorResponse {
  timestamp: string
  status: number
  erro: string
  mensagem: string
  path: string
}

export function getApiErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return (
      error.response?.data?.mensagem ??
      'Não foi possível conectar ao servidor.'
    )
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Ocorreu um erro inesperado.'
}