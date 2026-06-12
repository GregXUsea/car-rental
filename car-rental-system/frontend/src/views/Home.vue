<template>
  <div class="home">
    <header class="header">
      <div class="header-content">
        <h1>🚗 汽车租赁系统</h1>
        <div class="nav-right">
          <el-button type="primary" text @click="$router.push('/ai-assistant')">
            <el-icon><MagicStick /></el-icon> AI选车
          </el-button>
          <el-button type="primary" text @click="$router.push('/orders')">订单中心</el-button>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon> {{ userInfo.nickname || userInfo.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="main">
      <!-- 搜索和筛选 -->
      <div class="filter-section">
        <!-- 第一行：搜索 + 只看空闲 -->
        <div class="filter-row">
          <el-input v-model="keyword" placeholder="搜索品牌、型号..." size="large" clearable style="max-width: 400px;">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-checkbox v-model="onlyAvailable" size="large">只看空闲</el-checkbox>
        </div>

        <!-- 第二行：用途筛选 -->
        <div class="filter-row">
          <span class="filter-label">用途：</span>
          <el-radio-group v-model="filterUsage" size="large">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="商务">🏢 商务用车</el-radio-button>
            <el-radio-button label="婚庆">💒 婚庆用车</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 第三行：排序 -->
        <div class="filter-row">
          <span class="filter-label">排序：</span>
          <el-select v-model="sortBy" size="large" style="width: 200px;">
            <el-option label="默认排序" value="default" />
            <el-option label="押金从低到高" value="deposit_asc" />
            <el-option label="押金从高到低" value="deposit_desc" />
            <el-option label="日租从低到高" value="price_asc" />
            <el-option label="日租从高到低" value="price_desc" />
          </el-select>
        </div>
      </div>

      <!-- 统计 -->
      <div class="stats">
        <span>共 <strong>{{ filteredCars.length }}</strong> 辆车</span>
        <span v-if="onlyAvailable">· 仅显示空闲</span>
        <span v-if="filterUsage">· {{ filterUsage === '商务' ? '🏢 商务用车' : '💒 婚庆用车' }}</span>
        <span v-if="sortBy !== 'default'">· {{ sortLabel }}</span>
      </div>

      <!-- 车辆列表 -->
      <div class="car-grid">
        <div v-for="car in filteredCars" :key="car.id" class="car-card" @click="$router.push(`/car/${car.id}`)">
          <div class="car-image">
            <img :src="car.image" :alt="car.brand + ' ' + car.model" class="car-img" @error="handleImgError($event)" />
            <el-tag :type="statusType(car.status)" class="status-tag">{{ statusText(car.status) }}</el-tag>
            <div class="usage-tags">
              <el-tag v-if="isUsage(car, '商务')" type="primary" size="small" class="usage-tag">🏢 商务</el-tag>
              <el-tag v-if="isUsage(car, '婚庆')" type="danger" size="small" class="usage-tag">💒 婚庆</el-tag>
            </div>
          </div>
          <div class="car-info">
            <h3>{{ car.brand }} {{ car.model }}</h3>
            <p class="car-desc">{{ car.description }}</p>
            <div class="car-meta">
              <span><el-icon><User /></el-icon> {{ car.seats }}座</span>
              <span>{{ car.color }}</span>
            </div>
            <div class="car-price">
              <span class="price">¥{{ car.pricePerDay }}<small>/天</small></span>
              <span class="deposit">押金 ¥{{ car.deposit }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="filteredCars.length === 0" description="暂无符合条件的车辆" />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const cars = ref([])
const keyword = ref('')
const filterUsage = ref('')
const onlyAvailable = ref(false)
const sortBy = ref('default')
const userInfo = ref({})

onMounted(async () => {
  loadCars()
  const res = await api.get('/user/info')
  if (res.code === 200) userInfo.value = res.data
})

const loadCars = async () => {
  const res = await api.get('/cars/list')
  if (res.code === 200) cars.value = res.data
}

const isUsage = (car, usage) => {
  return car.usageType && car.usageType.includes(usage)
}

const sortLabel = computed(() => ({
  deposit_asc: '押金↑', deposit_desc: '押金↓',
  price_asc: '日租↑', price_desc: '日租↓'
}[sortBy.value] || ''))

const filteredCars = computed(() => {
  let list = [...cars.value]

  // 只看空闲
  if (onlyAvailable.value) {
    list = list.filter(c => c.status === 0)
  }

  // 用途筛选
  if (filterUsage.value) {
    list = list.filter(c => {
      if (!c.usageType) return false
      return c.usageType.includes(filterUsage.value)
    })
  }

  // 搜索品牌/型号
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(c =>
      c.brand.toLowerCase().includes(kw) ||
      c.model.toLowerCase().includes(kw) ||
      (c.description && c.description.toLowerCase().includes(kw))
    )
  }

  // 排序
  if (sortBy.value === 'deposit_asc') list.sort((a, b) => a.deposit - b.deposit)
  else if (sortBy.value === 'deposit_desc') list.sort((a, b) => b.deposit - a.deposit)
  else if (sortBy.value === 'price_asc') list.sort((a, b) => a.pricePerDay - b.pricePerDay)
  else if (sortBy.value === 'price_desc') list.sort((a, b) => b.pricePerDay - a.pricePerDay)

  return list
})

const statusText = (s) => ({ 0: '空闲', 1: '已租出', 2: '已预约', 3: '维护中' }[s] || '未知')
const statusType = (s) => ({ 0: 'success', 1: 'danger', 2: 'warning', 3: 'info' }[s] || 'info')
const handleImgError = (e) => { e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 220"><rect fill="%23e0e7ff" width="400" height="220"/><text x="200" y="120" text-anchor="middle" fill="%23667eea" font-size="48">🚗</text></svg>' }

const handleCommand = (cmd) => {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'logout') {
    localStorage.removeItem('token')
    router.push('/login')
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  position: sticky;
  top: 0;
  z-index: 100;
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
.header-content h1 { font-size: 22px; color: #667eea; }
.nav-right { display: flex; align-items: center; gap: 10px; }
.user-info { display: flex; align-items: center; gap: 5px; cursor: pointer; color: #666; }
.main { max-width: 1200px; margin: 0 auto; padding: 20px; }

.filter-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.filter-row:last-child { margin-bottom: 0; }
.filter-label { font-size: 14px; color: #666; white-space: nowrap; }

.stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #999;
}
.car-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.car-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.car-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}
.car-image {
  height: 180px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.car-img { width: 100%; height: 100%; object-fit: cover; }
.status-tag { position: absolute; top: 10px; right: 10px; }
.usage-tags { position: absolute; top: 10px; left: 10px; display: flex; gap: 4px; }
.car-info { padding: 16px; }
.car-info h3 { font-size: 16px; color: #333; margin-bottom: 8px; }
.car-desc {
  font-size: 13px; color: #999; margin-bottom: 10px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.car-meta {
  display: flex; gap: 16px; font-size: 13px; color: #666; margin-bottom: 12px;
}
.car-meta span { display: flex; align-items: center; gap: 4px; }
.car-price { display: flex; align-items: baseline; justify-content: space-between; }
.price { font-size: 22px; color: #f56c6c; font-weight: bold; }
.price small { font-size: 13px; font-weight: normal; color: #999; }
.deposit { font-size: 12px; color: #999; }
</style>
