<template>
  <div class="order-page">
    <header class="header">
      <div class="header-content">
        <el-button text @click="$router.push('/')"><el-icon><ArrowLeft /></el-icon> 返回首页</el-button>
        <h1>订单中心</h1>
        <div></div>
      </div>
    </header>

    <main class="main">
      <el-tabs v-model="activeTab" @tab-change="loadOrders">
        <el-tab-pane label="我的订单" name="my" />
        <el-tab-pane label="全部订单" name="all" v-if="isAdmin" />
      </el-tabs>

      <el-table :data="orders" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column label="车辆" width="200">
          <template #default="{ row }">
            <span v-if="row.car">{{ row.car.brand }} {{ row.car.model }}</span>
          </template>
        </el-table-column>
        <el-table-column label="租车时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="还车时间" width="180">
          <template #default="{ row }">
            {{ row.actualReturnTime ? formatTime(row.actualReturnTime) : formatTime(row.endTime) + '(预计)' }}
          </template>
        </el-table-column>
        <el-table-column label="费用" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ row.totalCost }}</span>
          </template>
        </el-table-column>
        <el-table-column label="押金" width="100">
          <template #default="{ row }">¥{{ row.deposit }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0 || row.status === 4" type="success" size="small" @click="handlePay(row)">支付押金</el-button>
            <el-button v-if="row.status === 1" type="primary" size="small" @click="handleReturn(row)">归还车辆</el-button>
            <el-button v-if="row.status === 0 || row.status === 4" type="danger" size="small" @click="handleCancel(row)">取消订单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="orders.length === 0" description="暂无订单" />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const orders = ref([])
const activeTab = ref('my')
const isAdmin = ref(false)

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200) isAdmin.value = userRes.data.role === 1
  loadOrders()
})

const loadOrders = async () => {
  const url = activeTab.value === 'all' ? '/orders/all' : '/orders/my'
  const res = await api.get(url)
  if (res.code === 200) orders.value = res.data
}

const handlePay = async (order) => {
  await ElMessageBox.confirm('确认支付押金？', '提示')
  const res = await api.post(`/orders/pay/${order.id}`)
  if (res.code === 200) { ElMessage.success('支付成功'); loadOrders() }
  else ElMessage.error(res.message)
}

const handleReturn = async (order) => {
  await ElMessageBox.confirm('确认归还车辆？将自动结算费用', '归还确认')
  const res = await api.post(`/orders/return/${order.id}`)
  if (res.code === 200) {
    ElMessage.success(`归还成功，费用: ¥${res.data.totalCost}`)
    loadOrders()
  } else ElMessage.error(res.message)
}

const handleCancel = async (order) => {
  await ElMessageBox.confirm('确认取消订单？', '提示')
  const res = await api.post(`/orders/cancel/${order.id}`)
  if (res.code === 200) { ElMessage.success('已取消'); loadOrders() }
  else ElMessage.error(res.message)
}

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : ''
const orderStatusText = (s) => ({ 0: '待支付', 1: '在租', 2: '已完成', 3: '已取消', 4: '预约中' }[s] || '未知')
const orderStatusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'warning' }[s] || 'info')
</script>

<style scoped>
.header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.header-content { max-width: 1200px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.header-content h1 { font-size: 18px; }
.main { max-width: 1200px; margin: 20px auto; padding: 0 20px; background: #fff; border-radius: 12px; padding: 20px; }
.price { color: #f56c6c; font-weight: bold; }
</style>
