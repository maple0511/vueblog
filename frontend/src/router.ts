import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import AuthView from '@/views/AuthView.vue'
import EditorView from '@/views/EditorView.vue'
import PostView from '@/views/PostView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/auth', component: AuthView },
    { path: '/posts/new', component: EditorView, meta: { auth: true } },
    { path: '/posts/:id/edit', component: EditorView, meta: { auth: true } },
    { path: '/posts/:id', component: PostView }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  if (to.meta.auth && !useAuthStore().loggedIn) return { path: '/auth', query: { redirect: to.fullPath } }
})

export default router

