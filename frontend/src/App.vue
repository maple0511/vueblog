<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
function logout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="app-shell">
    <header class="site-header">
      <router-link class="brand" to="/">
        <span class="brand-mark">CB</span>
        <span><strong>CampusBlog</strong><small>AI 校园知识博客</small></span>
      </router-link>
      <nav>
        <router-link to="/">探索文章</router-link>
        <router-link v-if="auth.loggedIn" to="/recommendations">我的推荐</router-link>
        <router-link v-if="auth.loggedIn" to="/posts/new">开始创作</router-link>
        <router-link v-if="auth.isAdmin" to="/admin">管理后台</router-link>
        <button v-if="auth.loggedIn" class="text-button" @click="logout">退出</button>
        <router-link v-else class="nav-primary" to="/auth">登录 / 注册</router-link>
      </nav>
    </header>
    <main><router-view /></main>
    <footer>CampusBlog AI · 让校园知识被更好地创作、理解与传递</footer>
  </div>
</template>
