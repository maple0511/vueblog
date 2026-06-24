<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import type { AdminUser, ApiResponse, PageResult, Post } from '@/types'

const activeTab = ref('posts')
const posts = ref<Post[]>([])
const users = ref<AdminUser[]>([])
const postStatus = ref('')
const userStatus = ref('')
const keyword = ref('')
const loading = ref(false)

async function loadPosts() {
  loading.value = true
  try {
    const { data } = await api.get<ApiResponse<PageResult<Post>>>('/api/admin/posts', {
      params: { keyword: keyword.value || undefined, reviewStatus: postStatus.value || undefined, size: 50 }
    })
    posts.value = data.data.items
  } finally { loading.value = false }
}

async function loadUsers() {
  loading.value = true
  try {
    const { data } = await api.get<ApiResponse<PageResult<AdminUser>>>('/api/admin/users', {
      params: { keyword: keyword.value || undefined, status: userStatus.value || undefined, size: 50 }
    })
    users.value = data.data.items
  } finally { loading.value = false }
}

async function load() {
  if (activeTab.value === 'posts') await loadPosts()
  else await loadUsers()
}

async function review(post: Post, reviewStatus: 'APPROVED' | 'REJECTED' | 'HIDDEN') {
  await api.put(`/api/admin/posts/${post.id}/review`, {
    reviewStatus,
    reason: reviewStatus === 'APPROVED' ? '管理员审核通过' : '管理员调整可见性'
  })
  ElMessage.success('帖子审核状态已更新')
  await loadPosts()
}

async function updateUser(user: AdminUser, status: 'ACTIVE' | 'DISABLED') {
  await api.put(`/api/admin/users/${user.id}/status`, { status })
  ElMessage.success('成员状态已更新')
  await loadUsers()
}

onMounted(load)
</script>

<template>
  <section class="surface admin-page">
    <div class="eyebrow">Admin Console</div>
    <h1>审核管理后台</h1>
    <p>用于课程演示的基础治理能力：管理员可以调整帖子可见性，并停用异常成员账号。</p>

    <el-tabs v-model="activeTab" @tab-change="load">
      <el-tab-pane label="帖子审核" name="posts">
        <div class="toolbar admin-toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索标题或正文" @keyup.enter="loadPosts" />
          <el-select v-model="postStatus" clearable placeholder="审核状态">
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已隐藏" value="HIDDEN" />
          </el-select>
          <el-button type="primary" @click="loadPosts">筛选</el-button>
        </div>
        <el-table v-loading="loading" :data="posts" class="admin-table">
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column prop="authorName" label="作者" width="120" />
          <el-table-column prop="reviewStatus" label="状态" width="110" />
          <el-table-column label="标签" min-width="180">
            <template #default="{ row }">
              <span v-for="tag in row.tags" :key="`${row.id}-${tag.id}-${tag.source}`" class="tag mini">{{ tag.name }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="review(row, 'APPROVED')">通过</el-button>
              <el-button size="small" type="warning" @click="review(row, 'REJECTED')">驳回</el-button>
              <el-button size="small" type="danger" @click="review(row, 'HIDDEN')">隐藏</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="成员管理" name="users">
        <div class="toolbar admin-toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索用户名或邮箱" @keyup.enter="loadUsers" />
          <el-select v-model="userStatus" clearable placeholder="成员状态">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
          <el-button type="primary" @click="loadUsers">筛选</el-button>
        </div>
        <el-table v-loading="loading" :data="users" class="admin-table">
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column prop="email" label="邮箱" min-width="220" />
          <el-table-column prop="role" label="角色" width="100" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="兴趣引导" width="110">
            <template #default="{ row }">{{ row.profileCompleted ? '已完成' : '未完成' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="updateUser(row, 'ACTIVE')">启用</el-button>
              <el-button size="small" type="danger" @click="updateUser(row, 'DISABLED')">停用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
