<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { api } from '@/api'
import type { ApiResponse, PageResult, Post, Tag } from '@/types'

const posts = ref<Post[]>([])
const tags = ref<Tag[]>([])
const keyword = ref('')
const selectedTag = ref('')
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const { data } = await api.get<ApiResponse<PageResult<Post>>>('/api/posts', {
      params: { keyword: keyword.value || undefined, tag: selectedTag.value || undefined, size: 20 }
    })
    posts.value = data.data.items
  } finally {
    loading.value = false
  }
}
onMounted(async () => {
  const { data } = await api.get<ApiResponse<Tag[]>>('/api/tags')
  tags.value = data.data
  await load()
})
watch(selectedTag, load)
</script>

<template>
  <section class="hero">
    <div>
      <div class="eyebrow">Human × AI Knowledge</div>
      <h1>把课堂之外的思考，变成可交流的知识。</h1>
      <p>面向校园创作者的知识博客。AI 帮你提炼摘要、发现标签、完善表达，读者还能直接向文章提问。</p>
    </div>
    <div class="hero-aside">
      <strong>4</strong>
      <span>项 AI 能力围绕文章创作与阅读形成闭环</span>
    </div>
  </section>
  <section class="toolbar">
    <el-input v-model="keyword" size="large" clearable placeholder="搜索标题或正文" :prefix-icon="Search" @keyup.enter="load" />
    <el-button size="large" type="primary" @click="load">搜索知识</el-button>
  </section>
  <div class="tag-row">
    <button class="tag" :class="{ active: !selectedTag }" @click="selectedTag = ''">全部</button>
    <button v-for="tag in tags" :key="tag.id" class="tag" @click="selectedTag = tag.name">{{ tag.name }}</button>
  </div>
  <div v-if="loading" class="loading-state">正在整理校园知识…</div>
  <div v-else-if="!posts.length" class="empty-state">暂时没有匹配的文章。登录后发布第一篇吧。</div>
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

