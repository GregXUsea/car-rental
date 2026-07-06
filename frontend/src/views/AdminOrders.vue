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
          <router-link to="/admin/orders" class="nav-item active">订单管理</router-link>
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
        <h2>订单管理</h2>
        <div class="filter-group">
          <select v-model="statusFilter" class="filter-select">
            <option value="">全部状态</option>
            <option value="0">待支付</option>
            <option value="1">在租中</option>
            <option value="2">已完成</option>
            <option value="3">已取消</option>
            <option value="4">预约中</option>
          </select>
        </div>
      </div>

      <div class="order-table">
        <table>
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>车辆</th>
              <th>租期</th>
              <th>费用</th>
              <th>押金</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in filteredOrders" :key="order.id">
              <td class="order-no">{{ order.orderNo }}</td>
              <td>
                <div class="user-cell">
                  <span>{{ order.username || '未知' }}</span>
                  <span class="user-id">ID:{{ order.userId }}</span>
                </div>
              </td>
              <td>{{ order.car ? order.car.brand + ' ' + order.car.model : '未知' }}</td>
              <td>
                <div class="time-cell">
                  <span>{{ formatTime(order.startTime) }}</span>
                  <span class="time-arrow">→</span>
                  <span>{{ formatTime(order.endTime) }}</span>
                </div>
              </td>
              <td class="price">¥{{ order.totalCost }}</td>
              <td>
                <span>¥{{ order.deposit }}</span>
                <span v-if="order.depositRefund > 0" class="refund">已退¥{{ order.depositRefund }}</span>
              </td>
              <td>
                <span :class="'status-tag status-' + order.status">{{ orderStatusText(order.status) }}</span>
              </td>
              <td>
                <div class="action-group">
                  <router-link :to="`/admin/messages?userId=${order.userId}`" class="action-btn message">发消息</router-link>
                  <router-link :to="`/admin/users?userId=${order.userId}`" class="action-btn">查看用户</router-link>
                </div>
              </td>
            </tr>
            <tr v-if="filteredOrders.length === 0">
              <td colspan="8" class="empty-row">暂无订单数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const orders = ref([])
const statusFilter = ref('')

const orderStatusText = (s) => ({ 0: '待支付', 1: '在租中', 2: '已完成', 3: '已取消', 4: '预约中' }[s] || '未知')

const filteredOrders = computed(() => {
  if (!statusFilter.value) return orders.value
  return orders.value.filter(o => String(o.status) === statusFilter.value)
})

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''

const loadOrders = async () => {
  const res = await api.get('/admin/orders')
  if (res.code === 200) {
    orders.value = res.data
  }
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200 && userRes.data.role !== 1) {
    router.push('/')
    return
  }
  loadOrders()

  // 每30秒自动刷新
  setInterval(loadOrders, 30000)
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; color: #333; }
.filter-select { padding: 8px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; outline: none; }
.filter-select:focus { border-color: #667eea; }

.order-table { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; padding: 14px 16px; font-size: 13px; color: #999; font-weight: 500; background: #fafafa; border-bottom: 1px solid #eee; }
td { padding: 14px 16px; font-size: 14px; color: #333; border-bottom: 1px solid #f5f5f5; }
.order-no { font-family: monospace; color: #667eea; white-space: nowrap; }
.user-cell { display: flex; flex-direction: column; }
.user-id { font-size: 11px; color: #999; }
.time-cell { display: flex; flex-direction: column; font-size: 12px; color: #666; }
.time-arrow { color: #ccc; }
.price { color: #f56c6c; font-weight: 600; }
.refund { display: block; font-size: 11px; color: #67c23a; margin-top: 2px; }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-0 { background: #fdf6ec; color: #e6a23c; }
.status-1 { background: #ecf5ff; color: #409eff; }
.status-2 { background: #f0f9eb; color: #67c23a; }
.status-3 { background: #f4f4f5; color: #909399; }
.status-4 { background: #fdf6ec; color: #e6a23c; }
.action-group { display: flex; gap: 8px; }
.action-btn { color: #667eea; text-decoration: none; font-size: 13px; }
.action-btn:hover { text-decoration: underline; }
.action-btn.message { color: #409eff; }
.empty-row { text-align: center; color: #999; padding: 60px !important; }
</style>
