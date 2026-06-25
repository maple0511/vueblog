import { afterEach, describe, expect, it, vi } from 'vitest'
import { streamPost } from './api'

describe('streamPost', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  it('parses SSE chunk events in order', async () => {
    sessionStorage.setItem('campusblog-token', 'token')
    const encoder = new TextEncoder()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      body: new ReadableStream({
        start(controller) {
          controller.enqueue(encoder.encode('event: chunk\ndata: 标题\n\n'))
          controller.enqueue(encoder.encode('event: chunk\ndata: 建议\n\n'))
          controller.enqueue(encoder.encode('event: done\ndata: [DONE]\n\n'))
          controller.close()
        }
      })
    }))

    const chunks: string[] = []
    await streamPost('/api/ai/writing/stream', { action: 'TITLE_SUGGESTIONS' }, chunk => chunks.push(chunk), new AbortController().signal)

    expect(chunks).toEqual(['标题', '建议'])
    expect(fetch).toHaveBeenCalledWith('/api/ai/writing/stream', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ Authorization: 'Bearer token' })
    }))
  })

  it('throws backend SSE error events', async () => {
    const encoder = new TextEncoder()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      body: new ReadableStream({
        start(controller) {
          controller.enqueue(encoder.encode('event: error\ndata: AI 服务超时\n\n'))
          controller.close()
        }
      })
    }))

    await expect(streamPost('/api/ai/writing/stream', {}, () => {}, new AbortController().signal))
      .rejects.toThrow('AI 服务超时')
  })
})
