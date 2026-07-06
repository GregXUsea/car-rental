<template>
  <div class="support-page">
    <header class="header">
      <div class="header-content">
        <button class="back-btn" @click="$router.push('/')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          返回首页
        </button>
        <h1>客服中心</h1>
        <div></div>
      </div>
    </header>

    <main class="support-main">
      <div class="chat-header">
        <div class="admin-info">
          <div class="admin-avatar">管</div>
          <div class="admin-detail">
            <span class="admin-name">御途客服</span>
            <span class="admin-status">在线</span>
          </div>
        </div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-chat">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ddd" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <p>有什么可以帮您的？请发送消息</p>
        </div>
        <div v-for="msg in messages" :key="msg.id"
             :class="['message', { mine: msg.senderId === currentUserId }]">
          <div v-if="msg.senderId !== currentUserId" class="msg-avatar">管</div>
          <div v-else class="msg-avatar user">我</div>
          <div class="msg-content">
            <div class="msg-text">{{ msg.content }}</div>
            <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <input v-model="inputMessage" placeholder="输入消息..." @keyup.enter="sendMessage" />
        <button @click="sendMessage" :disabled="!inputMessage.trim()">发送</button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const messages = ref([])
const currentUserId = ref(null)
const inputMessage = ref('')
const messagesContainer = ref(null)

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 获取管理员ID（假设管理员角色为1）
const getAdminId = async () => {
  try {
    const res = await api.get('/admin/users')
    if (res.code === 200 && res.data.length > 0) {
      return null // 需要后端提供获取管理员ID的接口
    }
  } catch (e) {
    return null
  }
  return null
}

const loadMessages = async () => {
  // 这里需要知道管理员的ID，暂时使用固定值或从对话列表获取
  const convRes = await api.get('/messages/conversations')
  if (convRes.code === 200 && convRes.data.length > 0) {
    const adminConv = convRes.data.find(c => c.userRole === 1)
    if (adminConv) {
      const res = await api.get(`/messages/conversation/${adminConv.userId}`)
      if (res.code === 200) {
        messages.value = res.data
        await nextTick()
        scrollToBottom()
      }
    }
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return

  // 获取管理员ID
  const adminRes = await api.get('/messages/admin-id')
  if (adminRes.code !== 200) {
    return
  }
  const adminId = adminRes.data

  const res = await api.post('/messages/send', {
    receiverId: adminId,
    content: inputMessage.value.trim()
  })

  if (res.code === 200) {
    inputMessage.value = ''
    loadMessages()
  }
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200) {
    currentUserId.value = userRes.data.id
  }
  loadMessages()
})
</script>

<style scoped>
.support-page { min-height: 100vh; background: #f5f7fa; }
.header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.header-content { max-width: 800px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.back-btn { display: flex; align-items: center; gap: 6px; background: none; border: none; color: #666; cursor: pointer; font-size: 14px; }
.back-btn:hover { color: #667eea; }
.header-content h1 { font-size: 18px; }

.support-main { max-width: 800px; margin: 20px auto; background: #fff; border-radius: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); display: flex; flex-direction: column; height: calc(100vh - 120px); }
.chat-header { padding: 16px 20px; border-bottom: 1px solid #eee; }
.admin-info { display: flex; align-items: center; gap: 12px; }
.admin-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; }
.admin-name { font-weight: 500; color: #333; }
.admin-status { font-size: 12px; color: #67c23a; margin-left: 8px; }

.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.empty-chat { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #ccc; }
.empty-chat p { margin-top: 12px; }
.message { display: flex; gap: 10px; max-width: 70%; }
.message.mine { margin-left: auto; flex-direction: row-reverse; }
.msg-avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.msg-avatar.user { background: #67c23a; }
.msg-content { background: #f5f5f5; padding: 10px 14px; border-radius: 12px; }
.message.mine .msg-content { background: #667eea; color: #fff; }
.msg-text { font-size: 14px; line-height: 1.5; }
.msg-time { font-size: 11px; color: #ccc; margin-top: 4px; }
.message.mine .msg-time { color: rgba(255,255,255,0.7); }

.chat-input { display: flex; gap: 12px; padding: 16px 20px; border-top: 1px solid #eee; }
.chat-input input { flex: 1; padding: 10px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; outline: none; }
.chat-input input:focus { border-color: #667eea; }
.chat-input button { padding: 10px 24px; background: #667eea; color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; }
.chat-input button:hover { background: #5a6fd6; }
.chat-input button:disabled { background: #ccc; cursor: not-allowed; }
</style>
