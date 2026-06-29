<template>
  <div class="order-page">
    <header class="header">
      <div class="header-content">
        <el-button text @click="$router.push('/')">
          <IconSvg name="back" :size="18" /> 返回首页
        </el-button>
        <h1>
          <IconSvg name="order" :size="22" color="#333" />
          订单中心
        </h1>
        <div></div>
      </div>
    </header>

    <main class="main">
      <!-- 统计卡片（可点击筛选） -->
      <div class="stats-row">
        <div class="stat-card pending" :class="{ active: statusFilter === 'pending' }" @click="statusFilter = statusFilter === 'pending' ? '' : 'pending'">
          <div class="stat-icon">
            <IconSvg name="clock" :size="28" color="#e6a23c" />
          </div>
          <div class="stat-info">
            <span class="stat-num">{{ pendingCount }}</span>
            <span class="stat-label">待处理</span>
          </div>
        </div>
        <div class="stat-card active-card" :class="{ active: statusFilter === 'active' }" @click="statusFilter = statusFilter === 'active' ? '' : 'active'">
          <div class="stat-icon">
            <IconSvg name="car" :size="28" color="#409eff" />
          </div>
          <div class="stat-info">
            <span class="stat-num">{{ activeCount }}</span>
            <span class="stat-label">进行中</span>
          </div>
        </div>
        <div class="stat-card completed" :class="{ active: statusFilter === 'completed' }" @click="statusFilter = statusFilter === 'completed' ? '' : 'completed'">
          <div class="stat-icon">
            <IconSvg name="shield" :size="28" color="#67c23a" />
          </div>
          <div class="stat-info">
            <span class="stat-num">{{ completedCount }}</span>
            <span class="stat-label">已完成</span>
          </div>
        </div>
      </div>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-change="loadOrders" class="order-tabs">
        <el-tab-pane label="我的订单" name="my" />
        <el-tab-pane label="全部订单" name="all" v-if="isAdmin" />
      </el-tabs>

      <!-- 筛选按钮组 -->
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" size="default">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="0">待支付</el-radio-button>
          <el-radio-button label="1">在租中</el-radio-button>
          <el-radio-button label="4">预约中</el-radio-button>
          <el-radio-button label="2">已完成</el-radio-button>
          <el-radio-button label="3">已取消</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 订单卡片列表 -->
      <div class="order-list" v-if="filteredOrders.length > 0">
        <div v-for="order in filteredOrders" :key="order.id" class="order-card" :class="'status-' + order.status">
          <div class="card-header">
            <div class="order-status">
              <span class="status-dot" :class="'dot-' + order.status"></span>
              <span class="status-text">{{ orderStatusText(order.status) }}</span>
            </div>
            <span class="order-no">{{ order.orderNo }}</span>
          </div>

          <div class="card-body">
            <div class="car-section" v-if="order.car">
              <div class="car-icon" :class="'car-' + (order.car.category || 'default')">
                <img v-if="order.car.image" :src="order.car.image" :alt="order.car.brand" class="car-thumb"
                     @error="handleImgError($event, order.car)" />
                <IconSvg v-else :name="getCarIcon(order.car.category)" :size="28" :color="getCarIconColor(order.car.category)" />
              </div>
              <div class="car-info">
                <h3>{{ order.car.brand }} {{ order.car.model }}</h3>
                <p class="car-meta">{{ order.car.color }} · {{ order.car.seats }}座 · {{ order.car.category }}</p>
              </div>
            </div>

            <!-- 司机信息 -->
            <div class="driver-section" v-if="order.driver">
              <div class="driver-badge">
                <IconSvg name="user" :size="16" color="#667eea" />
                <span>专属司机</span>
              </div>
              <div class="driver-info">
                <span class="driver-name">{{ order.driver.name }}</span>
                <span class="driver-detail">{{ order.driver.licenseType }} · {{ order.driver.experienceYears || '-' }}年驾龄</span>
                <span class="driver-rating">★ {{ order.driver.rating }}</span>
              </div>
            </div>

            <div class="info-grid">
              <div class="info-item">
                <IconSvg name="calendar" :size="18" color="#666" />
                <div class="info-content">
                  <span class="info-label">取车时间</span>
                  <span class="info-value">{{ formatTime(order.startTime) }}</span>
                </div>
              </div>
              <div class="info-item">
                <IconSvg name="refresh" :size="18" color="#666" />
                <div class="info-content">
                  <span class="info-label">还车时间</span>
                  <span class="info-value">{{ order.actualReturnTime ? formatTime(order.actualReturnTime) : formatTime(order.endTime) + ' (预计)' }}</span>
                </div>
              </div>
              <div class="info-item highlight">
                <IconSvg name="money" :size="18" color="#f56c6c" />
                <div class="info-content">
                  <span class="info-label">订单费用</span>
                  <span class="info-value price">¥{{ order.totalCost }}</span>
                  <span v-if="order.discount > 0" class="discount-badge">已优惠 ¥{{ order.discount }}</span>
                  <span v-if="order.driverCost > 0" class="driver-cost">含司机费 ¥{{ order.driverCost }}</span>
                </div>
              </div>
              <div class="info-item">
                <IconSvg name="shield" :size="18" color="#666" />
                <div class="info-content">
                  <span class="info-label">押金</span>
                  <span class="info-value">¥{{ order.deposit }}</span>
                  <span v-if="order.depositRefund > 0" class="refund-text">已退 ¥{{ order.depositRefund }}</span>
                </div>
              </div>
            </div>

            <!-- 评价区域 -->
            <div class="rating-section" v-if="order.status === 2 && !order.userRating">
              <p class="rating-title">请为本次服务评分：</p>
              <div class="rating-stars">
                <span v-for="s in 5" :key="s" class="star" :class="{ active: s <= (ratingForm.rating || 0) }" @click="ratingForm.rating = s">★</span>
              </div>
              <el-input v-model="ratingForm.comment" placeholder="说说您的感受（选填）" size="small" style="margin-top: 8px; width: 300px;" />
              <el-button type="primary" size="small" style="margin-left: 8px;" @click="submitRating(order.id)" :disabled="!ratingForm.rating">提交评价</el-button>
            </div>
            <div class="rating-result" v-if="order.userRating">
              <span class="rated-stars">
                <span v-for="s in 5" :key="s" :class="{ active: s <= order.userRating }">★</span>
              </span>
              <span class="rated-text">{{ order.userComment || '用户已评价' }}</span>
            </div>
          </div>

          <div class="card-footer">
            <!-- 待支付押金 -->
            <el-button v-if="(order.status === 0 || order.status === 4) && !order.depositPaid" type="success" @click="handlePayDeposit(order)">
              <IconSvg name="card" :size="16" color="#fff" /> 支付押金 ¥{{ order.deposit }}
            </el-button>
            <!-- 押金已支付，待支付租金 -->
            <el-button v-if="order.status === 1 && !order.rentalPaid" type="warning" @click="handlePayRental(order)">
              <IconSvg name="money" :size="16" color="#fff" /> 支付租金 ¥{{ order.totalCost }}
            </el-button>
            <!-- 归还车辆 -->
            <el-button v-if="order.status === 1" type="primary" @click="handleReturn(order)">
              <IconSvg name="van" :size="16" color="#fff" /> 归还车辆
            </el-button>
            <!-- 状态标签 -->
            <span v-if="order.status === 1 && order.rentalPaid" class="paid-badge">已付清</span>
            <span v-if="order.status === 2" class="completed-badge">已完成</span>

            <!-- 取消订单（待支付：随时可取消） -->
            <el-button v-if="order.status === 0 && !order.depositPaid" type="danger" plain @click="handleCancel(order)">
              <IconSvg name="close" :size="16" /> 取消订单
            </el-button>

            <!-- 取消订单（已付押金：倒计时内可取消） -->
            <template v-if="(order.status === 1 || order.status === 4) && order.depositPaid === 1">
              <div v-if="cancelInfos[order.id]" class="cancel-section">
                <div v-if="cancelInfos[order.id].cancellable && cancelInfos[order.id].remainSeconds > 0" class="cancel-countdown">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  <span>可取消剩余 <strong>{{ formatCountdown(cancelInfos[order.id].remainSeconds) }}</strong></span>
                </div>
                <div v-else-if="!cancelInfos[order.id].cancellable" class="cancel-expired">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                  <span>{{ cancelInfos[order.id].reason || '已过取消时限' }}</span>
                </div>
                <el-button v-if="cancelInfos[order.id].cancellable" type="danger" plain size="small" @click="handleCancelWithWarning(order)">
                  <IconSvg name="close" :size="14" /> 取消订单
                </el-button>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-state" v-else>
        <div class="empty-icon">
          <IconSvg name="empty" :size="80" color="#dcdfe6" />
        </div>
        <h3>暂无订单</h3>
        <p>您还没有相关的订单记录</p>
        <el-button type="primary" @click="$router.push('/')">去租车</el-button>
      </div>
    </main>

    <!-- 支付弹窗 -->
    <el-dialog v-model="showPayDialog" :title="payDialogTitle" width="420px" :close-on-click-modal="false" :show-close="payStep !== 'processing'">
      <div v-if="payStep === 'confirm'" class="pay-confirm">
        <div class="pay-amount-wrap">
          <span class="pay-label">{{ payBoth ? '押金 + 租金 一起支付' : (payType === 'deposit' ? '押金支付' : '租金支付') }}</span>
          <span class="pay-amount">¥{{ payBoth ? (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) : payAmount }}</span>
        </div>
        <div class="pay-info">
          <div class="pay-info-row"><span>押金</span><span>¥{{ payDepositAmount }}（可退）</span></div>
          <div class="pay-info-row"><span>租金</span><span>¥{{ payRentalAmount }}</span></div>
          <div class="pay-info-row total-row" v-if="payBoth"><span>合计</span><span>¥{{ (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) }}</span></div>
        </div>
        <div class="pay-both-option" v-if="payType === 'deposit' && !payBoth">
          <button class="both-btn" @click="payBoth = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
            同时支付租金 ¥{{ payRentalAmount }}，合计 ¥{{ (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) }}
          </button>
        </div>
        <div class="pay-both-selected" v-if="payBoth && payType === 'deposit'">
          <span class="both-tag">已选择一起支付</span>
          <button class="both-cancel" @click="payBoth = false">取消，只付押金</button>
        </div>
        <div class="pay-card-input">
          <div class="card-label">模拟支付方式</div>
          <div class="card-row">
            <input v-model="cardNum1" maxlength="4" placeholder="6222" class="card-input" />
            <span class="card-dash">-</span>
            <input v-model="cardNum2" maxlength="4" placeholder="8888" class="card-input" />
            <span class="card-dash">-</span>
            <input v-model="cardNum3" maxlength="4" placeholder="6666" class="card-input" />
            <span class="card-dash">-</span>
            <input v-model="cardNum4" maxlength="4" placeholder="0001" class="card-input" />
          </div>
        </div>
      </div>
      <div v-if="payStep === 'processing'" class="pay-processing">
        <div class="pay-spinner"></div>
        <p>正在处理支付...</p>
      </div>
      <div v-if="payStep === 'success'" class="pay-success">
        <div class="success-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <h3>支付成功</h3>
        <p>¥{{ payBoth && payType === 'deposit' ? (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) : payAmount }} 已支付</p>
      </div>
      <template #footer>
        <el-button v-if="payStep === 'confirm'" @click="showPayDialog = false; payBoth = false">取消</el-button>
        <el-button v-if="payStep === 'confirm'" type="primary" @click="processPayment">
          确认支付 ¥{{ payBoth && payType === 'deposit' ? (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) : payAmount }}
        </el-button>
        <el-button v-if="payStep === 'success'" type="primary" @click="onPaySuccess">
          完成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import IconSvg from '../components/IconSvg.vue'

const orders = ref([])
const activeTab = ref('my')
const isAdmin = ref(false)
const statusFilter = ref('')
const ratingForm = ref({ rating: 0, comment: '' })

// 取消订单倒计时
const cancelInfos = ref({}) // { orderId: { cancellable, remainSeconds, reason, alreadyCancelledToday } }
let countdownTimer = null

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200) isAdmin.value = userRes.data.role === 1
  await loadOrders()
  startCountdownTimer()
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

const loadOrders = async () => {
  const url = activeTab.value === 'all' ? '/orders/all' : '/orders/my'
  const res = await api.get(url)
  if (res.code === 200) {
    orders.value = res.data
    loadCancelInfos()
  }
}

// 加载所有可取消订单的取消信息
const loadCancelInfos = async () => {
  const cancellableOrders = orders.value.filter(o =>
    (o.status === 0 && !o.depositPaid) ||
    ((o.status === 1 || o.status === 4) && o.depositPaid === 1)
  )
  for (const order of cancellableOrders) {
    try {
      const res = await api.get(`/orders/cancel-info/${order.id}`)
      if (res.code === 200) {
        cancelInfos.value[order.id] = res.data
      }
    } catch (e) { /* ignore */ }
  }
}

// 每秒更新倒计时
const startCountdownTimer = () => {
  countdownTimer = setInterval(() => {
    for (const id in cancelInfos.value) {
      const info = cancelInfos.value[id]
      if (info.remainSeconds > 0) {
        info.remainSeconds--
        if (info.remainSeconds <= 0) {
          info.cancellable = false
          info.reason = '已过取消时限'
        }
      }
    }
  }, 1000)
}

// 格式化倒计时 HH:MM:SS
const formatCountdown = (seconds) => {
  if (seconds <= 0) return '00:00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const filteredOrders = computed(() => {
  if (!statusFilter.value) return orders.value
  if (statusFilter.value === 'pending') return orders.value.filter(o => o.status === 0 || o.status === 4)
  if (statusFilter.value === 'active') return orders.value.filter(o => o.status === 1)
  if (statusFilter.value === 'completed') return orders.value.filter(o => o.status === 2)
  return orders.value.filter(o => String(o.status) === statusFilter.value)
})

const pendingCount = computed(() => orders.value.filter(o => o.status === 0 || o.status === 4).length)
const activeCount = computed(() => orders.value.filter(o => o.status === 1).length)
const completedCount = computed(() => orders.value.filter(o => o.status === 2).length)

const getCarIcon = (category) => {
  const icons = { 'SUV': 'suv', 'MPV': 'mpv', '新能源': 'electric', '轿车': 'car' }
  return icons[category] || 'car'
}

const getCarIconColor = (category) => {
  const colors = { 'SUV': '#10b981', 'MPV': '#3b82f6', '新能源': '#22c55e', '轿车': '#667eea' }
  return colors[category] || '#667eea'
}

// 车辆图片错误回退SVG
const carStyles = {
  '丰田': { body: '#c0392b', roof: '#a93226', wheel: '#2c3e50' },
  '本田': { body: '#2980b9', roof: '#2471a3', wheel: '#2c3e50' },
  '大众': { body: '#27ae60', roof: '#229954', wheel: '#2c3e50' },
  '宝马': { body: '#2c3e50', roof: '#1a252f', wheel: '#1c1c1c' },
  '奔驰': { body: '#7f8c8d', roof: '#6c7a7d', wheel: '#1c1c1c' },
  '别克': { body: '#8e44ad', roof: '#7d3c98', wheel: '#2c3e50' },
  '比亚迪': { body: '#16a085', roof: '#138d75', wheel: '#2c3e50' },
  '理想': { body: '#e67e22', roof: '#d35400', wheel: '#2c3e50' },
  '奥迪': { body: '#2c3e50', roof: '#1c2833', wheel: '#1c1c1c' },
  '特斯拉': { body: '#ecf0f1', roof: '#bdc3c7', wheel: '#1c1c1c' },
  '蔚来': { body: '#2980b9', roof: '#2471a3', wheel: '#2c3e50' },
  '小米': { body: '#FF6700', roof: '#E55D00', wheel: '#2c3e50' },
}

const handleImgError = (e, car) => {
  const brand = car?.brand || ''
  const category = car?.category || ''
  const s = carStyles[brand] || { body: '#667eea', roof: '#5a67d8', wheel: '#2c3e50' }
  const shapeIdx = category === 'SUV' ? 1 : category === 'MPV' ? 2 : 0
  const shapes = [
    'M15,22 Q15,16 22,16 L30,12 L50,12 L58,16 Q65,16 65,22 L65,24 L15,24Z',
    'M15,22 Q15,15 22,15 L28,8 L52,8 L58,15 Q65,15 65,22 L65,24 L15,24Z',
    'M13,22 Q13,14 20,14 L25,6 L55,6 L60,14 Q67,14 67,22 L67,24 L13,24Z',
  ]
  const idx = Math.floor(Math.random() * 1000)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 32">
    <defs><linearGradient id="b${idx}" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="${s.body}"/><stop offset="100%" stop-color="${s.roof}"/></linearGradient></defs>
    <rect fill="#e8f4f8" width="80" height="32" rx="3"/>
    <rect fill="#666" y="25" width="80" height="7"/>
    <path d="${shapes[shapeIdx]}" fill="url(#b${idx})" stroke="${s.roof}" stroke-width="0.3"/>
    <circle cx="25" cy="23" r="4" fill="${s.wheel}"/><circle cx="25" cy="23" r="2" fill="#666"/>
    <circle cx="55" cy="23" r="4" fill="${s.wheel}"/><circle cx="55" cy="23" r="2" fill="#666"/>
  </svg>`
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent(svg)
}

// 支付弹窗相关
const showPayDialog = ref(false)
const payStep = ref('confirm')
const payType = ref('deposit')
const payAmount = ref(0)
const payDepositAmount = ref(0)
const payRentalAmount = ref(0)
const payOrderId = ref(null)
const payDialogTitle = ref('')
const payBoth = ref(false)
const cardNum1 = ref('6222')
const cardNum2 = ref('8888')
const cardNum3 = ref('6666')
const cardNum4 = ref('0001')

const openPayDialog = (type, amount, orderId, order) => {
  payType.value = type
  payAmount.value = amount
  payOrderId.value = orderId
  payStep.value = 'confirm'
  payBoth.value = false
  payDialogTitle.value = type === 'deposit' ? '支付押金' : '支付租金'
  payDepositAmount.value = order?.deposit || 0
  payRentalAmount.value = order?.totalCost || 0
  showPayDialog.value = true
}

const processPayment = async () => {
  payStep.value = 'processing'
  await new Promise(resolve => setTimeout(resolve, 1500))

  try {
    // 先支付押金
    const depositRes = await api.post(`/orders/pay-deposit/${payOrderId.value}`)
    if (depositRes.code !== 200) {
      ElMessage.error(depositRes.message)
      payStep.value = 'confirm'
      return
    }

    // 如果选择一起支付租金
    if (payBoth.value && payType.value === 'deposit') {
      const rentalRes = await api.post(`/orders/pay-rental/${payOrderId.value}`)
      if (rentalRes.code !== 200) {
        ElMessage.error(rentalRes.message)
        payStep.value = 'confirm'
        return
      }
    }

    payStep.value = 'success'
  } catch (e) {
    ElMessage.error('支付失败，请重试')
    payStep.value = 'confirm'
  }
}

const onPaySuccess = () => {
  showPayDialog.value = false
  payBoth.value = false
  ElMessage.success(payBoth.value ? '押金和租金支付成功！' : (payType.value === 'deposit' ? '押金支付成功！' : '租金支付成功！'))
  loadOrders() // 重新加载订单和取消信息（支付押金后开始倒计时）
}

const handlePayDeposit = (order) => {
  openPayDialog('deposit', order.deposit, order.id, order)
}

const handlePayRental = (order) => {
  openPayDialog('rental', order.totalCost, order.id, order)
}

const handleReturn = async (order) => {
  // 检查租金是否已付
  if (!order.rentalPaid) {
    ElMessage.warning('请先支付租金后再归还车辆')
    openPayDialog('rental', order.totalCost, order.id)
    return
  }
  await ElMessageBox.confirm('确认归还车辆？系统将退还押金', '归还确认')
  const res = await api.post(`/orders/return/${order.id}`)
  if (res.code === 200) {
    ElMessage.success(`归还成功，押金 ¥${order.deposit} 已退还`)
    loadOrders()
  } else ElMessage.error(res.message)
}

const handleCancel = async (order) => {
  await ElMessageBox.confirm('确认取消订单？押金将全额退还', '提示')
  const res = await api.post(`/orders/cancel/${order.id}`)
  if (res.code === 200) { ElMessage.success('已取消，押金已退还'); loadOrders() }
  else ElMessage.error(res.message)
}

// 已付押金的订单取消（带警告提醒）
const handleCancelWithWarning = async (order) => {
  const isReservation = order.status === 4
  const windowText = isReservation ? '2小时' : '1小时'
  try {
    await ElMessageBox.confirm(
      `尊敬的用户您好，支付押金后${windowText}内可取消订单。\n\n一天内最多可以取消一次订单，押金会原路退回。\n\n确认取消此订单吗？`,
      '取消订单提醒',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '再想想',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
  } catch (e) {
    return // 用户点取消或关闭
  }
  const res = await api.post(`/orders/cancel/${order.id}`)
  if (res.code === 200) {
    ElMessage.success('订单已取消，押金将在1-3个工作日内原路退回')
    loadOrders()
  } else {
    ElMessage.error(res.message)
  }
}

const submitRating = async (orderId) => {
  if (!ratingForm.value.rating) { ElMessage.warning('请选择评分'); return }
  const res = await api.post(`/orders/rate/${orderId}`, {
    rating: ratingForm.value.rating,
    comment: ratingForm.value.comment
  })
  if (res.code === 200) {
    ElMessage.success('评价成功')
    ratingForm.value = { rating: 0, comment: '' }
    loadOrders()
  } else ElMessage.error(res.message)
}

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''
const orderStatusText = (s) => ({ 0: '待支付', 1: '在租中', 2: '已完成', 3: '已取消', 4: '预约中' }[s] || '未知')
</script>

<style scoped>
.header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.header-content { max-width: 1200px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.header-content h1 { font-size: 18px; display: flex; align-items: center; gap: 8px; }

.main { max-width: 900px; margin: 20px auto; padding: 0 20px; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); transition: all 0.2s; cursor: pointer; border: 2px solid transparent; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.stat-card.active { border-color: #667eea; box-shadow: 0 4px 16px rgba(102,126,234,0.2); }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.stat-card.pending .stat-icon { background: #fdf6ec; }
.stat-card.active-card .stat-icon { background: #ecf5ff; }
.stat-card.completed .stat-icon { background: #f0f9eb; }
.stat-num { font-size: 28px; font-weight: bold; color: #333; }
.stat-label { font-size: 13px; color: #999; }
.stat-card.pending .stat-num { color: #e6a23c; }
.stat-card.active-card .stat-num { color: #409eff; }
.stat-card.completed .stat-num { color: #67c23a; }
.stat-info { display: flex; flex-direction: column; }

.order-tabs { margin-bottom: 16px; }
.filter-bar { margin-bottom: 20px; }

.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); border-left: 4px solid #e4e7ed; transition: box-shadow 0.2s; }
.order-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
.order-card.status-0 { border-left-color: #e6a23c; }
.order-card.status-1 { border-left-color: #409eff; }
.order-card.status-2 { border-left-color: #67c23a; }
.order-card.status-3 { border-left-color: #909399; }
.order-card.status-4 { border-left-color: #e6a23c; }

.card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f5f5f5; background: #fafafa; }
.order-status { display: flex; align-items: center; gap: 8px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; background: #909399; }
.dot-0 { background: #e6a23c; animation: pulse 2s infinite; }
.dot-1 { background: #409eff; animation: pulse 2s infinite; }
.dot-2 { background: #67c23a; }
.dot-3 { background: #909399; }
.dot-4 { background: #e6a23c; animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.status-text { font-weight: 600; color: #333; }
.order-no { font-size: 13px; color: #999; font-family: monospace; }

.card-body { padding: 20px; }

.car-section { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px dashed #eee; }
.car-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); overflow: hidden; }
.car-icon.car-SUV { background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%); }
.car-icon.car-MPV { background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%); }
.car-icon.car-新能源 { background: linear-gradient(135deg, #dcfce7 0%, #86efac 100%); }
.car-thumb { width: 100%; height: 100%; object-fit: cover; border-radius: 12px; }
.car-info h3 { font-size: 18px; color: #333; margin-bottom: 4px; }
.car-meta { font-size: 13px; color: #999; }

/* 司机信息 */
.driver-section { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 12px 16px; background: linear-gradient(135deg, #e0e7ff 0%, #f0f4ff 100%); border-radius: 10px; }
.driver-badge { display: flex; align-items: center; gap: 4px; padding: 4px 10px; background: #667eea; color: #fff; border-radius: 12px; font-size: 12px; }
.driver-info { display: flex; align-items: center; gap: 12px; flex: 1; }
.driver-name { font-weight: 600; color: #333; }
.driver-detail { font-size: 13px; color: #666; }
.driver-rating { color: #f7ba2a; font-weight: 600; }

.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.info-item { display: flex; align-items: flex-start; gap: 10px; }
.info-content { display: flex; flex-direction: column; }
.info-label { font-size: 12px; color: #999; margin-bottom: 2px; }
.info-value { font-size: 14px; color: #333; }
.info-value.price { font-size: 20px; font-weight: bold; color: #f56c6c; }
.driver-cost { font-size: 12px; color: #909399; margin-top: 2px; }
.discount-badge { display: inline-block; font-size: 11px; color: #fff; background: linear-gradient(135deg, #f56c6c, #e74c3c); padding: 1px 8px; border-radius: 10px; margin-top: 4px; font-weight: 600; }
.refund-text { font-size: 12px; color: #67c23a; margin-top: 2px; }

/* 评价 */
.rating-section { margin-top: 16px; padding: 12px; background: #f9f9f9; border-radius: 8px; }
.rating-title { font-size: 13px; color: #666; margin-bottom: 8px; }
.rating-stars { display: flex; gap: 4px; }
.star { font-size: 24px; color: #ddd; cursor: pointer; transition: color 0.2s; }
.star.active, .star:hover { color: #f7ba2a; }
.rating-result { margin-top: 12px; display: flex; align-items: center; gap: 8px; }
.rated-stars span { color: #ddd; font-size: 16px; }
.rated-stars span.active { color: #f7ba2a; }
.rated-text { font-size: 13px; color: #999; }

.card-footer { padding: 16px 20px; border-top: 1px solid #f5f5f5; display: flex; gap: 12px; justify-content: flex-end; background: #fafafa; }
.card-footer .el-button { display: flex; align-items: center; gap: 4px; }

.empty-state { text-align: center; padding: 60px 20px; background: #fff; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.empty-icon { margin-bottom: 16px; }
.empty-state h3 { color: #333; margin-bottom: 8px; }
.empty-state p { color: #999; margin-bottom: 20px; }

/* 支付状态标签 */
.paid-badge { display: inline-flex; align-items: center; padding: 6px 14px; background: #f0f9eb; color: #67c23a; border-radius: 8px; font-size: 13px; font-weight: 600; }
.completed-badge { display: inline-flex; align-items: center; padding: 6px 14px; background: #f5f5f5; color: #909399; border-radius: 8px; font-size: 13px; font-weight: 600; }

/* 支付弹窗 */
.pay-confirm { display: flex; flex-direction: column; gap: 20px; }
.pay-amount-wrap { text-align: center; padding: 20px; background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 12px; color: #fff; }
.pay-label { font-size: 14px; opacity: 0.9; display: block; margin-bottom: 8px; }
.pay-amount { font-size: 36px; font-weight: 800; }
.pay-info { background: #f8f9fb; border-radius: 10px; padding: 12px 16px; }
.pay-info-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; }
.pay-info-row span:first-child { color: #999; }
.pay-info-row span:last-child { color: #333; font-weight: 500; }
.pay-card-input { background: #f8f9fb; border-radius: 10px; padding: 12px 16px; }
.card-label { font-size: 13px; color: #999; margin-bottom: 10px; }
.card-row { display: flex; align-items: center; gap: 8px; }
.card-input { width: 60px; padding: 8px; text-align: center; border: 1px solid #ddd; border-radius: 6px; font-size: 15px; font-weight: 600; letter-spacing: 2px; outline: none; }
.card-input:focus { border-color: #667eea; }
.card-dash { color: #999; font-weight: 600; }

.pay-processing { text-align: center; padding: 40px 0; }
.pay-spinner { width: 48px; height: 48px; border: 4px solid #e5e7eb; border-top-color: #667eea; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.pay-processing p { color: #666; font-size: 15px; }

.pay-success { text-align: center; padding: 20px 0; }
.success-icon { margin-bottom: 16px; animation: popIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes popIn { 0% { transform: scale(0); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
.pay-success h3 { font-size: 20px; color: #67c23a; margin-bottom: 8px; }
.pay-success p { font-size: 14px; color: #999; }

/* 一起支付选项 */
.pay-both-option { margin-top: 4px; }
.both-btn { display: flex; align-items: center; gap: 8px; width: 100%; padding: 12px 16px; background: linear-gradient(135deg, #fff3e0, #ffe0b2); border: 1px solid #ffb74d; border-radius: 10px; color: #e65100; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.both-btn:hover { background: linear-gradient(135deg, #ffe0b2, #ffcc80); transform: translateY(-1px); }
.pay-both-selected { display: flex; align-items: center; gap: 10px; padding: 10px 16px; background: #e8f5e9; border: 1px solid #81c784; border-radius: 10px; margin-top: 4px; }
.both-tag { font-size: 13px; color: #2e7d32; font-weight: 600; }
.both-cancel { font-size: 12px; color: #999; background: none; border: none; cursor: pointer; text-decoration: underline; margin-left: auto; }
.total-row { font-weight: 600; color: #333; border-top: 1px solid #eee; padding-top: 8px; margin-top: 4px; }

/* 取消订单倒计时 */
.cancel-section { display: flex; align-items: center; gap: 10px; margin-left: auto; }
.cancel-countdown { display: flex; align-items: center; gap: 6px; padding: 6px 12px; background: #fffbe6; border: 1px solid #ffe58f; border-radius: 8px; font-size: 12px; color: #e6a23c; }
.cancel-countdown strong { font-family: 'Courier New', monospace; font-size: 14px; color: #f56c6c; letter-spacing: 1px; }
.cancel-expired { display: flex; align-items: center; gap: 6px; padding: 6px 12px; background: #f5f5f5; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 12px; color: #999; }
</style>
