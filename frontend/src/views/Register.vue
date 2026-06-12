<template>
  <div class="register-container">
    <div class="register-card">
      <h2>注册账号</h2>
      <p class="subtitle">创建您的汽车租赁账号</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="2-20位字符" prefix-icon="User" size="large" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-20位字符" prefix-icon="Lock" size="large" show-password @input="checkPasswordStrength" />
        </el-form-item>

        <!-- 密码强度显示 -->
        <div class="password-strength" v-if="form.password">
          <div class="strength-bars">
            <div class="bar" :class="{ active: passwordStrength.level >= 1, weak: passwordStrength.level === 1, medium: passwordStrength.level === 2, strong: passwordStrength.level === 3 }"></div>
            <div class="bar" :class="{ active: passwordStrength.level >= 2, medium: passwordStrength.level === 2, strong: passwordStrength.level === 3 }"></div>
            <div class="bar" :class="{ active: passwordStrength.level >= 3, strong: passwordStrength.level === 3 }"></div>
          </div>
          <span class="strength-text" :class="passwordStrength.colorClass">{{ passwordStrength.text }}</span>
        </div>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>

        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" size="large" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入11位手机号" size="large" maxlength="11" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱地址" size="large" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" @click="handleRegister" :loading="loading" :disabled="passwordStrength.level === 1">注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="links">
        <router-link to="/login">已有账号？去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
})

// 密码强度检测
const passwordStrength = reactive({
  level: 0,
  text: '',
  colorClass: ''
})

const checkPasswordStrength = () => {
  const pwd = form.password
  let score = 0

  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(pwd)) score++

  if (score <= 2) {
    passwordStrength.level = 1
    passwordStrength.text = '弱'
    passwordStrength.colorClass = 'weak'
  } else if (score <= 3) {
    passwordStrength.level = 2
    passwordStrength.text = '中'
    passwordStrength.colorClass = 'medium'
  } else {
    passwordStrength.level = 3
    passwordStrength.text = '强'
    passwordStrength.colorClass = 'strong'
  }
}

// 自定义校验：确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 自定义校验：密码强度
const validatePasswordStrength = (rule, value, callback) => {
  if (passwordStrength.level === 1) {
    callback(new Error('密码强度太弱，请使用更复杂的密码'))
  } else {
    callback()
  }
}

// 自定义校验：手机号
const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的11位手机号'))
  } else {
    callback()
  }
}

// 自定义校验：邮箱
const validateEmail = (rule, value, callback) => {
  if (value && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) {
    callback(new Error('请输入正确的邮箱地址'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度2-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度6-20位', trigger: 'blur' },
    { validator: validatePasswordStrength, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await api.post('/auth/register', {
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      phone: form.phone,
      email: form.email
    })
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.register-card h2 {
  text-align: center;
  margin-bottom: 8px;
  color: #333;
  font-size: 28px;
}
.subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 30px;
}
.links {
  text-align: center;
  margin-top: 10px;
}
.links a {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
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
.strength-bars {
  display: flex;
  gap: 4px;
}
.bar {
  width: 60px;
  height: 6px;
  border-radius: 3px;
  background: #e0e0e0;
  transition: all 0.3s;
}
.bar.active.weak { background: #f56c6c; }
.bar.active.medium { background: #e6a23c; }
.bar.active.strong { background: #67c23a; }
.strength-text {
  font-size: 13px;
  font-weight: 500;
}
.strength-text.weak { color: #f56c6c; }
.strength-text.medium { color: #e6a23c; }
.strength-text.strong { color: #67c23a; }
</style>
