<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, PreferenceOptions } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const options = ref<string[]>([])
const selected = ref<string[]>([])
const saving = ref(false)

onMounted(async () => {
  const { data } = await api.get<ApiResponse<PreferenceOptions>>('/api/users/preferences')
  options.value = data.data.options
  selected.value = data.data.selected
})

function toggle(tag: string) {
  if (selected.value.includes(tag)) selected.value = selected.value.filter(item => item !== tag)
  else if (selected.value.length < 10) selected.value.push(tag)
  else ElMessage.warning('最多选择 10 个兴趣标签')
}

async function save() {
  saving.value = true
  try {
    await api.put('/api/users/preferences', { tags: selected.value })
    await auth.refreshMe()
    ElMessage.success('兴趣标签已保存')
    router.push(String(route.query.redirect || '/recommendations'))
  } finally {
    saving.value = false
  }
}

async function skip() {
  selected.value = []
  await save()
}
</script>

<template>
  <section class="surface onboarding">
    <div class="eyebrow">Personalized Campus Feed</div>
    <h1>选择你想看的校园内容</h1>
    <p>选择后，系统会优先推荐相同标签的文章。你也可以跳过，稍后仍能看到最新文章。</p>
    <div class="interest-grid">
      <button
        v-for="tag in options"
        :key="tag"
        class="interest-card"
        :class="{ active: selected.includes(tag) }"
        @click="toggle(tag)"
      >
        {{ tag }}
      </button>
    </div>
    <div class="tag-row">
      <button class="primary-button" :disabled="saving" @click="save">保存并查看推荐</button>
      <button class="text-button" :disabled="saving" @click="skip">跳过</button>
    </div>
  </section>
</template>
