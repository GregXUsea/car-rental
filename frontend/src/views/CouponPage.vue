<template>
  <div class="coupon-page">
    <header class="header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">
            <svg width="38" height="38" viewBox="0 0 48 48" fill="none">
              <defs>
                <linearGradient id="gCouponLogo" x1="4" y1="2" x2="44" y2="46">
                  <stop offset="0%" stop-color="#FFD700"/>
                  <stop offset="100%" stop-color="#FF8C00"/>
                </linearGradient>
              </defs>
              <path d="M24 3 L7 11 L7 22 C7 31 14.5 39 24 43 C33.5 39 41 31 41 22 L41 11 Z" fill="url(#gCouponLogo)"/>
              <path d="M16 23 L21 28 L32 17" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="logo-text">
            <span class="logo-name">御途租车</span>
            <span class="logo-sub">YUTU CAR RENTAL</span>
          </div>
        </div>
        <nav class="nav-right">
          <a class="nav-link" @click.prevent="$router.push('/')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            首页
          </a>
          <a class="nav-link" @click.prevent="$router.push('/orders')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            订单
          </a>
        </nav>
      </div>
    </header>

    <main class="main">
      <div class="page-title-area">
        <h1 class="page-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 12v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6"/><path d="M2 8h20v4H2z"/><path d="M12 2v6"/><path d="M12 2l-3 3"/><path d="M12 2l3 3"/></svg>
          我的优惠券
        </h1>
        <p class="page-subtitle">新用户专享优惠，首次租车立省最高200元</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-wrap">
        <div class="loading-spinner"></div>
        <p>正在加载优惠券信息...</p>
      </div>

      <template v-else>
        <!-- 优惠券卡片 -->
        <div class="coupon-card" :class="{ 'coupon-used': !couponStatus.eligible }">
          <div class="coupon-left">
            <div class="coupon-amount">
              <span class="coupon-symbol">¥</span>
              <span class="coupon-value">200</span>
            </div>
            <div class="coupon-label">新用户专享</div>
          </div>
          <div class="coupon-divider">
            <div class="divider-circle top"></div>
            <div class="divider-line"></div>
            <div class="divider-circle bottom"></div>
          </div>
          <div class="coupon-right">
            <div class="coupon-title">首单立减优惠券</div>
            <div class="coupon-desc">首次租车金额减免50%，最高减200元</div>
            <div class="coupon-meta">
              <span class="coupon-tag" :class="couponStatus.eligible ? 'tag-active' : 'tag-used'">
                {{ couponStatus.eligible ? '可使用' : '已使用/不符合条件' }}
              </span>
              <span v-if="couponStatus.eligible && couponStatus.daysLeft > 0" class="coupon-expire">
                剩余 {{ couponStatus.daysLeft }} 天到期
              </span>
              <span v-if="!couponStatus.eligible" class="coupon-expire expired">
                优惠券已失效
              </span>
            </div>
            <button v-if="couponStatus.eligible" class="coupon-btn" @click="$router.push('/')">
              立即使用
            </button>
          </div>
        </div>

        <!-- 使用规则 -->
        <div class="rules-section">
          <h2 class="section-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            使用规则
          </h2>
          <div class="rules-card">
            <div class="rule-item">
              <div class="rule-icon">🎯</div>
              <div class="rule-content">
                <div class="rule-title">适用人群</div>
                <div class="rule-text">注册30天内的新用户可使用本优惠券，每个账号限用一次。</div>
              </div>
            </div>
            <div class="rule-item">
              <div class="rule-icon">💰</div>
              <div class="rule-content">
                <div class="rule-title">优惠金额</div>
                <div class="rule-text">首次租车订单金额减免50%，单笔最高减免200元。例如：租车费用400元，实际支付200元；租车费用600元，实际支付400元（最高减200元）。</div>
              </div>
            </div>
            <div class="rule-item">
              <div class="rule-icon">🚗</div>
              <div class="rule-content">
                <div class="rule-title">适用车型</div>
                <div class="rule-text">全车型通用，包括轿车、SUV、MPV等所有在租车辆。</div>
              </div>
            </div>
            <div class="rule-item">
              <div class="rule-icon">📋</div>
              <div class="rule-content">
                <div class="rule-title">使用条件</div>
                <div class="rule-text">仅限首次下单使用，优惠在下单时自动抵扣，无需手动领取或输入优惠码。已取消的订单不计入首次使用。</div>
              </div>
            </div>
            <div class="rule-item">
              <div class="rule-icon">⏰</div>
              <div class="rule-content">
                <div class="rule-title">有效期</div>
                <div class="rule-text">自注册之日起30天内有效，过期自动失效。请在有效期内完成下单。</div>
              </div>
            </div>
            <div class="rule-item">
              <div class="rule-icon">🔄</div>
              <div class="rule-content">
                <div class="rule-title">叠加规则</div>
                <div class="rule-text">本优惠券不可与其他优惠活动、折扣码叠加使用。系统将自动选择最优惠方案。</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 免责声明 -->
        <div class="disclaimer-section">
          <h2 class="section-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            免责声明
          </h2>
          <div class="disclaimer-card">
            <div class="disclaimer-item">
              <span class="disclaimer-num">1</span>
              <p>本优惠券活动最终解释权归御途租车所有。御途租车保留随时修改、暂停或终止本优惠活动的权利，恕不另行通知。</p>
            </div>
            <div class="disclaimer-item">
              <span class="disclaimer-num">2</span>
              <p>优惠券仅限本人使用，不可转让、不可赠予、不可兑换现金或等价物。任何转让或售卖优惠券的行为均属无效。</p>
            </div>
            <div class="disclaimer-item">
              <span class="disclaimer-num">3</span>
              <p>因系统升级、维护或不可抗力因素导致优惠券无法正常使用的，御途租车将在系统恢复后为受影响用户补发等值优惠券，但不承担其他赔偿责任。</p>
            </div>
            <div class="disclaimer-item">
              <span class="disclaimer-num">4</span>
              <p>若发现用户通过虚假注册、恶意刷单、利用系统漏洞等不正当手段获取或使用优惠券，御途租车有权取消其优惠资格并追究相关责任。</p>
            </div>
            <div class="disclaimer-item">
              <span class="disclaimer-num">5</span>
              <p>优惠金额以实际下单时系统计算为准。因车辆价格调整、租期变更等原因导致的优惠金额变化，以最终结算金额为准。</p>
            </div>
            <div class="disclaimer-item">
              <span class="disclaimer-num">6</span>
              <p>使用优惠券的订单如发生退款，退款金额按实际支付金额计算，优惠券部分不予退还，且优惠券使用资格不予恢复。</p>
            </div>
            <div class="disclaimer-footer">
              如有疑问，请联系客服：400-888-8888 | 邮箱：support@yutu-car.com
            </div>
          </div>
        </div>
      </template>
    </main>

    <footer class="footer">
      <div class="footer-bottom">
        <p>© 2026 御途租车 YUTU CAR RENTAL · 优惠券中心</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const loading = ref(true)
const couponStatus = ref({
  eligible: false,
  daysLeft: 0,
  registerDate: null,
  expireDate: null
})

onMounted(async () => {
  try {
    const res = await api.get('/user/coupon-status')
    if (res.code === 200) {
      couponStatus.value = res.data
    }
  } catch (e) {
    console.error('加载优惠券状态失败', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.coupon-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* Header */
.header {
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 0 rgba(0,0,0,0.06);
}
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.logo-icon {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text {
  display: flex;
  flex-direction: column;
}
.logo-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: 2px;
}
.logo-sub {
  font-size: 9px;
  color: #999;
  letter-spacing: 1px;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  color: #666;
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.nav-link:hover {
  background: #f5f7fa;
  color: #333;
}

/* Main */
.main {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
  flex: 1;
  width: 100%;
  box-sizing: border-box;
}

/* Page Title */
.page-title-area {
  margin-bottom: 32px;
}
.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}
.page-subtitle {
  font-size: 14px;
  color: #999;
  margin-left: 34px;
}

/* Loading */
.loading-wrap {
  text-align: center;
  padding: 60px 0;
}
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.loading-wrap p {
  color: #666;
  font-size: 14px;
}

/* Coupon Card */
.coupon-card {
  display: flex;
  align-items: stretch;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
  margin-bottom: 32px;
  transition: all 0.3s;
}
.coupon-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.4);
}
.coupon-card.coupon-used {
  background: linear-gradient(135deg, #999 0%, #777 100%);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.coupon-card.coupon-used:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
}

.coupon-left {
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 160px;
}
.coupon-amount {
  display: flex;
  align-items: flex-start;
  color: #fff;
}
.coupon-symbol {
  font-size: 24px;
  font-weight: 600;
  margin-top: 8px;
}
.coupon-value {
  font-size: 56px;
  font-weight: 800;
  line-height: 1;
}
.coupon-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8px;
  font-weight: 500;
}

.coupon-divider {
  position: relative;
  width: 1px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.divider-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f5f7fa;
  position: absolute;
}
.divider-circle.top {
  top: -10px;
}
.divider-circle.bottom {
  bottom: -10px;
}
.divider-line {
  flex: 1;
  width: 1px;
  background: rgba(255, 255, 255, 0.3);
  border: 1px dashed rgba(255, 255, 255, 0.4);
}

.coupon-right {
  flex: 1;
  padding: 28px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.coupon-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}
.coupon-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 16px;
  line-height: 1.5;
}
.coupon-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.coupon-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}
.tag-active {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}
.tag-used {
  background: rgba(0, 0, 0, 0.2);
  color: rgba(255, 255, 255, 0.7);
}
.coupon-expire {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}
.coupon-expire.expired {
  color: rgba(255, 200, 200, 0.9);
}
.coupon-btn {
  align-self: flex-start;
  padding: 10px 28px;
  background: #fff;
  color: #667eea;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.coupon-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* Rules Section */
.rules-section {
  margin-bottom: 32px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 16px;
}
.rules-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.rule-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}
.rule-item:last-child {
  border-bottom: none;
}
.rule-icon {
  font-size: 24px;
  flex-shrink: 0;
  width: 40px;
  text-align: center;
}
.rule-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 6px;
}
.rule-text {
  font-size: 13px;
  color: #666;
  line-height: 1.7;
}

/* Disclaimer Section */
.disclaimer-section {
  margin-bottom: 32px;
}
.disclaimer-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border-left: 4px solid #e6a23c;
}
.disclaimer-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.disclaimer-item:last-of-type {
  margin-bottom: 0;
}
.disclaimer-num {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.disclaimer-item p {
  font-size: 13px;
  color: #666;
  line-height: 1.7;
  margin: 0;
}
.disclaimer-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
  text-align: center;
}

/* Footer */
.footer {
  background: #1a1a2e;
  color: #fff;
  margin-top: auto;
}
.footer-bottom {
  padding: 20px 24px;
  text-align: center;
}
.footer-bottom p {
  color: #666;
  font-size: 12px;
}

@media (max-width: 640px) {
  .coupon-card {
    flex-direction: column;
  }
  .coupon-left {
    padding: 24px;
    flex-direction: row;
    gap: 16px;
    min-width: auto;
  }
  .coupon-divider {
    width: auto;
    height: 1px;
    flex-direction: row;
  }
  .divider-circle {
    position: static;
  }
  .divider-line {
    flex: 1;
    height: 1px;
    width: auto;
  }
  .coupon-right {
    padding: 24px;
  }
}
</style>
