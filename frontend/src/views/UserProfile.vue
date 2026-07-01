<template>
  <div class="profile-page">
    <header class="header">
      <div class="header-content">
        <button class="back-btn" @click="$router.push('/')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          返回
        </button>
        <h1>个人中心</h1>
        <div></div>
      </div>
    </header>

    <main class="main">
      <!-- 主卡片 -->
      <div class="profile-card">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="triggerUpload">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar" />
            <div v-else class="avatar avatar-text">{{ (userInfo.nickname || userInfo.username || '?')[0] }}</div>
            <div class="avatar-overlay">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2">
                <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
              <span>更换头像</span>
            </div>
          </div>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleAvatarUpload" />
          <h2>{{ userInfo.nickname || userInfo.username }}</h2>
          <span class="role-badge" :class="userInfo.role === 1 ? 'admin' : 'user'">
            {{ userInfo.role === 1 ? '管理员' : '普通用户' }}
          </span>
        </div>

        <!-- 功能菜单 -->
        <div class="menu-list">
          <div class="menu-item" @click="showSection('info')">
            <div class="menu-icon info">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
            </div>
            <div class="menu-content">
              <span class="menu-title">查看账号信息</span>
              <span class="menu-desc">查看您的账号详细信息</span>
            </div>
            <svg class="menu-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </div>

          <div class="menu-item" @click="showSection('edit')">
            <div class="menu-icon edit">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </div>
            <div class="menu-content">
              <span class="menu-title">修改个人信息</span>
              <span class="menu-desc">修改昵称、手机号、邮箱</span>
            </div>
            <svg class="menu-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </div>

          <div class="menu-item" @click="showSection('password')">
            <div class="menu-icon password">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
            </div>
            <div class="menu-content">
              <span class="menu-title">修改登录密码</span>
              <span class="menu-desc">修改密码需要验证旧密码</span>
            </div>
            <svg class="menu-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </div>
        </div>
      </div>

      <!-- 查看账号信息 -->
      <transition name="slide">
        <div class="section-card" v-if="currentSection === 'info'">
          <div class="section-header">
            <h3>账号信息</h3>
            <button class="close-btn" @click="currentSection = ''; infoVerified = false">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">用户名（登录用）</span>
              <span class="value">{{ userInfo.username }}</span>
            </div>
            <div class="info-item">
              <span class="label">昵称</span>
              <span class="value">{{ userInfo.nickname || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="label">手机号</span>
              <span class="value">{{ infoVerified ? (userInfo.phone || '未设置') : maskPhone(userInfo.phone) }}</span>
            </div>
            <div class="info-item">
              <span class="label">邮箱</span>
              <span class="value">{{ infoVerified ? (userInfo.email || '未设置') : maskEmail(userInfo.email) }}</span>
            </div>
            <div class="info-item">
              <span class="label">注册时间</span>
              <span class="value">{{ formatTime(userInfo.createTime) }}</span>
            </div>
          </div>
          <!-- 查看完整信息按钮 -->
          <div v-if="!infoVerified" class="verify-inline">
            <div class="verify-row">
              <input v-model="verifyPassword" type="password" placeholder="输入密码查看完整手机号和邮箱" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @keyup.enter="verifyIdentity('info')" />
              <button class="verify-btn" @click="verifyIdentity('info')" :disabled="!verifyPassword || verifying">
                {{ verifying ? '验证中...' : '查看完整信息' }}
              </button>
            </div>
          </div>
        </div>
      </transition>

      <!-- 修改个人信息 - 需要验证 -->
      <transition name="slide">
        <div class="section-card" v-if="currentSection === 'edit' && !editVerified">
          <div class="section-header">
            <h3>身份验证</h3>
            <button class="close-btn" @click="currentSection = ''">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <p class="verify-tip">为保护您的账号安全，请输入密码进行验证</p>
          <div class="form-group">
            <label>登录密码</label>
            <input v-model="verifyPassword" type="password" placeholder="请输入您的登录密码" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @keyup.enter="verifyIdentity('edit')" />
          </div>
          <button class="submit-btn" @click="verifyIdentity('edit')" :disabled="!verifyPassword || verifying">
            {{ verifying ? '验证中...' : '验证身份' }}
          </button>
        </div>
      </transition>

      <!-- 修改个人信息表单 -->
      <transition name="slide">
        <div class="section-card" v-if="currentSection === 'edit' && editVerified">
          <div class="section-header">
            <h3>修改个人信息</h3>
            <button class="close-btn" @click="resetEdit">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <div class="form-group">
            <label>昵称</label>
            <input v-model="editForm.nickname" placeholder="请输入新昵称" />
          </div>
          <div class="form-group">
            <label>手机号</label>
            <input v-model="editForm.phone" placeholder="请输入手机号" maxlength="11" @input="editForm.phone = editForm.phone.replace(/\s/g, '')" />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input v-model="editForm.email" placeholder="请输入邮箱" @input="editForm.email = editForm.email.replace(/\s/g, '')" />
          </div>
          <button class="submit-btn" @click="handleUpdate" :disabled="saving">
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </transition>

      <!-- 修改密码 - 需要验证旧密码 -->
      <transition name="slide">
        <div class="section-card" v-if="currentSection === 'password' && !pwdVerified">
          <div class="section-header">
            <h3>验证旧密码</h3>
            <button class="close-btn" @click="currentSection = ''">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <p class="verify-tip">请先输入当前密码进行验证</p>
          <div class="form-group">
            <label>当前密码</label>
            <input v-model="pwdForm.oldPassword" type="password" placeholder="请输入当前密码" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @keyup.enter="verifyIdentity('password')" />
          </div>
          <button class="submit-btn" @click="verifyIdentity('password')" :disabled="!pwdForm.oldPassword || verifying">
            {{ verifying ? '验证中...' : '验证密码' }}
          </button>
        </div>
      </transition>

      <!-- 修改密码表单 -->
      <transition name="slide">
        <div class="section-card" v-if="currentSection === 'password' && pwdVerified">
          <div class="section-header">
            <h3>设置新密码</h3>
            <button class="close-btn" @click="resetPwd">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <div class="form-group">
            <label>新密码</label>
            <input v-model="pwdForm.newPassword" type="password" placeholder="至少6位，建议包含大小写和数字" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @input="checkStrength" />
            <div class="strength" v-if="pwdForm.newPassword">
              <div class="bar" :class="[strength.cls, { active: strength.level >= 1 }]"></div>
              <div class="bar" :class="[strength.cls, { active: strength.level >= 2 }]"></div>
              <div class="bar" :class="[strength.cls, { active: strength.level >= 3 }]"></div>
              <span :class="strength.cls">{{ strength.text }}</span>
            </div>
          </div>
          <div class="form-group">
            <label>确认新密码</label>
            <input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" />
          </div>
          <div class="form-hint" v-if="pwdForm.newPassword && pwdForm.confirmPassword && pwdForm.newPassword !== pwdForm.confirmPassword">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            两次密码不一致
          </div>
          <div class="form-hint" v-if="pwdForm.newPassword && pwdForm.newPassword === pwdForm.oldPassword">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            新密码不能与当前密码相同
          </div>
          <button class="submit-btn danger" @click="handleChangePwd" :disabled="!isPwdValid || saving">
            {{ saving ? '修改中...' : '确认修改密码' }}
          </button>
        </div>
      </transition>
    </main>

    <!-- 裁剪弹窗 -->
    <el-dialog v-model="showCropper" title="裁剪头像" width="400px">
      <div class="cropper-area">
        <img ref="cropperImg" :src="previewUrl" />
      </div>
      <template #footer>
        <el-button @click="showCropper = false">取消</el-button>
        <el-button type="primary" @click="confirmCrop">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const userInfo = ref({})
const currentSection = ref('')
const saving = ref(false)
const verifying = ref(false)
const fileInput = ref(null)
const showCropper = ref(false)
const previewUrl = ref('')
const cropperImg = ref(null)

// 身份验证
const verifyPassword = ref('')
const editVerified = ref(false)
const pwdVerified = ref(false)

// 编辑表单
const editForm = reactive({ nickname: '', phone: '', email: '' })

// 密码表单
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const strength = reactive({ level: 0, text: '', cls: '' })

onMounted(async () => {
  const res = await api.get('/user/info')
  if (res.code === 200) {
    userInfo.value = res.data
    editForm.nickname = res.data.nickname
    editForm.phone = res.data.phone
    editForm.email = res.data.email
  }
})

// 显示对应区域
const showSection = (section) => {
  currentSection.value = section
  // 重置验证状态（info不重置，保持已验证状态）
  if (section === 'info') { verifyPassword.value = '' }
  if (section === 'edit') { editVerified.value = false; verifyPassword.value = '' }
  if (section === 'password') { pwdVerified.value = false; pwdForm.oldPassword = '' }
}

// 验证身份
const verifyIdentity = async (type) => {
  const password = type === 'password' ? pwdForm.oldPassword : verifyPassword.value
  if (!password) { ElMessage.warning('请输入密码'); return }

  verifying.value = true
  try {
    // 使用登录接口验证密码
    const res = await api.post('/auth/login', { username: userInfo.value.username, password: password })
    if (res.code === 200) {
      ElMessage.success('验证成功')
      if (type === 'edit') editVerified.value = true
      else if (type === 'info') infoVerified.value = true
      else pwdVerified.value = true
    } else {
      ElMessage.error('密码错误，请重试')
    }
  } finally { verifying.value = false }
}

// 头像上传
const triggerUpload = () => fileInput.value.click()

const handleAvatarUpload = (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.error('请上传图片'); return }
  if (file.size > 5 * 1024 * 1024) { ElMessage.error('图片不能超过5MB'); return }

  const reader = new FileReader()
  reader.onload = (ev) => { previewUrl.value = ev.target.result; showCropper.value = true }
  reader.readAsDataURL(file)
}

const confirmCrop = async () => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  const img = cropperImg.value
  const size = 400
  canvas.width = size; canvas.height = size
  // 居中裁剪为正方形
  const minSide = Math.min(img.naturalWidth, img.naturalHeight)
  const sx = (img.naturalWidth - minSide) / 2
  const sy = (img.naturalHeight - minSide) / 2
  ctx.drawImage(img, sx, sy, minSide, minSide, 0, 0, size, size)
  const compressed = canvas.toDataURL('image/jpeg', 0.92)

  saving.value = true
  try {
    const res = await api.put('/user/update', { avatar: compressed })
    if (res.code === 200) {
      userInfo.value.avatar = compressed
      ElMessage.success('头像更新成功')
      showCropper.value = false
    } else ElMessage.error(res.message)
  } finally { saving.value = false }
}

// 修改信息
const handleUpdate = async () => {
  saving.value = true
  try {
    const res = await api.put('/user/update', editForm)
    if (res.code === 200) {
      ElMessage.success('修改成功')
      userInfo.value = { ...userInfo.value, ...editForm }
      currentSection.value = ''
      editVerified.value = false
    } else ElMessage.error(res.message)
  } finally { saving.value = false }
}

// 密码强度
const checkStrength = () => {
  const p = pwdForm.newPassword
  let s = 0
  if (p.length >= 6) s++
  if (p.length >= 10) s++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) s++
  if (/\d/.test(p)) s++
  if (/[!@#$%^&*]/.test(p)) s++
  if (s <= 2) { strength.level = 1; strength.text = '弱'; strength.cls = 'weak' }
  else if (s <= 3) { strength.level = 2; strength.text = '中'; strength.cls = 'medium' }
  else { strength.level = 3; strength.text = '强'; strength.cls = 'strong' }
}

const isPwdValid = computed(() => {
  return pwdForm.newPassword && pwdForm.confirmPassword &&
    pwdForm.newPassword === pwdForm.confirmPassword &&
    pwdForm.newPassword.length >= 6 && strength.level >= 2 &&
    pwdForm.newPassword !== pwdForm.oldPassword
})

const handleChangePwd = async () => {
  if (!isPwdValid.value) return
  await ElMessageBox.confirm('修改密码后需要重新登录，确定继续？', '提示')
  saving.value = true
  try {
    const res = await api.post('/user/change-password', { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('token')
      setTimeout(() => window.location.href = '/login', 1500)
    } else ElMessage.error(res.message)
  } finally { saving.value = false }
}

const resetEdit = () => { currentSection.value = ''; editVerified.value = false; verifyPassword.value = '' }
const resetPwd = () => { currentSection.value = ''; pwdVerified.value = false; pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = '' }

const maskPhone = (phone) => phone && phone.length >= 7 ? phone.slice(0, 3) + '****' + phone.slice(7) : phone
const maskEmail = (email) => {
  if (!email) return '未设置'
  const [user, domain] = email.split('@')
  if (!domain) return email
  if (user.length <= 2) return user[0] + '***@' + domain
  return user.substring(0, 2) + '***@' + domain
}
const infoVerified = ref(false)
const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : ''
</script>

<style scoped>
.profile-page { min-height: 100vh; background: #f0f2f5; }
.header { background: #fff; box-shadow: 0 1px 0 rgba(0,0,0,0.06); position: sticky; top: 0; z-index: 100; }
.header-content { max-width: 600px; margin: 0 auto; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.header-content h1 { font-size: 16px; }
.back-btn { display: flex; align-items: center; gap: 6px; background: none; border: none; color: #666; font-size: 14px; cursor: pointer; padding: 8px 12px; border-radius: 8px; transition: all 0.2s; }
.back-btn:hover { background: #f5f7fa; }

.main { max-width: 500px; margin: 24px auto; padding: 0 20px; }

.profile-card { background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }

.avatar-section { padding: 40px; text-align: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; }
.avatar-wrapper { position: relative; width: 100px; height: 100px; margin: 0 auto 16px; cursor: pointer; border-radius: 50%; overflow: hidden; border: 4px solid rgba(255,255,255,0.3); }
.avatar { width: 100px; height: 100px; border-radius: 50%; object-fit: cover; }
.avatar-text { background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; font-size: 36px; font-weight: 700; }
.avatar-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.5); display: flex; flex-direction: column; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s; gap: 4px; }
.avatar-wrapper:hover .avatar-overlay { opacity: 1; }
.avatar-section h2 { font-size: 20px; margin-bottom: 8px; }
.role-badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; background: rgba(255,255,255,0.2); }

.menu-list { padding: 12px; }
.menu-item { display: flex; align-items: center; gap: 16px; padding: 16px; border-radius: 12px; cursor: pointer; transition: all 0.2s; }
.menu-item:hover { background: #f5f7fa; }
.menu-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.menu-icon.info { background: #ecf5ff; color: #409eff; }
.menu-icon.edit { background: #f0f9eb; color: #67c23a; }
.menu-icon.password { background: #fdf6ec; color: #e6a23c; }
.menu-content { flex: 1; }
.menu-title { display: block; font-size: 15px; font-weight: 500; color: #333; }
.menu-desc { display: block; font-size: 12px; color: #999; margin-top: 2px; }
.menu-arrow { flex-shrink: 0; }

.section-card { background: #fff; border-radius: 16px; padding: 24px; margin-top: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.section-header h3 { font-size: 18px; font-weight: 600; }
.close-btn { background: none; border: none; padding: 8px; border-radius: 8px; cursor: pointer; color: #999; transition: all 0.2s; }
.close-btn:hover { background: #f5f7fa; color: #333; }

.verify-tip { color: #666; font-size: 14px; margin-bottom: 20px; padding: 12px; background: #f5f7fa; border-radius: 8px; }

.form-group { margin-bottom: 20px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 8px; font-weight: 500; }
.form-group input { width: 100%; padding: 12px 16px; border: 2px solid #e5e7eb; border-radius: 10px; font-size: 14px; transition: all 0.2s; outline: none; box-sizing: border-box; }
.form-group input:focus { border-color: #667eea; }

.form-hint { display: flex; align-items: center; gap: 6px; color: #f56c6c; font-size: 13px; margin-top: -12px; margin-bottom: 16px; }

.strength { display: flex; align-items: center; gap: 6px; margin-top: 8px; }
.bar { width: 40px; height: 4px; border-radius: 2px; background: #eee; }
.bar.active.weak { background: #f56c6c; }
.bar.active.medium { background: #e6a23c; }
.bar.active.strong { background: #67c23a; }
.strength span { font-size: 12px; margin-left: 4px; }
.weak { color: #f56c6c; } .medium { color: #e6a23c; } .strong { color: #67c23a; }

.verify-inline { margin-top: 20px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.verify-row { display: flex; gap: 10px; }
.verify-row input { flex: 1; padding: 10px 14px; border: 2px solid #e5e7eb; border-radius: 8px; font-size: 13px; outline: none; transition: border-color 0.2s; }
.verify-row input:focus { border-color: #667eea; }
.verify-btn { padding: 10px 18px; background: #667eea; color: #fff; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; white-space: nowrap; transition: all 0.2s; }
.verify-btn:hover:not(:disabled) { background: #5a67d8; }
.verify-btn:disabled { background: #c0c4cc; cursor: not-allowed; }

.info-grid { display: flex; flex-direction: column; gap: 16px; }
.info-item { display: flex; justify-content: space-between; padding-bottom: 12px; border-bottom: 1px solid #f5f5f5; }
.info-item:last-child { border-bottom: none; }
.info-item .label { color: #999; font-size: 14px; }
.info-item .value { color: #333; font-size: 14px; font-weight: 500; }

.submit-btn { width: 100%; padding: 14px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.3s; }
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(102,126,234,0.4); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.submit-btn.danger { background: linear-gradient(135deg, #f56c6c, #e74c3c); }

.cropper-area { text-align: center; }
.cropper-area img { max-width: 100%; max-height: 300px; border-radius: 8px; }

.slide-enter-active, .slide-leave-active { transition: all 0.3s ease; }
.slide-enter-from { opacity: 0; transform: translateY(-10px); }
.slide-leave-to { opacity: 0; transform: translateY(10px); }
</style>
