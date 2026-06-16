<template>
  <div class="ai-page">
    <header class="header">
      <div class="header-content">
        <el-button text @click="$router.push('/')"><el-icon><ArrowLeft /></el-icon> 返回首页</el-button>
        <h1><el-icon><MagicStick /></el-icon> AI智能选车助手</h1>
        <div></div>
      </div>
    </header>

    <main class="main">
      <div class="ai-card">
        <div class="input-section">
          <h2>描述您的用车需求</h2>
          <p class="tip">例如：周末家庭出游三天，需要坐5人，预算400元/天</p>
          <el-input
            v-model="requirement"
            type="textarea"
            :rows="4"
            placeholder="请描述您的用车需求，包括人数、用途、预算、时间等..."
          />
          <el-button type="primary" size="large" @click="handleRecommend" :loading="loading" style="margin-top: 16px; width: 100%;">
            <el-icon><MagicStick /></el-icon> AI智能推荐
          </el-button>
        </div>

        <div v-if="result" class="result-section">
          <el-alert :title="result.summary" type="success" :closable="false" show-icon style="margin-bottom: 20px;" />

          <div class="recommend-list">
            <div v-for="(item, index) in result.recommendations" :key="index" class="recommend-card">
              <div class="rank">{{ index + 1 }}</div>
              <div class="car-img-wrapper">
                <img :src="item.car?.image" :alt="item.car?.brand" class="car-img" @error="handleImgError($event)" />
              </div>
              <div class="recommend-info">
                <h3>{{ item.car?.brand }} {{ item.car?.model }}</h3>
                <p class="reason">{{ item.reason }}</p>
                <div class="meta">
                  <el-tag type="success">{{ item.matchScore }}</el-tag>
                  <span class="price">¥{{ item.car?.pricePerDay }}/天</span>
                  <span>{{ item.car?.seats }}座 · {{ item.car?.category }}</span>
                </div>
                <el-button type="primary" size="small" @click="$router.push(`/car/${item.car?.id}`)">查看详情</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="example-section" v-if="!result && !loading">
          <h3>试试这些需求：</h3>
          <div class="examples">
            <el-button v-for="ex in examples" :key="ex" @click="requirement = ex" text class="example-btn">
              "{{ ex }}"
            </el-button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const requirement = ref('')
const loading = ref(false)
const result = ref(null)

const examples = [
  '周末家庭出游三天，需要坐5人，预算400元/天',
  '商务接待客户，需要高档轿车，预算300-500元/天',
  '一家六口人自驾游，需要大空间，预算350元/天以内',
  '城市通勤代步，省油经济，预算200元/天以下'
]

const handleRecommend = async () => {
  if (!requirement.value.trim()) {
    ElMessage.warning('请输入您的用车需求')
    return
  }
  loading.value = true
  result.value = null
  try {
    const res = await api.post('/ai/recommend', { requirement: requirement.value })
    if (res.code === 200) {
      result.value = res.data
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    if (e.response && e.response.status === 401) {
      // 401已由拦截器处理，不重复提示
    } else {
      ElMessage.error('请求失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const handleImgError = (e) => { e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 120"><rect fill="%23e0e7ff" width="200" height="120"/><text x="100" y="70" text-anchor="middle" fill="%23667eea" font-size="40">🚗</text></svg>' }
</script>

<style scoped>
.header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.header-content { max-width: 900px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.header-content h1 { font-size: 18px; display: flex; align-items: center; gap: 8px; }
.main { max-width: 900px; margin: 20px auto; padding: 0 20px; }
.ai-card { background: #fff; border-radius: 12px; padding: 30px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.input-section h2 { margin-bottom: 8px; }
.tip { color: #999; margin-bottom: 16px; font-size: 14px; }
.result-section { margin-top: 30px; }
.recommend-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  align-items: flex-start;
}
.rank {
  width: 32px; height: 32px;
  background: #667eea;
  color: #fff;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
}
.car-img-wrapper {
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #e0e7ff;
}
.car-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.recommend-info { flex: 1; }
.recommend-info h3 { margin-bottom: 8px; }
.reason { color: #666; margin-bottom: 10px; font-size: 14px; }
.meta { display: flex; gap: 12px; align-items: center; margin-bottom: 10px; }
.price { color: #f56c6c; font-weight: bold; }
.example-section { margin-top: 30px; }
.example-section h3 { margin-bottom: 12px; color: #666; }
.examples { display: flex; flex-wrap: wrap; gap: 8px; }
.example-btn { color: #667eea !important; }
</style>
