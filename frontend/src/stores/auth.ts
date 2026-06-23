import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '@/api'
import type { ApiResponse, User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(JSON.parse(sessionStorage.getItem('campusblog-user') || 'null'))
  const token = ref(sessionStorage.getItem('campusblog-token') || '')
  const loggedIn = computed(() => Boolean(token.value && user.value))

  function save(auth: { token: string; user: User }) {
    token.value = auth.token
    user.value = auth.user
    sessionStorage.setItem('campusblog-token', auth.token)
    sessionStorage.setItem('campusblog-user', JSON.stringify(auth.user))
  }

  async function login(account: string, password: string) {
    const { data } = await api.post<ApiResponse<{ token: string; user: User }>>('/api/auth/login', { account, password })
    save(data.data)
  }

  async function register(username: string, email: string, password: string) {
    const { data } = await api.post<ApiResponse<{ token: string; user: User }>>('/api/auth/register', {
      username, email, password
    })
    save(data.data)
  }

  function logout() {
    token.value = ''
    user.value = null
    sessionStorage.removeItem('campusblog-token')
    sessionStorage.removeItem('campusblog-user')
  }

  return { user, token, loggedIn, login, register, logout }
})

