<template>
  <div class="admin-page">
    <header class="admin-header">
      <div class="header-content">
        <div class="logo">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#667eea" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
          <span>御途管理后台</span>
        </div>
        <nav class="admin-nav">
          <router-link to="/admin" class="nav-item">仪表盘</router-link>
          <router-link to="/admin/orders" class="nav-item">订单管理</router-link>
          <router-link to="/admin/users" class="nav-item">用户管理</router-link>
          <router-link to="/admin/messages" class="nav-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            消息中心
          </router-link>
          <router-link to="/admin/maintenance" class="nav-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/></svg>
            维护看板
          </router-link>
        </nav>
        <div class="header-right">
          <span class="admin-badge">管理员</span>
          <router-link to="/" class="back-link">返回前台</router-link>
        </div>
      </div>
    </header>

    <main class="admin-main">
      <div class="page-header">
        <h2>优惠券管理</h2>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <span class="stat-value">{{ stats.totalCoupons || 0 }}</span>
          <span class="stat-label">总优惠券</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ stats.unusedCoupons || 0 }}</span>
          <span class="stat-label">未使用</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ stats.usedCoupons || 0 }}</span>
          <span class="stat-label">已使用</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ stats.expiredCoupons || 0 }}</span>
          <span class="stat-label">已过期</span>
        </div>
      </div>

      <!-- 优惠券列表 -->
      <div class="coupon-table">
        <table>
          <thead>
            <tr>
              <th>用户</th>
              <th>券码</th>
              <th>类型</th>
              <th>面值/折扣</th>
              <th>最低消费</th>
              <th>状态</th>
              <th>过期时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="coupon in stats.couponList" :key="coupon.id">
              <td>{{ coupon.userName || '未知' }}</td>
              <td class="code">{{ coupon.couponCode }}</td>
              <td>
                <span :class="'type-tag ' + (coupon.couponType === 1 ? 'discount' : 'percent')">
                  {{ coupon.couponType === 1 ? '立减券' : '折扣券' }}
                </span>
              </td>
              <td class="value">
                {{ coupon.couponType === 1 ? `¥${coupon.discountAmount}` : `${coupon.discountRate * 10}折` }}
              </td>
              <td>¥{{ coupon.minAmount }}</td>
              <td>
                <span :class="'status-tag status-' + coupon.status">
                  {{ { 0: '未使用', 1: '已使用', 2: '已过期' }[coupon.status] }}
                </span>
              </td>
              <td>{{ formatTime(coupon.expireTime) }}</td>
            </tr>
            <tr v-if="!stats.couponList || stats.couponList.length === 0">
              <td colspan="7" class="empty-row">暂无优惠券数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const stats = ref({})

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''

const loadCoupons = async () => {
  const res = await api.get('/admin/coupons')
  if (res.code === 200) {
    stats.value = res.data
  }
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200 && userRes.data.role !== 1) {
    router.push('/')
    return
  }
  loadCoupons()
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f7fa; }
.admin-header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); position: sticky; top: 0; z-index: 100; }
.header-content { max-width: 1400px; margin: 0 auto; padding: 0 24px; height: 60px; display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; color: #333; }
.admin-nav { display: flex; gap: 4px; flex: 1; }
.nav-item { padding: 8px 16px; border-radius: 8px; font-size: 14px; color: #666; text-decoration: none; transition: all 0.2s; }
.nav-item:hover { background: #f0f2ff; color: #667eea; }
.nav-item.active { background: #667eea; color: #fff; }
.header-right { display: flex; align-items: center; gap: 12px; }
.admin-badge { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.back-link { font-size: 13px; color: #999; text-decoration: none; }
.back-link:hover { color: #667eea; }

.admin-main { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; color: #333; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.stat-value { display: block; font-size: 28px; font-weight: 700; color: #667eea; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }

.coupon-table { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; padding: 14px 16px; font-size: 13px; color: #999; font-weight: 500; background: #fafafa; border-bottom: 1px solid #eee; }
td { padding: 14px 16px; font-size: 14px; color: #333; border-bottom: 1px solid #f5f5f5; }
.code { font-family: monospace; color: #667eea; }
.type-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.type-tag.discount { background: #fff2f0; color: #f56c6c; }
.type-tag.percent { background: #f0f2ff; color: #667eea; }
.value { font-weight: 600; color: #f56c6c; }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-0 { background: #f0f9eb; color: #67c23a; }
.status-1 { background: #f4f4f5; color: #909399; }
.status-2 { background: #fef0f0; color: #f56c6c; }
.empty-row { text-align: center; color: #999; padding: 60px !important; }

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
