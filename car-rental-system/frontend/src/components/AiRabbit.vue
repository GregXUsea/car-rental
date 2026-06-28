<template>
  <div class="ai-rabbit-wrap" :class="{ animate: animate }">
    <svg :width="size" :height="size" :viewBox="`0 0 ${vb} ${vb}`" fill="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="ringGrad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#667eea" stop-opacity="0.6"/>
          <stop offset="100%" stop-color="#764ba2" stop-opacity="0.3"/>
        </linearGradient>
        <linearGradient id="ringGrad2" x1="1" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#764ba2" stop-opacity="0.4"/>
          <stop offset="100%" stop-color="#667eea" stop-opacity="0.2"/>
        </linearGradient>
        <filter id="glow">
          <feGaussianBlur stdDeviation="1.5" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>

      <!-- 科技光环 1：外圈虚线弧 + 节点 -->
      <g class="ring ring-1">
        <circle :cx="cx" :cy="cy" r="34" fill="none" stroke="url(#ringGrad)" stroke-width="1" stroke-dasharray="12 5 4 5" opacity="0.55"/>
        <circle :cx="cx" :cy="cy - 34" r="1.8" :fill="glassColor" opacity="0.7"/>
        <circle :cx="cx + 34" :cy="cy" r="1.2" :fill="glassColor" opacity="0.5"/>
        <circle :cx="cx - 34" :cy="cy" r="1.2" :fill="glassColor" opacity="0.5"/>
      </g>
      <!-- 科技光环 2：中圈反向 + 方形节点 -->
      <g class="ring ring-2">
        <circle :cx="cx" :cy="cy" r="29" fill="none" stroke="url(#ringGrad2)" stroke-width="0.8" stroke-dasharray="3 8" opacity="0.4"/>
        <rect :x="cx + 28" :y="cy - 1.5" width="3" height="3" rx="0.5" :fill="glassColor" opacity="0.45" transform="rotate(45 29 29)"/>
        <rect :x="cx - 31" :y="cy - 1.5" width="3" height="3" rx="0.5" :fill="glassColor" opacity="0.35" transform="rotate(45 -29 29)"/>
      </g>
      <!-- 科技光环 3：内圈细线 + 小三角 -->
      <g class="ring ring-3">
        <circle :cx="cx" :cy="cy" r="24" fill="none" stroke="#667eea" stroke-width="0.5" stroke-dasharray="2 12" opacity="0.3"/>
        <polygon :points="`${cx} ${cy-25.5} ${cx-1.5} ${cy-23} ${cx+1.5} ${cy-23}`" :fill="glassColor" opacity="0.35"/>
        <polygon :points="`${cx} ${cy+25.5} ${cx-1.5} ${cy+23} ${cx+1.5} ${cy+23}`" :fill="glassColor" opacity="0.25"/>
      </g>

      <!-- 兔耳 -->
      <g class="ears">
        <ellipse :cx="cx - 10" :cy="cy - 20" rx="5.5" ry="14" :fill="earColor" opacity="0.9"/>
        <ellipse :cx="cx - 10" :cy="cy - 20" rx="3" ry="10" :fill="earInner"/>
        <ellipse :cx="cx + 10" :cy="cy - 20" rx="5.5" ry="14" :fill="earColor" opacity="0.9"/>
        <ellipse :cx="cx + 10" :cy="cy - 20" rx="3" ry="10" :fill="earInner"/>
      </g>

      <!-- 头部（兔脸：上窄下宽） -->
      <path :d="`M${cx} ${cy-18} Q${cx-22} ${cy-14} ${cx-18} ${cy+6} Q${cx-14} ${cy+18} ${cx} ${cy+20} Q${cx+14} ${cy+18} ${cx+18} ${cy+6} Q${cx+22} ${cy-14} ${cx} ${cy-18}Z`" :fill="headColor"/>
      <path :d="`M${cx} ${cy-18} Q${cx-22} ${cy-14} ${cx-18} ${cy+6} Q${cx-14} ${cy+18} ${cx} ${cy+20} Q${cx+14} ${cy+18} ${cx+18} ${cy+6} Q${cx+22} ${cy-14} ${cx} ${cy-18}Z`" :stroke="headStroke" stroke-width="1.5" fill="none" opacity="0.3"/>
      <!-- 腮帮（增加可爱感） -->
      <ellipse :cx="cx - 14" :cy="cy + 8" rx="6" ry="4" :fill="cheekColor" opacity="0.4"/>
      <ellipse :cx="cx + 14" :cy="cy + 8" rx="6" ry="4" :fill="cheekColor" opacity="0.4"/>

      <!-- 数码眼镜框 -->
      <rect :x="cx - 17" :y="cy - 6" width="14" height="10" rx="5" fill="none" :stroke="glassColor" stroke-width="2.5"/>
      <rect :x="cx + 3" :y="cy - 6" width="14" height="10" rx="5" fill="none" :stroke="glassColor" stroke-width="2.5"/>
      <line :x1="cx - 3" :y1="cy - 1" :x2="cx + 3" :y2="cy - 1" :stroke="glassColor" stroke-width="2"/>

      <!-- 心跳波动线 -->
      <g class="heartbeat" :filter="'url(#glow)'">
        <polyline class="pulse-line" :points="pulsePoints" fill="none" :stroke="pulseColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </g>

      <!-- 眼睛 -->
      <circle :cx="cx - 10" :cy="cy - 1" r="3" :fill="eyeColor"/>
      <circle :cx="cx - 10" :cy="cy - 1" r="1.5" fill="#fff"/>
      <circle :cx="cx + 10" :cy="cy - 1" r="3" :fill="eyeColor"/>
      <circle :cx="cx + 10" :cy="cy - 1" r="1.5" fill="#fff"/>

      <!-- 鼻子和嘴 -->
      <ellipse :cx="cx" :cy="cy + 4" rx="2" ry="1.5" :fill="noseColor"/>
      <path :d="`M${cx-2} ${cy+6} Q${cx} ${cy+9} ${cx+2} ${cy+6}`" fill="none" stroke="#ccc" stroke-width="1.2" stroke-linecap="round"/>

      <!-- 胡须 -->
      <line :x1="cx-22" :y1="cy+2" :x2="cx-12" :y2="cy+3" stroke="#ccc" stroke-width="0.8"/>
      <line :x1="cx-22" :y1="cy+6" :x2="cx-12" :y2="cy+5" stroke="#ccc" stroke-width="0.8"/>
      <line :x1="cx+12" :y1="cy+3" :x2="cx+22" :y2="cy+2" stroke="#ccc" stroke-width="0.8"/>
      <line :x1="cx+12" :y1="cy+5" :x2="cx+22" :y2="cy+6" stroke="#ccc" stroke-width="0.8"/>

      <!-- 招手的手臂 -->
      <g class="waving-arm" :transform-origin="`${cx + 18} ${cy + 10}`">
        <path :d="`M${cx+16} ${cy+10} Q${cx+24} ${cy+2} ${cx+22} ${cy-6}`" fill="none" :stroke="armColor" stroke-width="2.5" stroke-linecap="round"/>
        <!-- 手掌 -->
        <circle :cx="cx+22" :cy="cy-7" r="3" :fill="armColor"/>
        <!-- 手指 -->
        <line :x1="cx+20" :y1="cy-9" :x2="cx+19" :y2="cy-12" :stroke="armColor" stroke-width="1.5" stroke-linecap="round"/>
        <line :x1="cx+22" :y1="cy-10" :x2="cx+22" :y2="cy-13" :stroke="armColor" stroke-width="1.5" stroke-linecap="round"/>
        <line :x1="cx+24" :y1="cy-9" :x2="cx+25" :y2="cy-12" :stroke="armColor" stroke-width="1.5" stroke-linecap="round"/>
      </g>

      <!-- 科技光点 -->
      <circle :cx="cx - 14" :cy="cy - 3" r="1" :fill="glassColor" opacity="0.5" class="sparkle s1"/>
      <circle :cx="cx + 14" :cy="cy - 3" r="1" :fill="glassColor" opacity="0.5" class="sparkle s2"/>
      <circle :cx="cx" :cy="cy - 20" r="0.8" fill="#fff" opacity="0.4" class="sparkle s3"/>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: { type: [Number, String], default: 56 },
  variant: { type: String, default: 'default' }, // default, white
  animate: { type: Boolean, default: true }
})

const vb = 72
const cx = 36
const cy = 40

const isWhite = computed(() => props.variant === 'white')

const earColor = computed(() => isWhite.value ? '#fff' : '#667eea')
const earInner = computed(() => isWhite.value ? 'rgba(255,255,255,0.3)' : '#e0e7ff')
const headColor = computed(() => isWhite.value ? 'rgba(255,255,255,0.15)' : '#f0f0f5')
const headStroke = computed(() => isWhite.value ? 'rgba(255,255,255,0.4)' : '#667eea')
const glassColor = computed(() => isWhite.value ? '#fff' : '#667eea')
const eyeColor = computed(() => isWhite.value ? '#fff' : '#667eea')
const noseColor = computed(() => isWhite.value ? 'rgba(255,255,255,0.6)' : '#f5a0a0')
const cheekColor = computed(() => isWhite.value ? 'rgba(255,200,200,0.5)' : '#f8b4b4')
const armColor = computed(() => isWhite.value ? '#fff' : '#667eea')
const pulseColor = computed(() => isWhite.value ? 'rgba(100,230,255,0.9)' : '#00d4ff')

// 心跳波动线坐标（在眼镜框内水平穿过）
const pulsePoints = computed(() => {
  const y = cy - 1
  const x1 = cx - 16
  const x2 = cx + 16
  return `${x1},${y} ${x1+2},${y} ${x1+3},${y-3} ${x1+4},${y+3} ${x1+5},${y} ${x1+7},${y} ${x1+8},${y-5} ${x1+9},${y+4} ${x1+10},${y} ${x2-2},${y} ${x2},${y}`
})
</script>

<style scoped>
.ai-rabbit-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  animation: rabbitEntrance 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

/* 入场：由远到近 */
@keyframes rabbitEntrance {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.08); opacity: 1; }
  100% { transform: scale(1); opacity: 1; }
}

/* 旋转科技光环 */
.ring-1 { animation: spinCW 10s linear infinite; transform-origin: center; }
.ring-2 { animation: spinCCW 15s linear infinite; transform-origin: center; }
.ring-3 { animation: spinCW 7s linear infinite; transform-origin: center; }

@keyframes spinCW { to { transform: rotate(360deg); } }
@keyframes spinCCW { to { transform: rotate(-360deg); } }

/* 招手动画 */
.waving-arm {
  animation: wave 1.2s ease-in-out 0.4s infinite;
}
@keyframes wave {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-20deg); }
  75% { transform: rotate(15deg); }
}

/* 心跳波动线 */
.pulse-line {
  stroke-dasharray: 60;
  stroke-dashoffset: 60;
  animation: heartbeatDraw 2s ease-in-out 0.6s infinite;
}
@keyframes heartbeatDraw {
  0% { stroke-dashoffset: 60; opacity: 0.3; }
  30% { stroke-dashoffset: 0; opacity: 1; }
  70% { stroke-dashoffset: 0; opacity: 1; }
  100% { stroke-dashoffset: -60; opacity: 0.3; }
}

/* 光点闪烁 */
.sparkle { animation: sparkle 2s ease-in-out infinite; }
.s1 { animation-delay: 0s; }
.s2 { animation-delay: 0.7s; }
.s3 { animation-delay: 1.4s; }
@keyframes sparkle {
  0%, 100% { opacity: 0.2; r: 0.8; }
  50% { opacity: 0.8; r: 1.5; }
}
</style>
