<template>
  <div class="ai-page">
    <!-- 左侧边栏 -->
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="createNewChat">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新对话
        </button>
      </div>
      <div class="sidebar-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="sidebar-item"
          :class="{ active: currentConvId === conv.id }"
          @click="switchConversation(conv.id)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          <span class="item-title">{{ conv.title }}</span>
        </div>
        <div v-if="conversations.length === 0" class="sidebar-empty">
          <p>暂无历史对话</p>
        </div>
      </div>
      <div class="sidebar-footer">
        <button class="back-home-btn" @click="$router.push('/')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          返回首页
        </button>
      </div>
    </aside>

    <!-- 右侧聊天区 -->
    <div class="chat-main">
      <!-- 顶部标题栏 -->
      <div class="chat-header">
        <button class="back-btn" @click="$router.push('/')" title="返回首页">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        </button>
        <button class="toggle-sidebar-btn" @click="toggleSidebar" title="切换侧边栏">
          <svg v-if="sidebarOpen" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
        </button>
        <div class="chat-title-area">
          <h1>AI智能选车助手</h1>
          <p class="ai-disclaimer">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            AI 生成内容可能有误，请以实际信息为准 · 如果不想选车，也可以和我聊聊其他话题
          </p>
        </div>
      </div>

      <!-- 对话内容区 -->
      <div class="chat-body" ref="chatArea" @scroll="onChatScroll">
        <!-- 欢迎区 -->
        <div v-if="messages.length === 0" class="welcome-section">
          <div class="welcome-avatar">
            <img src="/img/ai-avatar.png" alt="AI" />
          </div>
          <h2>你好，我是AI助手</h2>
          <p>我可以为您推荐车型、解答租车问题，也可以回答其他问题</p>
          <div class="example-cards">
            <div v-for="(ex, i) in examples" :key="i" class="example-card" @click="sendMessage(ex.text)">
              <span class="ex-text">{{ ex.text }}</span>
            </div>
          </div>
        </div>

        <!-- 对话区 -->
        <div v-else class="messages-area">
          <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
            <div class="msg-avatar ai-msg-avatar" v-if="msg.role === 'ai'">
              <img src="/img/ai-avatar.png" alt="AI" class="ai-avatar-img" />
            </div>
            <div class="msg-avatar user-msg-avatar" v-else>
              <img v-if="userAvatar" :src="userAvatar" class="user-avatar-img" />
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <div class="msg-content">
              <!-- 用户消息 -->
              <div v-if="msg.role === 'user'" class="user-text">{{ msg.text }}</div>
              <!-- AI回复 - 文本 -->
              <div v-else-if="msg.type === 'text' || (!msg.type && !msg.result)" class="ai-text-response">
                <div v-if="msg.loading" class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
                <div v-else class="ai-text" v-html="formatText(msg.reply)"></div>
              </div>
              <!-- AI回复 - 推荐结果 -->
              <div v-else class="ai-response">
                <div class="ai-summary" v-if="msg.result?.recommendations?.length">
                  <p>{{ msg.result.summary || '根据您的需求，为您推荐以下车型：' }}</p>
                </div>
                <div class="recommend-cards" v-if="msg.result?.recommendations?.length">
                  <div v-for="(item, idx) in msg.result.recommendations" :key="idx" class="recommend-card">
                    <div class="card-header">
                      <span class="rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                      <div class="card-title">
                        <h3>{{ item.car?.brand }} {{ item.car?.model }}</h3>
                        <div class="card-tags">
                          <span class="tag category">{{ item.car?.category }}</span>
                          <span class="tag seats">{{ item.car?.seats }}座</span>
                          <span v-if="item.car?.usageType?.includes('商务')" class="tag business">商务</span>
                          <span v-if="item.car?.usageType?.includes('婚庆')" class="tag wedding">婚庆</span>
                          <span v-if="item.car?.usageType?.includes('家庭')" class="tag family">家庭</span>
                          <span v-if="item.car?.usageType?.includes('通勤')" class="tag commute">通勤</span>
                        </div>
                      </div>
                      <div class="match-score">
                        <div class="score-bar">
                          <div class="score-fill" :style="{ width: item.matchScore }"></div>
                        </div>
                        <span class="score-text">{{ item.matchScore }}</span>
                      </div>
                    </div>
                    <div class="card-body">
                      <div class="car-img-wrap">
                        <img :src="item.car?.image" :alt="item.car?.brand" class="car-img" :data-brand="item.car?.brand" :data-model="item.car?.model" :data-color="item.car?.color" :data-category="item.car?.category" @error="handleImgError($event)" />
                      </div>
                      <div class="card-info">
                        <p class="reason">{{ item.reason }}</p>
                        <div class="card-meta">
                          <span class="price">¥{{ item.car?.pricePerDay }}<small>/天</small></span>
                          <span class="deposit">押金 ¥{{ item.car?.deposit }}</span>
                          <span class="mileage">{{ item.car?.mileage?.toLocaleString() }}km</span>
                        </div>
                        <el-button type="primary" size="small" @click="goToCarDetail(item.car?.id)">
                          查看详情 & 立即租车 →
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 思考中动画 -->
                <div v-if="msg.loading" class="thinking">
                  <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 回到顶部按钮 -->
      <transition name="fade">
        <button v-if="showScrollTop" class="scroll-top-btn" @click="scrollToTop" title="回到顶部">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>
        </button>
      </transition>

      <!-- 输入区 -->
      <div class="input-bar">
        <div class="input-wrap">
          <input
            v-model="inputText"
            placeholder="描述您的需求或问题，如：推荐一辆商务车、租车多少钱..."
            @keydown.enter.prevent="handleSend"
            :disabled="loading"
          />
          <button class="send-btn" @click="handleSend" :disabled="!inputText.trim() || loading">
            <svg v-if="!loading" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            <div v-else class="btn-spinner"></div>
          </button>
        </div>
        <p class="input-hint">按 Enter 发送 · AI基于车队实时数据为您推荐</p>
      </div>
    </div>

    <!-- 移动端遮罩 -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const $router = useRouter()

const inputText = ref('')
const loading = ref(false)
const messages = ref([])
const chatArea = ref(null)
const userAvatar = ref(localStorage.getItem('userAvatar') || '')
const sidebarOpen = ref(true) // 桌面端默认打开
const showScrollTop = ref(false)

// 多会话管理 - 每个用户独立的会话历史
const conversations = ref([])
const currentConvId = ref('')
const currentUserId = ref('')

// 当前是否正在创建新对话（防重复点击）
const creatingNew = ref(false)

// 获取用户独立的storage key
const getConversationsKey = () => `ai_conversations_${currentUserId.value}`

onMounted(async () => {
  // 获取用户信息
  try {
    const userRes = await api.get('/user/info')
    if (userRes.code === 200) {
      currentUserId.value = userRes.data.id
      if (userRes.data.avatar) {
        userAvatar.value = userRes.data.avatar
        localStorage.setItem('userAvatar', userRes.data.avatar)
      }
    }
  } catch (e) { /* ignore */ }

  loadConversations()
  loadCarImages() // 加载车辆图片缓存
})

const examples = [
  { icon: '💼', text: '商务接待客户，需要高档黑色轿车，预算400-500元/天' },
  { icon: '💒', text: '下个月结婚，需要婚庆头车，红色或白色豪华车型' },
  { icon: '👨‍👩‍👧‍👦', text: '周末家庭出游三天，需要坐5人，预算400元/天' },
  { icon: '🚗', text: '推荐几款适合日常通勤的经济型轿车' },
  { icon: '🏔️', text: '想自驾去川西旅游，推荐什么SUV？' },
  { icon: '💰', text: '有没有新能源车推荐？租金多少？' },
]

// ====== 多会话管理 ======
const loadConversations = () => {
  if (!currentUserId.value) return
  try {
    const key = getConversationsKey()
    const saved = localStorage.getItem(key)
    if (saved) {
      conversations.value = JSON.parse(saved)
    }
    // 选中会话：优先恢复上次活跃的会话，否则选最新
    if (conversations.value.length > 0) {
      const lastConvId = sessionStorage.getItem('ai_last_conv_id')
      const savedScroll = sessionStorage.getItem('ai_scroll_top')
      const target = lastConvId && conversations.value.find(c => c.id === lastConvId)
        ? lastConvId
        : conversations.value[0].id
      // 如果有保存的滚动位置，恢复到点击位置而非底部
      if (savedScroll !== null && lastConvId) {
        sessionStorage.removeItem('ai_scroll_top')
        switchConversation(target, parseInt(savedScroll) || 0)
      } else {
        switchConversation(target)
      }
    }
  } catch (e) { /* ignore */ }
}

const saveConversations = () => {
  if (!currentUserId.value) return
  try {
    const key = getConversationsKey()
    localStorage.setItem(key, JSON.stringify(conversations.value))
  } catch (e) { /* ignore */ }
}

const buildConvFromMessages = (msgs) => {
  const validMsgs = msgs.filter(m => !m.loading)
  const firstUserMsg = validMsgs.find(m => m.role === 'user')
  const title = extractTitle(firstUserMsg?.text || '新对话')
  return {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
    title,
    messages: validMsgs,
    createTime: Date.now()
  }
}

// 从用户第一个问题提炼标题（最多20字）
const extractTitle = (text) => {
  if (!text) return '新对话'
  // 去掉标点和多余空格
  let t = text.replace(/[，。！？、\.\,\!\?\s]+/g, ' ').trim()
  if (t.length <= 20) return t
  return t.slice(0, 20) + '...'
}

// 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

// 创建新对话
const createNewChat = () => {
  // 如果已有空对话，直接切换过去，不重复创建
  const emptyConv = conversations.value.find(c => c.messages.length === 0)
  if (emptyConv) {
    switchConversation(emptyConv.id)
    return
  }
  const conv = {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
    title: '新对话',
    messages: [],
    createTime: Date.now()
  }
  conversations.value.unshift(conv)
  saveConversations()
  switchConversation(conv.id)
}

const switchConversation = (id, scrollTo) => {
  currentConvId.value = id
  const conv = conversations.value.find(c => c.id === id)
  messages.value = conv ? [...conv.messages] : []
  sessionStorage.setItem('ai_last_conv_id', id)
  // 移动端自动收起侧边栏
  if (window.innerWidth <= 768) sidebarOpen.value = false
  // 恢复滚动位置或滚到底部
  if (scrollTo !== undefined) {
    nextTick(() => { if (chatArea.value) chatArea.value.scrollTop = scrollTo })
  } else {
    scrollToBottom()
  }
}

// 跳转车辆详情前保存当前对话ID，返回时恢复
const goToCarDetail = (carId) => {
  sessionStorage.setItem('ai_last_conv_id', currentConvId.value)
  // 保存当前滚动位置，返回时恢复
  if (chatArea.value) {
    sessionStorage.setItem('ai_scroll_top', chatArea.value.scrollTop)
  }
  sessionStorage.setItem('car_detail_from', '/ai-assistant')
  $router.push(`/car/${carId}`)
}

// 保存当前会话消息
const saveCurrentMessages = () => {
  const conv = conversations.value.find(c => c.id === currentConvId.value)
  if (conv) {
    conv.messages = messages.value.filter(m => !m.loading)
    // 如果是第一条消息，更新标题
    if (conv.messages.length === 1 && conv.messages[0].role === 'user') {
      conv.title = extractTitle(conv.messages[0].text)
    }
    saveConversations()
  }
}

// ====== 对话逻辑 ======
const buildApiHistory = () => {
  return messages.value
    .filter(m => !m.loading && (m.role === 'user' || (m.role === 'ai' && m.type === 'text' && m.reply)))
    .slice(-20)
    .map(m => {
      if (m.role === 'user') return { role: 'user', content: m.text }
      return { role: 'assistant', content: m.reply || m.result?.summary || '' }
    })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatArea.value) chatArea.value.scrollTop = chatArea.value.scrollHeight
  })
}

// 回到顶部
const scrollToTop = () => {
  if (chatArea.value) chatArea.value.scrollTo({ top: 0, behavior: 'smooth' })
}

// 打字机效果：逐字显示AI回复（通过索引操作确保Vue响应式更新）
const typeWriter = (msgIndex, fullText) => {
  return new Promise((resolve) => {
    let charIdx = 0
    const speed = 18 // 每字符间隔(ms)
    const chunkSize = 2 // 每次显示2个字符

    const type = () => {
      if (charIdx < fullText.length) {
        // 通过 messages.value[msgIndex] 触发 Vue 响应式更新
        messages.value[msgIndex].reply += fullText.slice(charIdx, charIdx + chunkSize)
        charIdx += chunkSize
        scrollToBottom()
        const lastChar = fullText[charIdx - 1]
        const delay = '。！？，；：'.includes(lastChar) ? speed * 4 : speed
        setTimeout(type, delay)
      } else {
        resolve()
      }
    }
    type()
  })
}

// 监听滚动，显示/隐藏回到顶部按钮
const onChatScroll = () => {
  if (!chatArea.value) return
  const { scrollTop, scrollHeight, clientHeight } = chatArea.value
  // 距离底部超过200px时显示
  showScrollTop.value = (scrollHeight - scrollTop - clientHeight) > 200
}

const sendMessage = async (text) => {
  if (!text.trim() || loading.value) return
  inputText.value = ''

  // 如果没有当前会话，创建一个
  if (!currentConvId.value) {
    createNewChat()
  }

  // 添加用户消息
  messages.value.push({ role: 'user', text: text.trim() })
  scrollToBottom()
  saveCurrentMessages()

  // 添加AI思考中的占位消息
  const aiMsg = { role: 'ai', type: 'text', reply: '', result: null, loading: true }
  messages.value.push(aiMsg)
  scrollToBottom()

  loading.value = true
  const msgIndex = messages.value.length - 1 // aiMsg 在数组中的索引
  try {
    const history = buildApiHistory()
    const res = await api.post('/ai/chat', { message: text.trim(), history })
    if (res.code === 200) {
      const data = res.data
      messages.value[msgIndex].type = data.type || 'text'
      if (data.type === 'recommend') {
        messages.value[msgIndex].result = { summary: data.reply, recommendations: data.recommendations || [] }
        messages.value[msgIndex].loading = false
      } else {
        // 文本回复：打字机效果逐字显示
        const fullReply = data.reply || '抱歉，我暂时无法回答这个问题。'
        messages.value[msgIndex].reply = ''
        messages.value[msgIndex].loading = false
        await typeWriter(msgIndex, fullReply)
      }
    } else {
      messages.value[msgIndex].reply = ''
      messages.value[msgIndex].loading = false
      await typeWriter(msgIndex, '抱歉，服务暂时不可用：' + res.message)
    }
  } catch (e) {
    messages.value[msgIndex].reply = ''
    messages.value[msgIndex].loading = false
    await typeWriter(msgIndex, '请求失败，请稍后重试')
  } finally {
    loading.value = false
    scrollToBottom()
    saveCurrentMessages()
  }
}

const handleSend = () => {
  sendMessage(inputText.value)
}

// 车辆数据缓存（包含图片、价格等完整信息）
const carDataCache = ref({})

const formatText = (text) => {
  if (!text) return ''
  // 将Markdown格式的链接转换为车辆卡片HTML
  let formatted = text.replace(/\[([^\]]+)\]\(\/car\/(\d+)\)/g, (match, title, carId) => {
    const car = carDataCache.value[carId]
    if (car) {
      return `<div class="inline-car-card" onclick="event.preventDefault();sessionStorage.setItem('car_detail_from','/ai-assistant');window.location.href='/car/${carId}'">
        <img src="${car.image}" class="inline-car-img" onerror="this.style.display='none'" />
        <div class="inline-car-info">
          <div class="inline-car-name">${car.brand} ${car.model}</div>
          <div class="inline-car-tags">
            <span class="inline-tag">${car.category || '轿车'}</span>
            <span class="inline-tag">${car.seats}座</span>
            ${car.usageType?.includes('商务') ? '<span class="inline-tag business">商务</span>' : ''}
            ${car.usageType?.includes('家庭') ? '<span class="inline-tag family">家庭</span>' : ''}
          </div>
          <div class="inline-car-price">¥${car.pricePerDay}/天</div>
          <div class="inline-car-btn">查看详情 & 立即租车 →</div>
        </div>
      </div>`
    }
    return `<a href="/car/${carId}" class="car-card-link" onclick="event.preventDefault();sessionStorage.setItem('car_detail_from','/ai-assistant');window.location.href='/car/${carId}'"><span class="car-card-icon">🚗</span><span class="car-card-text">${title}</span><span class="car-card-arrow">→</span></a>`
  })
  formatted = formatted.replace(/\n/g, '<br>')
  return formatted
}

// 加载车辆数据缓存
const loadCarImages = async () => {
  try {
    const res = await api.get('/cars/list')
    if (res.code === 200) {
      res.data.forEach(car => {
        carDataCache.value[car.id] = car
      })
    }
  } catch (e) {}
}

const getBrandTheme = (brand) => {
  const themes = {
    '丰田': { primary: '#e74c3c', secondary: '#c0392b', bg1: '#fde8e8', bg2: '#f5b7b1' },
    '本田': { primary: '#3498db', secondary: '#2980b9', bg1: '#d6eaf8', bg2: '#aed6f1' },
    '大众': { primary: '#2ecc71', secondary: '#27ae60', bg1: '#d5f5e3', bg2: '#a9dfbf' },
    '宝马': { primary: '#3498db', secondary: '#2471a3', bg1: '#d4e6f1', bg2: '#a9cce3' },
    '奔驰': { primary: '#95a5a6', secondary: '#7f8c8d', bg1: '#ebedef', bg2: '#d5d8dc' },
    '别克': { primary: '#e67e22', secondary: '#d35400', bg1: '#fdebd0', bg2: '#f5cba7' },
    '比亚迪': { primary: '#1abc9c', secondary: '#16a085', bg1: '#d1f2eb', bg2: '#a3e4d7' },
    '红旗': { primary: '#c0392b', secondary: '#922b21', bg1: '#f9ebea', bg2: '#f2d7d5' },
    '蔚来': { primary: '#3498db', secondary: '#2471a3', bg1: '#d4e6f1', bg2: '#a9cce3' },
    '小鹏': { primary: '#2ecc71', secondary: '#27ae60', bg1: '#d5f5e3', bg2: '#a9dfbf' },
  }
  return themes[brand] || { primary: '#667eea', secondary: '#5a67d8', bg1: '#e0e7ff', bg2: '#c7d2fe' }
}

const handleImgError = (e) => {
  const brand = e.target.dataset.brand || '默认'
  const category = e.target.dataset.category || '轿车'
  const theme = getBrandTheme(brand)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 120">
    <defs>
      <linearGradient id="bg_${brand}" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" style="stop-color:${theme.bg1}"/>
        <stop offset="100%" style="stop-color:${theme.bg2}"/>
      </linearGradient>
      <linearGradient id="body_${brand}" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" style="stop-color:${theme.primary}"/>
        <stop offset="100%" style="stop-color:${theme.secondary}"/>
      </linearGradient>
    </defs>
    <rect fill="url(#bg_${brand})" width="200" height="120" rx="8"/>
    <g transform="translate(100,55) scale(0.65)">
      <ellipse cx="0" cy="20" rx="90" ry="8" fill="${theme.bg2}" opacity="0.6"/>
      <path d="M-65,-10 Q-65,-35 -40,-35 L-20,-35 L0,-55 L30,-55 Q55,-55 60,-35 L70,-35 Q80,-35 80,-25 L80,-10 Q80,5 65,5 L-55,5 Q-65,5 -65,-10Z" fill="url(#body_${brand})" stroke="${theme.secondary}" stroke-width="1.5"/>
      <circle cx="-40" cy="8" r="10" fill="#2d3748"/><circle cx="55" cy="8" r="10" fill="#2d3748"/>
    </g>
    <text x="100" y="108" text-anchor="middle" fill="${theme.primary}" font-size="11" font-family="system-ui,sans-serif" opacity="0.8">${brand} ${category}</text>
  </svg>`
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent(svg)
}
</script>

<style scoped>
.ai-page { min-height: 100vh; display: flex; background: #f5f7fa; }

/* ====== 左侧边栏 ====== */
.sidebar { width: 260px; background: #1a1a2e; color: #ccc; display: flex; flex-direction: column; flex-shrink: 0; position: fixed; top: 0; left: 0; bottom: 0; z-index: 200; }
.sidebar-header { padding: 16px; border-bottom: 1px solid rgba(255,255,255,0.08); }
.new-chat-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px; background: rgba(255,255,255,0.08); border: 1px dashed rgba(255,255,255,0.2); border-radius: 10px; color: #fff; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s; }
.new-chat-btn:hover { background: rgba(255,255,255,0.14); border-color: rgba(255,255,255,0.35); }

.sidebar-list { flex: 1; overflow-y: auto; padding: 8px; }
.sidebar-list::-webkit-scrollbar { width: 4px; }
.sidebar-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15); border-radius: 2px; }

.sidebar-item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; border-radius: 10px; cursor: pointer; transition: all 0.15s; margin-bottom: 2px; position: relative; }
.sidebar-item:hover { background: rgba(255,255,255,0.08); }
.sidebar-item.active { background: rgba(102,126,234,0.25); color: #fff; }
.sidebar-item svg { flex-shrink: 0; opacity: 0.6; }
.sidebar-item.active svg { opacity: 1; }
.item-title { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.sidebar-empty { text-align: center; padding: 40px 20px; color: #666; font-size: 13px; }

.sidebar-footer { padding: 16px; border-top: 1px solid rgba(255,255,255,0.08); }
.back-home-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 10px; background: none; border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; color: #999; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.back-home-btn:hover { background: rgba(255,255,255,0.06); color: #fff; border-color: rgba(255,255,255,0.2); }

/* ====== 右侧聊天区 ====== */
.chat-main { flex: 1; display: flex; flex-direction: column; margin-left: 260px; height: 100vh; overflow: hidden; position: relative; }

.chat-header { background: #fff; box-shadow: 0 1px 0 rgba(0,0,0,0.06); padding: 14px 24px; display: flex; align-items: center; gap: 16px; flex-shrink: 0; z-index: 100; }
.back-btn { background: none; border: none; color: #666; cursor: pointer; padding: 8px; border-radius: 8px; transition: all 0.2s; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.back-btn:hover { background: #f0f2f5; color: #333; }
.toggle-sidebar-btn { background: none; border: none; color: #666; cursor: pointer; padding: 8px; border-radius: 8px; transition: all 0.2s; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.toggle-sidebar-btn:hover { background: #f0f2f5; color: #333; }

.chat-title-area { flex: 1; }
.chat-title-area h1 { font-size: 16px; font-weight: 600; color: #1a1a2e; display: flex; align-items: center; gap: 10px; margin: 0; }
.welcome-avatar { margin: 0 auto 24px; }
.welcome-avatar img { width: 96px; height: 96px; border-radius: 24px; object-fit: cover; box-shadow: 0 8px 32px rgba(102,126,234,0.2); }
.ai-disclaimer { font-size: 12px; color: #aaa; margin: 3px 0 0; display: flex; align-items: center; gap: 4px; }


.chat-body { flex: 1; overflow-y: auto; padding: 20px 24px; min-height: 0; }

/* 欢迎区 */
.welcome-section { text-align: center; padding: 60px 0 40px; }
.welcome-section h2 { font-size: 28px; color: #1a1a2e; margin-bottom: 8px; }
.welcome-section p { color: #999; font-size: 15px; margin-bottom: 36px; }

.example-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; max-width: 600px; margin: 0 auto; }
.example-card { display: flex; align-items: center; padding: 14px 18px; background: #fff; border-radius: 10px; cursor: pointer; transition: all 0.2s; border: 1px solid #ebeef5; text-align: left; }
.example-card:hover { border-color: #667eea; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(102,126,234,0.15); }
.ex-text { font-size: 13px; color: #333; line-height: 1.5; }

/* 对话区 */
.messages-area { max-width: 800px; margin: 0 auto; padding-bottom: 20px; }
.message { display: flex; gap: 12px; margin-bottom: 24px; }
.message.user { flex-direction: row-reverse; }

.msg-avatar { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden; }
.ai-msg-avatar { background: none; padding: 2px; }
.user-msg-avatar { background: linear-gradient(135deg, #f56c6c, #e74c3c); }
.ai-avatar-img { width: 32px; height: 32px; border-radius: 6px; object-fit: cover; display: block; }
.user-avatar-img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; display: block; }

.msg-content { max-width: 80%; }
.user-text { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 12px 18px; border-radius: 16px 16px 4px 16px; font-size: 14px; line-height: 1.6; }

.ai-response { background: #fff; border-radius: 16px 16px 16px 4px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.ai-summary { padding: 14px 16px; background: linear-gradient(135deg, #f8f9ff, #f0f2ff); border-left: 3px solid #667eea; border-radius: 0 8px 8px 0; margin-bottom: 16px; }
.ai-summary p { color: #333; font-size: 14px; line-height: 1.7; margin: 0; }

/* 推荐卡片 */
.recommend-cards { display: flex; flex-direction: column; gap: 16px; }
.recommend-card { border: 1px solid #ebeef5; border-radius: 12px; overflow: hidden; transition: all 0.2s; }
.recommend-card:hover { border-color: #667eea; box-shadow: 0 4px 16px rgba(102,126,234,0.12); }

.card-header { display: flex; align-items: center; gap: 12px; padding: 14px 16px; background: #fafbfc; border-bottom: 1px solid #ebeef5; }
.rank { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; color: #fff; flex-shrink: 0; }
.rank-1 { background: linear-gradient(135deg, #f56c6c, #e74c3c); }
.rank-2 { background: linear-gradient(135deg, #e6a23c, #f39c12); }
.rank-3 { background: linear-gradient(135deg, #67c23a, #27ae60); }

.card-title { flex: 1; min-width: 0; }
.card-title h3 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 4px; }
.card-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.tag { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.tag.category { background: #f0f2f5; color: #666; }
.tag.seats { background: #f0f2f5; color: #666; }
.tag.business { background: #ecf5ff; color: #409eff; }
.tag.wedding { background: #fef0f0; color: #f56c6c; }
.tag.family { background: #f0f9eb; color: #67c23a; }
.tag.commute { background: #fdf6ec; color: #e6a23c; }

.match-score { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.score-bar { width: 60px; height: 6px; background: #ebeef5; border-radius: 3px; overflow: hidden; }
.score-fill { height: 100%; background: linear-gradient(90deg, #667eea, #764ba2); border-radius: 3px; transition: width 0.6s; }
.score-text { font-size: 13px; font-weight: 600; color: #667eea; white-space: nowrap; }

.card-body { display: flex; gap: 16px; padding: 16px; }
.car-img-wrap { width: 140px; height: 90px; border-radius: 8px; overflow: hidden; flex-shrink: 0; background: #e0e7ff; }
.car-img { width: 100%; height: 100%; object-fit: cover; }
.card-info { flex: 1; min-width: 0; }
.reason { color: #666; font-size: 13px; line-height: 1.6; margin-bottom: 10px; }
.card-meta { display: flex; gap: 16px; align-items: baseline; margin-bottom: 12px; }
.price { font-size: 18px; font-weight: 700; color: #f56c6c; }
.price small { font-size: 12px; font-weight: normal; color: #999; }
.deposit { font-size: 12px; color: #999; }
.mileage { font-size: 12px; color: #999; }

/* 思考中动画 */
.thinking { display: flex; gap: 6px; padding: 8px 0; }
.thinking .dot { width: 8px; height: 8px; background: #667eea; border-radius: 50%; animation: bounce 1.4s ease-in-out infinite; }
.thinking .dot:nth-child(2) { animation-delay: 0.16s; }
.thinking .dot:nth-child(3) { animation-delay: 0.32s; }
@keyframes bounce { 0%,80%,100% { transform: scale(0); } 40% { transform: scale(1); } }

/* AI文本回复 */
.ai-text-response { max-width: 100%; }
.ai-text { background: #f8f9fb; padding: 14px 18px; border-radius: 0 16px 16px 16px; font-size: 14px; line-height: 1.7; color: #333; word-break: break-word; }
.ai-text strong { color: #667eea; }
.ai-text .car-card-link { display: inline-flex; align-items: center; gap: 8px; padding: 10px 16px; background: linear-gradient(135deg, #f0f2ff, #e8eaff); border: 1px solid #d0d5ff; border-radius: 12px; color: #667eea; text-decoration: none; font-size: 14px; font-weight: 500; margin-top: 10px; transition: all 0.2s; }
.ai-text .car-card-link:hover { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(102,126,234,0.4); }
.ai-text .car-card-icon { font-size: 18px; }
.ai-text .car-card-text { flex: 1; }
.ai-text .car-card-arrow { font-size: 16px; opacity: 0.6; }
/* 内联车辆卡片样式 */
.ai-text .inline-car-card { display: flex; align-items: center; gap: 12px; padding: 10px; background: #fff; border: 1px solid #e8e8e8; border-radius: 10px; margin: 8px 0; cursor: pointer; transition: all 0.2s; max-width: 320px; }
.ai-text .inline-car-card:hover { border-color: #667eea; box-shadow: 0 2px 8px rgba(102,126,234,0.2); }
.ai-text .inline-car-img { width: 100px; height: 70px; object-fit: cover; border-radius: 6px; }
.ai-text .inline-car-info { flex: 1; min-width: 0; }
.ai-text .inline-car-name { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ai-text .inline-car-tags { display: flex; gap: 4px; margin-bottom: 4px; flex-wrap: wrap; }
.ai-text .inline-tag { font-size: 11px; padding: 2px 6px; background: #f0f2ff; color: #667eea; border-radius: 4px; }
.ai-text .inline-tag.business { background: #e8f4fd; color: #1890ff; }
.ai-text .inline-tag.family { background: #f6ffed; color: #52c41a; }
.ai-text .inline-car-price { font-size: 14px; font-weight: 600; color: #f56c6c; }
.ai-text .inline-car-btn { font-size: 11px; color: #667eea; margin-top: 4px; }

/* 打字动画 */
.typing-indicator { display: flex; gap: 4px; padding: 14px 18px; background: #f8f9fb; border-radius: 0 16px 16px 16px; }
.typing-indicator span { width: 8px; height: 8px; background: #b0b0b0; border-radius: 50%; animation: typing 1.4s infinite; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* 输入区 */
.input-bar { background: #fff; border-top: 1px solid #ebeef5; padding: 12px 24px 8px; flex-shrink: 0; position: relative; z-index: 10; }
.input-wrap { display: flex; gap: 12px; max-width: 800px; margin: 0 auto; }
.input-wrap input { flex: 1; padding: 14px 18px; border: 2px solid #e5e7eb; border-radius: 12px; font-size: 14px; outline: none; transition: border-color 0.2s; }
.input-wrap input:focus { border-color: #667eea; }
.input-wrap input:disabled { background: #f5f7fa; }
.send-btn { width: 48px; height: 48px; background: linear-gradient(135deg, #667eea, #764ba2); border: none; border-radius: 12px; color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; flex-shrink: 0; }
.send-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(102,126,234,0.4); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-spinner { width: 20px; height: 20px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.input-hint { text-align: center; font-size: 12px; color: #999; margin-top: 4px; max-width: 800px; margin-left: auto; margin-right: auto; }

/* 回到顶部按钮 */
.scroll-top-btn { position: absolute; bottom: 100px; right: 36px; z-index: 50; width: 40px; height: 40px; border-radius: 50%; background: #fff; border: 1px solid #e5e7eb; box-shadow: 0 4px 16px rgba(0,0,0,0.1); color: #667eea; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.3s; }
.scroll-top-btn:hover { background: #667eea; color: #fff; transform: translateY(-2px); box-shadow: 0 6px 20px rgba(102,126,234,0.4); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: scale(0.8); }

/* 移动端适配 */
.sidebar-overlay { display: none; }
@media (max-width: 768px) {
  .sidebar { transform: translateX(-100%); transition: transform 0.3s ease; }
  .sidebar.open { transform: translateX(0); }
  .ai-page { flex-direction: column; }
  .chat-main { margin-left: 0; }
  .example-cards { grid-template-columns: 1fr; }
  .card-body { flex-direction: column; }
  .car-img-wrap { width: 100%; height: 160px; }
  .sidebar-overlay { display: block; position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 199; }
}

/* 桌面端侧边栏收起/展开 */
@media (min-width: 769px) {
  .sidebar { transition: transform 0.3s ease, width 0.3s ease; }
  .ai-page:not(:has(.sidebar.open)) .sidebar { transform: translateX(-100%); }
  .ai-page:not(:has(.sidebar.open)) .chat-main { margin-left: 0; }
}
</style>
