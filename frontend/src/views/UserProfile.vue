<template>
  <div class="profile-page">
    <header class="header">
      <div class="header-content">
        <el-button text @click="$router.push('/')"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <h1>个人中心</h1>
        <div></div>
      </div>
    </header>

    <main class="main">
      <div class="profile-card">
        <div class="avatar-section">
          <el-avatar :size="100" :src="userInfo.avatar">{{ (userInfo.nickname || '')[0] }}</el-avatar>
          <h2>{{ userInfo.nickname || userInfo.username }}</h2>
          <p>{{ userInfo.role === 1 ? '管理员' : '普通用户' }}</p>
        </div>

        <el-divider />

        <el-tabs v-model="activeTab">
          <el-tab-pane label="账号信息" name="info">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
              <el-descriptions-item label="昵称">{{ userInfo.nickname }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ userInfo.phone || '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ userInfo.email || '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ formatTime(userInfo.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="修改信息" name="edit">
            <el-form :model="editForm" label-width="80px" style="max-width: 500px;">
              <el-form-item label="昵称">
                <el-input v-model="editForm.nickname" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="editForm.phone" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="editForm.email" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleUpdate">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-width="100px" style="max-width: 500px;">
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" @input="checkPasswordStrength" />
              </el-form-item>

              <!-- 密码强度显示 -->
              <div class="password-strength" v-if="pwdForm.newPassword">
                <div class="strength-bars">
                  <div class="bar" :class="{ active: passwordStrength.level >= 1, weak: passwordStrength.level === 1, medium: passwordStrength.level === 2, strong: passwordStrength.level === 3 }"></div>
                  <div class="bar" :class="{ active: passwordStrength.level >= 2, medium: passwordStrength.level === 2, strong: passwordStrength.level === 3 }"></div>
                  <div class="bar" :class="{ active: passwordStrength.level >= 3, strong: passwordStrength.level === 3 }"></div>
                </div>
                <span class="strength-text" :class="passwordStrength.colorClass">{{ passwordStrength.text }}</span>
              </div>

              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleChangePwd" :disabled="passwordStrength.level === 1">修改密码</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const userInfo = ref({})
const activeTab = ref('info')
const pwdRef = ref(null)

const editForm = reactive({ nickname: '', phone: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 密码强度检测
const passwordStrength = reactive({ level: 0, text: '', colorClass: '' })

const checkPasswordStrength = () => {
  const pwd = pwdForm.newPassword
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(pwd)) score++

  if (score <= 2) {
    passwordStrength.level = 1; passwordStrength.text = '弱'; passwordStrength.colorClass = 'weak'
  } else if (score <= 3) {
    passwordStrength.level = 2; passwordStrength.text = '中'; passwordStrength.colorClass = 'medium'
  } else {
    passwordStrength.level = 3; passwordStrength.text = '强'; passwordStrength.colorClass = 'strong'
  }
}

// 确认密码校验
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 密码强度校验
const validatePasswordStrength = (rule, value, callback) => {
  if (passwordStrength.level === 1) {
    callback(new Error('密码强度太弱，请使用更复杂的密码'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '至少6位', trigger: 'blur' },
    { validator: validatePasswordStrength, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(async () => {
  const res = await api.get('/user/info')
  if (res.code === 200) {
    userInfo.value = res.data
    editForm.nickname = res.data.nickname
    editForm.phone = res.data.phone
    editForm.email = res.data.email
  }
})

const handleUpdate = async () => {
  const res = await api.put('/user/update', editForm)
  if (res.code === 200) {
    ElMessage.success('修改成功')
    userInfo.value = { ...userInfo.value, ...editForm }
  } else ElMessage.error(res.message)
}

const handleChangePwd = async () => {
  await pwdRef.value.validate()
  const res = await api.post('/user/change-password', { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
  if (res.code === 200) {
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    passwordStrength.level = 0
  } else {
    ElMessage.error(res.message)
  }
}

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : ''
</script>

<style scoped>
.header { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.header-content { max-width: 900px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.header-content h1 { font-size: 18px; }
.main { max-width: 900px; margin: 20px auto; padding: 0 20px; }
.profile-card { background: #fff; border-radius: 12px; padding: 30px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.avatar-section { text-align: center; }
.avatar-section h2 { margin-top: 12px; font-size: 22px; }
.avatar-section p { color: #999; margin-top: 4px; }

/* 密码强度样式 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  padding-left: 100px;
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
</style>
