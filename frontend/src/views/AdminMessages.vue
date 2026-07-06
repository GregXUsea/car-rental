<template>
  <div class="admin-page">
    <header class="admin-header">
      <div class="header-content">
        <div class="logo">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#667eea" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
          <span>御途管理后台</span>
        </div>
        <nav class="admin-nav">
          <router-link to="/admin" class="nav-item">仪表盘</router-link>
          <router-link to="/admin/orders" class="nav-item">订单管理</router-link>
          <router-link to="/admin/users" class="nav-item">用户管理</router-link>
          <router-link to="/admin/messages" class="nav-item active">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            消息中心
            <span class="unread-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
          </router-link>
          <router-link to="/admin/maintenance" class="nav-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/></svg>
            维护看板
          </router-link>
          <router-link to="/ai-assistant" class="nav-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1a1 1 0 011 1v3a1 1 0 01-1 1h-1v1a2 2 0 01-2 2H5a2 2 0 01-2-2v-1H2a1 1 0 01-1-1v-3a1 1 0 011-1h1a7 7 0 017-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 012-2z"/></svg>
            AI助手
          </router-link>
        </nav>
        <div class="header-right">
          <span class="admin-badge">管理员</span>
          <router-link to="/" class="back-link">返回前台</router-link>
        </div>
      </div>
    </header>

    <main class="admin-main messages-layout">
      <!-- 新建对话弹窗 -->
      <div class="new-chat-modal" v-if="showNewChat" @click.self="showNewChat = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>选择用户</h3>
            <button class="close-btn" @click="showNewChat = false">×</button>
          </div>
          <div class="modal-body">
            <input v-model="searchUser" placeholder="搜索用户名..." class="search-input" />
            <div class="user-list">
              <div v-for="user in filteredUsers" :key="user.id" class="user-item" @click="startNewChat(user.id)">
                <img v-if="user.avatar" :src="user.avatar" class="user-avatar" />
                <div v-else class="user-avatar">{{ (user.nickname || user.username).charAt(0) }}</div>
                <div class="user-info">
                  <span class="user-name">{{ user.nickname || user.username }}</span>
                  <span class="user-orders">{{ user.orderCount || 0 }}个订单</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 左侧对话列表 -->
      <div class="conversation-list">
        <div class="list-header">
          <h3>消息列表</h3>
          <button class="new-chat-btn" @click="showNewChat = true; loadAllUsers()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            新建
          </button>
        </div>
        <div class="list-body">
          <div v-for="conv in conversations" :key="conv.userId"
               :class="['conv-item', { active: currentUserId === conv.userId }]"
               @click="openConversation(conv.userId)">
            <img v-if="conv.userAvatar" :src="conv.userAvatar" class="conv-avatar-img" />
            <div v-else class="conv-avatar">{{ conv.userName.charAt(0) }}</div>
            <div class="conv-info">
              <div class="conv-name">
                {{ conv.userName }}
                <span class="role-tag" v-if="conv.userRole === 1">管理员</span>
              </div>
              <div class="conv-last">{{ conv.latestMessage }}</div>
            </div>
            <div class="conv-meta">
              <span class="conv-time">{{ formatTime(conv.latestTime) }}</span>
              <span class="unread-badge" v-if="conv.unreadCount > 0">{{ conv.unreadCount }}</span>
            </div>
          </div>
          <div class="empty-list" v-if="conversations.length === 0">
            暂无消息记录，点击"新建"开始对话
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-area" v-if="currentUserId">
        <div class="chat-header">
          <span class="chat-name">{{ currentUserName }}</span>
        </div>
        <div class="chat-messages" ref="messagesContainer">
          <div v-for="msg in messages" :key="msg.id"
               :class="['message', { mine: msg.senderId === currentAdminId }]">
            <div v-if="msg.senderId === currentAdminId" class="msg-avatar">管</div>
            <div v-else class="msg-avatar">{{ currentUserName.charAt(0) }}</div>
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
      </div>

      <div class="chat-placeholder" v-else>
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#ddd" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        <p>选择一个对话开始聊天</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../api'

const router = useRouter()
const route = useRoute()
const conversations = ref([])
const messages = ref([])
const currentUserId = ref(null)
const currentUserName = ref('')
const currentAdminId = ref(null)
const inputMessage = ref('')
const messagesContainer = ref(null)
const unreadCount = ref(0)

const loadUnreadCount = async () => {
  const res = await api.get('/messages/unread')
  if (res.code === 200) {
    unreadCount.value = res.data
  }
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

const loadConversations = async () => {
  const res = await api.get('/messages/conversations')
  if (res.code === 200) {
    conversations.value = res.data
  }
}

// 加载所有用户（用于新建对话）
const allUsers = ref([])
const showNewChat = ref(false)
const searchUser = ref('')

const loadAllUsers = async () => {
  const res = await api.get('/admin/users')
  if (res.code === 200) {
    allUsers.value = res.data
  }
}

const filteredUsers = computed(() => {
  if (!searchUser.value) return allUsers.value
  const kw = searchUser.value.toLowerCase()
  return allUsers.value.filter(u =>
    u.username.toLowerCase().includes(kw) ||
    (u.nickname && u.nickname.toLowerCase().includes(kw))
  )
})

const startNewChat = (userId) => {
  showNewChat.value = false
  searchUser.value = ''
  openConversation(userId)
}

const openConversation = async (userId) => {
  currentUserId.value = userId
  const conv = conversations.value.find(c => c.userId === userId)
  currentUserName.value = conv ? conv.userName : ''

  const res = await api.get(`/messages/conversation/${userId}`)
  if (res.code === 200) {
    messages.value = res.data
    await nextTick()
    scrollToBottom()
  }

  // 刷新对话列表以更新未读状态
  loadConversations()
  // 刷新全局未读数
  loadUnreadCount()
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentUserId.value) return

  const res = await api.post('/messages/send', {
    receiverId: currentUserId.value,
    content: inputMessage.value.trim()
  })

  if (res.code === 200) {
    inputMessage.value = ''
    openConversation(currentUserId.value)
  }
}

onMounted(async () => {
  const userRes = await api.get('/user/info')
  if (userRes.code === 200) {
    if (userRes.data.role !== 1) {
      router.push('/')
      return
    }
    currentAdminId.value = userRes.data.id
  }

  loadConversations()
  loadUnreadCount()

  // 如果URL带userId参数，直接打开对话
  if (route.query.userId) {
    openConversation(Number(route.query.userId))
  }

  // 每10秒刷新对话列表和未读数
  setInterval(() => {
    loadConversations()
    loadUnreadCount()
  }, 10000)
})

watch(() => route.query.userId, (newUserId) => {
  if (newUserId) {
    openConversation(Number(newUserId))
  }
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f7fa; }
.admin-header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); position: sticky; top: 0; z-index: 100; }
.header-content { max-width: 1400px; margin: 0 auto; padding: 0 24px; height: 60px; display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; color: #333; }
.admin-nav { display: flex; gap: 4px; flex: 1; }
.nav-item { padding: 8px 16px; border-radius: 8px; font-size: 14px; color: #666; text-decoration: none; transition: all 0.2s; }
.nav-item:hover { background: #f0f2ff; color: #667eea; }
.nav-item.active { background: #667eea; color: #fff; }
.header-right { display: flex; align-items: center; gap: 12px; }
.admin-badge { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.back-link { font-size: 13px; color: #999; text-decoration: none; }
.back-link:hover { color: #667eea; }

.messages-layout { display: flex; gap: 0; height: calc(100vh - 60px); padding: 0 !important; }

.conversation-list { width: 300px; background: #fff; border-right: 1px solid #eee; display: flex; flex-direction: column; }
.list-header { padding: 16px 20px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; }
.list-header h3 { font-size: 16px; color: #333; margin: 0; }
.list-body { flex: 1; overflow-y: auto; }

/* 新建对话弹窗 */
.new-chat-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal-content { background: #fff; border-radius: 12px; width: 400px; max-height: 500px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #eee; }
.modal-header h3 { font-size: 16px; }
.close-btn { background: none; border: none; font-size: 24px; color: #999; cursor: pointer; }
.modal-body { padding: 16px; }
.search-input { width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; outline: none; margin-bottom: 12px; }
.search-input:focus { border-color: #667eea; }
.user-list { max-height: 350px; overflow-y: auto; }
.user-item { display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 8px; cursor: pointer; }
.user-item:hover { background: #f5f7fa; }
.user-avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; flex-shrink: 0; }
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: 14px; color: #333; }
.user-orders { font-size: 12px; color: #999; }
.conv-item { display: flex; align-items: center; gap: 12px; padding: 14px 20px; cursor: pointer; transition: background 0.15s; border-bottom: 1px solid #f5f5f5; }
.conv-item:hover { background: #f9f9f9; }
.conv-item.active { background: #f0f2ff; }
.conv-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; flex-shrink: 0; }
.conv-avatar-img { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.conv-info { flex: 1; min-width: 0; }
.conv-name { font-size: 14px; font-weight: 500; color: #333; display: flex; align-items: center; gap: 6px; }
.role-tag { font-size: 10px; background: #e6e6ff; color: #667eea; padding: 1px 6px; border-radius: 4px; }
.conv-last { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 4px; }
.conv-meta { text-align: right; flex-shrink: 0; }
.conv-time { font-size: 11px; color: #ccc; display: block; margin-bottom: 4px; }
.unread-badge { background: #f56c6c; color: #fff; font-size: 10px; padding: 1px 6px; border-radius: 10px; }
.empty-list { text-align: center; color: #999; padding: 40px; }

.chat-area { flex: 1; display: flex; flex-direction: column; background: #f9f9f9; }
.chat-header { padding: 16px 20px; background: #fff; border-bottom: 1px solid #eee; }
.chat-name { font-size: 16px; font-weight: 500; color: #333; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.message { display: flex; gap: 10px; max-width: 70%; }
.message.mine { margin-left: auto; flex-direction: row-reverse; }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; background: #e0e0e0; color: #666; display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.msg-avatar-img { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.message.mine .msg-avatar { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.msg-content { background: #fff; padding: 10px 14px; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.message.mine .msg-content { background: #667eea; color: #fff; }
.msg-text { font-size: 14px; line-height: 1.5; }
.msg-time { font-size: 11px; color: #ccc; margin-top: 4px; }
.message.mine .msg-time { color: rgba(255,255,255,0.7); }
.chat-input { display: flex; gap: 12px; padding: 16px 20px; background: #fff; border-top: 1px solid #eee; }
.chat-input input { flex: 1; padding: 10px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; outline: none; }
.chat-input input:focus { border-color: #667eea; }
.chat-input button { padding: 10px 24px; background: #667eea; color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; }
.chat-input button:hover { background: #5a6fd6; }
.chat-input button:disabled { background: #ccc; cursor: not-allowed; }
.chat-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #ccc; }
.chat-placeholder p { margin-top: 16px; }
</style>
