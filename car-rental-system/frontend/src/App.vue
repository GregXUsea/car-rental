<template>
  <router-view />
  <!-- 回到顶部 -->
  <transition name="fade">
    <div v-if="showBackTop" class="back-top" @click="scrollToTop">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M18 15l-6-6-6 6"/>
      </svg>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const showBackTop = ref(false)

const handleScroll = () => {
  showBackTop.value = window.scrollY > 300
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => window.addEventListener('scroll', handleScroll))
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

:root {
  /* 状态色全局统一 */
  --color-available: #67c23a;
  --color-rented: #f56c6c;
  --color-reserved: #e6a23c;
  --color-maintenance: #909399;
  --color-primary: #667eea;
  --color-primary-dark: #5a6fd6;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #f0f2f5;
  color: #333;
  -webkit-font-smoothing: antialiased;
}

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #d0d0d0; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #bbb; }

/* 回到顶部按钮 */
.back-top {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 44px;
  height: 44px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
  transition: all 0.3s;
  color: #666;
}
.back-top:hover {
  background: var(--color-primary);
  color: #fff;
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(102,126,234,0.4);
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 骨架屏动画 */
@keyframes skeleton-loading {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 8px;
}

/* 状态色工具类 */
.status-available { color: var(--color-available); }
.status-rented { color: var(--color-rented); }
.status-reserved { color: var(--color-reserved); }
.status-maintenance { color: var(--color-maintenance); }

/* Element Plus 覆盖 */
.el-button--primary {
  background: linear-gradient(135deg, var(--color-primary) 0%, #764ba2 100%) !important;
  border: none !important;
  border-radius: 10px !important;
}
.el-button--primary:hover {
  background: linear-gradient(135deg, var(--color-primary-dark) 0%, #6a4192 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102,126,234,0.4) !important;
}
.el-input__wrapper { border-radius: 10px !important; }
.el-dialog { border-radius: 16px !important; }

/* 响应式 */
@media (max-width: 768px) {
  .hide-mobile { display: none !important; }
}
@media (min-width: 769px) {
  .show-mobile-only { display: none !important; }
}
</style>
