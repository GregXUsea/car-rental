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
            <span class="admin-status" :class="{ online: adminOnline }">
              {{ adminOnline ? '在线' : '离线' }}
            </span>
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
            <div class="msg-meta">
              <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
              <span v-if="msg.senderId === currentUserId" class="read-status">
                {{ msg.isRead ? '已读' : '未读' }}
              </span>
            </div>
          </div>
        </div>
        <!-- 对方正在输入提示 -->
        <div v-if="adminTyping" class="typing-indicator">
          <div class="typing-avatar">管</div>
          <div class="typing-bubble">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <!-- 表情面板 -->
        <div class="emoji-picker" v-if="showEmoji" @click.self="showEmoji = false"
             @mouseenter="onEmojiEnter" @mouseleave="onEmojiLeave">
          <div class="emoji-panel">
            <div class="emoji-category" v-for="(emojis, category) in emojiGroups" :key="category">
              <div class="category-title">{{ category }}</div>
              <div class="emoji-list">
                <span v-for="emoji in emojis" :key="emoji" class="emoji-item" @click="insertEmoji(emoji)">{{ emoji }}</span>
              </div>
            </div>
          </div>
        </div>
        <button class="emoji-btn" @click="showEmoji = !showEmoji">😊</button>
        <input v-model="inputMessage" placeholder="输入消息..."
               @keyup.enter="sendMessage"
               @input="onTyping" />
        <button @click="sendMessage" :disabled="!inputMessage.trim()">发送</button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const messages = ref([])
const currentUserId = ref(null)
const adminId = ref(null)
const inputMessage = ref('')
const messagesContainer = ref(null)
const adminOnline = ref(true)
const adminTyping = ref(false)
const showEmoji = ref(false)
let typingTimer = null
let pollTimer = null

// 表情包分组
const emojiGroups = {
  '常用': ['😊', '😂', '😍', '🥰', '😘', '😎', '🤩', '👍', '👏', '🙏', '❤️', '💕', '🎉', '🎊', '✨', '🔥'],
  '汽车': ['🚗', '🚕', '🚙', '🏎️', '🚓', '🚐', '🛻', '🚚', '🛺', '🚲', '⛽', '🔧', '🛠️', '🏢', '🏠', '🛣️'],
  '表情': ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😉', '😊', '😇', '🙂', '🙃', '😌', '😍', '🥰', '😘'],
  '手势': ['👍', '👎', '👌', '✌️', '🤞', '🫰', '🤟', '🤘', '🤙', '👈', '👉', '👆', '👇', '☝️', '✋', '🤚'],
  '物品': ['💰', '💳', '📱', '🔑', '📋', '📝', '📌', '🏷️', '🎁', '🎈', '🎯', '🏆', '⭐', '💫', '🌈', '☀️']
}

const insertEmoji = (emoji) => {
  inputMessage.value += emoji
  // 点击后延迟隐藏
  setTimeout(() => { showEmoji.value = false }, 200)
}

// 表情面板鼠标离开自动隐藏
let emojiHideTimer = null
const onEmojiEnter = () => {
  if (emojiHideTimer) clearTimeout(emojiHideTimer)
}
const onEmojiLeave = () => {
  emojiHideTimer = setTimeout(() => { showEmoji.value = false }, 200)
}

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

const loadMessages = async () => {
  if (!adminId.value) return
  const res = await api.get(`/messages/conversation/${adminId.value}`)
  if (res.code === 200) {
    messages.value = res.data
    await nextTick()
    scrollToBottom()
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !adminId.value) return

  const res = await api.post('/messages/send', {
    receiverId: adminId.value,
    content: inputMessage.value.trim()
  })

  if (res.code === 200) {
    inputMessage.value = ''
    adminTyping.value = false
    loadMessages()
  }
}

// 用户输入时发送"正在输入"状态
const onTyping = async () => {
  if (!adminId.value || !inputMessage.value.trim()) return

  // 发送输入状态
  api.post('/messages/typing', { receiverId: adminId.value }).catch(() => {})
}

// 轮询：刷新消息 + 检查管理员输入状态 + 检查在线状态
const startPolling = () => {
  // 发送心跳（每10秒）
  const heartbeatTimer = setInterval(() => {
    api.post('/messages/heartbeat').catch(() => {})
  }, 10000)

  pollTimer = setInterval(async () => {
    await loadMessages()
    if (adminId.value) {
      // 检查管理员是否在输入
      const typingRes = await api.get(`/messages/typing-status/${adminId.value}`)
      if (typingRes.code === 200) {
        adminTyping.value = typingRes.data
      }
      // 检查管理员是否在线
      const onlineRes = await api.get(`/messages/online-status/${adminId.value}`)
      if (onlineRes.code === 200) {
        adminOnline.value = onlineRes.data
      }
    }
  }, 3000) // 每3秒轮询
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200) {
    currentUserId.value = userRes.data.id
  }

  // 获取管理员ID
  const adminRes = await api.get('/messages/admin-id')
  if (adminRes.code === 200) {
    adminId.value = adminRes.data
  }

  await loadMessages()
  startPolling()
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (typingTimer) clearTimeout(typingTimer)
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
.msg-meta { display: flex; align-items: center; gap: 8px; margin-top: 4px; }
.read-status { font-size: 11px; }
.read-status { color: #ccc; }
.message.mine .read-status { color: rgba(255,255,255,0.6); }

/* 正在输入动画 */
.typing-indicator { display: flex; align-items: center; gap: 8px; }
.typing-avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.typing-bubble { background: #f5f5f5; padding: 12px 16px; border-radius: 12px; display: flex; gap: 4px; }
.typing-bubble span { width: 6px; height: 6px; background: #999; border-radius: 50%; animation: typing 1.4s infinite; }
.typing-bubble span:nth-child(2) { animation-delay: 0.2s; }
.typing-bubble span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.chat-input { display: flex; gap: 12px; padding: 16px 20px; border-top: 1px solid #eee; position: relative; align-items: center; }
.chat-input input { flex: 1; padding: 10px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; outline: none; }
.chat-input input:focus { border-color: #667eea; }
.chat-input button { padding: 10px 24px; background: #667eea; color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; }
.chat-input button:hover { background: #5a6fd6; }
.chat-input button:disabled { background: #ccc; cursor: not-allowed; }
.emoji-btn { width: 40px; height: 40px; padding: 0 !important; font-size: 20px; background: transparent !important; border: 1px solid #ddd !important; border-radius: 8px !important; }
.emoji-btn:hover { background: #f5f5f5 !important; }

/* 表情面板 */
.emoji-picker { position: fixed; bottom: 80px; left: 50%; transform: translateX(-50%); z-index: 100; }
.emoji-panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); padding: 16px; width: 350px; max-height: 300px; overflow-y: auto; }
.category-title { font-size: 12px; color: #999; margin-bottom: 8px; font-weight: 500; }
.emoji-list { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 12px; }
.emoji-item { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; font-size: 22px; cursor: pointer; border-radius: 6px; transition: background 0.15s; }
.emoji-item:hover { background: #f0f2ff; }
</style>
