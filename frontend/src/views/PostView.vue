<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, streamPost } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, Comment, Post } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const id = Number(route.params.id)
const post = ref<Post | null>(null)
const comments = ref<Comment[]>([])
const commentText = ref('')
const question = ref('')
const answer = ref('')
const qaHistory = ref<{ question: string; answer: string }[]>([])
const asking = ref(false)
let controller: AbortController | null = null
const rendered = computed(() => DOMPurify.sanitize(marked.parse(post.value?.content || '') as string))
const own = computed(() => post.value?.authorId === auth.user?.id)

async function load() {
  const [postResponse, commentsResponse] = await Promise.all([
    api.get<ApiResponse<Post>>(`/api/posts/${id}`),
    api.get<ApiResponse<Comment[]>>(`/api/posts/${id}/comments`)
  ])
  post.value = postResponse.data.data
  comments.value = commentsResponse.data.data
}
onMounted(load)

async function regenerate() {
  await api.post(`/api/posts/${id}/ai-metadata/regenerate`)
  if (post.value) post.value.aiMetadataStatus = 'PENDING'
  ElMessage.success('AI 正在重新分析文章')
}
async function removePost() {
  await ElMessageBox.confirm('删除后无法恢复，确认继续？', '删除文章')
  await api.delete(`/api/posts/${id}`)
  router.push('/')
}
async function addComment() {
  if (!commentText.value.trim()) return
  await api.post(`/api/posts/${id}/comments`, { content: commentText.value })
  commentText.value = ''
  await load()
}
async function removeComment(commentId: number) {
  await api.delete(`/api/comments/${commentId}`)
  await load()
}
async function ask() {
  if (!auth.loggedIn) return router.push('/auth')
  if (!question.value.trim()) return
  const asked = question.value
  answer.value = ''
  asking.value = true
  controller = new AbortController()
  try {
    await streamPost(`/api/posts/${id}/questions/stream`, {
      question: asked, history: qaHistory.value.slice(-5)
    }, chunk => { answer.value += chunk }, controller.signal)
    qaHistory.value.push({ question: asked, answer: answer.value })
    question.value = ''
  } catch (error) {
    if ((error as Error).name !== 'AbortError') ElMessage.error((error as Error).message)
  } finally { asking.value = false }
}
</script>

<template>
  <div v-if="!post" class="loading-state">正在打开文章…</div>
  <div v-else class="article-layout">
    <article>
      <header class="article-header">
        <div class="eyebrow">{{ post.authorName }} · Campus knowledge</div>
        <h1>{{ post.title }}</h1>
        <div class="article-meta">{{ new Date(post.createdAt).toLocaleString() }}</div>
        <div class="tag-row">
          <span v-for="tag in post.tags" :key="`${tag.id}-${tag.source}`" class="tag" :class="{ ai: tag.source === 'AI' }">{{ tag.name }}</span>
        </div>
        <div v-if="own" class="tag-row">
          <router-link class="primary-button" :to="`/posts/${post.id}/edit`">编辑文章</router-link>
          <button class="text-button" @click="removePost">删除</button>
          <button class="text-button" @click="regenerate">重新生成 AI 摘要与标签</button>
        </div>
      </header>
      <aside class="ai-summary">
        <strong>AI 内容导读</strong>
        <p>{{ post.aiSummary || post.summary || 'AI 摘要尚未生成，可直接阅读正文。' }}</p>
        <small>状态：{{ post.aiMetadataStatus }}{{ post.aiSummaryEdited ? ' · 已由作者修订' : '' }}</small>
      </aside>
      <div class="markdown-body" v-html="rendered"></div>
      <section style="margin-top:64px">
        <h2>讨论</h2>
        <div v-if="auth.loggedIn" class="toolbar">
          <el-input v-model="commentText" placeholder="写下你的观点" maxlength="1000" @keyup.enter="addComment" />
          <el-button type="primary" @click="addComment">发布</el-button>
        </div>
        <p v-else><router-link to="/auth">登录后参与讨论</router-link></p>
        <div v-for="comment in comments" :key="comment.id" class="comment">
          <header><strong>{{ comment.authorName }}</strong><time>{{ new Date(comment.createdAt).toLocaleString() }}</time></header>
          <p>{{ comment.content }}</p>
          <button v-if="comment.authorId === auth.user?.id || own" class="text-button" @click="removeComment(comment.id)">删除</button>
        </div>
      </section>
    </article>
    <aside class="side-panel surface">
      <div class="eyebrow">Ask this article</div>
      <h2>向文章提问</h2>
      <p>回答严格限定于当前文章；没有依据时，AI 会明确说明。</p>
      <div class="qa-log" aria-live="polite">
        <div v-for="(item, index) in qaHistory" :key="index" class="qa-item">
          <strong>问：{{ item.question }}</strong><p>{{ item.answer }}</p>
        </div>
        <div v-if="asking || answer" class="qa-item"><strong>AI：</strong><p>{{ answer || '正在阅读文章…' }}</p></div>
      </div>
      <el-input v-model="question" type="textarea" maxlength="500" show-word-limit placeholder="这篇文章的核心观点是什么？" />
      <div class="tag-row">
        <button class="primary-button" :disabled="asking" @click="ask">开始提问</button>
        <button v-if="asking" class="text-button" @click="controller?.abort()">停止</button>
      </div>
    </aside>
  </div>
</template>

