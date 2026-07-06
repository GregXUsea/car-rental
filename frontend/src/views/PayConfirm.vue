<template>
  <div class="pay-page">
    <div class="pay-header">
      <svg width="32" height="32" viewBox="0 0 48 48" fill="none">
        <defs><linearGradient id="gPayLogo" x1="4" y1="2" x2="44" y2="46"><stop offset="0%" stop-color="#FFD700"/><stop offset="100%" stop-color="#FF8C00"/></linearGradient></defs>
        <path d="M24 3 L7 11 L7 22 C7 31 14.5 39 24 43 C33.5 39 41 31 41 22 L41 11 Z" fill="url(#gPayLogo)"/>
        <path d="M16 23 L21 28 L32 17" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span>御途租车</span>
    </div>

    <div class="pay-card" v-if="!paySuccess">
      <h2>确认支付</h2>
      <div class="pay-amount">¥{{ amount }}</div>

      <div class="form-group">
        <label>请输入验证码</label>
        <input v-model="inputCode" type="text" placeholder="请输入6位验证码" maxlength="6"
               class="code-input" :class="{ error: codeError }" />
        <span class="error-msg" v-if="codeError">{{ codeError }}</span>
      </div>

      <div class="pay-code-hint">
        <p>验证码：<strong>{{ code }}</strong></p>
        <p class="hint-text">请确认您已收到此验证码</p>
      </div>

      <button class="confirm-btn" @click="confirmPay" :disabled="loading">
        {{ loading ? '处理中...' : '确认支付' }}
      </button>
    </div>

    <div class="pay-success-card" v-else>
      <div class="success-icon">✓</div>
      <h2>支付成功</h2>
      <div class="pay-amount">¥{{ amount }}</div>
      <p>您已完成支付，可关闭此页面</p>
      <button class="close-btn" @click="window.close()">关闭页面</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const code = ref('')
const amount = ref('')
const inputCode = ref('')
const codeError = ref('')
const loading = ref(false)
const paySuccess = ref(false)

// 获取API基础URL
const API_BASE = window.location.hostname === 'localhost'
  ? 'http://localhost:8080'
  : `http://${window.location.hostname}:8080`

onMounted(() => {
  code.value = route.query.code || ''
  amount.value = route.query.amount || '0'
})

const confirmPay = async () => {
  if (!inputCode.value) {
    codeError.value = '请输入验证码'
    return
  }
  if (inputCode.value !== code.value) {
    codeError.value = '验证码错误，请重新输入'
    return
  }
  loading.value = true
  try {
    // 调用后端API确认支付
    await axios.post(`${API_BASE}/api/payment/confirm`, { code: code.value })
    paySuccess.value = true
    loading.value = false
  } catch (e) {
    codeError.value = '确认失败，请重试'
    loading.value = false
  }
}
</script>

<style scoped>
.pay-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}
.pay-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 40px;
}
.pay-card, .pay-success-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  text-align: center;
}
.pay-card h2, .pay-success-card h2 {
  font-size: 20px;
  color: #333;
  margin-bottom: 16px;
}
.pay-amount {
  font-size: 36px;
  font-weight: 700;
  color: #333;
  margin-bottom: 24px;
}
.form-group {
  margin-bottom: 20px;
  text-align: left;
}
.form-group label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}
.code-input {
  width: 100%;
  padding: 14px;
  border: 2px solid #ddd;
  border-radius: 10px;
  font-size: 18px;
  text-align: center;
  letter-spacing: 8px;
  outline: none;
  transition: border-color 0.2s;
}
.code-input:focus {
  border-color: #07C160;
}
.code-input.error {
  border-color: #f56c6c;
}
.error-msg {
  color: #f56c6c;
  font-size: 13px;
  margin-top: 8px;
  display: block;
}
.pay-code-hint {
  background: #f0faf4;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 20px;
}
.pay-code-hint p {
  margin: 4px 0;
  font-size: 14px;
  color: #666;
}
.pay-code-hint strong {
  color: #07C160;
  font-size: 16px;
  letter-spacing: 2px;
}
.hint-text {
  font-size: 12px !important;
  color: #999 !important;
}
.confirm-btn {
  width: 100%;
  padding: 14px;
  background: #07C160;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.confirm-btn:hover {
  background: #06ad56;
}
.confirm-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.success-icon {
  width: 80px;
  height: 80px;
  background: #07C160;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #fff;
  margin: 0 auto 20px;
}
.pay-success-card p {
  color: #999;
  margin: 12px 0 24px;
}
.close-btn {
  padding: 10px 24px;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.close-btn:hover {
  background: #eee;
}
</style>
