<template>
  <div class="flex min-h-screen">
    <!-- 侧边导航 -->
    <aside class="w-48 bg-surface border-r border-border flex-shrink-0 flex flex-col">
      <h2 class="text-lg font-bold text-text-primary px-4 pt-4 mb-6">管理后台</h2>
      <nav class="space-y-0.5 text-sm flex-1 px-2">
        <router-link to="/admin/dashboard" class="block py-2 px-3 rounded-lg hover:bg-warm transition-colors"
                     :class="{'bg-coral-light text-coral font-semibold': $route.path==='/admin/dashboard'}">仪表盘</router-link>
        <router-link to="/admin/users" class="block py-2 px-3 rounded-lg hover:bg-warm transition-colors"
                     :class="{'bg-coral-light text-coral font-semibold': $route.path==='/admin/users'}">用户管理</router-link>
        <router-link to="/admin/traces" class="block py-2 px-3 rounded-lg hover:bg-warm transition-colors"
                     :class="{'bg-coral-light text-coral font-semibold': $route.path.startsWith('/admin/traces')}">链路追踪</router-link>
        <router-link to="/admin/modules" class="block py-2 px-3 rounded-lg hover:bg-warm transition-colors"
                     :class="{'bg-coral-light text-coral font-semibold': $route.path==='/admin/modules'}">模块管理</router-link>
        <router-link to="/admin/knowledge" class="block py-2 px-3 rounded-lg hover:bg-warm transition-colors"
                     :class="{'bg-coral-light text-coral font-semibold': $route.path==='/admin/knowledge'}">知识库</router-link>
        <router-link to="/admin/rag-eval" class="block py-2 px-3 rounded-lg hover:bg-warm transition-colors"
                     :class="{'bg-coral-light text-coral font-semibold': $route.path==='/admin/rag-eval'}">RAG 评估</router-link>
        <router-link to="/admin/settings" class="block py-2 px-3 rounded-lg hover:bg-warm transition-colors"
                     :class="{'bg-coral-light text-coral font-semibold': $route.path==='/admin/settings'}">系统设置</router-link>
        <router-link to="/admin/monitor" class="block py-2 px-3 rounded-lg hover:bg-warm transition-colors"
                     :class="{'bg-coral-light text-coral font-semibold': $route.path==='/admin/monitor'}">运维监控</router-link>
      </nav>
      <div class="px-4 pt-4 pb-4 border-t border-border">
        <router-link to="/home" class="text-xs text-text-muted hover:text-coral transition-colors">← 返回主页</router-link>
      </div>
    </aside>

    <!-- 内容区 -->
    <main class="flex-1 min-h-screen overflow-auto">
      <router-view />
    </main>
  </div>
</template>

<script setup>
// 纯布局壳，无需额外逻辑
</script>
