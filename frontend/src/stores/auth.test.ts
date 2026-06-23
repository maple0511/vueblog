import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'

vi.mock('@/api', () => ({ api: { post: vi.fn() } }))

describe('auth store', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('clears authentication state', () => {
    sessionStorage.setItem('campusblog-token', 'token')
    const store = useAuthStore()
    store.logout()
    expect(store.loggedIn).toBe(false)
    expect(sessionStorage.getItem('campusblog-token')).toBeNull()
  })
})

