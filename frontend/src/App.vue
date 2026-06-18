<template>
  <div class="min-h-screen flex flex-col">
    <!-- 导航栏 -->
    <nav v-if="auth.isLoggedIn" class="bg-white/80 backdrop-blur border-b border-border sticky top-0 z-50">
      <div class="max-w-5xl mx-auto flex items-center h-14 px-4">
        <router-link to="/recite"
          class="px-4 py-2 text-sm text-text-secondary rounded-lg hover:text-text-primary hover:bg-coral-light transition-colors"
          active-class="!text-coral !bg-coral-light font-semibold">背诵</router-link>
        <router-link to="/achievements"
          class="px-4 py-2 text-sm text-text-secondary rounded-lg hover:text-text-primary hover:bg-coral-light transition-colors"
          active-class="!text-coral !bg-coral-light font-semibold">成就</router-link>

        <template v-if="auth.isAdmin">
          <router-link to="/admin/modules"
            class="px-4 py-2 text-sm text-text-secondary rounded-lg hover:text-text-primary hover:bg-coral-light transition-colors"
            active-class="!text-coral !bg-coral-light font-semibold">模块管理</router-link>
          <router-link to="/admin/monitor"
            class="px-4 py-2 text-sm text-text-secondary rounded-lg hover:text-text-primary hover:bg-coral-light transition-colors"
            active-class="!text-coral !bg-coral-light font-semibold">运维监控</router-link>
        </template>

        <button @click="logout" class="ml-auto px-4 py-2 text-sm text-red-500 rounded-lg hover:bg-red-50 transition-colors">
          退出
        </button>
      </div>
    </nav>

    <!-- 主内容区 -->
    <main class="flex-1 max-w-5xl mx-auto w-full px-4 py-8">
      <router-view v-slot="{ Component }">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>

    <!-- 全局 Toast -->
    <ToastContainer />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/authStore'
import ToastContainer from './components/common/ToastContainer.vue'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

