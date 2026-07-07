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
          <router-link to="/admin/cars" class="nav-item active">车辆管理</router-link>
          <router-link to="/admin/orders" class="nav-item">订单管理</router-link>
          <router-link to="/admin/users" class="nav-item">用户管理</router-link>
          <router-link to="/admin/messages" class="nav-item">消息中心</router-link>
          <router-link to="/admin/maintenance" class="nav-item">维护看板</router-link>
        </nav>
        <div class="header-right">
          <span class="admin-badge">管理员</span>
          <router-link to="/" class="back-link">返回前台</router-link>
        </div>
      </div>
    </header>

    <main class="admin-main">
      <div class="page-header">
        <h2>车辆管理</h2>
        <button class="add-btn" @click="openAddDialog">+ 添加车辆</button>
      </div>

      <div class="car-grid">
        <div v-for="car in cars" :key="car.id" class="car-card">
          <div class="car-img-wrap">
            <img :src="car.image" :alt="car.brand + car.model" class="car-img" @error="handleImgError" />
            <span :class="'status-tag status-' + car.status">{{ statusText(car.status) }}</span>
          </div>
          <div class="car-info">
            <h3>{{ car.brand }} {{ car.model }}</h3>
            <div class="car-meta">
              <span>{{ car.category }}</span>
              <span>{{ car.seats }}座</span>
              <span>¥{{ car.pricePerDay }}/天</span>
            </div>
            <div class="car-actions">
              <button class="btn-edit" @click="openEditDialog(car)">编辑</button>
              <button class="btn-delete" @click="handleDelete(car)">删除</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 添加/编辑车辆弹窗 -->
      <el-dialog v-model="showDialog" :title="isEdit ? '编辑车辆' : '添加车辆'" width="500px">
        <el-form :model="carForm" label-width="80px">
          <el-form-item label="品牌">
            <el-input v-model="carForm.brand" placeholder="如：丰田" />
          </el-form-item>
          <el-form-item label="型号">
            <el-input v-model="carForm.model" placeholder="如：卡罗拉 2024款" />
          </el-form-item>
          <el-form-item label="颜色">
            <el-input v-model="carForm.color" placeholder="如：白色" />
          </el-form-item>
          <el-form-item label="座位数">
            <el-input-number v-model="carForm.seats" :min="2" :max="9" />
          </el-form-item>
          <el-form-item label="日租金">
            <el-input-number v-model="carForm.pricePerDay" :min="0" :step="10" />
          </el-form-item>
          <el-form-item label="押金">
            <el-input-number v-model="carForm.deposit" :min="0" :step="100" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="carForm.category" placeholder="请选择">
              <el-option label="轿车" value="轿车" />
              <el-option label="SUV" value="SUV" />
              <el-option label="MPV" value="MPV" />
              <el-option label="新能源" value="新能源" />
            </el-select>
          </el-form-item>
          <el-form-item label="用途">
            <el-checkbox-group v-model="carForm.usageTypes">
              <el-checkbox label="商务">商务</el-checkbox>
              <el-checkbox label="婚庆">婚庆</el-checkbox>
              <el-checkbox label="家庭">家庭</el-checkbox>
              <el-checkbox label="通勤">通勤</el-checkbox>
              <el-checkbox label="旅游">旅游</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="carForm.status">
              <el-option label="空闲" :value="0" />
              <el-option label="维护中" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="carForm.description" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const cars = ref([])
const showDialog = ref(false)
const isEdit = ref(false)
const editingId = ref(null)

const carForm = ref({
  brand: '',
  model: '',
  color: '',
  seats: 5,
  pricePerDay: 158,
  deposit: 2000,
  category: '轿车',
  usageTypes: ['通勤'],
  status: 0,
  description: '',
  image: '/img/default-car.png'
})

const statusText = (s) => ({ 0: '空闲', 1: '已租出', 2: '已预约', 3: '维护中' }[s] || '未知')

const loadCars = async () => {
  const res = await api.get('/admin/cars')
  if (res.code === 200) cars.value = res.data
}

const openAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  carForm.value = {
    brand: '', model: '', color: '', seats: 5,
    pricePerDay: 158, deposit: 2000, category: '轿车',
    usageTypes: ['通勤'], status: 0, description: '', image: '/img/default-car.png'
  }
  showDialog.value = true
}

const openEditDialog = (car) => {
  isEdit.value = true
  editingId.value = car.id
  carForm.value = {
    brand: car.brand,
    model: car.model,
    color: car.color || '',
    seats: car.seats,
    pricePerDay: car.pricePerDay,
    deposit: car.deposit,
    category: car.category,
    usageTypes: car.usageType ? car.usageType.split(',') : [],
    status: car.status,
    description: car.description || '',
    image: car.image || '/img/default-car.png'
  }
  showDialog.value = true
}

const handleSave = async () => {
  const data = {
    ...carForm.value,
    usageType: carForm.value.usageTypes.join(',')
  }
  delete data.usageTypes

  let res
  if (isEdit.value) {
    res = await api.put(`/admin/cars/${editingId.value}`, data)
  } else {
    res = await api.post('/admin/cars', data)
  }

  if (res.code === 200) {
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    showDialog.value = false
    loadCars()
  } else {
    ElMessage.error(res.message)
  }
}

const handleDelete = async (car) => {
  try {
    await ElMessageBox.confirm(`确定删除 ${car.brand} ${car.model} 吗？`, '删除确认', { type: 'warning' })
    const res = await api.delete(`/admin/cars/${car.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadCars()
    }
  } catch (e) { /* 用户取消 */ }
}

const handleImgError = (e) => {
  e.target.src = '/img/default-car.png'
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200 && userRes.data.role !== 1) {
    router.push('/')
    return
  }
  loadCars()
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; color: #333; }
.add-btn { padding: 8px 20px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; }
.add-btn:hover { opacity: 0.9; }
.car-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.car-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: all 0.2s; }
.car-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.car-img-wrap { position: relative; height: 160px; }
.car-img { width: 100%; height: 100%; object-fit: cover; }
.status-tag { position: absolute; top: 10px; right: 10px; padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 500; }
.status-0 { background: rgba(103,194,58,0.9); color: #fff; }
.status-1 { background: rgba(245,108,108,0.9); color: #fff; }
.status-2 { background: rgba(230,162,60,0.9); color: #fff; }
.status-3 { background: rgba(144,147,153,0.9); color: #fff; }
.car-info { padding: 16px; }
.car-info h3 { font-size: 16px; color: #333; margin-bottom: 8px; }
.car-meta { display: flex; gap: 12px; font-size: 13px; color: #666; margin-bottom: 12px; }
.car-actions { display: flex; gap: 10px; }
.btn-edit { padding: 6px 14px; background: #667eea; color: #fff; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; }
.btn-edit:hover { background: #5a6fd6; }
.btn-delete { padding: 6px 14px; background: #f56c6c; color: #fff; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; }
.btn-delete:hover { background: #e74c3c; }
</style>
