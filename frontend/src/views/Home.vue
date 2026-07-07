<template>
  <div class="home">
    <header class="header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">
            <svg width="38" height="38" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="gLogo" x1="4" y1="2" x2="44" y2="46">
                  <stop offset="0%" stop-color="#FFD700"/>
                  <stop offset="100%" stop-color="#FF8C00"/>
                </linearGradient>
                <linearGradient id="gLogoInner" x1="24" y1="6" x2="24" y2="42">
                  <stop offset="0%" stop-color="#FFF8E1"/>
                  <stop offset="100%" stop-color="#FFB300"/>
                </linearGradient>
              </defs>
              <!-- 盾牌外轮廓（完全对称） -->
              <path d="M24 3 L7 11 L7 22 C7 31 14.5 39 24 43 C33.5 39 41 31 41 22 L41 11 Z" fill="url(#gLogo)" />
              <!-- 盾牌内凹面（光泽层） -->
              <path d="M24 5.5 L9.5 12.5 L9.5 22 C9.5 30 16 37 24 40.5 C32 37 38.5 30 38.5 22 L38.5 12.5 Z" fill="url(#gLogoInner)" opacity="0.15" />
              <!-- 对勾（完全居中对称） -->
              <path d="M16 23 L21 28 L32 17" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
              <!-- 盾牌顶部高光 -->
              <path d="M24 5 L14 10" stroke="#fff" stroke-width="1" opacity="0.4" stroke-linecap="round" />
              <path d="M24 5 L34 10" stroke="#fff" stroke-width="1" opacity="0.4" stroke-linecap="round" />
            </svg>
          </div>
          <div class="logo-text">
            <span class="logo-name">御途租车</span>
            <span class="logo-sub">YUTU CAR RENTAL</span>
          </div>
        </div>
        <nav class="nav-right">
          <a class="nav-link ai-nav" @click.prevent="$router.push('/ai-assistant')">
            <AiRabbit :size="48" />
            <span class="ai-label">AI问答</span>
          </a>
          <a class="nav-link" @click.prevent="$router.push('/orders')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            订单
          </a>
          <a class="nav-link coupon-nav" @click.prevent="$router.push('/coupon')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 12v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6"/><path d="M2 8h20v4H2z"/><path d="M12 2v6"/><path d="M12 2l-3 3"/><path d="M12 2l3 3"/></svg>
            优惠券
          </a>
          <a v-if="isAdmin" class="nav-link" @click.prevent="$router.push('/admin')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
            管理后台
          </a>
          <a v-if="!isAdmin" class="nav-link" @click.prevent="$router.push('/support')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            客服中心
            <span class="msg-badge" v-if="userUnreadCount > 0">{{ userUnreadCount }}</span>
          </a>
          <el-dropdown @command="handleCommand">
            <span class="user-btn">
              <img v-if="userInfo.avatar" :src="userInfo.avatar" class="user-avatar-img" />
              <span v-else class="user-avatar">{{ (userInfo.nickname || userInfo.username || '?')[0] }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </nav>
      </div>
    </header>

    <main class="main">
      <!-- 轮播广告 -->
      <div class="home-carousel">
        <el-carousel height="320px" :autoplay="true" :interval="5000" arrow="hover" indicator-position="outside">
          <el-carousel-item v-for="(ad, index) in homeAds" :key="index">
            <div class="home-ad-slide" :style="{ background: ad.bg }">
              <!-- 装饰元素 -->
              <div class="home-ad-deco">
                <div class="hdeco-ring" :style="{ borderColor: ad.numColor }"></div>
                <div class="hdeco-dot d1" :style="{ background: ad.numColor }"></div>
                <div class="hdeco-dot d2" :style="{ background: ad.numColor }"></div>
                <div class="hdeco-dot d3" :style="{ background: ad.numColor }"></div>
                <div class="hdeco-line" :style="{ background: `linear-gradient(90deg, transparent, ${ad.numColor}20, transparent)` }"></div>
              </div>
              <div class="home-ad-content">
                <span class="home-ad-tag anim-hitem" :style="{ background: ad.tagBg, color: ad.tagColor }">{{ ad.tag }}</span>
                <h2 class="home-ad-title anim-hitem">{{ ad.title }}</h2>
                <p class="home-ad-desc anim-hitem">{{ ad.desc }}</p>
                <div class="home-ad-features anim-hitem">
                  <span v-for="f in ad.features" :key="f" class="home-feature-chip" :style="{ borderColor: ad.numColor + '40', color: ad.numColor }">{{ f }}</span>
                </div>
              </div>
              <div class="home-ad-visual anim-hitem">
                <div class="home-ad-ring" :style="{ borderColor: ad.numColor + '25' }"></div>
                <div class="home-ad-circle" :style="{ background: ad.circleBg, border: `2px solid ${ad.numColor}40` }">
                  <span :style="{ color: ad.numColor }">{{ ad.num }}</span>
                </div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- 订单提醒 -->
      <div class="order-reminder" v-if="pendingOrders.length > 0" @click="$router.push('/orders')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        <span>您有 <strong>{{ pendingOrders.length }}</strong> 个待处理订单，请及时查看</span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </div>

      <!-- 搜索筛选 -->
      <div class="filter-bar">
        <div class="search-box" ref="searchBoxRef" @mouseenter="onSearchBoxEnter" @mouseleave="onSearchBoxLeave">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="keyword" placeholder="搜索品牌、型号..." @focus="showSuggestions = true" @input="onSearchInput" />
          <!-- 搜索建议 -->
          <div class="suggestions" v-if="showSuggestions && filteredBrands.length > 0">
            <div class="sug-title">猜你想选</div>
            <div v-for="b in filteredBrands" :key="b.brand" class="sug-item" @mousedown.prevent="selectBrand(b.brand)">
              <span class="sug-brand">{{ b.brand }}</span>
              <span class="sug-count">{{ b.count }}款</span>
            </div>
          </div>
        </div>
        <div class="filter-tags">
          <button :class="['tag', { active: filterUsage === '' }]" @click="filterUsage = ''">全部</button>
          <button :class="['tag', { active: filterUsage === '商务' }]" @click="filterUsage = '商务'">商务</button>
          <button :class="['tag', { active: filterUsage === '婚庆' }]" @click="filterUsage = '婚庆'">婚庆</button>
        </div>
        <div class="sort-tags">
          <button :class="['tag', { active: sortMode === 'default' }]" @click="sortMode = 'default'">默认</button>
          <button :class="['tag', { active: sortMode === 'available' }]" @click="sortMode = 'available'">只看可租</button>
          <button :class="['tag', { active: sortMode === 'price-asc' }]" @click="sortMode = 'price-asc'">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12l7 7 7-7"/></svg>
            价格低→高
          </button>
          <button :class="['tag', { active: sortMode === 'price-desc' }]" @click="sortMode = 'price-desc'">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19V5M5 12l7-7 7 7"/></svg>
            价格高→低
          </button>
        </div>
      </div>

      <!-- 车辆列表 -->
      <div class="section-title">
        <h2>精选车型</h2>
        <span>共 {{ filteredCars.length }} 辆可用车辆</span>
      </div>

      <div class="car-grid" ref="carGrid">
        <div v-for="(car, index) in filteredCars" :key="car.id" class="car-card" @click="$router.push(`/car/${car.id}`)">
          <div class="car-img-wrap">
            <img :src="car.image" :alt="car.brand + car.model" :data-brand="car.brand" :data-model="car.model" :data-color="car.color" :data-category="car.category" :data-index="index" @error="handleImgError($event)" />
            <div class="img-hover">
              <span class="view-btn">查看详情</span>
            </div>
            <span class="status-tag" :class="'s' + car.status">{{ statusText(car.status) }}</span>
            <div class="usage-tags" v-if="car.usageType">
              <span v-if="car.usageType.includes('商务')" class="tag business">商务</span>
              <span v-if="car.usageType.includes('婚庆')" class="tag wedding">婚庆</span>
            </div>
          </div>
          <div class="car-info">
            <div class="car-header">
              <h3>{{ car.brand }} {{ car.model }}</h3>
              <span class="seats">{{ car.seats }}座</span>
            </div>
            <p class="car-desc">{{ car.description }}</p>
            <div class="car-bottom">
              <div class="price">¥{{ car.pricePerDay }}<small>/天</small></div>
              <span class="deposit">押金 ¥{{ car.deposit }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="filteredCars.length === 0" class="empty">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#ddd" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/></svg>
        <p>暂无符合条件的车辆</p>
      </div>
    </main>

    <footer class="footer">
      <div class="footer-main">
        <div class="footer-brand">
          <div class="footer-logo">
            <svg width="24" height="24" viewBox="0 0 48 48" fill="none">
              <defs><linearGradient id="gFootLogo" x1="4" y1="2" x2="44" y2="46"><stop offset="0%" stop-color="#FFD700"/><stop offset="100%" stop-color="#FF8C00"/></linearGradient></defs>
              <path d="M24 3 L7 11 L7 22 C7 31 14.5 39 24 43 C33.5 39 41 31 41 22 L41 11 Z" fill="url(#gFootLogo)"/>
              <path d="M16 23 L21 28 L32 17" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>御途租车</span>
          </div>
          <p>专业汽车租赁服务<br/>让每一次出行都成为享受</p>
        </div>
        <div class="footer-col">
          <h4>服务项目</h4>
          <a href="#">自驾租车</a>
          <a href="#">带司机租车</a>
          <a href="#">企业用车</a>
          <a href="#">婚庆用车</a>
        </div>
        <div class="footer-col">
          <h4>帮助中心</h4>
          <a href="#">常见问题</a>
          <a href="#">租车流程</a>
          <a href="#">联系我们</a>
        </div>
        <div class="footer-col">
          <h4>联系我们</h4>
          <p>电话：400-888-9999</p>
          <p>地址：重庆市巴南区</p>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2026 御途租车 YUTU CAR RENTAL 版权所有</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import api from '../api'
import AiRabbit from '../components/AiRabbit.vue'

const router = useRouter()
const cars = ref([])
const keyword = ref('')
const filterUsage = ref('')
const sortMode = ref('default') // default, available, price-asc, price-desc
const showSuggestions = ref(false)
const searchBoxRef = ref(null)
let sugHideTimer = null
const pendingOrders = ref([]) // 待处理订单
const userUnreadCount = ref(0) // 用户未读消息数

// 品牌列表（用于搜索建议）
const allBrands = computed(() => {
  const map = {}
  cars.value.forEach(c => {
    if (c.status !== 3) {
      map[c.brand] = (map[c.brand] || 0) + 1
    }
  })
  return Object.entries(map).map(([brand, count]) => ({ brand, count }))
    .sort((a, b) => {
      // 小米、问界、零跑优先
      const pa = a.brand === '小米' ? 0 : (a.brand === '问界' || a.brand === '零跑') ? 1 : 2
      const pb = b.brand === '小米' ? 0 : (b.brand === '问界' || b.brand === '零跑') ? 1 : 2
      if (pa !== pb) return pa - pb
      return b.count - a.count
    })
})

// 根据输入筛选品牌建议
const filteredBrands = computed(() => {
  if (!keyword.value) return allBrands.value
  const kw = keyword.value.toLowerCase()
  return allBrands.value.filter(b => b.brand.toLowerCase().includes(kw))
})

const onSearchInput = () => {
  showSuggestions.value = true
}

const selectBrand = (brand) => {
  keyword.value = brand
  showSuggestions.value = false
}
const userInfo = ref({})
const isAdmin = computed(() => userInfo.value.role === 1)

const scrollToCars = () => {
  document.querySelector('.car-grid')?.scrollIntoView({ behavior: 'smooth' })
}

// 首页轮播广告
const homeAds = [
  {
    tag: '新用户专享',
    title: '首次租车立减200元',
    desc: '新用户注册即享首日半价优惠，全车型通用',
    features: ['✓ 新用户专属', '✓ 最高减200元', '✓ 全车型可用'],
    num: '¥200',
    bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    tagBg: 'rgba(255,255,255,0.2)', tagColor: '#fff',
    circleBg: 'rgba(255,255,255,0.15)', numColor: '#fff'
  },
  {
    tag: '商务出行',
    title: '企业用车专业服务',
    desc: '商务接待、企业长租、会议用车，彰显企业形象',
    features: ['✓ 豪华车型', '✓ 专业司机', '✓ 对公结算'],
    num: '5★',
    bg: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
    tagBg: 'rgba(201,169,98,0.2)', tagColor: '#c9a962',
    circleBg: 'rgba(201,169,98,0.15)', numColor: '#c9a962'
  },
  {
    tag: '安全出行',
    title: '全车险覆盖无忧',
    desc: '交强险、商业险、不计免赔三重保障，让您出行无忧',
    features: ['✓ 全车险覆盖', '✓ 不计免赔', '✓ 24小时救援'],
    num: '100%',
    bg: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
    tagBg: 'rgba(255,255,255,0.2)', tagColor: '#fff',
    circleBg: 'rgba(255,255,255,0.15)', numColor: '#fff'
  }
]

// 点击搜索框外部关闭建议
// 搜索框鼠标事件：进入时清除隐藏定时器，离开时0.2秒后隐藏
const onSearchBoxEnter = () => {
  if (sugHideTimer) { clearTimeout(sugHideTimer); sugHideTimer = null }
  if (keyword.value || showSuggestions.value) showSuggestions.value = true
}
const onSearchBoxLeave = () => {
  sugHideTimer = setTimeout(() => { showSuggestions.value = false }, 200)
}

onMounted(async () => {
  const res = await api.get('/cars/list')
  if (res.code === 200) cars.value = res.data
  const userRes = await api.get('/user/info')
  if (userRes.code === 200) {
    userInfo.value = userRes.data
    localStorage.setItem('userAvatar', userRes.data.avatar || '')
    // 获取待处理订单
    loadPendingOrders()
    // 获取未读消息数
    loadUserUnreadCount()
  }

  // 立即检查并显示欢迎弹窗（所有用户都显示）
  if (localStorage.getItem('showWelcome') === '1') {
    const welcomeName = localStorage.getItem('welcomeName') || '用户'
    const isNewUser = localStorage.getItem('welcomeIsNew') === '1'
    const welcomeAvatar = localStorage.getItem('userAvatar') || ''
    localStorage.removeItem('showWelcome')
    localStorage.removeItem('welcomeName')
    localStorage.removeItem('welcomeIsNew')
    showHomeWelcome(welcomeName, isNewUser, welcomeAvatar)
  }

  // 恢复滚动位置（从详情页返回时）
  const savedScroll = sessionStorage.getItem('homeScrollTop')
  if (savedScroll) {
    sessionStorage.removeItem('homeScrollTop')
    nextTick(() => window.scrollTo(0, parseInt(savedScroll)))
  }

})

onUnmounted(() => {
  if (sugHideTimer) clearTimeout(sugHideTimer)
})

// 离开首页时保存滚动位置和来源
onBeforeRouteLeave((to, from, next) => {
  if (to.path.startsWith('/car/')) {
    sessionStorage.setItem('homeScrollTop', window.scrollY)
    sessionStorage.setItem('car_detail_from', '/')
  }
  next()
})

// 首页欢迎弹窗（所有用户都显示）
const showHomeWelcome = (nickname, isNewUser, avatar) => {
  const avatarHtml = avatar
    ? `<img src="${avatar}" style="width:64px;height:64px;border-radius:50%;object-fit:cover;border:3px solid rgba(255,255,255,0.3);" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'" /><div style="display:none;width:64px;height:64px;border-radius:50%;background:linear-gradient(135deg,#667eea,#764ba2);align-items:center;justify-content:center;color:#fff;font-size:24px;font-weight:700">${(nickname||'?')[0]}</div>`
    : `<div style="width:64px;height:64px;border-radius:50%;background:linear-gradient(135deg,#667eea,#764ba2);display:flex;align-items:center;justify-content:center;color:#fff;font-size:24px;font-weight:700">${(nickname||'?')[0]}</div>`

  const div = document.createElement('div')
  div.className = 'home-welcome'
  div.innerHTML = `
    <div class="welcome-backdrop"></div>
    <div class="welcome-box">
      <div class="welcome-icon">
        ${avatarHtml}
      </div>
      <h2>欢迎回来，${nickname}！</h2>
      <p>${isNewUser ? '新用户专享200元优惠券已到账' : '很高兴再次见到您，祝您出行愉快'}</p>
      ${isNewUser ? '<span class="welcome-tag">¥200 新用户优惠</span>' : '<span class="welcome-tag secondary">开始您的旅程</span>'}
    </div>
  `
  document.body.appendChild(div)

  // 1.2秒后渐变消失
  setTimeout(() => {
    div.classList.add('hide')
    setTimeout(() => div.remove(), 400)
  }, 1200)
}

// 加载待处理订单
const loadPendingOrders = async () => {
  try {
    const res = await api.get('/orders/my')
    if (res.code === 200) {
      // 筛选待处理订单：待支付、在租中、预约中
      pendingOrders.value = res.data.filter(o => o.status === 0 || o.status === 1 || o.status === 4)
    }
  } catch (e) {
    // 静默处理
  }
}

// 加载用户未读消息数
const loadUserUnreadCount = async () => {
  try {
    const res = await api.get('/messages/unread')
    if (res.code === 200) {
      userUnreadCount.value = res.data
    }
  } catch (e) {
    // 静默处理
  }
}

const filteredCars = computed(() => {
  let list = [...cars.value]
  // 默认排除维护中的车
  list = list.filter(c => c.status !== 3)
  if (sortMode.value === 'available') list = list.filter(c => c.status === 0)
  if (filterUsage.value) list = list.filter(c => c.usageType && c.usageType.includes(filterUsage.value))
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(c => c.brand.toLowerCase().includes(kw) || c.model.toLowerCase().includes(kw))
  }
  // 排序逻辑
  if (sortMode.value === 'price-asc') {
    list.sort((a, b) => a.pricePerDay - b.pricePerDay)
  } else if (sortMode.value === 'price-desc') {
    list.sort((a, b) => b.pricePerDay - a.pricePerDay)
  } else {
    // 默认排序：小米 > 问界M7/零跑C11 > 其他
    const priority = (car) => {
      if (car.brand === '小米') return 0
      if ((car.brand === '问界' && car.model.includes('M7')) || (car.brand === '零跑' && car.model.includes('C11'))) return 1
      return 2
    }
    list.sort((a, b) => priority(a) - priority(b))
  }
  return list
})

const statusText = (s) => ({ 0: '空闲', 1: '已租出', 2: '已预约', 3: '维护中' }[s] || '')

// 写实风格汽车SVG生成
const brandThemes = {
  '丰田':   { body: '#c0392b', roof: '#a93226', accent: '#e74c3c' },
  '本田':   { body: '#2980b9', roof: '#2471a3', accent: '#3498db' },
  '日产':   { body: '#c0392b', roof: '#a93226', accent: '#e74c3c' },
  '大众':   { body: '#27ae60', roof: '#229954', accent: '#2ecc71' },
  '长安':   { body: '#2980b9', roof: '#2471a3', accent: '#3498db' },
  '哈弗':   { body: '#c0392b', roof: '#a93226', accent: '#e74c3c' },
  '宝马':   { body: '#1a252f', roof: '#0d1520', accent: '#2c3e50' },
  '奔驰':   { body: '#7f8c8d', roof: '#6c7a7d', accent: '#95a5a6' },
  '奥迪':   { body: '#1c2833', roof: '#111927', accent: '#2c3e50' },
  '沃尔沃': { body: '#2c3e50', roof: '#1a252f', accent: '#34495e' },
  '保时捷': { body: '#ecf0f1', roof: '#bdc3c7', accent: '#f5f5f5' },
  '红旗':   { body: '#c0392b', roof: '#922b21', accent: '#e74c3c' },
  '别克':   { body: '#7d3c98', roof: '#6c3483', accent: '#9b59b6' },
  '比亚迪': { body: '#16a085', roof: '#138d75', accent: '#1abc9c' },
  '特斯拉': { body: '#ecf0f1', roof: '#bdc3c7', accent: '#f5f5f5' },
  '蔚来':   { body: '#2471a3', roof: '#1a5276', accent: '#3498db' },
  '理想':   { body: '#d35400', roof: '#ba4a00', accent: '#e67e22' },
  '小鹏':   { body: '#27ae60', roof: '#229954', accent: '#2ecc71' },
  '零跑':   { body: '#2471a3', roof: '#1a5276', accent: '#3498db' },
  '极氪':   { body: '#2c3e50', roof: '#1a252f', accent: '#3498db' },
  '问界':   { body: '#7f8c8d', roof: '#6c7a7d', accent: '#95a5a6' },
}

const handleImgError = (e) => {
  const brand = e.target.dataset.brand || ''
  const category = e.target.dataset.category || '轿车'
  const s = brandThemes[brand] || { body: '#667eea', roof: '#5a67d8', accent: '#764ba2' }

  const isSUV = category === 'SUV'
  const isMPV = category === 'MPV'

  // 车身轮廓（小卡片尺寸）
  let bodyPath, windowPath
  if (isSUV) {
    bodyPath = 'M20,52 Q20,44 28,44 L34,30 L50,26 L110,26 L124,30 Q132,34 136,44 Q138,44 138,52 L138,55 L20,55 Z'
    windowPath = 'M36,42 L48,34 L48,46 L36,46 Z M52,33 L108,33 L108,46 L52,46 Z'
  } else if (isMPV) {
    bodyPath = 'M18,52 Q18,42 26,42 L32,26 L48,20 L112,20 L126,26 Q134,32 138,42 Q140,42 140,52 L140,55 L18,55 Z'
    windowPath = 'M34,38 L46,28 L46,42 L34,42 Z M50,26 L110,26 L110,42 L50,42 Z'
  } else {
    bodyPath = 'M22,52 Q22,46 30,46 L38,38 L52,32 L112,32 L124,38 Q132,42 136,46 Q138,46 138,52 L138,55 L22,55 Z'
    windowPath = 'M40,42 L50,36 L50,46 L40,46 Z M54,34 L110,34 L110,46 L54,46 Z'
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 160 80">
    <defs>
      <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#87CEEB"/>
        <stop offset="100%" stop-color="#E0F0FF"/>
      </linearGradient>
      <linearGradient id="bodyG" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="${s.body}"/>
        <stop offset="100%" stop-color="${s.roof}"/>
      </linearGradient>
      <linearGradient id="winG" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="#81D4FA"/>
        <stop offset="100%" stop-color="#E1F5FE"/>
      </linearGradient>
    </defs>
    <rect fill="url(#sky)" width="160" height="80" rx="6"/>
    <rect fill="#78909C" y="58" width="160" height="22"/>
    <rect fill="#607D8B" y="58" width="160" height="1.5"/>
    <ellipse cx="80" cy="57" rx="55" ry="4" fill="#000" opacity="0.1"/>
    <path d="${bodyPath}" fill="url(#bodyG)" stroke="${s.roof}" stroke-width="0.8"/>
    <path d="${windowPath}" fill="url(#winG)" opacity="0.85"/>
    <line x1="30" y1="48" x2="130" y2="48" stroke="${s.accent}" stroke-width="1" opacity="0.4"/>
    <rect x="24" y="47" width="5" height="3" rx="1" fill="#FDD835" opacity="0.8"/>
    <rect x="132" y="47" width="4" height="3" rx="1" fill="#E53935" opacity="0.8"/>
    <circle cx="42" cy="54" r="6" fill="#263238"/><circle cx="42" cy="54" r="3.5" fill="#455A64"/><circle cx="42" cy="54" r="1.5" fill="#78909C"/>
    <circle cx="120" cy="54" r="6" fill="#263238"/><circle cx="120" cy="54" r="3.5" fill="#455A64"/><circle cx="120" cy="54" r="1.5" fill="#78909C"/>
    <text x="80" y="16" text-anchor="middle" fill="${s.body}" font-size="9" font-weight="700" font-family="system-ui,sans-serif" opacity="0.7">${brand}</text>
  </svg>`
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent(svg)
}

import { ElMessageBox } from 'element-plus'

const handleCommand = async (cmd) => {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'logout') {
    const div = document.createElement('div')
    div.className = 'confirm-overlay'
    div.innerHTML = `
      <div class="confirm-modal">
        <div class="confirm-icon-wrap">
          <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </div>
        <h3>确认退出登录？</h3>
        <p>退出后将返回登录页面，需要重新输入账号密码</p>
        <div class="confirm-btns">
          <button class="confirm-cancel" onclick="this.closest('.confirm-overlay').remove()">取消</button>
          <button class="confirm-ok" id="confirmLogoutBtn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            确认退出
          </button>
        </div>
      </div>
    `
    document.body.appendChild(div)
    div.addEventListener('click', (e) => { if (e.target === div) div.remove() })

    // 绑定退出按钮事件
    document.getElementById('confirmLogoutBtn').addEventListener('click', () => {
      div.remove()
      localStorage.removeItem('token')
      sessionStorage.clear()
      // 强制跳转到登录页
      window.location.replace('/login')
    })
  }
}
</script>

<style scoped>
.home { min-height: 100vh; display: flex; flex-direction: column; background: #f5f7fa; }

/* Header */
.header { background: #fff; position: sticky; top: 0; z-index: 100; box-shadow: 0 1px 0 rgba(0,0,0,0.06); }
.header-content { max-width: 1200px; margin: 0 auto; padding: 0 24px; height: 64px; display: flex; align-items: center; justify-content: space-between; }
.logo { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.logo-icon { width: 42px; height: 42px; display: flex; align-items: center; justify-content: center; transition: transform 0.3s; }
.logo:hover .logo-icon { transform: scale(1.08) rotate(-3deg); }
.logo-text { display: flex; flex-direction: column; }
.logo-name { font-size: 18px; font-weight: 700; color: #1a1a2e; letter-spacing: 2px; }
.logo-sub { font-size: 9px; color: #999; letter-spacing: 1px; }
.nav-right { display: flex; align-items: center; gap: 8px; }
.nav-link { display: flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 8px; color: #666; text-decoration: none; font-size: 14px; cursor: pointer; transition: all 0.2s; position: relative; }
.nav-link:hover { background: #f5f7fa; color: #333; }
.msg-badge { position: absolute; top: 2px; right: 2px; background: #f56c6c; color: #fff; font-size: 10px; padding: 1px 5px; border-radius: 10px; }
.ai-nav { flex-direction: column; align-items: center; justify-content: center; gap: 0; padding: 4px 10px; position: relative; }
.ai-nav .ai-rabbit-wrap { line-height: 0; display: flex; align-items: center; justify-content: center; }
.ai-nav:hover .ai-rabbit-wrap { transform: scale(1.1) translateY(-2px); filter: drop-shadow(0 2px 8px rgba(102,126,234,0.4)); transition: all 0.3s; }
.ai-label { font-size: 11px; font-weight: 600; color: #667eea; letter-spacing: 0.5px; text-align: center; margin-top: 1px; }
.coupon-nav { color: #e6a23c; }
.coupon-nav:hover { background: #fdf6ec; color: #e6a23c; }
.coupon-nav svg { stroke: #e6a23c; }
.user-btn { width: 36px; height: 36px; border-radius: 50%; overflow: hidden; cursor: pointer; }
.user-avatar { width: 100%; height: 100%; background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: 14px; transition: transform 0.2s; }
.user-avatar-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; transition: transform 0.2s; }
.user-btn:hover .user-avatar, .user-btn:hover .user-avatar-img { transform: scale(1.1); }

.main { max-width: 1200px; margin: 0 auto; padding: 24px; flex: 1; width: 100%; box-sizing: border-box; }

/* 首页轮播 */
.home-carousel { margin-bottom: 24px; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
.home-carousel :deep(.el-carousel__indicator) { width: 24px; height: 6px; border-radius: 3px; background: #ddd; padding: 0; margin: 0 4px; transition: all 0.3s; }
.home-carousel :deep(.el-carousel__indicator.is-active) { background: #667eea; width: 32px; }
.home-ad-slide { height: 320px; display: flex; align-items: center; justify-content: space-between; padding: 0 60px; color: #fff; position: relative; overflow: hidden; }

/* 订单提醒 */
.order-reminder { display: flex; align-items: center; gap: 12px; padding: 14px 20px; background: linear-gradient(135deg, #fff9e6 0%, #fff3cd 100%); border: 1px solid #ffd43b; border-radius: 12px; margin-bottom: 20px; cursor: pointer; transition: all 0.3s; }
.order-reminder:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(255,212,59,0.3); }
.order-reminder span { flex: 1; font-size: 14px; color: #666; }
.order-reminder strong { color: #e6a23c; font-size: 16px; }

/* 装饰元素 */
.home-ad-deco { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.hdeco-ring { position: absolute; width: 180px; height: 180px; border-radius: 50%; border: 1px solid; top: -30px; right: 10%; opacity: 0.15; animation: hRingSpin 15s linear infinite; }
@keyframes hRingSpin { to { transform: rotate(360deg); } }
.hdeco-dot { position: absolute; border-radius: 50%; opacity: 0.12; }
.hdeco-dot.d1 { width: 8px; height: 8px; top: 20%; right: 25%; animation: hDotFloat 5s ease-in-out infinite; }
.hdeco-dot.d2 { width: 5px; height: 5px; top: 60%; right: 8%; animation: hDotFloat 7s ease-in-out infinite 1s; }
.hdeco-dot.d3 { width: 6px; height: 6px; top: 40%; left: 45%; animation: hDotFloat 6s ease-in-out infinite 2s; }
@keyframes hDotFloat { 0%,100%{transform:translateY(0);opacity:0.12} 50%{transform:translateY(-12px);opacity:0.35} }
.hdeco-line { position: absolute; height: 1px; width: 200px; bottom: 30%; left: 5%; opacity: 0.3; }

/* 内容入场动画 */
@keyframes hSlideUp { from{opacity:0;transform:translateY(20px)} to{opacity:1;transform:translateY(0)} }
.el-carousel__item--active .anim-hitem { animation: hSlideUp 0.5s cubic-bezier(0.22,1,0.36,1) both; }
.el-carousel__item--active .anim-hitem:nth-child(1) { animation-delay: 0s; }
.el-carousel__item--active .anim-hitem:nth-child(2) { animation-delay: 0.1s; }
.el-carousel__item--active .anim-hitem:nth-child(3) { animation-delay: 0.2s; }
.el-carousel__item--active .anim-hitem:nth-child(4) { animation-delay: 0.3s; }

.home-ad-content { max-width: 500px; position: relative; z-index: 1; }
.home-ad-tag { display: inline-block; padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; margin-bottom: 16px; }
.home-ad-title { font-size: 32px; font-weight: 700; margin-bottom: 12px; text-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.home-ad-desc { font-size: 14px; opacity: 0.85; margin-bottom: 20px; line-height: 1.6; }
.home-ad-features { display: flex; gap: 10px; flex-wrap: wrap; }
.home-feature-chip { font-size: 13px; padding: 5px 14px; border-radius: 20px; border: 1px solid; background: rgba(255,255,255,0.08); backdrop-filter: blur(4px); transition: all 0.2s; cursor: default; }
.home-feature-chip:hover { background: rgba(255,255,255,0.15); transform: translateY(-1px); }

.home-ad-visual { position: relative; z-index: 1; display: flex; align-items: center; justify-content: center; }
.home-ad-ring { position: absolute; width: 150px; height: 150px; border-radius: 50%; border: 1px solid; animation: hRingPulse 3s ease-in-out infinite; }
@keyframes hRingPulse { 0%,100%{transform:scale(1);opacity:0.3} 50%{transform:scale(1.12);opacity:0.1} }
.home-ad-circle { width: 120px; height: 120px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 32px; font-weight: 700; backdrop-filter: blur(8px); position: relative; z-index: 1; transition: transform 0.3s; }
.home-ad-circle:hover { transform: scale(1.06); }

/* Banner */
.banner-wrapper { margin-bottom: 32px; border-radius: 16px; overflow: hidden; }
.banner-item { height: 380px; display: flex; align-items: center; padding: 0 80px; position: relative; overflow: hidden; color: #fff; }
.banner-bg { position: absolute; inset: 0; pointer-events: none; }
.circle { position: absolute; border-radius: 50%; }
.c1 { width: 400px; height: 400px; top: -150px; right: -50px; background: rgba(255,215,0,0.15); animation: float 8s ease-in-out infinite; }
.c2 { width: 200px; height: 200px; bottom: -80px; left: 15%; background: rgba(255,179,71,0.1); animation: float 6s ease-in-out infinite reverse; }
.c3 { width: 400px; height: 400px; top: -150px; right: -50px; background: rgba(201,169,98,0.15); animation: float 8s ease-in-out infinite; }
.c4 { width: 200px; height: 200px; bottom: -80px; left: 15%; background: rgba(166,124,82,0.1); animation: float 6s ease-in-out infinite reverse; }
.c5 { width: 400px; height: 400px; top: -150px; right: -50px; background: rgba(255,255,255,0.1); animation: float 8s ease-in-out infinite; }
.c6 { width: 200px; height: 200px; bottom: -80px; left: 15%; background: rgba(255,255,255,0.08); animation: float 6s ease-in-out infinite reverse; }
@keyframes float { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-20px)} }

.banner-1 { background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); }
.banner-2 { background: linear-gradient(135deg, #0f0f0f 0%, #1a1a1a 100%); }
.banner-3 { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }

.banner-content { position: relative; z-index: 10; max-width: 500px; }
.badge { display: inline-block; padding: 6px 16px; border-radius: 20px; font-size: 12px; font-weight: 500; margin-bottom: 16px; background: rgba(255,255,255,0.15); backdrop-filter: blur(10px); }
.banner-content h2 { font-size: 36px; font-weight: 700; line-height: 1.3; margin-bottom: 12px; }
.banner-content h2 em { font-style: normal; color: #ffd700; }
.banner-2 .banner-content h2 em { color: #c9a962; }
.banner-3 .banner-content h2 em { color: #fff; }
.banner-content p { font-size: 15px; opacity: 0.8; margin-bottom: 24px; }
.features { display: flex; gap: 20px; margin-bottom: 28px; }
.features span { display: flex; align-items: center; gap: 6px; font-size: 13px; opacity: 0.9; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 12px 28px; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.3s; border: none; }
.btn-primary.gold { background: linear-gradient(135deg, #ffd700, #ffb347); color: #1a1a2e; }
.btn-primary.gold:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(255,215,0,0.4); }
.btn-primary.white { background: #fff; color: #667eea; }
.btn-primary.white:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(255,255,255,0.4); }

.banner-visual { position: relative; width: 250px; height: 250px; margin-left: auto; z-index: 10; }
.visual-circle { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); width: 140px; height: 140px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.gold-circle { background: rgba(255,215,0,0.2); border: 2px solid rgba(255,215,0,0.3); }
.purple-circle { background: rgba(255,255,255,0.15); border: 2px solid rgba(255,255,255,0.2); }
.float-card { position: absolute; padding: 12px 16px; border-radius: 10px; background: #fff; box-shadow: 0 4px 20px rgba(0,0,0,0.15); animation: floatCard 4s ease-in-out infinite; }
.float-card span { display: block; font-size: 11px; color: #999; margin-top: 2px; }
.float-card.top { top: 20px; right: 0; color: #f56c6c; font-size: 20px; font-weight: 700; }
.float-card.bottom { bottom: 30px; left: 0; color: #667eea; font-size: 20px; font-weight: 700; animation-delay: 1s; }
@keyframes floatCard { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-6px)} }

/* Filter */
.filter-bar { display: flex; align-items: center; gap: 20px; margin-bottom: 24px; padding: 16px 20px; background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); flex-wrap: wrap; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: #f5f7fa; border-radius: 10px; min-width: 220px; position: relative; overflow: visible; }
.search-box input { border: none; background: transparent; outline: none; font-size: 14px; width: 100%; }

/* 搜索建议下拉 */
.suggestions { position: absolute; top: 100%; left: 0; right: 0; margin-top: 8px; background: #fff; border-radius: 12px; box-shadow: 0 8px 30px rgba(0,0,0,0.12); z-index: 200; overflow: hidden; }
.sug-title { padding: 10px 16px 6px; font-size: 12px; color: #999; font-weight: 500; }
.sug-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 16px; cursor: pointer; transition: background 0.15s; }
.sug-item:hover { background: #f0f2ff; }
.sug-brand { font-size: 14px; color: #333; font-weight: 500; }
.sug-count { font-size: 12px; color: #999; background: #f5f7fa; padding: 2px 8px; border-radius: 10px; }
.filter-tags { display: flex; gap: 8px; }
.sort-tags { display: flex; gap: 6px; margin-left: auto; }
.tag { padding: 8px 14px; border-radius: 8px; border: 1px solid #e5e7eb; background: #fff; font-size: 13px; color: #666; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 4px; }
.tag:hover { border-color: #667eea; color: #667eea; }
.tag.active { background: #667eea; color: #fff; border-color: #667eea; }

/* Section Title */
.section-title { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; }
.section-title h2 { font-size: 20px; font-weight: 700; color: #1a1a2e; }
.section-title span { font-size: 14px; color: #999; }

/* Car Grid */
.car-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 24px; }
.car-card { background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.04); cursor: pointer; transition: all 0.35s cubic-bezier(0.4,0,0.2,1); }
.car-card:hover { transform: translateY(-8px); box-shadow: 0 20px 40px rgba(0,0,0,0.12); }
.car-img-wrap { height: 200px; position: relative; overflow: hidden; }
.car-img-wrap img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s cubic-bezier(0.4,0,0.2,1); }
.car-card:hover .car-img-wrap img { transform: scale(1.08); }
.img-hover { position: absolute; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s; }
.car-card:hover .img-hover { opacity: 1; }
.view-btn { padding: 8px 20px; background: #fff; border-radius: 8px; font-size: 13px; font-weight: 600; color: #333; transform: translateY(10px); transition: all 0.3s; }
.car-card:hover .view-btn { transform: translateY(0); }
.status-tag { position: absolute; top: 12px; right: 12px; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; color: #fff; backdrop-filter: blur(8px); }
.s0 { background: rgba(103,194,58,0.9); }
.s1 { background: rgba(245,108,108,0.9); }
.s2 { background: rgba(230,162,60,0.9); }
.s3 { background: rgba(144,147,153,0.9); }
.usage-tags { position: absolute; top: 12px; left: 12px; display: flex; gap: 6px; }
.usage-tags .tag { padding: 4px 8px; border-radius: 6px; font-size: 10px; font-weight: 600; backdrop-filter: blur(8px); border: none; }
.tag.business { background: rgba(102,126,234,0.9); color: #fff; }
.tag.wedding { background: rgba(245,108,108,0.9); color: #fff; }

.car-info { padding: 20px; }
.car-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.car-header h3 { font-size: 16px; font-weight: 600; color: #1a1a2e; }
.seats { font-size: 12px; color: #999; padding: 4px 8px; background: #f5f7fa; border-radius: 6px; }
.car-desc { font-size: 13px; color: #999; margin-bottom: 16px; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.car-bottom { display: flex; justify-content: space-between; align-items: baseline; padding-top: 12px; border-top: 1px solid #f5f5f5; }
.price { font-size: 22px; font-weight: 700; color: #f56c6c; }
.price small { font-size: 12px; font-weight: normal; color: #999; margin-left: 2px; }
.deposit { font-size: 12px; color: #999; }

/* Empty */
.empty { text-align: center; padding: 80px 0; color: #999; }
.empty svg { margin-bottom: 16px; }
.empty p { margin-top: 16px; }

/* Footer */
.footer { background: #1a1a2e; color: #fff; margin-top: 60px; }
.footer-main { max-width: 1200px; margin: 0 auto; padding: 48px 24px; display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 40px; }
.footer-logo { display: flex; align-items: center; gap: 10px; font-size: 16px; font-weight: 700; margin-bottom: 12px; }
.footer-brand p { color: #888; font-size: 13px; line-height: 1.8; }
.footer-col h4 { font-size: 14px; font-weight: 600; margin-bottom: 16px; }
.footer-col a { display: block; color: #888; font-size: 13px; margin-bottom: 10px; text-decoration: none; transition: color 0.2s; }
.footer-col a:hover { color: #fff; }
.footer-col p { color: #888; font-size: 13px; margin-bottom: 8px; }
.footer-bottom { border-top: 1px solid rgba(255,255,255,0.08); padding: 20px 24px; text-align: center; }
.footer-bottom p { color: #666; font-size: 12px; }

@media (max-width: 768px) {
  .banner-item { padding: 0 30px; }
  .banner-content h2 { font-size: 24px; }
  .banner-visual { display: none; }
  .footer-main { grid-template-columns: 1fr; }
}

</style>

<style>
/* 首页欢迎弹窗（无scoped，因为是手动创建DOM追加到body的） */
.home-welcome {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.home-welcome.hide .welcome-backdrop {
  animation: welcomeFadeOut 0.4s ease forwards;
}
.home-welcome.hide .welcome-box {
  animation: welcomeBoxOut 0.4s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}
@keyframes welcomeBoxOut { from { transform: scale(1); opacity: 1; } to { transform: scale(0.85); opacity: 0; } }
.welcome-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.35);
  backdrop-filter: blur(6px);
  animation: welcomeFadeIn 0.3s ease;
}
.welcome-box {
  position: relative;
  background: #fff;
  padding: 44px 52px;
  border-radius: 20px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,0.25);
  animation: welcomeScaleIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes welcomeFadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes welcomeFadeOut { from { opacity: 1; } to { opacity: 0; } }
@keyframes welcomeScaleIn { from { transform: scale(0.6); opacity: 0; } to { transform: scale(1); opacity: 1; } }

.welcome-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18px;
}
.welcome-box h2 { font-size: 22px; color: #1a1a2e; margin-bottom: 8px; font-weight: 700; }
.welcome-box p { font-size: 14px; color: #666; margin-bottom: 14px; }
.welcome-tag {
  display: inline-block;
  background: linear-gradient(135deg, #f56c6c, #e74c3c);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 20px;
  border-radius: 20px;
}
.welcome-tag.secondary {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

/* 退出确认弹窗 */
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.25s ease;
  backdrop-filter: blur(6px);
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.confirm-modal {
  background: #fff;
  border-radius: 24px;
  padding: 40px;
  max-width: 380px;
  width: 90%;
  text-align: center;
  animation: modalPop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 25px 80px rgba(0,0,0,0.25);
}
@keyframes modalPop {
  from { transform: scale(0.8) translateY(20px); opacity: 0; }
  to { transform: scale(1) translateY(0); opacity: 1; }
}

.confirm-icon-wrap {
  width: 88px;
  height: 88px;
  background: linear-gradient(135deg, #fff5e6, #ffedcc);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  animation: iconPulse 0.6s ease 0.2s;
}
@keyframes iconPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

.confirm-modal h3 {
  font-size: 20px;
  color: #1a1a2e;
  margin-bottom: 10px;
  font-weight: 700;
}

.confirm-modal p {
  color: #999;
  font-size: 14px;
  margin-bottom: 28px;
  line-height: 1.5;
}

.confirm-btns {
  display: flex;
  gap: 12px;
}

.confirm-cancel {
  flex: 1;
  padding: 14px;
  background: #f5f7fa;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.confirm-cancel:hover {
  background: #e8eaed;
}

.confirm-ok {
  flex: 1;
  padding: 14px;
  background: linear-gradient(135deg, #f56c6c, #e74c3c);
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.confirm-ok:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(245,108,108,0.4);
}
</style>
