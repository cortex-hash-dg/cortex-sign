import type { LoginResponse } from '../types/auth'

const AUTH_STORAGE_KEY = 'cortex-sign-auth'
const LEGACY_AUTH_STORAGE_KEY = 'cortext-sign-auth'

type AuthStorageType = 'local' | 'session'

function getStorageByType(storageType: AuthStorageType) {
  return storageType === 'local' ? localStorage : sessionStorage
}

function readStorage(storage: Storage): LoginResponse | null {
  const rawSession = storage.getItem(AUTH_STORAGE_KEY)
  const legacyRawSession = storage.getItem(LEGACY_AUTH_STORAGE_KEY)
  const value = rawSession ?? legacyRawSession

  if (!value) {
    return null
  }

  try {
    return JSON.parse(value) as LoginResponse
  } catch {
    storage.removeItem(AUTH_STORAGE_KEY)
    storage.removeItem(LEGACY_AUTH_STORAGE_KEY)
    return null
  }
}

function findStoredAuthSession(): {
  session: LoginResponse
  storageType: AuthStorageType
} | null {
  const sessionSession = readStorage(sessionStorage)

  if (sessionSession) {
    return { session: sessionSession, storageType: 'session' }
  }

  const localSession = readStorage(localStorage)

  if (localSession) {
    return { session: localSession, storageType: 'local' }
  }

  return null
}

export function getAuthSession() {
  return findStoredAuthSession()?.session ?? null
}

export function getAccessToken() {
  return getAuthSession()?.tokenAcesso ?? null
}

export function saveAuthSession(
  session: LoginResponse,
  rememberMe: boolean,
) {
  clearAuthSession()

  const storage = rememberMe ? localStorage : sessionStorage
  storage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
}

export function saveRefreshedAuthSession(session: LoginResponse) {
  const storedSession = findStoredAuthSession()
  const storageType = storedSession?.storageType ?? 'session'
  const storage = getStorageByType(storageType)
  const otherStorage = getStorageByType(
    storageType === 'local' ? 'session' : 'local',
  )

  storage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
  storage.removeItem(LEGACY_AUTH_STORAGE_KEY)
  otherStorage.removeItem(AUTH_STORAGE_KEY)
  otherStorage.removeItem(LEGACY_AUTH_STORAGE_KEY)
}

export function clearAuthSession() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
  localStorage.removeItem(LEGACY_AUTH_STORAGE_KEY)
  sessionStorage.removeItem(AUTH_STORAGE_KEY)
  sessionStorage.removeItem(LEGACY_AUTH_STORAGE_KEY)
}
