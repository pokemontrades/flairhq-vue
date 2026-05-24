import { createRouter, createWebHistory } from 'vue-router'
import LandingView from '../views/LandingView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: LandingView,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mod',
      name: 'mod',
      component: () => import('../views/ModView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('../views/SearchView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/users/:username',
      name: 'userProfile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/banlist',
      name: 'banlist',
      component: () => import('../views/BanListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/rejection-reasons',
      name: 'rejectionReasons',
      component: () => import('../views/RejectionReasonsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/events',
      name: 'eventLog',
      component: () => import('../views/EventLogView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/ban',
      name: 'banUser',
      component: () => import('../views/BanUserView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/pending-references',
      name: 'pendingReciprocal',
      component: () => import('../views/PendingReciprocalView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/info',
      name: 'info',
      component: () => import('../views/InfoView.vue'),
    },
    {
      path: '/privacy',
      name: 'privacy',
      component: () => import('../views/PrivacyView.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.init()

  const homeDest = auth.user?.isMod ? 'mod' : 'profile'

  if ((to.name === 'landing' || to.meta.guestOnly) && auth.isAuthenticated) {
    return { name: homeDest }
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'landing' }
  }
})

export default router
