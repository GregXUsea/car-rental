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
        <!-- 输入区域 -->
        <div class="input-section">
          <h2>描述您的用车需求</h2>
          <p class="tip">用自然语言描述即可，AI会理解您的需求并推荐最合适的车型</p>
          <el-input
            v-model="requirement"
            type="textarea"
            :rows="4"
            placeholder="请描述您的用车需求，包括人数、用途、预算、时间等..."
            @keydown.enter.exact="handleEnter"
          />
          <div class="input-actions">
            <el-button type="primary" size="large" @click="handleRecommend" :loading="loading" class="submit-btn">
              <el-icon><MagicStick /></el-icon> AI智能推荐
            </el-button>
            <span class="enter-hint">Enter 发送 · Ctrl+Enter 换行</span>
          </div>
        </div>

        <!-- 加载骨架屏 -->
        <div v-if="loading" class="loading-section">
          <el-skeleton :rows="3" animated />
          <div class="loading-text">
            <el-icon class="is-loading"><Loading /></el-icon>
            {{ loadingHint }}
          </div>
        </div>

        <!-- 错误状态 -->
        <div v-if="errorMsg" class="error-section">
          <el-result icon="error" :title="errorMsg" sub-title="AI服务暂时不可用，请稍后重试">
            <template #extra>
              <el-button type="primary" @click="handleRetry">重新尝试</el-button>
              <el-button @click="errorMsg = ''">手动修改需求</el-button>
            </template>
          </el-result>
        </div>

        <!-- 推荐结果区域 -->
        <div v-if="result && !loading && !errorMsg" class="result-section">
          <!-- 对话历史 -->
          <div v-if="conversationHistory.length > 0" class="conversation-history">
            <div v-for="(turn, idx) in conversationHistory" :key="idx" class="history-item">
              <div class="history-user"><strong>🙋 您：</strong>{{ turn.user }}</div>
              <div class="history-ai"><strong>🤖 AI：</strong>{{ turn.assistant }}</div>
            </div>
          </div>

          <!-- 推荐摘要 -->
          <el-alert type="success" :closable="false" show-icon style="margin-bottom: 20px;">
            <template #title>
              {{ result.summary }}
              <el-tag size="small" :type="result.poweredBy === 'AI' ? '' : 'info'" style="margin-left: 8px;">
                {{ result.poweredBy === 'AI' ? '🤖 AI推荐' : '📋 本地推荐' }}
              </el-tag>
            </template>
          </el-alert>

          <!-- 空结果 -->
          <el-empty v-if="!result.recommendations || result.recommendations.length === 0"
            description="未找到完全匹配的车辆，请尝试调整您的需求条件">
            <el-button type="primary" @click="requirement = ''; result = null; errorMsg = ''">重新输入</el-button>
          </el-empty>

          <!-- 推荐列表 -->
          <div v-else class="recommend-list">
            <div v-for="(item, index) in result.recommendations" :key="index" class="recommend-card">
              <div class="rank">{{ index + 1 }}</div>
              <!-- 单车展示 -->
              <template v-if="item.cars && item.cars.length === 1">
                <div class="car-img-wrapper">
                  <el-image :src="item.cars[0].image" fit="cover" class="car-img">
                    <template #error><div class="img-placeholder">🚗</div></template>
                  </el-image>
                </div>
                <div class="recommend-info">
                  <h3>{{ item.cars[0].brand }} {{ item.cars[0].model }}</h3>
                  <p class="reason">{{ item.reason }}</p>
                  <div class="meta">
                    <el-tag type="success">{{ item.matchScore }}</el-tag>
                    <span class="price">¥{{ item.cars[0].pricePerDay }}/天</span>
                    <span>{{ item.cars[0].seats }}座 · {{ item.cars[0].category }}</span>
                  </div>
                  <el-button type="primary" size="small" @click="$router.push(`/car/${item.cars[0].id}`)">查看详情</el-button>
                </div>
              </template>
              <!-- 多车组合展示 -->
              <template v-else>
                <div class="combo-cars">
                  <div v-for="(car, ci) in item.cars" :key="ci" class="combo-car">
                    <div class="car-img-wrapper small">
                      <el-image :src="car.image" fit="cover" class="car-img">
                        <template #error><div class="img-placeholder">🚗</div></template>
                      </el-image>
                    </div>
                    <div class="combo-car-info">
                      <span class="combo-car-name">{{ car.brand }} {{ car.model }}</span>
                      <span class="combo-car-meta">{{ car.seats }}座 · ¥{{ car.pricePerDay }}/天</span>
                    </div>
                  </div>
                </div>
                <div class="recommend-info">
                  <h3>🚗 多车组合方案</h3>
                  <p class="reason">{{ item.reason }}</p>
                  <div class="meta">
                    <el-tag type="success">{{ item.matchScore }}</el-tag>
                    <span class="price">总价 ¥{{ comboTotalPrice(item) }}/天</span>
                    <span>{{ comboTags(item) }}</span>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <!-- 追问区域 -->
          <div v-if="result && result.recommendations && result.recommendations.length > 0" class="follow-up-section">
            <el-divider />
            <p class="follow-up-tip">对推荐结果不满意？可以继续追问，比如"太贵了"、"有没有SUV"、"预算降到200以内"</p>
            <div class="follow-up-input">
              <el-input
                v-model="followUp"
                placeholder="输入追问需求..."
                @keydown.enter="handleFollowUp"
                :disabled="followUpLoading"
              />
              <el-button type="primary" @click="handleFollowUp" :loading="followUpLoading" :disabled="!followUp.trim()">
                追问
              </el-button>
              <el-button @click="handleRetry">换一批</el-button>
            </div>
          </div>
        </div>

        <!-- 示例需求（初始状态） -->
        <div class="example-section" v-if="!result && !loading && !errorMsg">
          <h3>💡 试试这些需求：</h3>
          <div class="examples">
            <el-button
              v-for="ex in examples"
              :key="ex"
              @click="quickTry(ex)"
              text
              class="example-btn"
            >
              "{{ ex }}"
            </el-button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const requirement = ref('')
const loading = ref(false)
const elapsedSeconds = ref(0)
let elapsedTimer = null

const result = ref(null)
const errorMsg = ref('')
const conversationId = ref(null)
const conversationHistory = ref([])

// 追问相关
const followUp = ref('')
const followUpLoading = ref(false)
const lastRequirement = ref('')

const examples = [
  '周末家庭出游三天，需要坐5人，预算400元/天',
  '商务接待客户，需要高档轿车，预算300-500元/天',
  '一家六口人自驾游，需要大空间，预算350元/天以内',
  '城市通勤代步，省油经济，预算200元/天以下'
]

const loadingHint = computed(() => {
  if (elapsedSeconds.value < 10) return 'AI正在分析您的需求，为您匹配最佳车型...'
  if (elapsedSeconds.value < 20) return '正在深度思考中，综合考虑车辆属性与您需求的匹配度...'
  return '大模型推理中，请耐心等待...若长时间无响应请刷新重试'
})

const handleEnter = (e) => {
  // Ctrl+Enter 换行，纯 Enter 提交
  if (!e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    handleRecommend()
  }
}

const startTimer = () => {
  elapsedSeconds.value = 0
  elapsedTimer = setInterval(() => { elapsedSeconds.value++ }, 1000)
}

const stopTimer = () => {
  if (elapsedTimer) { clearInterval(elapsedTimer); elapsedTimer = null }
}

const handleRecommend = async () => {
  if (!requirement.value.trim()) {
    ElMessage.warning('请输入您的用车需求')
    return
  }
  await doRecommend(requirement.value.trim())
}

const doRecommend = async (reqText) => {
  loading.value = true
  errorMsg.value = ''
  result.value = null
  followUp.value = ''
  startTimer()

  try {
    const payload = { requirement: reqText }
    if (conversationId.value) {
      payload.conversationId = conversationId.value
    }
    const res = await api.post('/ai/recommend', payload, { timeout: 90000 })
    if (res.code === 200) {
      result.value = res.data
      conversationId.value = res.data.conversationId || null
      lastRequirement.value = reqText
      // 记录对话历史
      conversationHistory.value.push({
        user: reqText.length > 100 ? reqText.substring(0, 100) + '...' : reqText,
        assistant: res.data.summary || '已为您推荐'
      })
    } else {
      errorMsg.value = res.message || '推荐失败'
    }
  } catch (e) {
    if (e.response && e.response.status === 401) {
      // 401由拦截器处理
    } else {
      errorMsg.value = '网络请求失败，请检查网络后重试'
    }
  } finally {
    loading.value = false
    stopTimer()
  }
}

const handleFollowUp = async () => {
  if (!followUp.value.trim() || followUpLoading.value) return
  followUpLoading.value = true
  const q = followUp.value.trim()
  followUp.value = ''
  await doRecommend(q)
  followUpLoading.value = false
}

const handleRetry = () => {
  errorMsg.value = ''
  if (lastRequirement.value) {
    doRecommend(lastRequirement.value)
  } else {
    handleRecommend()
  }
}

const quickTry = (text) => {
  requirement.value = text
  handleRecommend()
}

const comboTotalPrice = (item) => {
  if (!item.cars) return 0
  return item.cars.reduce((sum, c) => sum + (c.pricePerDay || 0), 0)
}

const comboTags = (item) => {
  if (!item.cars) return ''
  const totalSeats = item.cars.reduce((sum, c) => sum + (c.seats || 0), 0)
  return `${totalSeats}座 · ${item.cars.length}辆车`
}
</script>

<style scoped>
.header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.header-content { max-width: 900px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.header-content h1 { font-size: 18px; display: flex; align-items: center; gap: 8px; }
.main { max-width: 900px; margin: 20px auto; padding: 0 20px; }
.ai-card { background: #fff; border-radius: 12px; padding: 30px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.input-section h2 { margin-bottom: 8px; }
.tip { color: #999; margin-bottom: 16px; font-size: 14px; }
.input-actions { display: flex; align-items: center; gap: 16px; margin-top: 16px; }
.submit-btn { flex-shrink: 0; }
.enter-hint { color: #bbb; font-size: 12px; }

/* 加载状态 */
.loading-section { margin-top: 30px; padding: 20px; }
.loading-text { text-align: center; margin-top: 16px; color: #667eea; font-size: 14px; display: flex; align-items: center; justify-content: center; gap: 8px; }

/* 错误状态 */
.error-section { margin-top: 20px; }

/* 对话历史 */
.conversation-history { margin-bottom: 16px; padding: 12px; background: #fafafa; border-radius: 8px; }
.history-item { margin-bottom: 8px; font-size: 13px; line-height: 1.6; }
.history-user { color: #666; }
.history-ai { color: #667eea; }

/* 推荐结果 */
.result-section { margin-top: 30px; }
.recommend-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  align-items: flex-start;
  transition: box-shadow 0.2s;
}
.recommend-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
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
.car-img-wrapper.small { width: 80px; height: 60px; }
.car-img { width: 100%; height: 100%; }
.img-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 36px; background: #e0e7ff;
}
.car-img-wrapper.small .img-placeholder { font-size: 24px; }
.recommend-info { flex: 1; }
.recommend-info h3 { margin-bottom: 8px; }
.reason { color: #666; margin-bottom: 10px; font-size: 14px; line-height: 1.5; }
.meta { display: flex; gap: 12px; align-items: center; margin-bottom: 10px; flex-wrap: wrap; }
.price { color: #f56c6c; font-weight: bold; }

/* 多车组合 */
.combo-cars { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; }
.combo-car { display: flex; gap: 8px; align-items: center; }
.combo-car-info { display: flex; flex-direction: column; gap: 2px; }
.combo-car-name { font-size: 13px; font-weight: 600; }
.combo-car-meta { font-size: 12px; color: #999; }

/* 追问 */
.follow-up-section { margin-top: 20px; }
.follow-up-tip { color: #999; font-size: 13px; margin-bottom: 10px; }
.follow-up-input { display: flex; gap: 10px; }
.follow-up-input .el-input { flex: 1; }

/* 示例 */
.example-section { margin-top: 30px; }
.example-section h3 { margin-bottom: 12px; color: #666; }
.examples { display: flex; flex-wrap: wrap; gap: 8px; }
.example-btn { color: #667eea !important; cursor: pointer; }
</style>
