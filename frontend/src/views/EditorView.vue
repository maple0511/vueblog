<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, streamPost } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, Post } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const editingId = computed(() => route.params.id ? Number(route.params.id) : null)
const form = reactive({ title: '', summary: '', content: '', aiSummary: '', tagsText: '' })
const saving = ref(false)
const aiOutput = ref('')
const aiLoading = ref(false)
let controller: AbortController | null = null

onMounted(async () => {
  if (!editingId.value) return
  const { data } = await api.get<ApiResponse<Post>>(`/api/posts/${editingId.value}`)
  if (data.data.authorId !== auth.user?.id) {
    ElMessage.error('只能编辑自己的文章')
    router.push(`/posts/${editingId.value}`)
    return
  }
  Object.assign(form, {
    title: data.data.title,
    summary: data.data.summary,
    content: data.data.content,
    aiSummary: data.data.aiSummary || '',
    tagsText: data.data.tags.filter(tag => tag.source === 'MANUAL').map(tag => tag.name).join(',')
  })
})

async function save() {
  if (!form.title.trim() || !form.content.trim()) return ElMessage.warning('标题和正文不能为空')
  saving.value = true
  try {
    const payload = {
      title: form.title, summary: form.summary, content: form.content, aiSummary: form.aiSummary || undefined,
      tags: form.tagsText.split(/[,，]/).map(tag => tag.trim()).filter(Boolean)
    }
    const response = editingId.value
      ? await api.put<ApiResponse<Post>>(`/api/posts/${editingId.value}`, payload)
      : await api.post<ApiResponse<Post>>('/api/posts', payload)
    ElMessage.success('文章已保存，AI 元数据将在后台生成')
    router.push(`/posts/${response.data.data.id}`)
  } finally {
    saving.value = false
  }
}

async function askAi(action: string) {
  controller?.abort()
  controller = new AbortController()
  aiOutput.value = ''
  aiLoading.value = true
  try {
    await streamPost('/api/ai/writing/stream', {
      action, title: form.title, selectedText: '', context: form.content
    }, chunk => { aiOutput.value += chunk }, controller.signal)
  } catch (error) {
    if ((error as Error).name !== 'AbortError') ElMessage.error((error as Error).message)
  } finally {
    aiLoading.value = false
  }
}
function insertAi() { form.content += `\n\n${aiOutput.value}`; aiOutput.value = '' }
</script>

<template>
  <div class="editor-layout">
    <section class="surface">
      <div class="eyebrow">{{ editingId ? 'Edit knowledge' : 'Create knowledge' }}</div>
      <h1>{{ editingId ? '完善文章' : '记录一篇校园知识' }}</h1>
      <el-form label-position="top">
        <el-form-item label="文章标题"><el-input v-model="form.title" maxlength="100" show-word-limit /></el-form-item>
        <el-form-item label="人工摘要（AI 不可用时作为降级内容）">
          <el-input v-model="form.summary" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="人工标签（逗号分隔）"><el-input v-model="form.tagsText" /></el-form-item>
        <el-form-item label="Markdown 正文">
          <el-input v-model="form.content" class="editor-textarea" type="textarea" maxlength="50000" show-word-limit />
        </el-form-item>
        <el-form-item label="AI 摘要人工修订（可选）">
          <el-input v-model="form.aiSummary" type="textarea" maxlength="150" show-word-limit />
        </el-form-item>
        <el-button type="primary" size="large" :loading="saving" @click="save">保存并发布</el-button>
      </el-form>
    </section>
    <aside class="ai-panel">
      <div class="eyebrow">AI Writing Studio</div>
      <h2>写作助手</h2>
      <p>AI 只提供草稿，由你决定是否写入文章。</p>
      <div class="ai-actions">
        <button @click="askAi('OUTLINE')">生成大纲</button>
        <button @click="askAi('CONTINUE')">续写正文</button>
        <button @click="askAi('REWRITE')">润色全文</button>
        <button @click="askAi('TITLE_SUGGESTIONS')">标题建议</button>
      </div>
      <div class="ai-output" aria-live="polite">{{ aiOutput || (aiLoading ? '正在生成…' : 'AI 结果会显示在这里') }}</div>
      <div class="tag-row">
        <button class="primary-button" :disabled="!aiOutput" @click="insertAi">插入正文末尾</button>
        <button class="text-button" style="color:#fff" @click="controller?.abort()">停止</button>
        <button class="text-button" style="color:#fff" @click="aiOutput = ''">放弃</button>
      </div>
    </aside>
  </div>
</template>

