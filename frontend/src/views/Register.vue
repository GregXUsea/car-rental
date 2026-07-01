<template>
  <div class="register-page">
    <div class="animated-bg">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>

    <div class="register-box">
      <!-- 左侧广告 -->
      <div class="ad-side">
        <el-carousel height="100%" :autoplay="true" interval="5000" indicator-position="bottom" arrow="hover" class="ad-carousel">
          <el-carousel-item v-for="(ad, index) in ads" :key="index">
            <div class="ad-slide" :style="{ background: ad.bg }">
              <div class="ad-content">
                <div class="ad-icon-box" :style="{ background: ad.iconBg }">
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
                <span class="ad-tag" :style="{ background: ad.iconBg, color: ad.iconColor }">{{ ad.tag }}</span>
                <h2 class="ad-title">{{ ad.title }}</h2>
                <p class="ad-subtitle" :style="{ color: ad.iconColor }">{{ ad.subtitle }}</p>
                <p class="ad-desc">{{ ad.desc }}</p>
                <ul class="ad-list">
                  <li v-for="item in ad.list" :key="item">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" :stroke="ad.featureColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    {{ item }}
                  </li>
                </ul>
              </div>
              <div class="ad-visual">
                <div class="visual-circle" :style="{ background: ad.circleBg, border: '2px solid ' + ad.iconColor }">
                  <span class="visual-num" :style="{ color: ad.numColor }">{{ ad.num }}</span>
                  <span class="visual-unit" :style="{ color: ad.numColor, opacity: 0.8 }">{{ ad.unit }}</span>
                </div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
        <div class="ad-brand">
          <div class="brand-logo">
            <svg width="28" height="28" viewBox="0 0 40 40" fill="none">
              <path d="M20 2L4 10v14c0 9 7 17 16 20 9-3 16-11 16-20V10L20 2z" fill="url(#regLogo)"/>
              <path d="M13 20l5 5 9-9" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <defs><linearGradient id="regLogo" x1="4" y1="2" x2="36" y2="36"><stop stop-color="#FFD700"/><stop offset="1" stop-color="#FFA500"/></linearGradient></defs>
            </svg>
          </div>
          <div class="brand-text">
            <span class="brand-name">御途租车</span>
            <span class="brand-en">YUTU CAR RENTAL</span>
          </div>
        </div>
      </div>

      <!-- 右侧表单 -->
      <div class="form-side">
        <div class="form-inner">
          <div class="form-header">
            <h2>注册账号</h2>
            <p>创建御途账号，立享新用户优惠</p>
          </div>

          <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
            <el-form-item prop="username">
              <label class="field-label">用户名（用于登录） <span class="req">*</span></label>
              <div class="input-group" :class="{ error: errors.username, success: !errors.username && form.username }">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                <input v-model="form.username" placeholder="2-20位，中文/字母/数字/下划线" autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" @blur="checkUsernameAvail" @input="errors.username = ''; usernameChecked = false" />
              </div>
              <span class="error-msg" v-if="errors.username">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                {{ errors.username }}
              </span>
              <span class="success-msg" v-else-if="usernameChecked && form.username">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                该用户名可以使用
              </span>
            </el-form-item>

            <el-form-item>
              <label class="field-label">昵称 <span class="req">*</span></label>
              <div class="input-group" :class="{ error: errors.nickname }">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                <input v-model="form.nickname" placeholder="对外展示名称" autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" @blur="validate('nickname')" @input="errors.nickname = ''" />
              </div>
              <span class="error-msg" v-if="errors.nickname">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                {{ errors.nickname }}
              </span>
              <span class="field-hint" v-else>对外展示的名称，可与用户名不同</span>
            </el-form-item>

            <el-form-item prop="password">
              <label class="field-label">密码 <span class="req">*</span></label>
              <div class="input-group" :class="{ error: errors.password }">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="至少6位" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @blur="checkStrength(); validate('password')" @input="checkStrength(); errors.password = ''" />
                <span class="eye" @click="showPwd = !showPwd">
                  <svg v-if="!showPwd" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </span>
              </div>
              <span class="error-msg" v-if="errors.password">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                {{ errors.password }}
              </span>
              <div class="strength" v-if="form.password">
                <div class="bar" :class="[pwdStrength.cls, { active: pwdStrength.level >= 1 }]"></div>
                <div class="bar" :class="[pwdStrength.cls, { active: pwdStrength.level >= 2 }]"></div>
                <div class="bar" :class="[pwdStrength.cls, { active: pwdStrength.level >= 3 }]"></div>
                <span :class="pwdStrength.cls">{{ pwdStrength.text }}</span>
              </div>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <label class="field-label">确认密码 <span class="req">*</span></label>
              <div class="input-group" :class="{ error: errors.confirmPassword }">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/></svg>
                <input v-model="form.confirmPassword" type="password" placeholder="请再次输入" autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @blur="validate('confirmPassword')" @input="errors.confirmPassword = ''" />
              </div>
              <span class="error-msg" v-if="errors.confirmPassword">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                {{ errors.confirmPassword }}
              </span>
            </el-form-item>

            <el-form-item>
              <label class="field-label">手机号 <span class="req">*</span></label>
              <div class="input-group" :class="{ error: errors.phone }">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
                <input v-model="form.phone" placeholder="11位手机号" maxlength="11" autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" @blur="validate('phone')" @input="form.phone = form.phone.replace(/\s/g, ''); errors.phone = ''" />
              </div>
              <span class="error-msg" v-if="errors.phone">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                {{ errors.phone }}
              </span>
            </el-form-item>

            <el-form-item>
              <label class="field-label">邮箱 <span class="req">*</span></label>
              <div class="input-group" :class="{ error: errors.email }">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M22 4L12 13 2 4"/></svg>
                <input v-model="form.email" type="email" placeholder="用于找回密码（必填）" autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" @blur="validate('email')" @input="form.email = form.email.replace(/\s/g, ''); errors.email = ''" />
              </div>
              <span class="error-msg" v-if="errors.email">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                {{ errors.email }}
              </span>
              <span class="field-hint" v-else>邮箱将用于找回密码</span>
            </el-form-item>

            <el-form-item>
              <button type="button" class="register-btn" @click="handleRegister" :disabled="!isFormValid || loading">
                {{ loading ? '注册中...' : '注 册' }}
              </button>
            </el-form-item>
          </el-form>

          <div class="form-link">
            已有账号？<router-link to="/login">去登录</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 图形验证码弹窗 -->
    <el-dialog v-model="captchaDialogVisible" title="安全验证" width="400px" :close-on-click-modal="false" :show-close="false" class="captcha-dialog">
      <div class="captcha-dialog-content">
        <div class="captcha-question">
          请点击下方的 <strong>{{ captchaTarget.label }}</strong>
        </div>
        <div class="captcha-options">
          <div v-for="(opt, i) in captchaOptions" :key="i"
               class="captcha-item" :class="{ wrong: captchaWrong && lastClicked === i, passed: captchaPassed && captchaOptions[i].id === captchaTarget.id }"
               @click="verifyCaptcha(i)">
            <div class="captcha-icon" v-html="opt.svg"></div>
          </div>
        </div>
        <div class="captcha-status">
          <span class="captcha-ok" v-if="captchaPassed">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            验证通过，正在注册...
          </span>
          <span class="error-msg" v-if="captchaWrong">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            选择错误，请重新选择
          </span>
        </div>
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
const showPwd = ref(false)

const form = reactive({ username: '', nickname: '', password: '', confirmPassword: '', phone: '', email: '' })
const errors = reactive({ username: '', nickname: '', password: '', confirmPassword: '', phone: '', email: '' })
const usernameChecked = ref(false)
const pwdStrength = reactive({ level: 0, text: '', cls: '' })

const ads = [
  {
    tag: '限时优惠',
    title: '新用户首日半价',
    subtitle: '最高立减200元',
    desc: '注册即享专属优惠，首次租车5折起，全车型通用，有效期30天',
    list: ['新用户专属，每人限领一次', '最高立减200元，全车型可用', '优惠有效期30天，放心使用'],
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
    list: ['交强险全覆盖，合法上路', '商业险保障，事故无忧', '不计免赔，全额赔付'],
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
    list: ['丰田、本田、宝马、奔驰等品牌', '轿车、SUV、MPV全覆盖', '新能源车型，环保出行'],
    iconType: 'car',
    num: '10+', unit: '热门车型',
    bg: 'linear-gradient(135deg, #141e30 0%, #243b55 50%, #667eea 100%)',
    iconBg: 'rgba(102,126,234,0.2)', iconColor: '#667eea',
    circleBg: 'rgba(102,126,234,0.15)', numColor: '#667eea',
    featureColor: '#667eea'
  }
]

// 图形验证码
const captchaPool = [
  { id: 'car', label: '🚗 汽车', svg: '<svg width="48" height="48" viewBox="0 0 48 48"><path d="M8 30h32v4H8z" fill="#667eea"/><path d="M10 30l4-10h20l4 10" fill="#764ba2" stroke="#5a67d8" stroke-width="1"/><circle cx="14" cy="34" r="3" fill="#333"/><circle cx="34" cy="34" r="3" fill="#333"/><rect x="18" y="22" width="12" height="6" rx="1" fill="#85c1e9"/></svg>' },
  { id: 'shield', label: '🛡️ 盾牌', svg: '<svg width="48" height="48" viewBox="0 0 48 48"><path d="M24 4L8 12v12c0 10 7 18 16 22 9-4 16-12 16-22V12L24 4z" fill="#27ae60" stroke="#229954" stroke-width="1"/><path d="M18 24l4 4 8-8" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>' },
  { id: 'key', label: '🔑 钥匙', svg: '<svg width="48" height="48" viewBox="0 0 48 48"><circle cx="18" cy="18" r="10" fill="#e67e22" stroke="#d35400" stroke-width="1"/><circle cx="18" cy="18" r="4" fill="#fff"/><rect x="26" y="16" width="14" height="4" rx="2" fill="#e67e22"/><rect x="36" y="16" width="4" height="8" rx="1" fill="#e67e22"/></svg>' },
  { id: 'star', label: '⭐ 星星', svg: '<svg width="48" height="48" viewBox="0 0 48 48"><polygon points="24,4 29,18 44,18 32,28 36,42 24,33 12,42 16,28 4,18 19,18" fill="#f39c12" stroke="#e67e22" stroke-width="1"/></svg>' },
  { id: 'heart', label: '❤️ 爱心', svg: '<svg width="48" height="48" viewBox="0 0 48 48"><path d="M24 40s-16-10-16-22c0-6 4-10 8-10 3 0 6 2 8 6 2-4 5-6 8-6 4 0 8 4 8 10 0 12-16 22-16 22z" fill="#e74c3c" stroke="#c0392b" stroke-width="1"/></svg>' },
  { id: 'diamond', label: '💎 钻石', svg: '<svg width="48" height="48" viewBox="0 0 48 48"><polygon points="24,4 40,18 24,44 8,18" fill="#3498db" stroke="#2980b9" stroke-width="1"/><polygon points="24,4 16,18 24,44 32,18" fill="#5dade2" opacity="0.6"/><line x1="8" y1="18" x2="40" y2="18" stroke="#2980b9" stroke-width="1"/></svg>' },
  { id: 'tree', label: '🌲 树木', svg: '<svg width="48" height="48" viewBox="0 0 48 48"><polygon points="24,4 36,20 30,20 38,32 10,32 18,20 12,20" fill="#27ae60" stroke="#229954" stroke-width="1"/><rect x="21" y="32" width="6" height="8" fill="#8B4513"/></svg>' },
  { id: 'sun', label: '☀️ 太阳', svg: '<svg width="48" height="48" viewBox="0 0 48 48"><circle cx="24" cy="24" r="10" fill="#f1c40f" stroke="#f39c12" stroke-width="1"/><g stroke="#f39c12" stroke-width="2" stroke-linecap="round"><line x1="24" y1="4" x2="24" y2="10"/><line x1="24" y1="38" x2="24" y2="44"/><line x1="4" y1="24" x2="10" y2="24"/><line x1="38" y1="24" x2="44" y2="24"/><line x1="10" y1="10" x2="14" y2="14"/><line x1="34" y1="34" x2="38" y2="38"/><line x1="34" y1="14" x2="38" y2="10"/><line x1="10" y1="38" x2="14" y2="34"/></g></svg>' },
]

const captchaDialogVisible = ref(false)
const captchaOptions = ref([])
const captchaTarget = ref({ id: '', label: '' })
const captchaPassed = ref(false)
const captchaWrong = ref(false)
const lastClicked = ref(-1)
let registerAfterCaptcha = false // 标记验证码通过后是否需要提交注册

const generateCaptcha = () => {
  captchaPassed.value = false
  captchaWrong.value = false
  lastClicked.value = -1
  // 从池中随机选3个不同的
  const shuffled = [...captchaPool].sort(() => Math.random() - 0.5)
  captchaOptions.value = shuffled.slice(0, 3)
  // 随机指定一个为目标
  const targetIdx = Math.floor(Math.random() * 3)
  captchaTarget.value = { id: captchaOptions.value[targetIdx].id, label: captchaOptions.value[targetIdx].label }
}

const verifyCaptcha = async (idx) => {
  if (captchaPassed.value) return
  lastClicked.value = idx
  if (captchaOptions.value[idx].id === captchaTarget.value.id) {
    captchaPassed.value = true
    captchaWrong.value = false
    // 验证通过后自动提交注册
    if (registerAfterCaptcha) {
      registerAfterCaptcha = false
      setTimeout(() => doRegister(), 600)
    }
  } else {
    captchaWrong.value = true
    setTimeout(() => generateCaptcha(), 1000)
  }
}

// 初始化验证码
generateCaptcha()

const checkStrength = () => {
  const p = form.password
  let s = 0
  if (p.length >= 6) s++; if (p.length >= 10) s++; if (/[a-z]/.test(p) && /[A-Z]/.test(p)) s++; if (/\d/.test(p)) s++; if (/[!@#$%^&*]/.test(p)) s++
  if (s <= 2) { pwdStrength.level = 1; pwdStrength.text = '弱'; pwdStrength.cls = 'weak' }
  else if (s <= 3) { pwdStrength.level = 2; pwdStrength.text = '中'; pwdStrength.cls = 'medium' }
  else { pwdStrength.level = 3; pwdStrength.text = '强'; pwdStrength.cls = 'strong' }
}

// 用户名失焦：先格式校验，再查重
const checkUsernameAvail = async () => {
  usernameChecked.value = false
  validate('username')
  if (errors.username) return
  if (!form.username) return
  try {
    const res = await api.get('/auth/check-username', { params: { username: form.username } })
    if (res.code !== 200) {
      errors.username = res.message || '该用户名已被使用'
      usernameChecked.value = false
    } else {
      usernameChecked.value = true
    }
  } catch (e) {
    // 接口异常时不阻塞用户
  }
}

const validate = (field) => {
  let v = form[field]
  // 用户名：去首尾空格，并检测是否包含空格
  if (field === 'username') {
    const raw = form[field] || ''
    if (raw !== raw.trim()) { errors.username = '用户名不能包含空格'; return }
    v = raw.trim(); form[field] = v
    if (!v) errors.username = '请输入用户名'
    else if (v.length < 2) errors.username = '用户名至少需要2个字符'
    else if (v.length > 20) errors.username = '用户名不能超过20个字符'
    else if (!/^[一-龥a-zA-Z0-9_]+$/.test(v)) errors.username = '用户名只能包含中文、字母、数字和下划线'
    else errors.username = ''
  }
  if (field === 'nickname') {
    const raw = form[field] || ''
    if (raw !== raw.trim()) { errors.nickname = '昵称不能包含空格'; return }
    v = raw.trim(); form[field] = v
    if (!v) errors.nickname = '请输入昵称'
    else if (v.length < 1 || v.length > 20) errors.nickname = '昵称长度1-20位'
    else errors.nickname = ''
  }
  if (field === 'password') {
    const raw = form[field] || ''
    if (/\s/.test(raw)) { errors.password = '密码不能包含空格'; return }
    v = raw.trim(); form[field] = v
    if (!v) errors.password = '请设置密码'
    else if (v.length < 6) errors.password = '密码长度不能少于6位'
    else if (pwdStrength.level <= 1) errors.password = '密码强度太弱，请包含字母、数字或特殊字符'
    else errors.password = ''
  }
  if (field === 'confirmPassword') {
    if (!v) errors.confirmPassword = '请再次输入密码进行确认'
    else if (v !== form.password) errors.confirmPassword = '两次输入的密码不一致，请重新输入'
    else errors.confirmPassword = ''
  }
  if (field === 'phone') {
    if (!v) errors.phone = '请输入手机号'
    else if (!/^1[3-9]\d{9}$/.test(v)) errors.phone = '请输入正确的11位手机号码'
    else errors.phone = ''
  }
  if (field === 'email') {
    if (!v) errors.email = '邮箱不能为空（用于找回密码）'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) errors.email = '请输入正确的邮箱地址'
    else errors.email = ''
  }
}

const isFormValid = computed(() => form.username && form.nickname && form.password && form.confirmPassword && form.phone && form.email && !errors.username && !errors.nickname && !errors.password && !errors.confirmPassword && !errors.phone && !errors.email)

// 实际注册提交
const doRegister = async () => {
  loading.value = true
  try {
    const res = await api.post('/auth/register', form)
    if (res.code === 200) {
      captchaDialogVisible.value = false
      showRegisterSuccess(form.username)
    } else {
      if (res.message && res.message.includes('已存在')) {
        errors.username = '该用户名已被使用，请更换一个'
      } else {
        ElMessage.error(res.message)
      }
      captchaDialogVisible.value = false
    }
  } catch (e) {
    captchaDialogVisible.value = false
  } finally { loading.value = false }
}

const handleRegister = async () => {
  // 去所有字段首尾空格
  ['username', 'nickname', 'password', 'phone', 'email'].forEach(f => { if (form[f]) form[f] = form[f].trim() })
  Object.keys(form).forEach(f => { if (errors[f] !== undefined) validate(f) })
  if (!isFormValid.value) return

  // 密码强度为"中"时，先弹确认框
  if (pwdStrength.level === 2) {
    try {
      await ElMessageBox.confirm(
        '您的密码强度为「中」，建议使用大小写字母、数字和特殊字符组合来提高安全性。是否继续注册？',
        '密码强度提示',
        { confirmButtonText: '继续注册', cancelButtonText: '返回修改', type: 'warning' }
      )
    } catch {
      return // 用户点了取消
    }
  }

  // 弹出图形验证码
  generateCaptcha()
  captchaPassed.value = false
  registerAfterCaptcha = true
  captchaDialogVisible.value = true
}

// 注册成功浮现式提示
const showRegisterSuccess = (username) => {
  const div = document.createElement('div')
  div.className = 'welcome-popup'
  div.innerHTML = `
    <div class="popup-icon success">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
    </div>
    <h2>注册成功！</h2>
    <p>欢迎加入御途租车，${username}</p>
    <span class="popup-badge">请登录账号使用200元优惠</span>
  `
  document.body.appendChild(div)

  setTimeout(() => {
    div.classList.add('fade-out')
    setTimeout(() => { div.remove(); window.location.href = '/login' }, 2000)
  }, 1000)
}
</script>

<style scoped>
.register-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden; background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%); }

.animated-bg { position: absolute; inset: 0; pointer-events: none; }
.gradient-orb { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.4; }
.orb-1 { width: 500px; height: 500px; background: linear-gradient(135deg, #f093fb, #f5576c); top: -150px; right: -150px; animation: moveOrb 25s ease-in-out infinite; }
.orb-2 { width: 400px; height: 400px; background: linear-gradient(135deg, #4facfe, #00f2fe); bottom: -100px; left: -100px; animation: moveOrb 20s ease-in-out infinite reverse; }
.orb-3 { width: 350px; height: 350px; background: linear-gradient(135deg, #667eea, #764ba2); top: 40%; left: 40%; animation: moveOrb 22s ease-in-out infinite; }
@keyframes moveOrb { 0%,100%{transform:translate(0,0)} 25%{transform:translate(50px,-30px)} 50%{transform:translate(-20px,50px)} 75%{transform:translate(30px,20px)} }

.register-box { display: flex; width: 960px; min-height: 620px; background: #fff; border-radius: 24px; overflow: hidden; box-shadow: 0 25px 80px rgba(0,0,0,0.12); z-index: 10; }

.ad-side { flex: 1.1; position: relative; overflow: hidden; }
.ad-carousel { height: 100%; }
.ad-carousel :deep(.el-carousel__container) { height: 100%; }
.ad-carousel :deep(.el-carousel__indicator) { width: 24px; height: 4px; border-radius: 2px; background: rgba(255,255,255,0.4); padding: 0; margin: 0 4px; }
.ad-carousel :deep(.el-carousel__indicator.is-active) { background: #fff; width: 32px; }
.ad-slide { height: 100%; display: flex; flex-direction: column; justify-content: space-between; padding: 48px; color: #fff; position: relative; overflow: hidden; }
.ad-slide::before { content: ''; position: absolute; top: -50%; right: -20%; width: 400px; height: 400px; background: rgba(255,255,255,0.03); border-radius: 50%; }
.ad-slide::after { content: ''; position: absolute; bottom: -30%; left: -10%; width: 300px; height: 300px; background: rgba(255,255,255,0.02); border-radius: 50%; }
.ad-content { flex: 1; position: relative; z-index: 1; }
.ad-icon-box { width: 60px; height: 60px; border-radius: 16px; display: flex; align-items: center; justify-content: center; margin-bottom: 24px; backdrop-filter: blur(10px); }
.ad-tag { display: inline-block; padding: 6px 16px; border-radius: 20px; font-size: 12px; font-weight: 600; margin-bottom: 20px; letter-spacing: 0.5px; }
.ad-title { font-size: 32px; font-weight: 800; line-height: 1.2; margin-bottom: 8px; text-shadow: 0 2px 10px rgba(0,0,0,0.2); }
.ad-subtitle { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.ad-desc { font-size: 14px; opacity: 0.85; line-height: 1.7; margin-bottom: 28px; max-width: 380px; }
.ad-list { list-style: none; padding: 0; }
.ad-list li { display: flex; align-items: center; gap: 12px; font-size: 14px; margin-bottom: 14px; font-weight: 500; }
.ad-list li svg { flex-shrink: 0; opacity: 0.9; }

.ad-visual { display: flex; justify-content: flex-end; padding-right: 20px; position: relative; z-index: 1; }
.visual-circle { width: 120px; height: 120px; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; backdrop-filter: blur(10px); }
.visual-num { font-size: 32px; font-weight: 800; line-height: 1; }
.visual-unit { font-size: 12px; margin-top: 6px; font-weight: 500; }

.ad-brand { position: absolute; bottom: 24px; left: 48px; display: flex; align-items: center; gap: 12px; z-index: 10; }
.brand-logo { width: 36px; height: 36px; background: rgba(255,255,255,0.15); border-radius: 10px; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(10px); }
.brand-text { display: flex; flex-direction: column; }
.brand-name { font-size: 14px; font-weight: 600; color: rgba(255,255,255,0.9); }
.brand-en { font-size: 8px; color: rgba(255,255,255,0.6); letter-spacing: 1px; }

.form-side { flex: 0.9; display: flex; align-items: center; justify-content: center; background: #fff; overflow-y: auto; }
.form-inner { width: 100%; max-width: 320px; padding: 36px 40px; }
.form-header { text-align: center; margin-bottom: 28px; }
.form-header h2 { font-size: 24px; color: #1a1a2e; margin-bottom: 6px; font-weight: 700; }
.form-header p { color: #999; font-size: 13px; }

.field-label { display: block; font-size: 13px; color: #666; margin-bottom: 6px; font-weight: 500; }
.req { color: #f56c6c; }
.input-group { display: flex; align-items: center; gap: 10px; padding: 12px 16px; background: #f8f9fa; border-radius: 12px; border: 2px solid transparent; transition: all 0.25s; }
.input-group:focus-within { border-color: #667eea; background: #fff; box-shadow: 0 0 0 4px rgba(102,126,234,0.1); }
.input-group.error { border-color: #f56c6c; background: #fff5f5; }
.error-msg { display: flex; align-items: center; gap: 4px; margin-top: 6px; font-size: 12px; color: #f56c6c; line-height: 1.4; }
.success-msg { display: flex; align-items: center; gap: 4px; margin-top: 6px; font-size: 12px; color: #67c23a; line-height: 1.4; }
.field-hint { display: block; margin-top: 6px; font-size: 12px; color: #999; }
.input-group.success { border-color: #67c23a; background: #f0f9eb; }
.input-group input { border: none; background: transparent; outline: none; font-size: 14px; width: 100%; }
.input-group input::placeholder { color: #bbb; }
.input-group input::-ms-reveal { display: none; }
.eye { cursor: pointer; display: flex; padding: 4px; }

.strength { display: flex; align-items: center; gap: 6px; margin-top: 8px; padding: 0 4px; }
.bar { width: 40px; height: 4px; border-radius: 2px; background: #eee; }
.bar.active.weak { background: #f56c6c; }
.bar.active.medium { background: #e6a23c; }
.bar.active.strong { background: #67c23a; }
.strength span { font-size: 12px; font-weight: 500; margin-left: 4px; }
.weak { color: #f56c6c; } .medium { color: #e6a23c; } .strong { color: #67c23a; }

/* 图形验证码弹窗 */
.captcha-dialog-content { text-align: center; padding: 10px 0; }
.captcha-question { font-size: 16px; color: #333; margin-bottom: 20px; }
.captcha-question strong { color: #667eea; font-size: 18px; }
.captcha-options { display: flex; gap: 16px; justify-content: center; margin-bottom: 16px; }
.captcha-item { width: 88px; height: 88px; border: 2px solid #e5e7eb; border-radius: 16px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; background: #f8f9fa; }
.captcha-item:hover { border-color: #667eea; background: #f0f2ff; transform: translateY(-3px); box-shadow: 0 6px 16px rgba(102,126,234,0.2); }
.captcha-item.wrong { border-color: #f56c6c; background: #fff5f5; animation: shake 0.4s; }
.captcha-item.passed { border-color: #67c23a; background: #f0f9eb; }
.captcha-status { min-height: 24px; }
.captcha-ok { display: flex; align-items: center; gap: 4px; font-size: 14px; color: #67c23a; justify-content: center; font-weight: 600; }
@keyframes shake { 0%,100%{transform:translateX(0)} 25%{transform:translateX(-6px)} 75%{transform:translateX(6px)} }

.register-btn { width: 100%; padding: 14px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; border-radius: 12px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.3s; letter-spacing: 4px; }
.register-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 10px 30px rgba(102,126,234,0.4); }
.register-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.form-link { text-align: center; margin-top: 20px; font-size: 13px; color: #999; }
.form-link a { color: #667eea; text-decoration: none; font-weight: 600; }
</style>

<style>
.welcome-popup {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #fff;
  padding: 40px 50px;
  border-radius: 20px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  z-index: 9999;
  animation: popIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.welcome-popup.fade-out {
  animation: popFade 2s ease forwards;
}
@keyframes popIn { 0% { transform: translate(-50%, -50%) scale(0.6); opacity: 0; } 100% { transform: translate(-50%, -50%) scale(1); opacity: 1; } }
@keyframes popFade { 0% { transform: translate(-50%, -50%) scale(1); opacity: 1; } 100% { transform: translate(-50%, -50%) scale(0.9); opacity: 0; } }

.popup-icon { width: 72px; height: 72px; background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; }
.popup-icon.success { background: linear-gradient(135deg, #67c23a, #5daf34); }
.welcome-popup h2 { font-size: 22px; color: #1a1a2e; margin-bottom: 8px; font-weight: 700; }
.welcome-popup p { font-size: 14px; color: #666; margin-bottom: 12px; }
.popup-badge { display: inline-block; background: linear-gradient(135deg, #f56c6c, #e74c3c); color: #fff; font-size: 13px; font-weight: 600; padding: 8px 16px; border-radius: 20px; }
</style>
