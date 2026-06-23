import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('campusblog-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const messages: Record<number, string> = {
      400: '请检查输入内容',
      401: '登录状态已失效，请重新登录',
      403: '你没有权限执行此操作',
      404: '请求的内容不存在',
      429: '请求过于频繁，请稍后重试'
    }
    ElMessage.error(error.response?.data?.message || messages[status] || '服务暂时不可用')
    return Promise.reject(error)
  }
)

export async function streamPost(
  path: string,
  body: unknown,
  onChunk: (chunk: string) => void,
  signal: AbortSignal
) {
  const token = sessionStorage.getItem('campusblog-token')
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || ''}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(body),
    signal
  })
  if (!response.ok || !response.body) {
    const result = await response.json().catch(() => null) as ApiResponse<null> | null
    throw new Error(result?.message || 'AI 服务暂时不可用')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''
    for (const event of events) {
      const type = event.match(/^event:\s*(.+)$/m)?.[1]
      const data = event.match(/^data:\s*(.*)$/m)?.[1] || ''
      if (type === 'chunk') onChunk(data)
      if (type === 'error') throw new Error(data)
    }
  }
}

