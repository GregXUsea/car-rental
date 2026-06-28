<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <!-- 返回箭头 -->
    <template v-if="name === 'back'">
      <path d="M19 12H5M12 19l-7-7 7-7" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </template>

    <!-- 搜索 -->
    <template v-else-if="name === 'search'">
      <circle cx="11" cy="11" r="7" :stroke="color" stroke-width="2"/>
      <path d="M16 16l4 4" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- 用户 -->
    <template v-else-if="name === 'user'">
      <circle cx="12" cy="8" r="4" :stroke="color" stroke-width="2"/>
      <path d="M4 20c0-4 4-6 8-6s8 2 8 6" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- 锁/密码 -->
    <template v-else-if="name === 'lock'">
      <rect x="5" y="11" width="14" height="10" rx="2" :stroke="color" stroke-width="2"/>
      <path d="M8 11V7a4 4 0 118 0v4" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- AI魔法 -->
    <template v-else-if="name === 'magic'">
      <path d="M12 2l2 6h6l-5 4 2 6-5-4-5 4 2-6-5-4h6z" :stroke="color" stroke-width="1.5" fill="none"/>
      <circle cx="19" cy="5" r="1.5" :fill="color"/>
      <circle cx="5" cy="17" r="1" :fill="color"/>
    </template>

    <!-- 汽车 -->
    <template v-else-if="name === 'car'">
      <path d="M5 17h14M5 17a2 2 0 01-2-2v-3l2-5h14l2 5v3a2 2 0 01-2 2M5 17a2 2 0 00-2 2v1h4v-1a2 2 0 00-2-2zM19 17a2 2 0 012 2v1h-4v-1a2 2 0 012-2z" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <circle cx="7.5" cy="17" r="1.5" :fill="color"/>
      <circle cx="16.5" cy="17" r="1.5" :fill="color"/>
    </template>

    <!-- 购物车/租车 -->
    <template v-else-if="name === 'van'">
      <path d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <circle cx="7" cy="20" r="2" :stroke="color" stroke-width="2"/>
      <circle cx="17" cy="20" r="2" :stroke="color" stroke-width="2"/>
    </template>

    <!-- 时钟 -->
    <template v-else-if="name === 'clock'">
      <circle cx="12" cy="12" r="10" :stroke="color" stroke-width="2"/>
      <path d="M12 6v6l4 2" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- 信用卡/支付 -->
    <template v-else-if="name === 'card'">
      <rect x="2" y="5" width="20" height="14" rx="2" :stroke="color" stroke-width="2"/>
      <path d="M2 10h20" :stroke="color" stroke-width="2"/>
      <path d="M6 15h4" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- 关闭 -->
    <template v-else-if="name === 'close'">
      <path d="M18 6L6 18M6 6l12 12" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- 订单/文档 -->
    <template v-else-if="name === 'order'">
      <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" :stroke="color" stroke-width="2" stroke-linecap="round"/>
      <path d="M9 14l2 2 4-4" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </template>

    <!-- 日历/时间 -->
    <template v-else-if="name === 'calendar'">
      <rect x="3" y="4" width="18" height="18" rx="2" :stroke="color" stroke-width="2"/>
      <path d="M16 2v4M8 2v4M3 10h18" :stroke="color" stroke-width="2" stroke-linecap="round"/>
      <rect x="7" y="14" width="2" height="2" rx="0.5" :fill="color"/>
      <rect x="11" y="14" width="2" height="2" rx="0.5" :fill="color"/>
    </template>

    <!-- 循环/刷新 -->
    <template v-else-if="name === 'refresh'">
      <path d="M23 4v6h-6M1 20v-6h6" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </template>

    <!-- 金钱 -->
    <template v-else-if="name === 'money'">
      <circle cx="12" cy="12" r="10" :stroke="color" stroke-width="2"/>
      <path d="M12 6v12M15 9.5c0-1.38-1.34-2.5-3-2.5s-3 1.12-3 2.5 1.34 2.5 3 2.5 3 1.12 3 2.5-1.34 2.5-3 2.5" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- 安全/盾牌 -->
    <template v-else-if="name === 'shield'">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M9 12l2 2 4-4" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </template>

    <!-- 空盒子 -->
    <template v-else-if="name === 'empty'">
      <path d="M21 8v13H3V8M1 3h22v5H1z" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M10 12h4" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- 商务/公司 -->
    <template v-else-if="name === 'business'">
      <path d="M6 22V4a2 2 0 012-2h8a2 2 0 012 2v18H6z" :stroke="color" stroke-width="2"/>
      <path d="M6 12h12M10 22V16h4v6" :stroke="color" stroke-width="2" stroke-linecap="round"/>
      <rect x="9" y="7" width="2" height="2" rx="0.5" :fill="color"/>
      <rect x="13" y="7" width="2" height="2" rx="0.5" :fill="color"/>
    </template>

    <!-- 婚礼/爱心 -->
    <template v-else-if="name === 'heart'">
      <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" :stroke="color" stroke-width="2" fill="none"/>
    </template>

    <!-- 奖杯/优惠 -->
    <template v-else-if="name === 'trophy'">
      <path d="M8 21h8M12 17v4M6 4h12v3a6 6 0 01-12 0V4zM6 7H3v2a3 3 0 003 3M18 7h3v2a3 3 0 01-3 3" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </template>

    <!-- 家庭 -->
    <template v-else-if="name === 'family'">
      <circle cx="9" cy="7" r="3" :stroke="color" stroke-width="2"/>
      <circle cx="17" cy="7" r="2" :stroke="color" stroke-width="2"/>
      <path d="M3 21v-2a4 4 0 014-4h4a4 4 0 014 4v2" :stroke="color" stroke-width="2" stroke-linecap="round"/>
      <path d="M17 14a3 3 0 013 3v4" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- 座位 -->
    <template v-else-if="name === 'seat'">
      <path d="M4 18v-6a8 8 0 0116 0v6" :stroke="color" stroke-width="2" stroke-linecap="round"/>
      <path d="M4 18h16" :stroke="color" stroke-width="2" stroke-linecap="round"/>
      <path d="M8 18v2M16 18v2" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    </template>

    <!-- SUV -->
    <template v-else-if="name === 'suv'">
      <path d="M5 17h14M5 17a2 2 0 01-2-2v-4l1.5-4h13L19 11v4a2 2 0 01-2 2M5 17a2 2 0 00-2 2v1h4v-1a2 2 0 00-2-2zM19 17a2 2 0 012 2v1h-4v-1a2 2 0 012-2z" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <circle cx="7.5" cy="17" r="1.5" :fill="color"/>
      <circle cx="16.5" cy="17" r="1.5" :fill="color"/>
    </template>

    <!-- MPV -->
    <template v-else-if="name === 'mpv'">
      <path d="M5 17h14M5 17a2 2 0 01-2-2V9l2-5h12l2 5v6a2 2 0 01-2 2M5 17a2 2 0 00-2 2v1h4v-1a2 2 0 00-2-2zM19 17a2 2 0 012 2v1h-4v-1a2 2 0 012-2z" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <circle cx="7.5" cy="17" r="1.5" :fill="color"/>
      <circle cx="16.5" cy="17" r="1.5" :fill="color"/>
    </template>

    <!-- 电动/闪电 -->
    <template v-else-if="name === 'electric'">
      <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
    </template>

    <!-- 默认：圆圈 -->
    <template v-else>
      <circle cx="12" cy="12" r="10" :stroke="color" stroke-width="2"/>
    </template>
  </svg>
</template>

<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 24 },
  color: { type: String, default: 'currentColor' }
})
</script>
