<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api'
import type { ApiResponse, PageResult, Post, PreferenceOptions } from '@/types'

const posts = ref<Post[]>([])
const selected = ref<string[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const [preferencesResponse, postsResponse] = await Promise.all([
      api.get<ApiResponse<PreferenceOptions>>('/api/users/preferences'),
      api.get<ApiResponse<PageResult<Post>>>('/api/recommendations/posts', { params: { size: 20 } })
    ])
    selected.value = preferencesResponse.data.data.selected
    posts.value = postsResponse.data.data.items
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <section class="hero compact">
    <div>
      <div class="eyebrow">Recommendation</div>
      <h1>你的校园知识推荐</h1>
      <p v-if="selected.length">根据你选择的 {{ selected.join('、') }} 进行匹配。</p>
      <p v-else>你尚未选择兴趣标签，当前展示最新通过审核的文章。</p>
    </div>
    <router-link class="primary-button" to="/onboarding">调整兴趣</router-link>
  </section>
  <div v-if="loading" class="loading-state">正在生成推荐…</div>
  <div v-else-if="!posts.length" class="empty-state">暂时没有匹配文章，可以先去探索页看看。</div>
  <div v-else class="post-list">
    <router-link v-for="post in posts" :key="post.id" :to="`/posts/${post.id}`" class="post-item">
      <time class="post-date">{{ new Date(post.createdAt).toLocaleDateString() }}</time>
      <div>
        <h2>{{ post.title }}</h2>
        <p>{{ post.summary }}</p>
        <div class="tag-row">
          <span v-for="tag in post.tags" :key="`${tag.id}-${tag.source}`" class="tag" :class="{ ai: tag.source === 'AI' }">{{ tag.name }}</span>
        </div>
      </div>
      <span class="arrow">↗</span>
    </router-link>
  </div>
</template>
