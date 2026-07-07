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
          <router-link to="/admin/cars" class="nav-item">车辆管理</router-link>
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
        <button class="add-btn" @click="showGiveDialog = true">+ 发放优惠券</button>
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

      <!-- 发放优惠券弹窗 -->
      <el-dialog v-model="showGiveDialog" title="发放优惠券" width="450px">
        <el-form :model="giveForm" label-width="100px">
          <el-form-item label="选择用户">
            <el-select v-model="giveForm.userId" placeholder="请选择用户" filterable style="width: 100%;">
              <el-option v-for="user in users" :key="user.id" :label="user.nickname || user.username" :value="user.id">
                <span>{{ user.nickname || user.username }}</span>
                <span style="float:right;color:#999;font-size:12px">ID:{{ user.id }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="优惠券类型">
            <el-radio-group v-model="giveForm.type">
              <el-radio :label="1">立减券</el-radio>
              <el-radio :label="2">折扣券</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="金额/折扣" v-if="giveForm.type === 1">
            <el-input-number v-model="giveForm.amount" :min="1" :max="500" />
            <span style="margin-left:8px;color:#999">元</span>
          </el-form-item>
          <el-form-item label="折扣率" v-if="giveForm.type === 2">
            <el-input-number v-model="giveForm.rate" :min="1" :max="9" />
            <span style="margin-left:8px;color:#999">折（如5表示5折）</span>
          </el-form-item>
          <el-form-item label="最低消费">
            <el-input-number v-model="giveForm.minAmount" :min="0" :step="50" />
            <span style="margin-left:8px;color:#999">元（0表示无门槛）</span>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showGiveDialog = false">取消</el-button>
          <el-button type="primary" @click="handleGiveCoupon">确认发放</el-button>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const stats = ref({})
const users = ref([])
const showGiveDialog = ref(false)

const giveForm = ref({
  userId: null,
  type: 1,
  amount: 100,
  rate: 5,
  minAmount: 0
})

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''

const loadCoupons = async () => {
  const res = await api.get('/admin/coupons')
  if (res.code === 200) {
    stats.value = res.data
  }
}

const loadUsers = async () => {
  const res = await api.get('/admin/users')
  if (res.code === 200) {
    users.value = res.data
  }
}

const handleGiveCoupon = async () => {
  if (!giveForm.value.userId) {
    ElMessage.warning('请选择用户')
    return
  }
  const res = await api.post('/admin/coupons/give', {
    userId: giveForm.value.userId,
    type: giveForm.value.type,
    amount: giveForm.value.type === 1 ? giveForm.value.amount : 0,
    rate: giveForm.value.type === 2 ? giveForm.value.rate / 10 : 1,
    minAmount: giveForm.value.minAmount
  })
  if (res.code === 200) {
    ElMessage.success('优惠券发放成功')
    showGiveDialog.value = false
    loadCoupons()
  } else {
    ElMessage.error(res.message)
  }
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200 && userRes.data.role !== 1) {
    router.push('/')
    return
  }
  loadCoupons()
  loadUsers()
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
.add-btn { padding: 8px 20px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; }
.add-btn:hover { opacity: 0.9; }

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
