<template>
  <div class="dashboard">
    <header class="header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">
            <svg width="36" height="36" viewBox="0 0 40 40" fill="none">
              <path d="M20 2L4 10v14c0 9 7 17 16 20 9-3 16-11 16-20V10L20 2z" fill="url(#dashLogo)"/>
              <path d="M13 20l5 5 9-9" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <defs><linearGradient id="dashLogo" x1="4" y1="2" x2="36" y2="36"><stop stop-color="#FFD700"/><stop offset="1" stop-color="#FFA500"/></linearGradient></defs>
            </svg>
          </div>
          <div class="logo-text">
            <span class="logo-name">御途租车</span>
            <span class="logo-sub">AI维护预测看板</span>
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
      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon total">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-num">{{ predictions.length }}</span>
            <span class="stat-label">车辆总数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon high">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-num high-text">{{ highRiskCount }}</span>
            <span class="stat-label">高风险车辆</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon mid">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-num mid-text">{{ midRiskCount }}</span>
            <span class="stat-label">中风险车辆</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon low">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          </div>
          <div class="stat-info">
            <span class="stat-num low-text">{{ lowRiskCount }}</span>
            <span class="stat-label">低风险车辆</span>
          </div>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-left">
          <h2 class="page-title">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/></svg>
            AI车辆维护预测看板
          </h2>
          <span v-if="cacheInfo.lastRefreshedAt" class="cache-time">
            上次刷新：{{ formatTime(cacheInfo.lastRefreshedAt) }}
          </span>
          <span v-if="cacheInfo.source" class="source-badge" :class="cacheInfo.aiMode ? 'source-ai' : 'source-local'">
            {{ cacheInfo.aiMode ? '🤖 AI模式 (DeepSeek)' : '⚙️ 本地模式' }}
          </span>
        </div>
        <div class="filter-right">
          <!-- AI/本地模式切换 -->
          <div class="mode-switch">
            <button :class="['mode-btn', { active: aiMode }]" @click="switchMode(true)" :disabled="loading">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l2 6h6l-5 4 2 6-5-4-5 4 2-6-5-4h6z"/></svg>
              AI模式
            </button>
            <button :class="['mode-btn', { active: !aiMode }]" @click="switchMode(false)" :disabled="loading">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
              本地模式
            </button>
          </div>
          <div class="filter-tags">
            <button :class="['tag', { active: filterRisk === '' }]" @click="filterRisk = ''">全部</button>
            <button :class="['tag', { active: filterRisk === '高' }]" @click="filterRisk = '高'">
              <span class="dot high-dot"></span>高风险
            </button>
            <button :class="['tag', { active: filterRisk === '中' }]" @click="filterRisk = '中'">
              <span class="dot mid-dot"></span>中风险
            </button>
            <button :class="['tag', { active: filterRisk === '低' }]" @click="filterRisk = '低'">
              <span class="dot low-dot"></span>低风险
            </button>
          </div>
          <button class="alert-btn" @click="alertVisible = true" v-if="alertTotal > 0">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            预警看板
            <span class="alert-count">{{ alertTotal }}</span>
          </button>
          <button class="refresh-btn" @click="loadAll" :disabled="loading">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :class="{ spinning: loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
            {{ loading ? '加载中...' : '刷新' }}
          </button>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading && predictions.length === 0" class="loading-wrap">
        <div class="loading-spinner"></div>
        <p>正在加载维护预测数据...</p>
      </div>

      <!-- 车辆列表（紧凑表格） -->
      <div v-else class="table-wrap">
        <table class="maint-table">
          <thead>
            <tr>
              <th class="col-risk sortable" @click="toggleSort('risk')">
                风险
                <span class="sort-icon" :class="{ active: sortKey === 'risk' }">{{ sortKey === 'risk' ? (sortOrder === 'desc' ? '↓' : '↑') : '↕' }}</span>
              </th>
              <th class="col-score sortable" @click="toggleSort('score')">
                评分
                <span class="sort-icon" :class="{ active: sortKey === 'score' }">{{ sortKey === 'score' ? (sortOrder === 'desc' ? '↓' : '↑') : '↕' }}</span>
              </th>
              <th class="col-car sortable" @click="toggleSort('name')">
                车辆信息
                <span class="sort-icon" :class="{ active: sortKey === 'name' }">{{ sortKey === 'name' ? (sortOrder === 'desc' ? '↓' : '↑') : '↕' }}</span>
              </th>
              <th class="col-mileage sortable" @click="toggleSort('mileage')">
                当前里程
                <span class="sort-icon" :class="{ active: sortKey === 'mileage' }">{{ sortKey === 'mileage' ? (sortOrder === 'desc' ? '↓' : '↑') : '↕' }}</span>
              </th>
              <th class="col-last sortable" @click="toggleSort('lastMaintain')">
                上次保养
                <span class="sort-icon" :class="{ active: sortKey === 'lastMaintain' }">{{ sortKey === 'lastMaintain' ? (sortOrder === 'desc' ? '↓' : '↑') : '↕' }}</span>
              </th>
              <th class="col-next sortable" @click="toggleSort('nextDate')">
                预测保养
                <span class="sort-icon" :class="{ active: sortKey === 'nextDate' }">{{ sortKey === 'nextDate' ? (sortOrder === 'desc' ? '↓' : '↑') : '↕' }}</span>
              </th>
              <th class="col-type">保养类型</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredPredictions" :key="item.car.id"
                :class="'row-' + getRiskLevel(item.prediction)"
                @click="openDetail(item)">
              <td class="col-risk">
                <span class="risk-tag" :class="'risk-' + getRiskLevel(item.prediction)">
                  {{ getRiskLevel(item.prediction) }}
                </span>
              </td>
              <td class="col-score">
                <div class="score-ring" :class="'score-' + getScoreLevel(item.prediction)">
                  <span>{{ getRiskScore(item.prediction) }}</span>
                </div>
              </td>
              <td class="col-car">
                <div class="car-cell">
                  <img :src="item.car.image" :alt="item.car.brand" class="car-thumb"
                       :data-brand="item.car.brand" :data-category="item.car.category"
                       @error="handleImgError($event)" />
                  <div>
                    <div class="car-name">{{ item.car.brand }} {{ item.car.model }}</div>
                    <div class="car-meta">{{ item.car.category }} · {{ item.car.color }}</div>
                  </div>
                </div>
              </td>
              <td class="col-mileage">
                <span class="mileage-num">{{ item.car.mileage?.toLocaleString() }}</span>
                <span class="mileage-unit">km</span>
              </td>
              <td class="col-last">{{ item.car.lastMaintainDate || '无记录' }}</td>
              <td class="col-next">
                {{ item.prediction?.nextMaintenanceDate || '-' }}
                <span v-if="item.prediction?.source === 'AI' || (!item.prediction?.source && cacheInfo.aiMode)" class="row-source-ai" title="DeepSeek AI 生成">🤖</span>
              </td>
              <td class="col-type">
                {{ item.prediction?.nextMaintenanceType || '-' }}
                <span v-if="item.prediction?.similarCases?.length" class="rag-count" :title="'RAG检索到 ' + item.prediction.similarCases.length + ' 个相似案例'">
                  🔍{{ item.prediction.similarCases.length }}
                </span>
              </td>
              <td class="col-action" @click.stop>
                <button class="detail-btn" @click="openDetail(item)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="!loading && filteredPredictions.length === 0 && predictions.length > 0" class="empty">
        <p>当前筛选条件下无车辆</p>
      </div>
    </main>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailCar ? detailCar.brand + ' ' + detailCar.model + ' - 维护详情' : ''"
               width="680px" class="detail-dialog" destroy-on-close>
      <div v-if="detailItem" class="detail-content">
        <!-- 车辆概况 -->
        <div class="detail-car-info">
          <img :src="detailCar.image" class="detail-car-img"
               :data-brand="detailCar.brand" :data-category="detailCar.category"
               @error="handleImgError($event)" />
          <div class="detail-car-text">
            <h3>{{ detailCar.brand }} {{ detailCar.model }}</h3>
            <div class="detail-tags">
              <span class="dtag">{{ detailCar.category }}</span>
              <span class="dtag">{{ detailCar.color }}</span>
              <span class="dtag">{{ detailCar.seats }}座</span>
              <span :class="['status-tag', 's' + detailCar.status]">{{ statusText(detailCar.status) }}</span>
              <span v-if="cacheInfo.aiMode" class="dtag dtag-ai">🤖 AI预测</span>
              <span v-else class="dtag dtag-local">⚙️ 本地规则</span>
            </div>
          </div>
        </div>

        <!-- 风险评分 -->
        <div class="detail-risk-section">
          <div class="risk-score-big" :class="'score-' + getScoreLevel(detailItem.prediction)">
            {{ getRiskScore(detailItem.prediction) }}
          </div>
          <div class="risk-score-info">
            <div class="risk-score-label">风险评分</div>
            <div class="risk-level-text" :class="'risk-text-' + getRiskLevel(detailItem.prediction)">
              {{ getRiskLevel(detailItem.prediction) }}风险
            </div>
            <div class="risk-bar-detail">
              <div class="risk-bar-fill" :class="'risk-' + getRiskLevel(detailItem.prediction)"
                   :style="{ width: riskWidth(detailItem.prediction) }"></div>
            </div>
          </div>
          <div class="detail-stats-grid">
            <div class="detail-stat">
              <span class="ds-val">{{ detailCar.mileage?.toLocaleString() }}km</span>
              <span class="ds-label">当前里程</span>
            </div>
            <div class="detail-stat">
              <span class="ds-val">{{ detailItem.prediction?.mileageSinceLastMaintain?.toLocaleString() || '-' }}km</span>
              <span class="ds-label">距上次保养</span>
            </div>
            <div class="detail-stat">
              <span class="ds-val">{{ detailItem.prediction?.daysSinceLastMaintain || '-' }}天</span>
              <span class="ds-label">距上次保养</span>
            </div>
          </div>
        </div>

        <!-- 预测信息 -->
        <div class="detail-pred-grid">
          <div class="dp-item">
            <span class="dp-label">预测保养日期</span>
            <span class="dp-value">{{ detailItem.prediction?.nextMaintenanceDate || '-' }}</span>
          </div>
          <div class="dp-item">
            <span class="dp-label">保养类型</span>
            <span class="dp-value">{{ detailItem.prediction?.nextMaintenanceType || '-' }}</span>
          </div>
        </div>

        <!-- 保养建议 -->
        <div class="detail-suggestions" v-if="detailItem.prediction?.suggestions?.length">
          <div class="ds-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18h6M10 22h4M12 2a7 7 0 017 7c0 2.38-1.19 4.47-3 5.74V17a1 1 0 01-1 1h-6a1 1 0 01-1-1v-2.26C6.19 13.47 5 11.38 5 9a7 7 0 017-7z"/></svg>
            保养建议
            <span v-if="cacheInfo.aiMode" class="ai-badge">AI 生成</span>
            <span v-else class="local-badge">规则引擎</span>
          </div>
          <ul>
            <li v-for="(s, i) in detailItem.prediction.suggestions" :key="i">{{ s }}</li>
          </ul>
        </div>

        <!-- AI相似案例总结 -->
        <div class="detail-rag-summary" v-if="detailItem.prediction?.similarCasesSummary">
          <div class="ds-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
            RAG 知识库检索总结
          </div>
          <p class="rag-summary-text">{{ detailItem.prediction.similarCasesSummary }}</p>
        </div>

        <!-- RAG相似案例 -->
        <div class="detail-rag" v-if="detailItem.prediction?.similarCases?.length">
          <div class="ds-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            RAG相似案例 ({{ detailItem.prediction.similarCases.length }})
          </div>
          <div class="rag-list">
            <div v-for="(c, i) in detailItem.prediction.similarCases" :key="i" class="rag-item">
              <span class="rag-sim">{{ c.similarity }}%</span>
              <div class="rag-detail">
                <strong>{{ c.carName }}</strong> · {{ c.maintenanceType }}
                <span class="rag-meta">{{ c.mileage }}km · {{ c.date }} · ¥{{ c.cost }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 预警看板弹窗 -->
    <el-dialog v-model="alertVisible" title="⚠️ 预警看板" width="900px" :close-on-click-modal="true" class="alert-dialog">
      <div class="alert-content">
        <!-- 预警统计 -->
        <div class="alert-stats">
          <div class="alert-stat-card critical">
            <div class="asc-num">{{ alertCritical.length }}</div>
            <div class="asc-label">🚨 立即维修</div>
            <div class="asc-desc">高风险 + 超期未保养</div>
          </div>
          <div class="alert-stat-card warning">
            <div class="asc-num">{{ alertWarning.length }}</div>
            <div class="asc-label">⚠️ 尽快安排</div>
            <div class="asc-desc">中风险或里程过高</div>
          </div>
          <div class="alert-stat-card info">
            <div class="asc-num">{{ alertInfo.length }}</div>
            <div class="asc-label">📋 建议关注</div>
            <div class="asc-desc">接近保养周期</div>
          </div>
        </div>

        <!-- 立即维修 -->
        <div v-if="alertCritical.length" class="alert-section critical-section">
          <h3 class="alert-section-title">🚨 立即维修 — 需要马上处理</h3>
          <div v-for="item in alertCritical" :key="item.car.id" class="alert-card critical-card">
            <div class="alert-card-header">
              <div class="alert-car-info">
                <img :src="item.car.image" class="alert-car-img" :data-brand="item.car.brand" :data-category="item.car.category" @error="handleImgError($event)" />
                <div>
                  <span class="alert-car-name">{{ item.car.brand }} {{ item.car.model }}</span>
                  <span class="alert-car-meta">{{ item.car.category }} · {{ item.car.mileage?.toLocaleString() }}km</span>
                </div>
              </div>
              <div class="alert-score-badge critical-badge">{{ getRiskScore(item.prediction) }}分</div>
            </div>
            <div class="alert-reasons">
              <span v-for="(r, i) in getAlertReasons(item, 'critical')" :key="i" class="alert-reason-tag critical-reason">{{ r }}</span>
            </div>
            <div class="alert-suggestion" v-if="item.prediction?.suggestions?.length">
              💡 {{ item.prediction.suggestions[0] }}
            </div>
          </div>
        </div>

        <!-- 尽快安排 -->
        <div v-if="alertWarning.length" class="alert-section warning-section">
          <h3 class="alert-section-title">⚠️ 尽快安排 — 建议近期处理</h3>
          <div v-for="item in alertWarning" :key="item.car.id" class="alert-card warning-card">
            <div class="alert-card-header">
              <div class="alert-car-info">
                <img :src="item.car.image" class="alert-car-img" :data-brand="item.car.brand" :data-category="item.car.category" @error="handleImgError($event)" />
                <div>
                  <span class="alert-car-name">{{ item.car.brand }} {{ item.car.model }}</span>
                  <span class="alert-car-meta">{{ item.car.category }} · {{ item.car.mileage?.toLocaleString() }}km</span>
                </div>
              </div>
              <div class="alert-score-badge warning-badge">{{ getRiskScore(item.prediction) }}分</div>
            </div>
            <div class="alert-reasons">
              <span v-for="(r, i) in getAlertReasons(item, 'warning')" :key="i" class="alert-reason-tag warning-reason">{{ r }}</span>
            </div>
            <div class="alert-suggestion" v-if="item.prediction?.suggestions?.length">
              💡 {{ item.prediction.suggestions[0] }}
            </div>
          </div>
        </div>

        <!-- 建议关注 -->
        <div v-if="alertInfo.length" class="alert-section info-section">
          <h3 class="alert-section-title">📋 建议关注 — 接近保养周期</h3>
          <div v-for="item in alertInfo" :key="item.car.id" class="alert-card info-card">
            <div class="alert-card-header">
              <div class="alert-car-info">
                <img :src="item.car.image" class="alert-car-img" :data-brand="item.car.brand" :data-category="item.car.category" @error="handleImgError($event)" />
                <div>
                  <span class="alert-car-name">{{ item.car.brand }} {{ item.car.model }}</span>
                  <span class="alert-car-meta">{{ item.car.category }} · {{ item.car.mileage?.toLocaleString() }}km</span>
                </div>
              </div>
              <div class="alert-score-badge info-badge">{{ getRiskScore(item.prediction) }}分</div>
            </div>
            <div class="alert-reasons">
              <span v-for="(r, i) in getAlertReasons(item, 'info')" :key="i" class="alert-reason-tag info-reason">{{ r }}</span>
            </div>
          </div>
        </div>

        <!-- 无预警 -->
        <div v-if="!alertCritical.length && !alertWarning.length && !alertInfo.length" class="alert-empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="1.5"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <h3>🎉 全部正常</h3>
          <p>当前没有需要预警的车辆</p>
        </div>
      </div>
    </el-dialog>

    <footer class="footer">
      <div class="footer-bottom">
        <p>© 2026 御途租车 YUTU CAR RENTAL · AI维护预测看板</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const predictions = ref([])
const loading = ref(false)
const filterRisk = ref('')
const sortKey = ref('score')       // 当前排序字段
const sortOrder = ref('desc')      // desc=降序, asc=升序
const cacheInfo = reactive({ lastRefreshedAt: null, source: '本地', aiMode: false })
const aiMode = ref(false)

// 详情弹窗
const detailVisible = ref(false)
const detailItem = ref(null)

// ====== 预警看板 ======
const alertVisible = ref(false)

// 预警分级逻辑
const alertCritical = computed(() => {
  if (!predictions.value || !predictions.value.length) return []
  return predictions.value.filter(p => {
    const score = getRiskScore(p.prediction)
    const mileage = p.car?.mileage || 0
    return (score !== '-' && score >= 50 && (mileage > 60000 || score >= 70))
  })
})

const alertWarning = computed(() => {
  if (!predictions.value || !predictions.value.length) return []
  const critIds = new Set(alertCritical.value.map(p => p.car?.id))
  return predictions.value.filter(p => {
    if (critIds.has(p.car?.id)) return false
    const score = getRiskScore(p.prediction)
    const mileage = p.car?.mileage || 0
    const risk = getRiskLevel(p.prediction)
    return (score !== '-' && score >= 25 && score < 50) ||
           (risk === '高' && mileage > 40000) ||
           (mileage > 80000)
  })
})

const alertInfo = computed(() => {
  if (!predictions.value || !predictions.value.length) return []
  const skipIds = new Set([...alertCritical.value, ...alertWarning.value].map(p => p.car?.id))
  return predictions.value.filter(p => {
    if (skipIds.has(p.car?.id)) return false
    const mileage = p.car?.mileage || 0
    const score = getRiskScore(p.prediction)
    const lastDate = p.car?.lastMaintainDate
    let overDue = false
    if (lastDate) {
      const days = Math.floor((Date.now() - new Date(lastDate).getTime()) / 86400000)
      overDue = days > 180
    }
    return overDue || (mileage > 30000 && score !== '-' && score >= 15)
  })
})

const alertTotal = computed(() => alertCritical.value.length + alertWarning.value.length + alertInfo.value.length)

// 预警原因生成
const getAlertReasons = (item, level) => {
  const reasons = []
  const score = getRiskScore(item.prediction)
  const mileage = item.car.mileage || 0
  const lastDate = item.car.lastMaintainDate
  const risk = getRiskLevel(item.prediction)

  if (score >= 70) reasons.push('风险评分极高')
  else if (score >= 50) reasons.push('高风险')
  else if (score >= 25) reasons.push('中风险')

  if (mileage > 80000) reasons.push('里程超8万km')
  else if (mileage > 60000) reasons.push('里程超6万km')
  else if (mileage > 40000) reasons.push('里程超4万km')
  else if (mileage > 30000) reasons.push('里程超3万km')

  if (lastDate) {
    const days = Math.floor((Date.now() - new Date(lastDate).getTime()) / 86400000)
    if (days > 365) reasons.push('超1年未保养')
    else if (days > 180) reasons.push('超6个月未保养')
    else if (days > 90) reasons.push('超3个月未保养')
  } else {
    reasons.push('无保养记录')
  }

  if (item.car.maintainCount >= 5) reasons.push('维修频繁')
  else if (item.car.maintainCount >= 3) reasons.push('维修较多')

  if (item.car.status === 3) reasons.push('当前维护中')

  return reasons
}
const detailCar = ref(null)

const highRiskCount = computed(() => predictions.value.filter(p => getRiskLevel(p.prediction) === '高').length)
const midRiskCount = computed(() => predictions.value.filter(p => getRiskLevel(p.prediction) === '中').length)
const lowRiskCount = computed(() => predictions.value.filter(p => getRiskLevel(p.prediction) === '低').length)

const filteredPredictions = computed(() => {
  let list = filterRisk.value
    ? predictions.value.filter(p => getRiskLevel(p.prediction) === filterRisk.value)
    : [...predictions.value]
  list.sort((a, b) => {
    const dir = sortOrder.value === 'desc' ? 1 : -1
    switch (sortKey.value) {
      case 'risk': {
        const order = { '高': 3, '中': 2, '低': 1 }
        return dir * ((order[getRiskLevel(b.prediction)] || 0) - (order[getRiskLevel(a.prediction)] || 0))
      }
      case 'score': {
        const sa = getRiskScore(a.prediction), sb = getRiskScore(b.prediction)
        return dir * ((sb === '-' ? -1 : sb) - (sa === '-' ? -1 : sa))
      }
      case 'name':
        return dir * (a.car.brand + a.car.model).localeCompare(b.car.brand + b.car.model)
      case 'mileage':
        return dir * ((b.car.mileage || 0) - (a.car.mileage || 0))
      case 'lastMaintain':
        return dir * ((b.car.lastMaintainDate || '').localeCompare(a.car.lastMaintainDate || ''))
      case 'nextDate':
        return dir * ((b.prediction?.nextMaintenanceDate || '').localeCompare(a.prediction?.nextMaintenanceDate || ''))
      default:
        return 0
    }
  })
  return list
})

const toggleSort = (key) => {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
  } else {
    sortKey.value = key
    sortOrder.value = 'desc'
  }
}

const getRiskLevel = (pred) => {
  if (!pred || !pred.riskLevel) return '中'
  const level = String(pred.riskLevel)
  if (level.includes('高')) return '高'
  if (level.includes('低')) return '低'
  return '中'
}

const getRiskScore = (pred) => {
  if (!pred || pred.riskScore == null) return '-'
  return pred.riskScore
}

const getScoreLevel = (pred) => {
  const score = getRiskScore(pred)
  if (score === '-') return 'mid'
  if (score >= 50) return 'high'
  if (score >= 25) return 'mid'
  return 'low'
}

const riskWidth = (pred) => {
  const score = getRiskScore(pred)
  if (score === '-') return '50%'
  return Math.min(100, Math.max(5, score)) + '%'
}

const statusText = (s) => ({ 0: '空闲', 1: '已租出', 2: '已预约', 3: '维护中' }[s] || '未知')

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const openDetail = (item) => {
  detailItem.value = item
  detailCar.value = item.car
  detailVisible.value = true
}

const loadAll = async () => {
  loading.value = true
  try {
    // 并行请求预测数据和缓存状态
    const [allRes, statusRes] = await Promise.all([
      api.get('/ai/maintenance/all'),
      api.get('/ai/maintenance/cache-status')
    ])
    if (allRes.code === 200) {
      predictions.value = allRes.data
      cacheInfo.lastRefreshedAt = new Date()
    }
    if (statusRes.code === 200) {
      cacheInfo.source = statusRes.data.source || '本地'
      cacheInfo.aiMode = statusRes.data.aiMode !== false
      aiMode.value = cacheInfo.aiMode
    }
    if (allRes.code === 200) {
      ElMessage.success(`已加载 ${allRes.data.length} 辆车的预测数据`)
    }
  } catch (e) {
    console.error('加载维护预测失败', e)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 切换AI/本地模式
const switchMode = async (useAi) => {
  if (aiMode.value === useAi) return
  aiMode.value = useAi
  loading.value = true
  try {
    // 先切换模式
    await api.post('/ai/maintenance/mode', { aiMode: useAi })
    // 再刷新数据（使用新模式）
    const mode = useAi ? 'ai' : 'local'
    const res = await api.post(`/ai/maintenance/refresh?mode=${mode}`)
    if (res.code === 200) {
      cacheInfo.source = res.data.source || (useAi ? 'AI' : '本地')
      cacheInfo.aiMode = useAi
      // 重新加载全量数据
      await loadAll()
      ElMessage.success(useAi ? '已切换到 AI 模式 (DeepSeek API)' : '已切换到本地模式 (规则引擎)')
    }
  } catch (e) {
    ElMessage.error('切换模式失败: ' + (e.message || '请稍后重试'))
    aiMode.value = !useAi
  } finally {
    loading.value = false
  }
}

// 车辆图片错误回退SVG
const carStyles = {
  '丰田': { body: '#c0392b', roof: '#a93226', wheel: '#2c3e50' },
  '本田': { body: '#2980b9', roof: '#2471a3', wheel: '#2c3e50' },
  '大众': { body: '#27ae60', roof: '#229954', wheel: '#2c3e50' },
  '宝马': { body: '#2c3e50', roof: '#1a252f', wheel: '#1c1c1c' },
  '奔驰': { body: '#7f8c8d', roof: '#6c7a7d', wheel: '#1c1c1c' },
  '别克': { body: '#8e44ad', roof: '#7d3c98', wheel: '#2c3e50' },
  '比亚迪': { body: '#16a085', roof: '#138d75', wheel: '#2c3e50' },
  '理想': { body: '#e67e22', roof: '#d35400', wheel: '#2c3e50' },
  '奥迪': { body: '#2c3e50', roof: '#1c2833', wheel: '#1c1c1c' },
  '特斯拉': { body: '#ecf0f1', roof: '#bdc3c7', wheel: '#1c1c1c' },
  '蔚来': { body: '#2980b9', roof: '#2471a3', wheel: '#2c3e50' },
}

const handleImgError = (e) => {
  const brand = e.target.dataset.brand || ''
  const category = e.target.dataset.category || ''
  const s = carStyles[brand] || { body: '#667eea', roof: '#5a67d8', wheel: '#2c3e50' }
  const shapeIdx = category === 'SUV' ? 1 : category === 'MPV' ? 2 : 0
  const shapes = [
    'M15,22 Q15,16 22,16 L30,12 L50,12 L58,16 Q65,16 65,22 L65,24 L15,24Z',
    'M15,22 Q15,15 22,15 L28,8 L52,8 L58,15 Q65,15 65,22 L65,24 L15,24Z',
    'M13,22 Q13,14 20,14 L25,6 L55,6 L60,14 Q67,14 67,22 L67,24 L13,24Z',
  ]
  const idx = Math.floor(Math.random() * 1000)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 32">
    <defs><linearGradient id="b${idx}" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="${s.body}"/><stop offset="100%" stop-color="${s.roof}"/></linearGradient></defs>
    <rect fill="#e8f4f8" width="80" height="32" rx="3"/>
    <rect fill="#666" y="25" width="80" height="7"/>
    <path d="${shapes[shapeIdx]}" fill="url(#b${idx})" stroke="${s.roof}" stroke-width="0.3"/>
    <circle cx="25" cy="23" r="4" fill="${s.wheel}"/><circle cx="25" cy="23" r="2" fill="#666"/>
    <circle cx="55" cy="23" r="4" fill="${s.wheel}"/><circle cx="55" cy="23" r="2" fill="#666"/>
  </svg>`
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent(svg)
}

onMounted(() => { loadAll() })
</script>

<style scoped>
.dashboard { min-height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; }

/* Header */
.header { background: #fff; position: sticky; top: 0; z-index: 100; box-shadow: 0 1px 0 rgba(0,0,0,0.06); }
.header-content { max-width: 1400px; margin: 0 auto; padding: 0 24px; height: 64px; display: flex; align-items: center; justify-content: space-between; }
.logo { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.logo-icon { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; }
.logo-text { display: flex; flex-direction: column; }
.logo-name { font-size: 18px; font-weight: 700; color: #1a1a2e; letter-spacing: 2px; }
.logo-sub { font-size: 9px; color: #999; letter-spacing: 1px; }
.nav-right { display: flex; align-items: center; gap: 8px; }
.nav-link { display: flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 8px; color: #666; text-decoration: none; font-size: 14px; cursor: pointer; transition: all 0.2s; }
.nav-link:hover { background: #f5f7fa; color: #333; }

.main { max-width: 1400px; margin: 0 auto; padding: 24px; flex: 1; width: 100%; box-sizing: border-box; }

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 14px; padding: 18px; display: flex; align-items: center; gap: 14px; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.stat-icon { width: 44px; height: 44px; border-radius: 11px; display: flex; align-items: center; justify-content: center; }
.stat-icon.total { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.high { background: linear-gradient(135deg, #f56c6c, #e74c3c); }
.stat-icon.mid { background: linear-gradient(135deg, #e6a23c, #f39c12); }
.stat-icon.low { background: linear-gradient(135deg, #67c23a, #27ae60); }
.stat-info { display: flex; flex-direction: column; }
.stat-num { font-size: 26px; font-weight: 700; color: #1a1a2e; }
.high-text { color: #f56c6c; }
.mid-text { color: #e6a23c; }
.low-text { color: #67c23a; }
.stat-label { font-size: 13px; color: #999; margin-top: 2px; }

/* Filter */
.filter-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; padding: 14px 20px; background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); flex-wrap: wrap; gap: 12px; }
.filter-left { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.page-title { display: flex; align-items: center; gap: 8px; font-size: 17px; font-weight: 700; color: #1a1a2e; }

/* 预警按钮 */
.alert-btn { display: flex; align-items: center; gap: 6px; padding: 7px 14px; background: linear-gradient(135deg, #f56c6c, #e74c3c); color: #fff; border: none; border-radius: 7px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; animation: alertPulse 2s ease-in-out infinite; }
.alert-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(245,108,108,0.4); }
.alert-count { background: #fff; color: #f56c6c; font-size: 11px; font-weight: 700; padding: 1px 7px; border-radius: 10px; min-width: 18px; text-align: center; }
@keyframes alertPulse { 0%,100%{box-shadow:0 0 0 0 rgba(245,108,108,0.3)} 50%{box-shadow:0 0 0 6px rgba(245,108,108,0)} }

/* 预警看板弹窗 */
.alert-content { max-height: 65vh; overflow-y: auto; }
.alert-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
.alert-stat-card { padding: 16px; border-radius: 12px; text-align: center; }
.alert-stat-card.critical { background: linear-gradient(135deg, #fef0f0, #fde2e2); border: 1px solid #fbc4c4; }
.alert-stat-card.warning { background: linear-gradient(135deg, #fdf6ec, #faecd8); border: 1px solid #f5dab1; }
.alert-stat-card.info { background: linear-gradient(135deg, #ecf5ff, #d9ecff); border: 1px solid #b3d8ff; }
.asc-num { font-size: 28px; font-weight: 800; }
.critical .asc-num { color: #f56c6c; }
.warning .asc-num { color: #e6a23c; }
.info .asc-num { color: #409eff; }
.asc-label { font-size: 14px; font-weight: 600; color: #333; margin: 4px 0; }
.asc-desc { font-size: 11px; color: #999; }

.alert-section { margin-bottom: 24px; }
.alert-section-title { font-size: 15px; font-weight: 700; color: #333; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid #f0f0f0; }
.critical-section .alert-section-title { border-color: #f56c6c; }
.warning-section .alert-section-title { border-color: #e6a23c; }
.info-section .alert-section-title { border-color: #409eff; }

.alert-card { background: #fff; border-radius: 10px; padding: 14px 16px; margin-bottom: 10px; border-left: 4px solid #e5e7eb; transition: box-shadow 0.2s; }
.alert-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.critical-card { border-left-color: #f56c6c; background: #fffafa; }
.warning-card { border-left-color: #e6a23c; background: #fffbf5; }
.info-card { border-left-color: #409eff; background: #f8fbff; }

.alert-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.alert-car-info { display: flex; align-items: center; gap: 10px; }
.alert-car-img { width: 40px; height: 28px; border-radius: 4px; object-fit: cover; background: #f0f2f5; }
.alert-car-name { font-size: 14px; font-weight: 600; color: #333; display: block; }
.alert-car-meta { font-size: 12px; color: #999; }

.alert-score-badge { font-size: 13px; font-weight: 700; padding: 4px 10px; border-radius: 8px; }
.critical-badge { background: #fef0f0; color: #f56c6c; }
.warning-badge { background: #fdf6ec; color: #e6a23c; }
.info-badge { background: #ecf5ff; color: #409eff; }

.alert-reasons { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 6px; }
.alert-reason-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.critical-reason { background: #fef0f0; color: #f56c6c; }
.warning-reason { background: #fdf6ec; color: #e6a23c; }
.info-reason { background: #ecf5ff; color: #409eff; }

.alert-suggestion { font-size: 12px; color: #666; line-height: 1.5; padding: 6px 10px; background: #f8f9fb; border-radius: 6px; margin-top: 6px; }

.alert-empty { text-align: center; padding: 40px; }
.alert-empty h3 { color: #67c23a; margin: 12px 0 6px; }
.alert-empty p { color: #999; font-size: 14px; }
.cache-time { font-size: 12px; color: #999; background: #f5f7fa; padding: 4px 10px; border-radius: 6px; }
.filter-right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.source-badge { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; margin-left: 8px; }
.source-ai { background: linear-gradient(135deg, #e0e7ff, #c7d2fe); color: #667eea; }
.source-local { background: #f0f9eb; color: #67c23a; }
.mode-switch { display: flex; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.mode-btn { display: flex; align-items: center; gap: 4px; padding: 6px 12px; background: #fff; border: none; font-size: 12px; font-weight: 500; color: #666; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.mode-btn:hover { background: #f5f7fa; }
.mode-btn.active { background: #667eea; color: #fff; }
.mode-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.filter-tags { display: flex; gap: 6px; }
.tag { display: flex; align-items: center; gap: 5px; padding: 6px 14px; border-radius: 7px; border: 1px solid #e5e7eb; background: #fff; font-size: 13px; color: #666; cursor: pointer; transition: all 0.2s; }
.tag:hover { border-color: #667eea; color: #667eea; }
.tag.active { background: #667eea; color: #fff; border-color: #667eea; }
.dot { width: 7px; height: 7px; border-radius: 50%; }
.high-dot { background: #f56c6c; }
.mid-dot { background: #e6a23c; }
.low-dot { background: #67c23a; }
.tag.active .dot { background: #fff; }
.refresh-btn { display: flex; align-items: center; gap: 6px; padding: 7px 18px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; border-radius: 7px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.refresh-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(102,126,234,0.4); }
.refresh-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.spinning { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* Loading */
.loading-wrap { text-align: center; padding: 60px 0; }
.loading-spinner { width: 40px; height: 40px; border: 3px solid #e5e7eb; border-top-color: #667eea; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
.loading-wrap p { color: #666; font-size: 14px; }

/* Table */
.table-wrap { background: #fff; border-radius: 14px; box-shadow: 0 2px 12px rgba(0,0,0,0.04); overflow: hidden; }
.maint-table { width: 100%; border-collapse: collapse; }
.maint-table thead { background: #f8f9fb; }
.maint-table th { padding: 12px 14px; font-size: 12px; font-weight: 600; color: #999; text-align: left; border-bottom: 1px solid #f0f0f0; white-space: nowrap; user-select: none; }
.maint-table th.sortable { cursor: pointer; transition: color 0.15s; }
.maint-table th.sortable:hover { color: #667eea; background: #f0f2ff; }
.sort-icon { font-size: 11px; margin-left: 4px; color: #ccc; transition: color 0.15s; }
.sort-icon.active { color: #667eea; font-weight: 700; }
.maint-table td { padding: 12px 14px; font-size: 13px; color: #333; border-bottom: 1px solid #f5f5f5; }
.maint-table tbody tr { cursor: pointer; transition: background 0.15s; }
.maint-table tbody tr:hover { background: #f8f9fb; }
.maint-table tbody tr:last-child td { border-bottom: none; }

/* Row risk highlight */
.row-高 { border-left: 3px solid #f56c6c; }
.row-中 { border-left: 3px solid #e6a23c; }
.row-低 { border-left: 3px solid #67c23a; }

/* Risk tag */
.risk-tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 11px; font-weight: 700; color: #fff; }
.risk-tag.risk-高 { background: linear-gradient(135deg, #f56c6c, #e74c3c); }
.risk-tag.risk-中 { background: linear-gradient(135deg, #e6a23c, #f39c12); }
.risk-tag.risk-低 { background: linear-gradient(135deg, #67c23a, #27ae60); }

/* Score ring */
.score-ring { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; color: #fff; }
.score-ring.score-high { background: linear-gradient(135deg, #f56c6c, #e74c3c); }
.score-ring.score-mid { background: linear-gradient(135deg, #e6a23c, #f39c12); }
.score-ring.score-low { background: linear-gradient(135deg, #67c23a, #27ae60); }

/* Car cell */
.car-cell { display: flex; align-items: center; gap: 10px; }
.car-thumb { width: 48px; height: 32px; border-radius: 6px; object-fit: cover; background: #f5f7fa; flex-shrink: 0; }
.car-name { font-size: 13px; font-weight: 600; color: #1a1a2e; white-space: nowrap; }
.car-meta { font-size: 11px; color: #999; }

/* Mileage */
.mileage-num { font-size: 14px; font-weight: 700; color: #1a1a2e; }
.mileage-unit { font-size: 11px; color: #999; margin-left: 2px; }

/* Detail button */
.detail-btn { display: flex; align-items: center; gap: 4px; padding: 5px 12px; background: #f0f2ff; border: 1px solid #d4d8ff; border-radius: 6px; font-size: 12px; color: #667eea; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.detail-btn:hover { background: #667eea; color: #fff; border-color: #667eea; }

/* Empty */
.empty { text-align: center; padding: 60px 0; color: #999; }

/* Detail dialog */
.detail-content { display: flex; flex-direction: column; gap: 20px; }
.detail-car-info { display: flex; align-items: center; gap: 16px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.detail-car-img { width: 100px; height: 60px; border-radius: 8px; object-fit: cover; background: #f5f7fa; }
.detail-car-text h3 { font-size: 18px; font-weight: 700; color: #1a1a2e; margin-bottom: 6px; }
.detail-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.dtag { padding: 2px 8px; background: #f5f7fa; border-radius: 4px; font-size: 11px; color: #666; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.s0 { background: #e8f5e9; color: #27ae60; }
.s1 { background: #fdecea; color: #f56c6c; }
.s2 { background: #fff8e1; color: #e6a23c; }
.s3 { background: #f5f5f5; color: #909399; }

/* Risk section */
.detail-risk-section { display: flex; align-items: center; gap: 20px; padding: 16px; background: #f8f9fb; border-radius: 12px; }
.risk-score-big { width: 64px; height: 64px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 800; color: #fff; flex-shrink: 0; }
.risk-score-big.score-high { background: linear-gradient(135deg, #f56c6c, #e74c3c); }
.risk-score-big.score-mid { background: linear-gradient(135deg, #e6a23c, #f39c12); }
.risk-score-big.score-low { background: linear-gradient(135deg, #67c23a, #27ae60); }
.risk-score-info { flex: 1; }
.risk-score-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.risk-level-text { font-size: 18px; font-weight: 700; margin-bottom: 6px; }
.risk-text-高 { color: #f56c6c; }
.risk-text-中 { color: #e6a23c; }
.risk-text-低 { color: #67c23a; }
.risk-bar-detail { height: 6px; background: #e8e8e8; border-radius: 3px; overflow: hidden; }
.risk-bar-fill { height: 100%; border-radius: 3px; transition: width 0.6s ease; }
.risk-bar-fill.risk-高 { background: linear-gradient(90deg, #f56c6c, #e74c3c); }
.risk-bar-fill.risk-中 { background: linear-gradient(90deg, #e6a23c, #f39c12); }
.risk-bar-fill.risk-低 { background: linear-gradient(90deg, #67c23a, #27ae60); }

.detail-stats-grid { display: flex; gap: 16px; }
.detail-stat { display: flex; flex-direction: column; align-items: center; }
.ds-val { font-size: 15px; font-weight: 700; color: #1a1a2e; }
.ds-label { font-size: 11px; color: #999; }

/* Pred grid */
.detail-pred-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.dp-item { display: flex; flex-direction: column; gap: 4px; }
.dp-label { font-size: 12px; color: #999; }
.dp-value { font-size: 15px; font-weight: 600; color: #333; }

/* Suggestions */
.detail-suggestions { background: #f8f9fb; border-radius: 10px; padding: 14px 16px; }
.ds-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #666; margin-bottom: 8px; }
.ai-badge { font-size: 10px; padding: 2px 8px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border-radius: 10px; font-weight: 600; margin-left: 6px; }
.local-badge { font-size: 10px; padding: 2px 8px; background: #f0f9eb; color: #67c23a; border-radius: 10px; font-weight: 600; margin-left: 6px; border: 1px solid #c2e7b0; }
.detail-suggestions ul { margin: 0; padding-left: 18px; }
.detail-suggestions li { font-size: 13px; color: #555; line-height: 1.8; }

/* RAG Summary */
.detail-rag-summary { background: linear-gradient(135deg, #f0f4ff, #e8ecf8); border-radius: 10px; padding: 14px 16px; border-left: 3px solid #667eea; }
.rag-summary-text { font-size: 13px; color: #444; line-height: 1.7; margin: 0; }

/* 表格行内标识 */
.row-source-ai { font-size: 12px; margin-left: 4px; }
.rag-count { font-size: 11px; margin-left: 4px; opacity: 0.7; }

/* 详情标签 */
.dtag-ai { background: linear-gradient(135deg, #e0e7ff, #c7d2fe); color: #667eea; border-color: #667eea40; }
.dtag-local { background: #f0f9eb; color: #67c23a; border-color: #c2e7b0; }

/* RAG */
.detail-rag { background: #f0f4ff; border-radius: 10px; padding: 14px 16px; }
.rag-list { margin-top: 10px; display: flex; flex-direction: column; gap: 8px; }
.rag-item { display: flex; align-items: flex-start; gap: 10px; background: #fff; border-radius: 8px; padding: 8px 12px; }
.rag-sim { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 12px; white-space: nowrap; flex-shrink: 0; }
.rag-detail { font-size: 12px; color: #333; line-height: 1.5; }
.rag-detail strong { color: #1a1a2e; }
.rag-meta { display: block; font-size: 11px; color: #999; margin-top: 2px; }

/* Footer */
.footer { background: #1a1a2e; color: #fff; margin-top: 40px; }
.footer-bottom { padding: 20px 24px; text-align: center; }
.footer-bottom p { color: #666; font-size: 12px; }

@media (max-width: 1024px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .table-wrap { overflow-x: auto; }
  .maint-table { min-width: 700px; }
  .filter-bar { flex-direction: column; align-items: flex-start; }
  .filter-right { width: 100%; flex-wrap: wrap; }
}
</style>
