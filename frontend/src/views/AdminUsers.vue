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
          <router-link to="/admin/users" class="nav-item active">用户管理</router-link>
          <router-link to="/admin/messages" class="nav-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            消息中心
          </router-link>
          <router-link to="/admin/maintenance" class="nav-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/></svg>
            维护看板
          </router-link>
          <router-link to="/ai-assistant" class="nav-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1a1 1 0 011 1v3a1 1 0 01-1 1h-1v1a2 2 0 01-2 2H5a2 2 0 01-2-2v-1H2a1 1 0 01-1-1v-3a1 1 0 011-1h1a7 7 0 017-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 012-2z"/></svg>
            AI助手
          </router-link>
        </nav>
        <div class="header-right">
          <span class="admin-badge">管理员</span>
          <router-link to="/" class="back-link">返回前台</router-link>
        </div>
      </div>
    </header>

    <main class="admin-main">
      <!-- 用户详情弹窗 -->
      <div class="user-detail-modal" v-if="selectedUser" @click.self="selectedUser = null">
        <div class="modal-content">
          <div class="modal-header">
            <h3>用户详情</h3>
            <button class="close-btn" @click="selectedUser = null">×</button>
          </div>
          <div class="modal-body">
            <div class="user-info-section">
              <div class="info-row"><span class="label">用户名</span><span>{{ selectedUser.username }}</span></div>
              <div class="info-row"><span class="label">昵称</span><span>{{ selectedUser.nickname || '未设置' }}</span></div>
              <div class="info-row"><span class="label">手机</span><span>{{ selectedUser.phone || '未绑定' }}</span></div>
              <div class="info-row"><span class="label">邮箱</span><span>{{ selectedUser.email || '未绑定' }}</span></div>
              <div class="info-row"><span class="label">注册时间</span><span>{{ formatTime(selectedUser.createTime) }}</span></div>
              <div class="info-row"><span class="label">订单数</span><span>{{ selectedUser.orders ? selectedUser.orders.length : 0 }}</span></div>
            </div>

            <!-- 优惠券 -->
            <div class="section-title">优惠券 ({{ selectedUser.coupons ? selectedUser.coupons.length : 0 }})</div>
            <div class="coupon-list" v-if="selectedUser.coupons && selectedUser.coupons.length > 0">
              <div v-for="coupon in selectedUser.coupons" :key="coupon.id" class="coupon-item">
                <div class="coupon-info">
                  <span class="coupon-type" :class="coupon.couponType === 1 ? 'discount' : 'percent'">
                    {{ coupon.couponType === 1 ? `¥${coupon.discountAmount}` : `${coupon.discountRate * 10}折` }}
                  </span>
                  <span class="coupon-code">{{ coupon.couponCode }}</span>
                </div>
                <span :class="'coupon-status status-' + coupon.status">
                  {{ { 0: '未使用', 1: '已使用', 2: '已过期' }[coupon.status] }}
                </span>
              </div>
            </div>
            <div class="empty-text" v-else>暂无优惠券</div>

            <!-- 订单历史 -->
            <div class="section-title">订单历史</div>
            <div class="order-list" v-if="selectedUser.orders && selectedUser.orders.length > 0">
              <div v-for="order in selectedUser.orders" :key="order.id" class="order-item">
                <div class="order-info">
                  <span class="order-no">{{ order.orderNo }}</span>
                  <span class="order-car">{{ order.car ? order.car.brand + ' ' + order.car.model : '' }}</span>
                </div>
                <div class="order-meta">
                  <span class="order-cost">¥{{ order.totalCost }}</span>
                  <span :class="'order-status status-' + order.status">{{ orderStatusText(order.status) }}</span>
                </div>
              </div>
            </div>
            <div class="empty-text" v-else>暂无订单</div>

            <div class="modal-footer">
              <router-link :to="`/admin/messages?userId=${selectedUser.id}`" class="btn-primary">发送消息</router-link>
            </div>
          </div>
        </div>
      </div>

      <div class="page-header">
        <h2>用户管理</h2>
        <div class="user-count">共 {{ users.length }} 位用户</div>
      </div>

      <div class="user-grid">
        <div v-for="user in users" :key="user.id" class="user-card" @click="viewUser(user.id)">
          <img v-if="user.avatar" :src="user.avatar" class="user-avatar-img" />
          <div v-else class="user-avatar">{{ (user.nickname || user.username).charAt(0) }}</div>
          <div class="user-info">
            <div class="user-name">{{ user.nickname || user.username }}</div>
            <div class="user-meta">
              <span>{{ user.phone || '未绑定手机' }}</span>
              <span>{{ user.email || '未绑定邮箱' }}</span>
            </div>
          </div>
          <div class="user-stats">
            <div class="stat-item">
              <span class="stat-num">{{ user.orderCount || 0 }}</span>
              <span class="stat-text">订单</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ user.couponCount || 0 }}</span>
              <span class="stat-text">优惠券</span>
            </div>
          </div>
          <div class="user-action">
            <router-link :to="`/admin/messages?userId=${user.id}`" class="msg-btn" @click.stop>发消息</router-link>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../api'

const router = useRouter()
const route = useRoute()
const users = ref([])
const selectedUser = ref(null)

const orderStatusText = (s) => ({ 0: '待支付', 1: '在租中', 2: '已完成', 3: '已取消', 4: '预约中' }[s] || '未知')
const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : ''

const loadUsers = async () => {
  const res = await api.get('/admin/users')
  if (res.code === 200) {
    users.value = res.data
  }
}

const viewUser = async (userId) => {
  const res = await api.get(`/admin/users/${userId}`)
  if (res.code === 200) {
    selectedUser.value = res.data
  }
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200 && userRes.data.role !== 1) {
    router.push('/')
    return
  }
  loadUsers()

  // 如果URL带userId参数，直接打开用户详情
  if (route.query.userId) {
    viewUser(Number(route.query.userId))
  }

  // 每30秒自动刷新
  setInterval(loadUsers, 30000)
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
.user-count { font-size: 14px; color: #999; }

.user-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 16px; }
.user-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); cursor: pointer; transition: all 0.2s; }
.user-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.user-avatar { width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 600; flex-shrink: 0; }
.user-avatar-img { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.user-info { flex: 1; min-width: 0; }
.user-name { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 4px; }
.user-meta { display: flex; flex-direction: column; gap: 2px; }
.user-meta span { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-stats { display: flex; gap: 16px; }
.stat-item { text-align: center; }
.stat-num { display: block; font-size: 18px; font-weight: 600; color: #333; }
.stat-text { font-size: 11px; color: #999; }
.msg-btn { padding: 6px 12px; background: #f0f2ff; color: #667eea; border-radius: 6px; font-size: 12px; text-decoration: none; white-space: nowrap; }
.msg-btn:hover { background: #667eea; color: #fff; }

/* 用户详情弹窗 */
.user-detail-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal-content { background: #fff; border-radius: 16px; width: 90%; max-width: 600px; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px; border-bottom: 1px solid #eee; }
.modal-header h3 { font-size: 18px; }
.close-btn { background: none; border: none; font-size: 24px; color: #999; cursor: pointer; }
.modal-body { padding: 20px; }
.info-row { display: flex; padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.info-row .label { width: 80px; color: #999; flex-shrink: 0; }
.section-title { font-size: 15px; font-weight: 600; color: #333; margin: 20px 0 12px; }
.coupon-list, .order-list { display: flex; flex-direction: column; gap: 8px; }
.coupon-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: #f9f9f9; border-radius: 8px; }
.coupon-info { display: flex; align-items: center; gap: 8px; }
.coupon-type { font-weight: 600; }
.coupon-type.discount { color: #f56c6c; }
.coupon-type.percent { color: #667eea; }
.coupon-code { font-size: 12px; color: #999; }
.coupon-status { font-size: 12px; }
.coupon-status.status-0 { color: #67c23a; }
.coupon-status.status-1 { color: #999; }
.coupon-status.status-2 { color: #f56c6c; }
.order-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: #f9f9f9; border-radius: 8px; }
.order-no { font-family: monospace; color: #667eea; font-size: 13px; }
.order-car { font-size: 12px; color: #666; margin-left: 8px; }
.order-cost { font-weight: 600; color: #f56c6c; margin-right: 8px; }
.order-status { font-size: 12px; }
.order-status.status-0 { color: #e6a23c; }
.order-status.status-1 { color: #409eff; }
.order-status.status-2 { color: #67c23a; }
.order-status.status-3 { color: #999; }
.empty-text { color: #999; font-size: 13px; text-align: center; padding: 20px; }
.modal-footer { padding: 16px 20px; border-top: 1px solid #eee; text-align: right; }
.btn-primary { display: inline-block; padding: 10px 24px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border-radius: 8px; text-decoration: none; font-size: 14px; }
.btn-primary:hover { opacity: 0.9; }
</style>
