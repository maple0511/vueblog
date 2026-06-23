<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const mode = ref<'login' | 'register'>('login')
const form = reactive({ username: '', email: '', account: '', password: '' })
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function submit() {
  loading.value = true
  try {
    if (mode.value === 'login') await auth.login(form.account, form.password)
    else await auth.register(form.username, form.email, form.password)
    ElMessage.success('欢迎进入 CampusBlog')
    router.push(String(route.query.redirect || '/'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-grid">
    <div class="auth-story">
      <div class="eyebrow">CampusBlog AI</div>
      <h1>知识值得被认真记录，也值得被更快理解。</h1>
      <p>登录后使用 AI 写作、自动摘要、自动标签与文章问答。</p>
    </div>
    <div class="auth-panel">
      <h2>{{ mode === 'login' ? '继续你的创作' : '加入校园知识网络' }}</h2>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item v-if="mode === 'register'" label="用户名">
          <el-input v-model="form.username" size="large" maxlength="30" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="邮箱">
          <el-input v-model="form.email" size="large" type="email" />
        </el-form-item>
        <el-form-item v-else label="用户名或邮箱">
          <el-input v-model="form.account" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" size="large" type="password" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button class="primary-button" :loading="loading" @click="submit">
          {{ mode === 'login' ? '登录' : '创建账号' }}
        </el-button>
      </el-form>
      <button class="text-button" @click="mode = mode === 'login' ? 'register' : 'login'">
        {{ mode === 'login' ? '没有账号？立即注册' : '已有账号？返回登录' }}
      </button>
    </div>
  </section>
</template>

