<template>
  <div class="login-page">
    <!-- 动态背景 -->
    <div class="animated-bg">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>

    <div class="login-box">
      <!-- 左侧：轮播广告 -->
      <div class="ad-side">
        <el-carousel height="100%" :autoplay="true" interval="5000" indicator-position="bottom" arrow="hover" class="ad-carousel">
          <el-carousel-item v-for="(ad, index) in ads" :key="index">
            <div class="ad-slide" :style="{ background: ad.bg }">
              <!-- 装饰性浮动元素 -->
              <div class="ad-deco">
                <div class="deco-shape shape-1" :style="{ borderColor: ad.iconColor }"></div>
                <div class="deco-shape shape-2" :style="{ background: ad.iconColor }"></div>
                <div class="deco-shape shape-3" :style="{ borderColor: ad.iconColor }"></div>
                <div class="deco-dots">
                  <span v-for="n in 5" :key="n" :style="{ background: ad.iconColor }"></span>
                </div>
              </div>
              <div class="ad-content">
                <div class="ad-icon-box anim-item" :style="{ background: ad.iconBg }">
                  <svg v-if="ad.iconType === 'shield'" width="28" height="28" viewBox="0 0 24 24" fill="none" :stroke="ad.iconColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    <path d="M9 12l2 2 4-4"/>
                  </svg>
                  <svg v-else-if="ad.iconType === 'car'" width="28" height="28" viewBox="0 0 24 24" fill="none" :stroke="ad.iconColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 16H9m10 0h3v-3.15a1 1 0 00-.84-.99L16 11l-2.7-3.6a1 1 0 00-.8-.4H5.24a2 2 0 00-1.8 1.1l-.8 1.63A6 6 0 002 12.42V16h2"/>
                    <circle cx="6.5" cy="16.5" r="2.5"/><circle cx="16.5" cy="16.5" r="2.5"/>
                  </svg>
                  <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" :stroke="ad.iconColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 12 20 22 4 22 4 12"/><rect x="2" y="7" width="20" height="5"/><line x1="12" y1="22" x2="12" y2="7"/><path d="M12 7H7.5a2.5 2.5 0 010-5C11 2 12 7 12 7z"/><path d="M12 7h4.5a2.5 2.5 0 000-5C13 2 12 7 12 7z"/>
                  </svg>
                </div>
                <span class="ad-tag anim-item" :style="{ background: ad.iconBg, color: ad.iconColor }">{{ ad.tag }}</span>
                <h2 class="ad-title anim-item">{{ ad.title }}</h2>
                <p class="ad-subtitle anim-item" :style="{ color: ad.iconColor }">{{ ad.subtitle }}</p>
                <p class="ad-desc anim-item">{{ ad.desc }}</p>
                <div class="ad-features anim-item">
                  <div class="feature" v-for="f in ad.features" :key="f.text">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" :stroke="ad.featureColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    <span>{{ f.text }}</span>
                  </div>
                </div>
              </div>
              <div class="ad-visual anim-item">
                <div class="visual-ring" :style="{ borderColor: ad.iconColor + '30' }"></div>
                <div class="visual-circle" :style="{ background: ad.circleBg, border: '2px solid ' + ad.iconColor }">
                  <span class="visual-num" :style="{ color: ad.numColor }">{{ ad.num }}</span>
                  <span class="visual-unit" :style="{ color: ad.numColor, opacity: 0.8 }">{{ ad.unit }}</span>
                </div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
        <!-- 品牌标识 -->
        <div class="ad-brand">
          <div class="brand-logo">
            <svg width="28" height="28" viewBox="0 0 48 48" fill="none">
              <defs>
                <linearGradient id="gLoginLogo" x1="4" y1="2" x2="44" y2="46"><stop offset="0%" stop-color="#FFD700"/><stop offset="100%" stop-color="#FF8C00"/></linearGradient>
              </defs>
              <path d="M24 3 L7 11 L7 22 C7 31 14.5 39 24 43 C33.5 39 41 31 41 22 L41 11 Z" fill="url(#gLoginLogo)"/>
              <path d="M16 23 L21 28 L32 17" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="brand-text">
            <span class="brand-name">御途租车</span>
            <span class="brand-en">YUTU CAR RENTAL</span>
          </div>
        </div>
      </div>

      <!-- 右侧：登录表单 -->
      <div class="form-side">
        <div class="form-inner">
          <div class="form-header">
            <h2>欢迎回来</h2>
            <p>登录您的御途账号</p>
          </div>

          <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
            <el-form-item prop="username">
              <div class="input-group" :class="{ error: errors.username }">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                <input v-model="form.username" type="text" placeholder="请输入用户名" autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" @blur="validateField('username')" @input="form.username = form.username.replace(/\s/g, '')" />
              </div>
              <span class="error-msg" v-if="errors.username">{{ errors.username }}</span>
            </el-form-item>

            <el-form-item prop="password">
              <div class="input-group" :class="{ error: errors.password }">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="请输入密码" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @blur="validateField('password')" />
                <span class="eye" @click="showPwd = !showPwd">
                  <svg v-if="!showPwd" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </span>
              </div>
              <span class="error-msg" v-if="errors.password">{{ errors.password }}</span>
            </el-form-item>

            <el-form-item>
              <button type="button" class="login-btn" @click="handleLogin" :disabled="!isFormValid || loading">
                <span v-if="!loading">登 录</span>
                <span v-else class="loading"><span class="spinner"></span> 登录中...</span>
              </button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <router-link to="/register" class="link primary">注册账号</router-link>
            <a href="#" class="link" @click.prevent="showReset = true">忘记密码？</a>
          </div>

          <!-- 底部小广告 -->
          <div class="bottom-promo">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
            <span>新用户注册即享首日半价，最高立减200元</span>
          </div>

          <div class="login-lock" v-if="isLocked">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
            <span>登录失败次数过多，请 {{ lockMinutes }}:{{ lockSeconds.toString().padStart(2,'0') }} 后重试</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 找回密码弹窗 -->
    <el-dialog v-model="showReset" title="找回密码" width="420px" :show-close="true" @close="resetDialogState">
      <!-- 步骤条 -->
      <div class="reset-steps">
        <div :class="['step', { active: resetStep >= 1, done: resetStep > 1 }]">
          <span class="step-num">1</span>
          <span class="step-text">身份验证</span>
        </div>
        <div :class="['step-line', { active: resetStep > 1 }]"></div>
        <div :class="['step', { active: resetStep >= 2, done: resetStep > 2 }]">
          <span class="step-num">2</span>
          <span class="step-text">验证码</span>
        </div>
        <div :class="['step-line', { active: resetStep > 2 }]"></div>
        <div :class="['step', { active: resetStep >= 3 }]">
          <span class="step-num">3</span>
          <span class="step-text">重置密码</span>
        </div>
      </div>

      <!-- 第1步：身份验证 -->
      <div v-if="resetStep === 1" class="reset-form">
        <p class="reset-tip">请输入用户名和注册时绑定的邮箱</p>
        <div class="form-group">
          <label>用户名</label>
          <input v-model="resetForm.username" type="text" placeholder="请输入用户名" autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="resetForm.email" type="email" placeholder="请输入注册时绑定的邮箱" autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" @input="resetForm.email = resetForm.email.replace(/\s/g, '')" />
        </div>
        <button class="reset-btn" @click="handleVerifyIdentity" :disabled="!resetForm.username || !resetForm.email || resetLoading">
          {{ resetLoading ? '验证中...' : '下一步' }}
        </button>
      </div>

      <!-- 第2步：输入验证码 -->
      <div v-if="resetStep === 2" class="reset-form">
        <p class="reset-tip">验证码已发送至 <strong>{{ maskEmail(resetForm.email) }}</strong></p>
        <div class="form-group">
          <label>验证码</label>
          <div class="code-row">
            <input v-model="resetForm.code" type="text" placeholder="请输入6位验证码" maxlength="6" autocomplete="off" @input="onCodeInput" />
            <button class="resend-btn" @click="handleSendCode" :disabled="resetCountdown > 0">
              {{ resetCountdown > 0 ? `${resetCountdown}s` : '重新发送' }}
            </button>
          </div>
          <span class="code-ok" v-if="codeVerified === true">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            验证码正确
          </span>
          <span class="error-msg" v-if="codeVerified === false">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            {{ codeError }}
          </span>
        </div>
        <button class="reset-btn" @click="handleVerifyCode" :disabled="codeVerified !== true">
          下一步
        </button>
      </div>

      <!-- 第3步：设置新密码 -->
      <div v-if="resetStep === 3" class="reset-form">
        <p class="reset-tip">请设置新密码（不少于6位，建议包含大小写和数字）</p>
        <div class="form-group">
          <label>新密码</label>
          <input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @input="checkStrength" />
          <div class="pwd-strength" v-if="resetForm.newPassword">
            <div class="bars">
              <div class="bar" :class="[strength.cls, { active: strength.level >= 1 }]"></div>
              <div class="bar" :class="[strength.cls, { active: strength.level >= 2 }]"></div>
              <div class="bar" :class="[strength.cls, { active: strength.level >= 3 }]"></div>
            </div>
            <span :class="strength.cls">{{ strength.text }}</span>
          </div>
        </div>
        <div class="form-group">
          <label>确认新密码</label>
          <input v-model="resetForm.confirmPassword" type="password" placeholder="请再次输入新密码" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" />
        </div>
        <p class="error-text" v-if="resetForm.newPassword && resetForm.confirmPassword && resetForm.newPassword !== resetForm.confirmPassword">
          两次密码不一致
        </p>
        <button class="reset-btn" @click="handleResetByCode"
                :disabled="!resetForm.newPassword || resetForm.newPassword.length < 6 || resetForm.newPassword !== resetForm.confirmPassword || resetLoading">
          {{ resetLoading ? '重置中...' : '确认重置' }}
        </button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const showReset = ref(false)
const showPwd = ref(false)

const loginFailCount = ref(0)
const isLocked = ref(false)
const lockTime = ref(0)
const lockMinutes = computed(() => Math.floor(lockTime.value / 60))
const lockSeconds = computed(() => Math.floor(lockTime.value % 60))
let lockTimer = null

// 从 localStorage 恢复锁定状态
const restoreLock = () => {
  const saved = localStorage.getItem('loginLock')
  if (!saved) return
  try {
    const { count, unlockAt } = JSON.parse(saved)
    const remaining = Math.ceil((unlockAt - Date.now()) / 1000)
    if (remaining > 0) {
      loginFailCount.value = count
      isLocked.value = true
      lockTime.value = remaining
      startLockTimer()
    } else {
      localStorage.removeItem('loginLock')
    }
  } catch { localStorage.removeItem('loginLock') }
}

const startLockTimer = () => {
  if (lockTimer) clearInterval(lockTimer)
  lockTimer = setInterval(() => {
    lockTime.value--
    if (lockTime.value <= 0) {
      isLocked.value = false
      clearInterval(lockTimer)
      lockTimer = null
      localStorage.removeItem('loginLock')
    }
  }, 1000)
}

const saveLock = (count, duration) => {
  localStorage.setItem('loginLock', JSON.stringify({
    count,
    unlockAt: Date.now() + duration * 1000
  }))
}

restoreLock()

const form = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })

// 专业风格广告数据 - 高对比度版本
const ads = [
  {
    tag: '限时优惠',
    title: '新用户首日半价',
    subtitle: '最高立减200元',
    desc: '注册即享专属优惠，首次租车5折起，全车型通用，有效期30天',
    features: [
      { icon: 'check', text: '新用户专属，每人限领一次' },
      { icon: 'check', text: '最高立减200元，全车型可用' },
      { icon: 'check', text: '优惠有效期30天，放心使用' }
    ],
    iconType: 'gift',
    num: '¥200', unit: '新用户立减',
    bg: 'linear-gradient(135deg, #1a1a2e 0%, #2d1b69 50%, #11998e 100%)',
    iconBg: 'rgba(255,215,0,0.2)', iconColor: '#ffd700',
    circleBg: 'rgba(255,215,0,0.15)', numColor: '#ffd700',
    featureColor: '#ffd700'
  },
  {
    tag: '安全保障',
    title: '全车险全覆盖',
    subtitle: '出行无忧',
    desc: '所有车辆均购买全险，交强险、商业险、不计免赔三重保障',
    features: [
      { icon: 'check', text: '交强险全覆盖，合法上路' },
      { icon: 'check', text: '商业险保障，事故无忧' },
      { icon: 'check', text: '不计免赔，全额赔付' }
    ],
    iconType: 'shield',
    num: '100%', unit: '保险覆盖',
    bg: 'linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%)',
    iconBg: 'rgba(46,204,113,0.2)', iconColor: '#2ecc71',
    circleBg: 'rgba(46,204,113,0.15)', numColor: '#2ecc71',
    featureColor: '#2ecc71'
  },
  {
    tag: '车型齐全',
    title: '10+热门车型',
    subtitle: '总有一款适合您',
    desc: '轿车、SUV、MPV、新能源，多种车型满足商务、家庭、婚庆等不同需求',
    features: [
      { icon: 'check', text: '丰田、本田、宝马、奔驰等品牌' },
      { icon: 'check', text: '轿车、SUV、MPV全覆盖' },
      { icon: 'check', text: '新能源车型，环保出行' }
    ],
    iconType: 'car',
    num: '10+', unit: '热门车型',
    bg: 'linear-gradient(135deg, #141e30 0%, #243b55 50%, #667eea 100%)',
    iconBg: 'rgba(102,126,234,0.2)', iconColor: '#667eea',
    circleBg: 'rgba(102,126,234,0.15)', numColor: '#667eea',
    featureColor: '#667eea'
  }
]

// 校验
const validateUsername = (val) => {
  if (!val) return '请输入用户名'
  if (val.length < 2 || val.length > 20) return '长度2-20位'
  return ''
}
const validatePassword = (val) => {
  if (!val) return '请输入密码'
  if (val.length < 6) return '至少6位'
  return ''
}
const validateField = (field) => {
  errors[field] = field === 'username' ? validateUsername(form.username) : validatePassword(form.password)
}
const isFormValid = computed(() => form.username && form.password && !errors.username && !errors.password && !isLocked.value)

// 登录
const handleLogin = async () => {
  validateField('username')
  validateField('password')
  if (!isFormValid.value) return

  loading.value = true
  try {
    const res = await api.post('/auth/login', { username: form.username, password: form.password })
    if (res.code === 200) {
      localStorage.setItem('token', res.data)
      localStorage.removeItem('loginLock')
      loginFailCount.value = 0

      // 获取用户信息
      const userRes = await api.get('/user/info')
      const nickname = userRes.code === 200 ? (userRes.data.nickname || userRes.data.username) : '用户'
      // 新用户=注册30天内且未下过单
      const hasNoOrders = !userRes.data.orderCount || userRes.data.orderCount === 0
      const within30Days = (Date.now() - new Date(userRes.data.createTime).getTime() < 30 * 24 * 60 * 60 * 1000)
      const isNewUser = userRes.code === 200 && within30Days && hasNoOrders

      // 存储欢迎信息到 localStorage（更可靠）
      localStorage.setItem('showWelcome', '1')
      localStorage.setItem('welcomeName', nickname)
      localStorage.setItem('welcomeIsNew', isNewUser ? '1' : '0')

      // 立即跳转到首页
      window.location.href = '/'
    } else {
      loginFailCount.value++
      ElMessage.error(res.message)
      if (loginFailCount.value >= 5) {
        isLocked.value = true
        const lockDuration = (loginFailCount.value - 4) * 20
        lockTime.value = lockDuration
        saveLock(loginFailCount.value, lockDuration)
        startLockTimer()
        ElMessageBox.alert(
          `登录失败次数过多，请等待 ${lockDuration} 秒后重试。多次失败将递增锁定时间。`,
          '账号安全提示',
          { type: 'warning', confirmButtonText: '我知道了' }
        )
      }
    }
  } finally { loading.value = false }
}

// 找回密码 - 3步流程
const resetStep = ref(1)
const resetLoading = ref(false)
const resetCountdown = ref(0)
const resetForm = reactive({ username: '', email: '', code: '', newPassword: '', confirmPassword: '' })
const strength = reactive({ level: 0, text: '', cls: '' })

const resetDialogState = () => {
  resetStep.value = 1
  resetForm.username = ''
  resetForm.email = ''
  resetForm.code = ''
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  resetLoading.value = false
  resetCountdown.value = 0
  codeVerified.value = null
  codeError.value = ''
}

const maskEmail = (email) => {
  if (!email) return ''
  const [user, domain] = email.split('@')
  if (user.length <= 2) return user[0] + '***@' + domain
  return user.substring(0, 2) + '***@' + domain
}

const checkStrength = () => {
  const p = resetForm.newPassword
  let s = 0
  if (p.length >= 6) s++; if (p.length >= 10) s++; if (/[a-z]/.test(p) && /[A-Z]/.test(p)) s++; if (/\d/.test(p)) s++; if (/[!@#$%^&*]/.test(p)) s++
  if (s <= 2) { strength.level = 1; strength.text = '弱'; strength.cls = 'weak' }
  else if (s <= 3) { strength.level = 2; strength.text = '中'; strength.cls = 'medium' }
  else { strength.level = 3; strength.text = '强'; strength.cls = 'strong' }
}

const handleVerifyIdentity = async () => {
  resetLoading.value = true
  try {
    const res = await api.post('/auth/verify-identity', { username: resetForm.username, email: resetForm.email })
    if (res.code === 200) {
      await handleSendCode()
    } else {
      ElMessage.error(res.message)
    }
  } catch { ElMessage.error('验证失败') }
  finally { resetLoading.value = false }
}

const handleSendCode = async () => {
  try {
    const res = await api.post('/auth/send-reset-code', { username: resetForm.username, email: resetForm.email })
    if (res.code === 200) {
      ElMessage.success('验证码已发送')
      resetStep.value = 2
      resetCountdown.value = 60
      const t = setInterval(() => { resetCountdown.value--; if (resetCountdown.value <= 0) clearInterval(t) }, 1000)
    } else {
      ElMessage.error(res.message)
    }
  } catch { ElMessage.error('发送失败') }
}

// 验证码即时校验
const codeVerified = ref(null) // null=未验证, true=正确, false=错误
const codeError = ref('')
let verifyTimer = null

const onCodeInput = () => {
  codeVerified.value = null
  codeError.value = ''
  if (verifyTimer) clearTimeout(verifyTimer)
  if (resetForm.code.length === 6) {
    // 延迟500ms自动校验，避免频繁请求
    verifyTimer = setTimeout(() => doVerifyCode(), 500)
  }
}

const doVerifyCode = async () => {
  try {
    const res = await api.post('/auth/verify-code', {
      username: resetForm.username, email: resetForm.email, code: resetForm.code
    })
    if (res.code === 200) {
      codeVerified.value = true
    } else {
      codeVerified.value = false
      codeError.value = res.message || '验证码错误'
    }
  } catch {
    codeVerified.value = false
    codeError.value = '校验失败，请重试'
  }
}

const handleVerifyCode = () => {
  if (codeVerified.value !== true) return
  resetStep.value = 3
}

const handleResetByCode = async () => {
  if (resetForm.newPassword.length < 6) { ElMessage.error('密码长度不能少于6位'); return }
  if (resetForm.newPassword !== resetForm.confirmPassword) { ElMessage.error('两次密码不一致'); return }

  // 密码强度校验
  if (strength.level <= 1) {
    ElMessage.warning('密码强度太弱，请使用大小写字母、数字或特殊字符组合')
    return
  }
  // 中等强度弹窗确认
  if (strength.level === 2) {
    try {
      await ElMessageBox.confirm(
        '当前密码强度为「中」，建议使用大小写字母、数字和特殊字符组合来提高安全性。是否确认修改？',
        '密码强度提示',
        { confirmButtonText: '确认修改', cancelButtonText: '返回修改', type: 'warning' }
      )
    } catch { return }
  }

  resetLoading.value = true
  try {
    const res = await api.post('/auth/reset-password-by-code', {
      username: resetForm.username, email: resetForm.email,
      code: resetForm.code, newPassword: resetForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码重置成功，请重新登录')
      showReset.value = false
    } else {
      ElMessage.error(res.message)
    }
  } catch { ElMessage.error('重置失败') }
  finally { resetLoading.value = false }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden; background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%); }

/* 动态背景 */
.animated-bg { position: absolute; inset: 0; pointer-events: none; }
.gradient-orb { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.4; }
.orb-1 { width: 600px; height: 600px; background: linear-gradient(135deg, #667eea, #764ba2); top: -200px; left: -200px; animation: moveOrb 25s ease-in-out infinite; }
.orb-2 { width: 500px; height: 500px; background: linear-gradient(135deg, #f093fb, #f5576c); bottom: -150px; right: -150px; animation: moveOrb 20s ease-in-out infinite reverse; }
.orb-3 { width: 400px; height: 400px; background: linear-gradient(135deg, #4facfe, #00f2fe); top: 50%; left: 50%; animation: moveOrb 22s ease-in-out infinite; }
@keyframes moveOrb { 0%,100%{transform:translate(0,0)} 25%{transform:translate(60px,-40px)} 50%{transform:translate(-30px,60px)} 75%{transform:translate(40px,30px)} }

.login-box { display: flex; width: 960px; min-height: 560px; background: #fff; border-radius: 24px; overflow: hidden; box-shadow: 0 25px 80px rgba(0,0,0,0.12); z-index: 10; }

/* 左侧广告 */
.ad-side { flex: 1.1; position: relative; overflow: hidden; }
.ad-carousel { height: 100%; }
.ad-carousel :deep(.el-carousel__container) { height: 100%; }
.ad-carousel :deep(.el-carousel__indicator) { width: 24px; height: 4px; border-radius: 2px; background: rgba(255,255,255,0.4); padding: 0; margin: 0 4px; transition: all 0.3s; }
.ad-carousel :deep(.el-carousel__indicator.is-active) { background: #fff; width: 32px; }
.ad-slide { height: 100%; display: flex; flex-direction: column; justify-content: space-between; padding: 48px; color: #fff; position: relative; overflow: hidden; }
.ad-slide::before { content: ''; position: absolute; top: -50%; right: -20%; width: 400px; height: 400px; background: rgba(255,255,255,0.03); border-radius: 50%; }
.ad-slide::after { content: ''; position: absolute; bottom: -30%; left: -10%; width: 300px; height: 300px; background: rgba(255,255,255,0.02); border-radius: 50%; }
.ad-content { flex: 1; position: relative; z-index: 2; }

/* 装饰浮动元素 */
.ad-deco { position: absolute; inset: 0; z-index: 1; pointer-events: none; overflow: hidden; }
.deco-shape { position: absolute; border-radius: 50%; opacity: 0.08; }
.shape-1 { width: 200px; height: 200px; border: 2px solid; top: -40px; right: -30px; animation: floatA 8s ease-in-out infinite; }
.shape-2 { width: 12px; height: 12px; top: 30%; right: 15%; animation: floatB 6s ease-in-out infinite 1s; }
.shape-3 { width: 80px; height: 80px; border: 1.5px solid; bottom: 20%; left: 10%; animation: floatA 10s ease-in-out infinite 2s; border-radius: 30%; }
.deco-dots { position: absolute; top: 15%; right: 8%; display: flex; gap: 6px; }
.deco-dots span { width: 4px; height: 4px; border-radius: 50%; opacity: 0.2; animation: dotPulse 3s ease-in-out infinite; }
.deco-dots span:nth-child(2) { animation-delay: 0.4s; }
.deco-dots span:nth-child(3) { animation-delay: 0.8s; }
.deco-dots span:nth-child(4) { animation-delay: 1.2s; }
.deco-dots span:nth-child(5) { animation-delay: 1.6s; }
@keyframes floatA { 0%,100%{transform:translate(0,0) rotate(0deg)} 50%{transform:translate(-15px,20px) rotate(180deg)} }
@keyframes floatB { 0%,100%{transform:translate(0,0) scale(1)} 50%{transform:translate(10px,-15px) scale(1.5)} }
@keyframes dotPulse { 0%,100%{opacity:0.15;transform:scale(1)} 50%{opacity:0.5;transform:scale(1.8)} }

/* 内容入场动画 */
@keyframes slideUp { from{opacity:0;transform:translateY(24px)} to{opacity:1;transform:translateY(0)} }
.el-carousel__item--active .anim-item { animation: slideUp 0.6s cubic-bezier(0.22,1,0.36,1) both; }
.el-carousel__item--active .anim-item:nth-child(1) { animation-delay: 0s; }
.el-carousel__item--active .anim-item:nth-child(2) { animation-delay: 0.08s; }
.el-carousel__item--active .anim-item:nth-child(3) { animation-delay: 0.16s; }
.el-carousel__item--active .anim-item:nth-child(4) { animation-delay: 0.24s; }
.el-carousel__item--active .anim-item:nth-child(5) { animation-delay: 0.32s; }
.el-carousel__item--active .anim-item:nth-child(6) { animation-delay: 0.4s; }
.el-carousel__item--active .anim-item:nth-child(7) { animation-delay: 0.48s; }

.ad-icon-box { width: 60px; height: 60px; border-radius: 16px; display: flex; align-items: center; justify-content: center; margin-bottom: 24px; backdrop-filter: blur(10px); transition: transform 0.3s; }
.ad-icon-box:hover { transform: scale(1.08) rotate(-3deg); }
.ad-tag { display: inline-block; padding: 6px 16px; border-radius: 20px; font-size: 12px; font-weight: 600; margin-bottom: 20px; letter-spacing: 0.5px; }
.ad-title { font-size: 34px; font-weight: 800; line-height: 1.2; margin-bottom: 8px; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.ad-subtitle { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.ad-desc { font-size: 14px; opacity: 0.85; line-height: 1.7; margin-bottom: 32px; max-width: 380px; }
.ad-features { display: flex; flex-direction: column; gap: 14px; }
.feature { display: flex; align-items: center; gap: 12px; font-size: 14px; font-weight: 500; }
.feature svg { flex-shrink: 0; opacity: 0.9; }

.ad-visual { display: flex; justify-content: flex-end; align-items: center; padding-right: 20px; position: relative; z-index: 2; }
.visual-ring { position: absolute; width: 160px; height: 160px; border-radius: 50%; border: 1px solid; animation: ringPulse 3s ease-in-out infinite; }
@keyframes ringPulse { 0%,100%{transform:scale(1);opacity:0.3} 50%{transform:scale(1.15);opacity:0.1} }
.visual-circle { width: 130px; height: 130px; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; backdrop-filter: blur(10px); position: relative; z-index: 1; transition: transform 0.3s; }
.visual-circle:hover { transform: scale(1.06); }
.visual-num { font-size: 36px; font-weight: 800; line-height: 1; }
.visual-unit { font-size: 13px; margin-top: 6px; font-weight: 500; }

.ad-brand { position: absolute; bottom: 24px; left: 48px; display: flex; align-items: center; gap: 12px; z-index: 10; }
.brand-logo { width: 36px; height: 36px; background: rgba(255,255,255,0.15); border-radius: 10px; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(10px); }
.brand-text { display: flex; flex-direction: column; }
.brand-name { font-size: 14px; font-weight: 600; color: rgba(255,255,255,0.9); }
.brand-en { font-size: 8px; color: rgba(255,255,255,0.6); letter-spacing: 1px; }

/* 右侧表单 */
.form-side { flex: 0.9; display: flex; align-items: center; justify-content: center; background: #fff; }
.form-inner { width: 100%; max-width: 320px; padding: 40px; }
.form-header { text-align: center; margin-bottom: 36px; }
.form-header h2 { font-size: 26px; color: #1a1a2e; margin-bottom: 8px; font-weight: 700; }
.form-header p { color: #999; font-size: 14px; }

.input-group { display: flex; align-items: center; gap: 12px; padding: 14px 18px; background: #f8f9fa; border-radius: 12px; border: 2px solid transparent; transition: all 0.25s; }
.input-group:focus-within { border-color: #667eea; background: #fff; box-shadow: 0 0 0 4px rgba(102,126,234,0.1); }
.input-group.error { border-color: #f56c6c; background: #fff5f5; }
.input-group input { border: none; background: transparent; outline: none; font-size: 15px; width: 100%; color: #333; }
.input-group input::placeholder { color: #bbb; }
.input-group input::-ms-reveal { display: none; }
.eye { cursor: pointer; display: flex; padding: 4px; }
.error-msg { display: block; color: #f56c6c; font-size: 12px; margin-top: 8px; padding-left: 4px; }

.login-btn { width: 100%; padding: 16px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; border-radius: 12px; font-size: 16px; font-weight: 600; cursor: pointer; transition: all 0.3s; letter-spacing: 4px; }
.login-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 10px 30px rgba(102,126,234,0.4); }
.login-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.loading { display: flex; align-items: center; gap: 8px; justify-content: center; letter-spacing: 2px; }
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.form-footer { display: flex; justify-content: space-between; margin-top: 20px; }
.link { color: #667eea; font-size: 13px; text-decoration: none; transition: color 0.2s; }
.link:hover { color: #5a6fd6; }
.link.primary { font-weight: 600; }

.bottom-promo { display: flex; align-items: center; gap: 8px; margin-top: 24px; padding: 12px 16px; background: linear-gradient(135deg, #fff5f5, #fff0f0); border-radius: 10px; border: 1px solid #ffe0e0; }
.bottom-promo span { font-size: 12px; color: #f56c6c; }

.login-lock { display: flex; align-items: center; gap: 6px; margin-top: 16px; padding: 12px 16px; background: #fff5f5; border-radius: 10px; color: #f56c6c; font-size: 12px; }

.captcha-row { display: flex; gap: 12px; }
.captcha-row .el-input { flex: 1; }
.pwd-strength { display: flex; align-items: center; gap: 8px; margin: -10px 0 16px; }
.bars { display: flex; gap: 4px; }
.bar { width: 50px; height: 4px; border-radius: 2px; background: #eee; }
.bar.active.weak { background: #f56c6c; }
.bar.active.medium { background: #e6a23c; }
.bar.active.strong { background: #67c23a; }
.pwd-strength span { font-size: 12px; font-weight: 500; }
.weak { color: #f56c6c; } .medium { color: #e6a23c; } .strong { color: #67c23a; }

/* 找回密码弹窗 */
.reset-steps { display: flex; align-items: center; justify-content: center; margin-bottom: 28px; gap: 0; }
.step { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.step-num { width: 28px; height: 28px; border-radius: 50%; background: #e5e7eb; color: #999; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; transition: all 0.3s; }
.step.active .step-num { background: #667eea; color: #fff; }
.step.done .step-num { background: #67c23a; color: #fff; }
.step-text { font-size: 12px; color: #999; }
.step.active .step-text { color: #667eea; font-weight: 600; }
.step-line { width: 60px; height: 2px; background: #e5e7eb; margin: 0 8px; margin-bottom: 20px; transition: background 0.3s; }
.step-line.active { background: #67c23a; }

.reset-form { display: flex; flex-direction: column; gap: 16px; }
.reset-tip { font-size: 13px; color: #666; margin: 0; text-align: center; }
.reset-tip strong { color: #667eea; }
.reset-form .form-group { display: flex; flex-direction: column; gap: 6px; }
.reset-form .form-group label { font-size: 13px; color: #666; font-weight: 500; }
.reset-form .form-group input { padding: 12px 16px; border: 2px solid #e5e7eb; border-radius: 10px; font-size: 14px; outline: none; transition: border-color 0.2s; background: #f8f9fa; }
.reset-form .form-group input:focus { border-color: #667eea; background: #fff; }
.code-row { display: flex; gap: 10px; }
.code-row input { flex: 1; }
.resend-btn { padding: 0 16px; background: #667eea; color: #fff; border: none; border-radius: 10px; font-size: 13px; cursor: pointer; white-space: nowrap; transition: all 0.2s; }
.resend-btn:hover:not(:disabled) { background: #5a67d8; }
.resend-btn:disabled { background: #c0c4cc; cursor: not-allowed; }
.reset-btn { padding: 12px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.3s; margin-top: 4px; }
.reset-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(102,126,234,0.4); }
.reset-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.error-text { font-size: 12px; color: #f56c6c; margin: -8px 0 0; }
.code-ok { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #67c23a; margin-top: 4px; }
</style>
