<template>
  <div class="admin-page">
    <header class="admin-header">
      <div class="header-content">
        <div class="logo">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#667eea" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
          <span>御途管理后台</span>
        </div>
        <nav class="admin-nav">
          <router-link to="/admin" class="nav-item" :class="{ active: $route.path === '/admin' }">仪表盘</router-link>
          <router-link to="/admin/orders" class="nav-item" :class="{ active: $route.path === '/admin/orders' }">订单管理</router-link>
          <router-link to="/admin/users" class="nav-item" :class="{ active: $route.path === '/admin/users' }">用户管理</router-link>
          <router-link to="/admin/messages" class="nav-item" :class="{ active: $route.path === '/admin/messages' }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            消息中心
            <span class="unread-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
          </router-link>
          <router-link to="/admin/maintenance" class="nav-item" :class="{ active: $route.path === '/admin/maintenance' }">
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
      <!-- 数据概览 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboard.todayOrders || 0 }}</span>
            <span class="stat-label">今日订单</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboard.activeRentals || 0 }}</span>
            <span class="stat-label">在租车辆</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboard.pendingOrders || 0 }}</span>
            <span class="stat-label">待处理订单</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">¥{{ dashboard.totalRevenue || 0 }}</span>
            <span class="stat-label">总收入</span>
          </div>
        </div>
      </div>

      <!-- 第二行统计 -->
      <div class="stats-grid secondary">
        <div class="stat-card mini">
          <div class="stat-info">
            <span class="stat-value">{{ dashboard.totalUsers || 0 }}</span>
            <span class="stat-label">注册用户</span>
          </div>
        </div>
        <div class="stat-card mini">
          <div class="stat-info">
            <span class="stat-value">{{ dashboard.todayNewUsers || 0 }}</span>
            <span class="stat-label">今日新增</span>
          </div>
        </div>
        <div class="stat-card mini">
          <div class="stat-info">
            <span class="stat-value">{{ dashboard.totalCars || 0 }}</span>
            <span class="stat-label">总车辆</span>
          </div>
        </div>
        <div class="stat-card mini">
          <div class="stat-info">
            <span class="stat-value">{{ dashboard.availableCars || 0 }}</span>
            <span class="stat-label">可租车辆</span>
          </div>
        </div>
      </div>

      <!-- 在租订单 -->
      <div class="section-card" v-if="dashboard.activeOrders && dashboard.activeOrders.length > 0">
        <div class="section-header">
          <h3>在租订单</h3>
          <router-link to="/admin/orders" class="view-all">查看全部 →</router-link>
        </div>
        <div class="order-table">
          <table>
            <thead>
              <tr>
                <th>订单号</th>
                <th>用户</th>
                <th>车辆</th>
                <th>开始时间</th>
                <th>预计结束</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in dashboard.activeOrders" :key="order.id">
                <td class="order-no">{{ order.orderNo }}</td>
                <td>{{ order.username || '未知' }}</td>
                <td>{{ order.car ? order.car.brand + ' ' + order.car.model : '未知' }}</td>
                <td>{{ formatTime(order.startTime) }}</td>
                <td>{{ formatTime(order.endTime) }}</td>
                <td>
                  <router-link :to="`/admin/messages?userId=${order.userId}`" class="action-btn">发消息</router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 最近订单 -->
      <div class="section-card">
        <div class="section-header">
          <h3>最近订单</h3>
          <router-link to="/admin/orders" class="view-all">查看全部 →</router-link>
        </div>
        <div class="order-table">
          <table>
            <thead>
              <tr>
                <th>订单号</th>
                <th>用户</th>
                <th>车辆</th>
                <th>费用</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in dashboard.recentOrders" :key="order.id">
                <td class="order-no">{{ order.orderNo }}</td>
                <td>{{ order.username || '未知' }}</td>
                <td>{{ order.car ? order.car.brand + ' ' + order.car.model : '未知' }}</td>
                <td class="price">¥{{ order.totalCost }}</td>
                <td>
                  <span :class="'status-tag status-' + order.status">{{ orderStatusText(order.status) }}</span>
                </td>
                <td>
                  <router-link :to="`/admin/messages?userId=${order.userId}`" class="action-btn">发消息</router-link>
                </td>
              </tr>
              <tr v-if="!dashboard.recentOrders || dashboard.recentOrders.length === 0">
                <td colspan="6" class="empty-row">暂无订单数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const dashboard = ref({})
const unreadCount = ref(0)

const orderStatusText = (s) => ({ 0: '待支付', 1: '在租中', 2: '已完成', 3: '已取消', 4: '预约中' }[s] || '未知')
const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''

const loadDashboard = async () => {
  const res = await api.get('/admin/dashboard')
  if (res.code === 200) {
    dashboard.value = res.data
  }
}

const loadUnreadCount = async () => {
  const res = await api.get('/messages/unread')
  if (res.code === 200) {
    unreadCount.value = res.data
  }
}

onMounted(async () => {
  // 检查是否管理员
  const userRes = await api.get('/user/info')
  if (userRes.code === 200 && userRes.data.role !== 1) {
    router.push('/')
    return
  }
  loadDashboard()
  loadUnreadCount()

  // 每30秒自动刷新数据
  setInterval(() => {
    loadDashboard()
    loadUnreadCount()
  }, 30000)
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f7fa; }
.admin-header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); position: sticky; top: 0; z-index: 100; }
.header-content { max-width: 1400px; margin: 0 auto; padding: 0 24px; height: 60px; display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; color: #333; }
.admin-nav { display: flex; gap: 4px; flex: 1; }
.nav-item { padding: 8px 16px; border-radius: 8px; font-size: 14px; color: #666; text-decoration: none; transition: all 0.2s; position: relative; }
.nav-item:hover { background: #f0f2ff; color: #667eea; }
.nav-item.active { background: #667eea; color: #fff; }
.unread-badge { position: absolute; top: 2px; right: 2px; background: #f56c6c; color: #fff; font-size: 10px; padding: 1px 5px; border-radius: 10px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.admin-badge { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.back-link { font-size: 13px; color: #999; text-decoration: none; }
.back-link:hover { color: #667eea; }

.admin-main { max-width: 1400px; margin: 0 auto; padding: 24px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.stats-grid.secondary { grid-template-columns: repeat(3, 1fr); }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.stat-card.mini { justify-content: center; text-align: center; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 24px; font-weight: 700; color: #333; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }

.section-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h3 { font-size: 16px; color: #333; }
.view-all { font-size: 13px; color: #667eea; text-decoration: none; }

.order-table { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; padding: 12px; font-size: 13px; color: #999; font-weight: 500; border-bottom: 1px solid #eee; }
td { padding: 12px; font-size: 14px; color: #333; border-bottom: 1px solid #f5f5f5; }
.order-no { font-family: monospace; color: #667eea; }
.price { color: #f56c6c; font-weight: 600; }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-0 { background: #fdf6ec; color: #e6a23c; }
.status-1 { background: #ecf5ff; color: #409eff; }
.status-2 { background: #f0f9eb; color: #67c23a; }
.status-3 { background: #f4f4f5; color: #909399; }
.status-4 { background: #fdf6ec; color: #e6a23c; }
.action-btn { color: #667eea; text-decoration: none; font-size: 13px; }
.action-btn:hover { text-decoration: underline; }
.empty-row { text-align: center; color: #999; padding: 40px !important; }

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .admin-nav { display: none; }
}
</style>
