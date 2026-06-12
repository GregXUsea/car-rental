<template>
  <div class="detail-page">
    <header class="header">
      <div class="header-content">
        <el-button text @click="$router.push('/')"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <h1>车辆详情</h1>
        <div></div>
      </div>
    </header>

    <main class="main" v-if="car">
      <div class="detail-card">
        <div class="car-image-section">
          <img :src="car.image" :alt="car.brand + ' ' + car.model" class="car-detail-img" @error="handleImgError($event)" />
          <el-tag :type="statusType(car.status)" size="large" class="status-tag">{{ statusText(car.status) }}</el-tag>
          <div class="usage-tags" v-if="car.usageType">
            <el-tag v-if="car.usageType.includes('商务')" type="primary" size="large" class="usage-tag">🏢 商务用车</el-tag>
            <el-tag v-if="car.usageType.includes('婚庆')" type="danger" size="large" class="usage-tag">💒 婚庆用车</el-tag>
          </div>
        </div>
        <div class="car-detail-info">
          <h2>{{ car.brand }} {{ car.model }}</h2>
          <p class="desc">{{ car.description }}</p>
          <el-descriptions :column="2" border style="margin: 20px 0;">
            <el-descriptions-item label="品牌">{{ car.brand }}</el-descriptions-item>
            <el-descriptions-item label="型号">{{ car.model }}</el-descriptions-item>
            <el-descriptions-item label="颜色">{{ car.color }}</el-descriptions-item>
            <el-descriptions-item label="座位数">{{ car.seats }}座</el-descriptions-item>
            <el-descriptions-item label="类别">{{ car.category }}</el-descriptions-item>
            <el-descriptions-item label="用途">{{ car.usageType || '未分类' }}</el-descriptions-item>
            <el-descriptions-item label="总里程">{{ car.mileage }}km</el-descriptions-item>
            <el-descriptions-item label="日租金"><span class="price">¥{{ car.pricePerDay }}/天</span></el-descriptions-item>
            <el-descriptions-item label="押金">¥{{ car.deposit }}</el-descriptions-item>
            <el-descriptions-item label="上次保养">{{ car.lastMaintainDate }}</el-descriptions-item>
          </el-descriptions>

          <div class="actions" v-if="car.status === 0">
            <el-button type="primary" size="large" @click="openRentDialog">
              <el-icon><Van /></el-icon> 立即租车
            </el-button>
            <el-button type="warning" size="large" @click="openReserveDialog">
              <el-icon><Clock /></el-icon> 预约租车
            </el-button>
          </div>
          <div class="actions" v-else>
            <el-button size="large" disabled>
              {{ car.status === 1 ? '已被租用' : car.status === 2 ? '已被预约' : '维护中' }}
            </el-button>
          </div>
        </div>
      </div>
    </main>

    <!-- 租车对话框 -->
    <el-dialog v-model="showRentDialog" title="立即租车" width="500px">
      <el-alert title="租车规则：下午2点前可租当天，2点后只能从明天起租；最多租15天" type="info" :closable="false" show-icon style="margin-bottom: 16px;" />
      <el-form :model="rentForm" label-width="80px">
        <el-form-item label="开始时间">
          <el-date-picker v-model="rentForm.startTime" type="datetime" placeholder="选择开始时间" :disabled-date="disableStartDate" :disabled-hours="disabledStartHours" style="width:100%" @change="onStartTimeChange" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="rentForm.endTime" type="datetime" placeholder="选择结束时间" :disabled-date="disableEndDate" style="width:100%" />
        </el-form-item>
        <el-form-item label="预计天数">
          <span>{{ estimatedDays }} 天</span>
        </el-form-item>
        <el-form-item label="预计费用">
          <span class="price">¥{{ estimatedCost }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRentDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRent" :loading="renting">确认租车并支付押金</el-button>
      </template>
    </el-dialog>

    <!-- 预约对话框 -->
    <el-dialog v-model="showReserveDialog" title="预约租车" width="500px">
      <el-alert title="预约规则：下午2点前可预约当天，2点后只能从明天起预约；最多预约15天" type="info" :closable="false" show-icon style="margin-bottom: 16px;" />
      <el-form :model="rentForm" label-width="80px">
        <el-form-item label="开始时间">
          <el-date-picker v-model="rentForm.startTime" type="datetime" placeholder="选择用车开始时间" :disabled-date="disableStartDate" :disabled-hours="disabledStartHours" style="width:100%" @change="onStartTimeChange" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="rentForm.endTime" type="datetime" placeholder="选择用车结束时间" :disabled-date="disableEndDate" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReserveDialog = false">取消</el-button>
        <el-button type="warning" @click="handleReserve" :loading="renting">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const router = useRouter()
const car = ref(null)
const showRentDialog = ref(false)
const showReserveDialog = ref(false)
const renting = ref(false)

const rentForm = ref({
  startTime: '',
  endTime: ''
})

onMounted(async () => {
  const res = await api.get(`/cars/detail/${route.params.id}`)
  if (res.code === 200) car.value = res.data
  else { ElMessage.error(res.message); router.push('/') }
})

// 获取最早可租时间：如果当前时间在14:00之前，最早今天；否则最早明天
const getMinDate = () => {
  const now = new Date()
  const min = new Date(now)
  if (now.getHours() >= 14) {
    min.setDate(min.getDate() + 1)
  }
  min.setHours(8, 0, 0, 0) // 最早早上8点
  return min
}

// 获取最晚结束时间：开始时间 + 15天
const getMaxEndDate = () => {
  if (!rentForm.value.startTime) return null
  const max = new Date(rentForm.value.startTime)
  max.setDate(max.getDate() + 15)
  max.setHours(23, 59, 59, 999)
  return max
}

// 禁用开始日期：早于最早可租日期的不可选
const disableStartDate = (date) => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const minDate = new Date(today)
  if (now.getHours() >= 14) {
    minDate.setDate(minDate.getDate() + 1)
  }
  return date < minDate
}

// 禁用开始小时：如果是今天且14点前，14点之前的小时可选；如果是今天且14点后，所有今天不可选
const disabledStartHours = () => {
  const now = new Date()
  const minDate = getMinDate()
  // 如果最早可租日期不是今天，不限制小时
  if (minDate.toDateString() !== now.toDateString()) return []
  // 如果是今天，14点之前的小时可选（因为14点前还能租今天）
  return [] // getMinDate 已经处理了日期限制
}

// 禁用结束日期
const disableEndDate = (date) => {
  if (!rentForm.value.startTime) return true
  const start = new Date(rentForm.value.startTime)
  const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const maxEnd = new Date(startDate)
  maxEnd.setDate(maxEnd.getDate() + 15)
  return date < startDate || date > maxEnd
}

// 开始时间变化时，自动清理结束时间
const onStartTimeChange = () => {
  if (rentForm.value.endTime) {
    const start = new Date(rentForm.value.startTime)
    const end = new Date(rentForm.value.endTime)
    const maxEnd = new Date(start)
    maxEnd.setDate(maxEnd.getDate() + 15)
    if (end > maxEnd) {
      rentForm.value.endTime = ''
    }
  }
}

const estimatedDays = computed(() => {
  if (!rentForm.value.startTime || !rentForm.value.endTime) return 0
  const days = Math.max(1, Math.ceil((new Date(rentForm.value.endTime) - new Date(rentForm.value.startTime)) / 86400000))
  return Math.min(15, days)
})

const estimatedCost = computed(() => {
  if (!rentForm.value.startTime || !rentForm.value.endTime || !car.value) return '0.00'
  const days = Math.max(1, Math.ceil((new Date(rentForm.value.endTime) - new Date(rentForm.value.startTime)) / 86400000))
  return (car.value.pricePerDay * Math.min(15, days)).toFixed(2)
})

// 验证租车时间
const validateRentTime = () => {
  if (!rentForm.value.startTime || !rentForm.value.endTime) {
    ElMessage.warning('请选择租车时间')
    return false
  }
  const now = new Date()
  const start = new Date(rentForm.value.startTime)
  const end = new Date(rentForm.value.endTime)

  // 不能早于当前可租时间
  const minDate = getMinDate()
  if (start < minDate) {
    ElMessage.warning('开始时间不能早于最早可租时间')
    return false
  }

  // 结束时间必须晚于开始时间
  if (end <= start) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return false
  }

  // 最多15天
  const days = Math.ceil((end - start) / 86400000)
  if (days > 15) {
    ElMessage.warning('最多只能租15天')
    return false
  }

  return true
}

const openRentDialog = () => {
  rentForm.value = { startTime: '', endTime: '' }
  showRentDialog.value = true
}

const openReserveDialog = () => {
  rentForm.value = { startTime: '', endTime: '' }
  showReserveDialog.value = true
}

const handleRent = async () => {
  if (!validateRentTime()) return
  renting.value = true
  try {
    const res = await api.post('/orders/rent', {
      carId: car.value.id,
      startTime: rentForm.value.startTime,
      endTime: rentForm.value.endTime,
      isReservation: false
    })
    if (res.code === 200) {
      ElMessage.success('租车成功，请支付押金')
      showRentDialog.value = false
      await api.post(`/orders/pay/${res.data.id}`)
      ElMessage.success('押金支付成功')
      router.push('/orders')
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    renting.value = false
  }
}

const handleReserve = async () => {
  if (!validateRentTime()) return
  renting.value = true
  try {
    const res = await api.post('/orders/rent', {
      carId: car.value.id,
      startTime: rentForm.value.startTime,
      endTime: rentForm.value.endTime,
      isReservation: true
    })
    if (res.code === 200) {
      ElMessage.success('预约成功')
      showReserveDialog.value = false
      router.push('/orders')
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    renting.value = false
  }
}

const statusText = (s) => ({ 0: '空闲', 1: '已租出', 2: '已预约', 3: '维护中' }[s] || '未知')
const statusType = (s) => ({ 0: 'success', 1: 'danger', 2: 'warning', 3: 'info' }[s] || 'info')
const handleImgError = (e) => { e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 300"><rect fill="%23e0e7ff" width="800" height="300"/><text x="400" y="170" text-anchor="middle" fill="%23667eea" font-size="80">🚗</text></svg>' }
</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-content h1 { font-size: 18px; color: #333; }
.main { max-width: 900px; margin: 20px auto; padding: 0 20px; }
.detail-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.car-image-section {
  height: 280px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  position: relative;
  overflow: hidden;
}
.car-detail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.status-tag {
  position: absolute;
  top: 16px;
  right: 16px;
}
.tier-tag {
  position: absolute;
  top: 16px;
  left: 16px;
}
.usage-tags {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  gap: 6px;
}
.car-detail-info { padding: 24px; }
.car-detail-info h2 { font-size: 24px; color: #333; margin-bottom: 8px; }
.desc { color: #666; margin-bottom: 16px; }
.price { color: #f56c6c; font-size: 20px; font-weight: bold; }
.actions { margin-top: 20px; display: flex; gap: 16px; }
</style>
