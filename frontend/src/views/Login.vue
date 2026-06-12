<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo-section">
        <div class="logo-icon">🚗</div>
        <h2>汽车租赁系统</h2>
        <p class="subtitle">欢迎回来，请登录您的账号</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" @click="handleLogin" :loading="loading">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="links">
        <router-link to="/register">注册账号</router-link>
        <a href="#" @click.prevent="showReset = true">忘记密码？</a>
      </div>
    </div>

    <!-- 找回密码弹窗 -->
    <el-dialog v-model="showReset" title="找回密码" width="460px" @open="resetDialogOpen" @close="resetDialogClose">
      <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="resetForm.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="注册邮箱" prop="email">
          <el-input v-model="resetForm.email" placeholder="请输入注册时的邮箱" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-input-row">
            <el-input v-model="resetForm.code" placeholder="请输入6位验证码" maxlength="6" style="flex:1" />
            <el-button type="primary" :disabled="codeCountdown > 0" @click="handleSendCode" style="min-width:120px;margin-left:10px">
              {{ codeCountdown > 0 ? codeCountdown + 's 后重发' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码" prefix-icon="Lock" show-password @input="checkResetPasswordStrength" />
        </el-form-item>
        <!-- 密码强度显示 -->
        <div class="password-strength" v-if="resetForm.newPassword">
          <div class="strength-bars">
            <div class="bar" :class="{ active: resetStrength.level >= 1, weak: resetStrength.level === 1, medium: resetStrength.level === 2, strong: resetStrength.level === 3 }"></div>
            <div class="bar" :class="{ active: resetStrength.level >= 2, medium: resetStrength.level === 2, strong: resetStrength.level === 3 }"></div>
            <div class="bar" :class="{ active: resetStrength.level >= 3, strong: resetStrength.level === 3 }"></div>
          </div>
          <span class="strength-text" :class="resetStrength.colorClass">{{ resetStrength.text }}</span>
        </div>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" type="password" placeholder="请再次输入新密码" prefix-icon="Lock" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReset = false">取消</el-button>
        <el-button type="primary" @click="handleReset" :disabled="resetStrength.level === 1">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const formRef = ref(null)
const resetFormRef = ref(null)
const loading = ref(false)
const showReset = ref(false)

const form = reactive({ username: '', password: '' })
const resetForm = reactive({ username: '', email: '', code: '', newPassword: '', confirmPassword: '' })
const codeCountdown = ref(0)
let countdownTimer = null

// 密码强度
const resetStrength = reactive({ level: 0, text: '', colorClass: '' })

const checkResetPasswordStrength = () => {
  const pwd = resetForm.newPassword
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(pwd)) score++

  if (score <= 2) {
    resetStrength.level = 1; resetStrength.text = '弱'; resetStrength.colorClass = 'weak'
  } else if (score <= 3) {
    resetStrength.level = 2; resetStrength.text = '中'; resetStrength.colorClass = 'medium'
  } else {
    resetStrength.level = 3; resetStrength.text = '强'; resetStrength.colorClass = 'strong'
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== resetForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateResetStrength = (rule, value, callback) => {
  if (resetStrength.level === 1) {
    callback(new Error('密码强度太弱'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  if (value && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) {
    callback(new Error('请输入正确的邮箱地址'))
  } else {
    callback()
  }
}

const resetRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '至少6位', trigger: 'blur' },
    { validator: validateResetStrength, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await api.post('/auth/login', form)
    if (res.code === 200) {
      localStorage.setItem('token', res.data)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    loading.value = false
  }
}

// 发送验证码
const handleSendCode = async () => {
  // 先校验用户名和邮箱
  try {
    await Promise.all([
      resetFormRef.value.validateField('username'),
      resetFormRef.value.validateField('email')
    ])
  } catch {
    return
  }
  const res = await api.post('/auth/send-reset-code', {
    username: resetForm.username,
    email: resetForm.email
  })
  if (res.code === 200) {
    ElMessage.success('验证码已发送')
    // 演示版：弹窗显示验证码，生产环境应通过邮件发送
    ElMessageBox.alert('验证码：' + res.data, '演示版 - 验证码', {
      confirmButtonText: '我知道了',
      type: 'info',
      center: true
    })
    // 60秒倒计时
    codeCountdown.value = 60
    countdownTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
  } else {
    ElMessage.error(res.message)
  }
}

const handleReset = async () => {
  await resetFormRef.value.validate()
  const res = await api.post('/auth/reset-password', {
    username: resetForm.username,
    email: resetForm.email,
    code: resetForm.code,
    newPassword: resetForm.newPassword
  })
  if (res.code === 200) {
    ElMessage.success('密码重置成功，请重新登录')
    showReset.value = false
    resetForm.username = ''
    resetForm.email = ''
    resetForm.code = ''
    resetForm.newPassword = ''
    resetForm.confirmPassword = ''
    resetStrength.level = 0
  } else {
    ElMessage.error(res.message)
  }
}

const resetDialogClose = () => {
  resetForm.username = ''
  resetForm.email = ''
  resetForm.code = ''
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  resetStrength.level = 0
  codeCountdown.value = 0
  if (countdownTimer) clearInterval(countdownTimer)
}

const resetDialogOpen = () => {
  resetForm.username = ''
  resetForm.email = ''
  resetForm.code = ''
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  resetStrength.level = 0
  codeCountdown.value = 0
  if (countdownTimer) clearInterval(countdownTimer)
  resetFormRef.value?.clearValidate()
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.logo-section {
  text-align: center;
  margin-bottom: 30px;
}
.logo-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.logo-section h2 {
  margin-bottom: 8px;
  color: #333;
  font-size: 28px;
}
.subtitle {
  color: #999;
  font-size: 14px;
}
.links {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}
.links a {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}
.links a:hover {
  color: #5a6fd6;
}

/* 密码强度样式 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: -10px;
  margin-bottom: 18px;
  padding-left: 2px;
}
.strength-bars { display: flex; gap: 4px; }
.bar { width: 60px; height: 6px; border-radius: 3px; background: #e0e0e0; transition: all 0.3s; }
.bar.active.weak { background: #f56c6c; }
.bar.active.medium { background: #e6a23c; }
.bar.active.strong { background: #67c23a; }
.strength-text { font-size: 13px; font-weight: 500; }
.strength-text.weak { color: #f56c6c; }
.strength-text.medium { color: #e6a23c; }
.strength-text.strong { color: #67c23a; }

.code-input-row { display: flex; align-items: center; }
</style>
