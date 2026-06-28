<template>
  <div class="driver-page">
    <header class="header">
      <div class="header-content">
        <el-button text @click="$router.push('/')">
          <IconSvg name="back" :size="18" /> 返回首页
        </el-button>
        <h1>
          <IconSvg name="user" :size="22" color="#333" />
          司机管理
        </h1>
        <el-button type="primary" @click="openAddDialog">
          <IconSvg name="magic" :size="16" color="#fff" /> 添加司机
        </el-button>
      </div>
    </header>

    <main class="main">
      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon available">
            <IconSvg name="shield" :size="24" color="#67c23a" />
          </div>
          <div class="stat-info">
            <span class="stat-num">{{ availableCount }}</span>
            <span class="stat-label">空闲司机</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon busy">
            <IconSvg name="car" :size="24" color="#409eff" />
          </div>
          <div class="stat-info">
            <span class="stat-num">{{ busyCount }}</span>
            <span class="stat-label">服务中</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon total">
            <IconSvg name="user" :size="24" color="#667eea" />
          </div>
          <div class="stat-info">
            <span class="stat-num">{{ drivers.length }}</span>
            <span class="stat-label">总人数</span>
          </div>
        </div>
      </div>

      <!-- 筛选 -->
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" size="default">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="0">空闲</el-radio-button>
          <el-radio-button label="1">服务中</el-radio-button>
          <el-radio-button label="2">休假</el-radio-button>
          <el-radio-button label="3">离职</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 司机卡片列表 -->
      <div class="driver-grid" v-if="filteredDrivers.length > 0">
        <div v-for="driver in filteredDrivers" :key="driver.id" class="driver-card" :class="'status-' + driver.status">
          <div class="card-header">
            <div class="driver-avatar">
              <img v-if="driver.avatar" :src="driver.avatar" :alt="driver.name" />
              <span v-else>{{ driver.name[0] }}</span>
            </div>
            <div class="driver-basic">
              <h3>{{ driver.name }}</h3>
              <p class="phone">{{ driver.phone }}</p>
            </div>
            <div class="driver-price">
              <span class="price-amount">¥150</span>
              <span class="price-unit">/天</span>
            </div>
            <el-tag :type="statusType(driver.status)" size="small">{{ statusText(driver.status) }}</el-tag>
          </div>

          <div class="card-body">
            <div class="info-row">
              <div class="info-item">
                <IconSvg name="car" :size="16" color="#666" />
                <span>{{ driver.licenseType }}驾照</span>
              </div>
              <div class="info-item">
                <IconSvg name="clock" :size="16" color="#666" />
                <span>{{ driver.experienceYears }}年驾龄</span>
              </div>
            </div>
            <div class="info-row">
              <div class="info-item">
                <span class="star">★</span>
                <span>{{ driver.rating }}分</span>
              </div>
              <div class="info-item">
                <IconSvg name="order" :size="16" color="#666" />
                <span>{{ driver.serviceCount }}次服务</span>
              </div>
            </div>
            <p class="driver-desc">{{ driver.description || '暂无简介' }}</p>
          </div>

          <div class="card-footer">
            <el-button size="small" @click="openEditDialog(driver)">
              <IconSvg name="search" :size="14" /> 编辑
            </el-button>
            <el-button v-if="driver.status === 0" type="primary" size="small" @click="updateStatus(driver.id, 2)">
              设为休假
            </el-button>
            <el-button v-if="driver.status === 2" type="success" size="small" @click="updateStatus(driver.id, 0)">
              恢复空闲
            </el-button>
            <el-button v-if="driver.status !== 3" type="danger" size="small" plain @click="updateStatus(driver.id, 3)">
              设为离职
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无司机数据" />
    </main>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="editId ? '编辑司机' : '添加司机'" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入司机姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item label="驾照类型" prop="licenseType">
          <el-select v-model="form.licenseType" placeholder="请选择">
            <el-option label="C1" value="C1" />
            <el-option label="C2" value="C2" />
            <el-option label="B1" value="B1" />
            <el-option label="B2" value="B2" />
            <el-option label="A1" value="A1" />
            <el-option label="A2" value="A2" />
          </el-select>
        </el-form-item>
        <el-form-item label="驾龄（年）">
          <el-input-number v-model="form.experienceYears" :min="0" :max="50" />
        </el-form-item>
        <el-form-item label="驾照到期">
          <el-date-picker v-model="form.licenseExpireDate" type="date" placeholder="选择日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="司机简介（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import IconSvg from '../components/IconSvg.vue'

const drivers = ref([])
const statusFilter = ref('')
const showDialog = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = ref({
  name: '',
  phone: '',
  idCard: '',
  licenseType: 'C1',
  experienceYears: 5,
  licenseExpireDate: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  licenseType: [{ required: true, message: '请选择驾照类型', trigger: 'change' }]
}

onMounted(() => loadDrivers())

const loadDrivers = async () => {
  const res = await api.get('/drivers/list')
  if (res.code === 200) drivers.value = res.data
}

const filteredDrivers = computed(() => {
  if (!statusFilter.value) return drivers.value
  return drivers.value.filter(d => String(d.status) === statusFilter.value)
})

const availableCount = computed(() => drivers.value.filter(d => d.status === 0).length)
const busyCount = computed(() => drivers.value.filter(d => d.status === 1).length)

const statusText = (s) => ({ 0: '空闲', 1: '服务中', 2: '休假', 3: '离职' }[s] || '未知')
const statusType = (s) => ({ 0: 'success', 1: 'primary', 2: 'warning', 3: 'info' }[s] || 'info')

const openAddDialog = () => {
  editId.value = null
  form.value = { name: '', phone: '', idCard: '', licenseType: 'C1', experienceYears: 5, licenseExpireDate: '', description: '' }
  showDialog.value = true
}

const openEditDialog = (driver) => {
  editId.value = driver.id
  form.value = { ...driver }
  showDialog.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (editId.value) {
      const res = await api.put('/drivers/update', { id: editId.value, ...form.value })
      if (res.code === 200) { ElMessage.success('修改成功'); showDialog.value = false; loadDrivers() }
      else ElMessage.error(res.message)
    } else {
      const res = await api.post('/drivers/add', form.value)
      if (res.code === 200) { ElMessage.success('添加成功'); showDialog.value = false; loadDrivers() }
      else ElMessage.error(res.message)
    }
  } finally { submitting.value = false }
}

const updateStatus = async (id, status) => {
  const text = { 0: '恢复空闲', 2: '设为休假', 3: '设为离职' }[status]
  await ElMessageBox.confirm(`确认${text}？`, '提示')
  const res = await api.post(`/drivers/status/${id}`, { status })
  if (res.code === 200) { ElMessage.success('操作成功'); loadDrivers() }
  else ElMessage.error(res.message)
}
</script>

<style scoped>
.header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.header-content { max-width: 1200px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.header-content h1 { font-size: 18px; display: flex; align-items: center; gap: 8px; }
.main { max-width: 1200px; margin: 20px auto; padding: 0 20px; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.stat-icon.available { background: #f0f9eb; }
.stat-icon.busy { background: #ecf5ff; }
.stat-icon.total { background: #e0e7ff; }
.stat-num { font-size: 28px; font-weight: bold; color: #333; }
.stat-label { font-size: 13px; color: #999; }
.stat-info { display: flex; flex-direction: column; }

.filter-bar { margin-bottom: 20px; }

.driver-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.driver-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); transition: all 0.2s; }
.driver-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.1); transform: translateY(-2px); }
.driver-card.status-3 { opacity: 0.6; }

.card-header { display: flex; align-items: center; gap: 12px; padding: 20px 20px 0; }
.driver-avatar { width: 56px; height: 56px; border-radius: 50%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; font-weight: bold; flex-shrink: 0; overflow: hidden; }
.driver-avatar img { width: 100%; height: 100%; object-fit: cover; }
.driver-basic { flex: 1; }
.driver-basic h3 { font-size: 18px; color: #333; margin-bottom: 2px; }
.phone { font-size: 13px; color: #999; }
.driver-price { display: flex; align-items: baseline; gap: 2px; margin-right: 8px; }
.price-amount { font-size: 22px; font-weight: 800; color: #f56c6c; }
.price-unit { font-size: 12px; color: #999; }

.card-body { padding: 16px 20px; }
.info-row { display: flex; gap: 20px; margin-bottom: 10px; }
.info-item { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #666; }
.star { color: #f7ba2a; }
.driver-desc { font-size: 13px; color: #999; margin-top: 12px; padding-top: 12px; border-top: 1px dashed #eee; line-height: 1.6; }

.card-footer { padding: 12px 20px; border-top: 1px solid #f5f5f5; display: flex; gap: 8px; justify-content: flex-end; background: #fafafa; }
</style>
