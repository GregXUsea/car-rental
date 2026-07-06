<template>
  <div class="admin-page">
    <header class="admin-header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/admin')">
          <svg width="32" height="32" viewBox="0 0 48 48" fill="none">
            <defs><linearGradient id="gAdminLogo" x1="4" y1="2" x2="44" y2="46"><stop offset="0%" stop-color="#FFD700"/><stop offset="100%" stop-color="#FF8C00"/></linearGradient></defs>
            <path d="M24 3 L7 11 L7 22 C7 31 14.5 39 24 43 C33.5 39 41 31 41 22 L41 11 Z" fill="url(#gAdminLogo)"/>
            <path d="M16 23 L21 28 L32 17" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>御途管理后台</span>
        </div>
        <nav class="admin-nav">
          <router-link to="/admin" class="nav-item">仪表盘</router-link>
          <router-link to="/admin/orders" class="nav-item">订单管理</router-link>
          <router-link to="/admin/users" class="nav-item">用户管理</router-link>
          <router-link to="/admin/messages" class="nav-item">消息中心</router-link>
          <router-link to="/admin/reviews" class="nav-item active">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
            订单评价
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
        <h2>订单评价汇总</h2>
      </div>

      <!-- 评分统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #FFD700, #FFA500)">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ reviewData.avgRating || 0 }}</span>
            <span class="stat-label">平均评分</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ reviewData.totalReviews || 0 }}</span>
            <span class="stat-label">总评价数</span>
          </div>
        </div>
      </div>

      <!-- 评分分布 -->
      <div class="section-card">
        <div class="section-header">
          <h3>评分分布</h3>
        </div>
        <div class="rating-bars">
          <div class="rating-bar-item" v-for="star in [5,4,3,2,1]" :key="star">
            <span class="star-label">{{ star }}星</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: getPercent(star) + '%' }"></div>
            </div>
            <span class="bar-count">{{ reviewData['star' + star] || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 评价列表 -->
      <div class="section-card">
        <div class="section-header">
          <h3>最新评价</h3>
        </div>
        <div class="review-list">
          <div v-for="order in reviewData.reviews" :key="order.id" class="review-item">
            <div class="review-header">
              <div class="review-user">
                <img v-if="order.car && order.car.image" :src="order.car.image" class="review-car-img" />
                <div class="review-info">
                  <span class="review-car">{{ order.car ? order.car.brand + ' ' + order.car.model : '未知车辆' }}</span>
                  <span class="review-user-name">{{ order.username || '用户' }}</span>
                </div>
              </div>
              <div class="review-stars">
                <span v-for="s in 5" :key="s" :class="{ active: s <= order.userRating }">★</span>
              </div>
            </div>
            <div class="review-comment" v-if="order.userComment">
              {{ order.userComment }}
            </div>
            <div class="review-meta">
              <span>订单号：{{ order.orderNo }}</span>
              <span>费用：¥{{ order.totalCost }}</span>
              <span>{{ formatTime(order.createTime) }}</span>
            </div>
          </div>
          <div class="empty-row" v-if="!reviewData.reviews || reviewData.reviews.length === 0">
            暂无评价数据
          </div>
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
const reviewData = ref({})

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''

const getPercent = (star) => {
  const total = reviewData.value.totalReviews || 1
  const count = reviewData.value['star' + star] || 0
  return Math.round((count / total) * 100)
}

const loadReviews = async () => {
  const res = await api.get('/admin/reviews')
  if (res.code === 200) {
    reviewData.value = res.data
  }
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200 && userRes.data.role !== 1) {
    router.push('/')
    return
  }
  loadReviews()
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f7fa; }
.admin-header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); position: sticky; top: 0; z-index: 100; }
.header-content { max-width: 1400px; margin: 0 auto; padding: 0 24px; height: 60px; display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; color: #333; cursor: pointer; }
.admin-nav { display: flex; gap: 4px; flex: 1; }
.nav-item { padding: 8px 16px; border-radius: 8px; font-size: 14px; color: #666; text-decoration: none; transition: all 0.2s; display: flex; align-items: center; gap: 6px; }
.nav-item:hover { background: #f0f2ff; color: #667eea; }
.nav-item.active { background: #667eea; color: #fff; }
.header-right { display: flex; align-items: center; gap: 12px; }
.admin-badge { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.back-link { font-size: 13px; color: #999; text-decoration: none; }
.back-link:hover { color: #667eea; }

.admin-main { max-width: 1400px; margin: 0 auto; padding: 24px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; color: #333; }

.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 24px; font-weight: 700; color: #333; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }

.section-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); margin-bottom: 20px; }
.section-header { margin-bottom: 16px; }
.section-header h3 { font-size: 16px; color: #333; }

.rating-bars { display: flex; flex-direction: column; gap: 12px; }
.rating-bar-item { display: flex; align-items: center; gap: 12px; }
.star-label { width: 40px; font-size: 13px; color: #666; }
.bar-track { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; background: linear-gradient(90deg, #FFD700, #FFA500); border-radius: 4px; transition: width 0.3s; }
.bar-count { width: 30px; text-align: right; font-size: 13px; color: #999; }

.review-list { display: flex; flex-direction: column; gap: 16px; }
.review-item { padding: 16px; background: #f9f9f9; border-radius: 10px; }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.review-user { display: flex; align-items: center; gap: 12px; }
.review-car-img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; }
.review-info { display: flex; flex-direction: column; }
.review-car { font-weight: 600; color: #333; }
.review-user-name { font-size: 12px; color: #999; }
.review-stars { display: flex; gap: 2px; font-size: 18px; color: #ddd; }
.review-stars .active { color: #FFD700; }
.review-comment { font-size: 14px; color: #333; line-height: 1.6; margin-bottom: 10px; padding: 10px; background: #fff; border-radius: 8px; }
.review-meta { display: flex; gap: 20px; font-size: 12px; color: #999; }
.empty-row { text-align: center; color: #999; padding: 40px; }
</style>
