import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import AuthView from '@/views/AuthView.vue'
import EditorView from '@/views/EditorView.vue'
import PostView from '@/views/PostView.vue'
import OnboardingView from '@/views/OnboardingView.vue'
import RecommendationsView from '@/views/RecommendationsView.vue'
import AdminView from '@/views/AdminView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/auth', component: AuthView },
    { path: '/onboarding', component: OnboardingView, meta: { auth: true } },
    { path: '/recommendations', component: RecommendationsView, meta: { auth: true } },
    { path: '/admin', component: AdminView, meta: { auth: true, admin: true } },
    { path: '/posts/new', component: EditorView, meta: { auth: true } },
    { path: '/posts/:id/edit', component: EditorView, meta: { auth: true } },
    { path: '/posts/:id', component: PostView }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.loggedIn) return { path: '/auth', query: { redirect: to.fullPath } }
  if (to.meta.admin && !auth.isAdmin) return { path: '/' }
  if (auth.loggedIn && !auth.user?.profileCompleted && to.path !== '/onboarding' && to.path !== '/auth') {
    return { path: '/onboarding', query: { redirect: to.fullPath } }
  }
})

export default router
