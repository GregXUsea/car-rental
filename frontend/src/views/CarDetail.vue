<template>
  <div class="detail-page">
    <header class="header">
      <div class="header-content">
        <button class="back-btn" @click="goBack">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          返回
        </button>
        <h1>车辆详情</h1>
        <div></div>
      </div>
    </header>

    <main class="main" v-if="car">
      <div class="detail-wrapper" @touchstart="onTouchStart" @touchend="onTouchEnd">
        <div class="detail-card">
        <div class="img-section">
          <!-- 左箭头：上一辆（放在图片左侧） -->
          <button v-if="prevCar" class="nav-arrow img-nav-left" @click="navigateToCar(prevCar.id)" title="上一辆">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
          </button>

          <img :src="car.image" :alt="car.brand + car.model" class="car-img" :data-brand="car.brand" :data-category="car.category" @error="handleImgError($event)" />
          <div class="status-badge" :class="'status-' + car.status">{{ statusText(car.status) }}</div>
          <div class="usage-badges" v-if="car.usageType">
            <span v-if="car.usageType.includes('商务')" class="badge business">商务用车</span>
            <span v-if="car.usageType.includes('婚庆')" class="badge wedding">婚庆用车</span>
          </div>

          <!-- 右箭头：下一辆（放在图片右侧） -->
          <button v-if="nextCar" class="nav-arrow img-nav-right" @click="navigateToCar(nextCar.id)" title="下一辆">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
        </div>

        <div class="info-section">
          <h2>{{ car.brand }} {{ car.model }}</h2>
          <p class="desc">{{ car.description }}</p>

          <div class="info-grid">
            <div class="info-item">
              <span class="label">品牌</span>
              <span class="value">{{ car.brand }}</span>
            </div>
            <div class="info-item">
              <span class="label">型号</span>
              <span class="value">{{ car.model }}</span>
            </div>
            <div class="info-item">
              <span class="label">颜色</span>
              <span class="value">{{ car.color }}</span>
            </div>
            <div class="info-item">
              <span class="label">座位数</span>
              <span class="value">{{ car.seats }}座</span>
            </div>
            <div class="info-item">
              <span class="label">类别</span>
              <span class="value">{{ car.category }}</span>
            </div>
            <div class="info-item">
              <span class="label">总里程</span>
              <span class="value">{{ car.mileage?.toLocaleString() }}km</span>
            </div>
            <div class="info-item">
              <span class="label">保养次数</span>
              <span class="value">{{ car.maintainCount || 0 }}次</span>
            </div>
            <div class="info-item highlight">
              <span class="label">日租金</span>
              <span class="value price">¥{{ car.pricePerDay }}<small>/天</small></span>
            </div>
            <div class="info-item">
              <span class="label">押金</span>
              <span class="value">¥{{ car.deposit }}</span>
            </div>
            <div class="info-item" v-if="car.lastMaintainDate">
              <span class="label">上次保养</span>
              <span class="value">{{ car.lastMaintainDate }}</span>
            </div>
          </div>

          <div class="actions" v-if="car.status === 0">
            <button class="btn btn-primary" @click="openRentDialog">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
              立即租车
            </button>
            <button class="btn btn-outline" @click="openReserveDialog">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              预约租车
            </button>
          </div>
          <!-- 已租出：可预约未来时段 -->
          <div class="actions" v-else-if="car.status === 1">
            <p class="status-hint">当前车辆已被租用，可预约未来时段</p>
            <button class="btn btn-outline" @click="openReserveDialog">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              预约租车
            </button>
          </div>
          <!-- 已预约：不冲突时段可直接租 -->
          <div class="actions" v-else-if="car.status === 2">
            <p class="status-hint">当前车辆已有预约，不冲突时段可直接租用</p>
            <button class="btn btn-primary" @click="openRentDialog">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
              立即租车
            </button>
          </div>
          <!-- 维护中 -->
          <div class="actions" v-else>
            <button class="btn btn-disabled" disabled>
              维护中，暂不可用
            </button>
          </div>

          <!-- 未登录提示 -->
          <div class="login-hint" v-if="!isLoggedIn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>请先 <a href="/login">登录</a> 后再租车</span>
          </div>
        </div>
      </div>
      </div>
    </main>

    <!-- 租车弹窗 -->
    <el-dialog v-model="showRentDialog" title="立即租车" width="520px" :close-on-click-modal="false">
      <div class="dialog-tip">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#409eff" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
        <span>立即租车须在3天内开始，最长15天。预约前后6小时为周转缓冲期</span>
      </div>
      <!-- 已占用时间段 -->
      <div class="occupied-slots" v-if="occupiedSlots.length > 0">
        <div class="slots-title">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <span>以下时段不可用（含6小时周转缓冲）</span>
        </div>
        <div v-for="(slot, i) in occupiedSlots" :key="i" class="slot-item">
          <span class="slot-type" :class="slot.type">{{ slot.type === 'rented' ? '在租' : '已预约' }}</span>
          <span class="slot-time">{{ formatSlotTime(slot.bufferedStart) }} ~ {{ formatSlotTime(slot.bufferedEnd) }}</span>
        </div>
      </div>
      <div v-else class="occupied-slots empty">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>当前车辆全部时段可用</span>
      </div>
      <el-form :model="rentForm" label-width="80px">
        <el-form-item label="开始时间">
          <el-date-picker v-model="rentForm.startTime" type="datetime" placeholder="选择开始时间（须在3天内）" :disabled-date="disableStartDate" :disabled-hours="disabledStartHours" style="width:100%" @change="onStartTimeChange" />
          <div v-if="timeWarning.start" class="time-warning">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            {{ timeWarning.start }}
          </div>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="rentForm.endTime" type="datetime" placeholder="选择结束时间（最长15天）" :disabled-date="disableEndDate" style="width:100%" @change="onEndTimeChange" />
          <div v-if="timeWarning.end" class="time-warning">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            {{ timeWarning.end }}
          </div>
        </el-form-item>
        <el-form-item label="取车门店">
          <el-select v-model="rentForm.pickupStoreId" placeholder="请选择取车门店" style="width: 100%;">
            <el-option v-for="s in pickupStores" :key="s.id" :value="s.id">
              <span>{{ s.name }}</span>
              <span style="float:right;color:#999;font-size:12px">{{ s.city }} · {{ s.address.substring(0, 15) }}...</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="还车门店">
          <el-select v-model="rentForm.returnStoreId" placeholder="请选择还车门店（默认同取车门店）" clearable style="width: 100%;">
            <el-option v-for="s in returnStores" :key="s.id" :value="s.id">
              <span>{{ s.name }}</span>
              <span style="float:right;color:#999;font-size:12px">{{ s.city }} · {{ s.address.substring(0, 15) }}...</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="选择司机">
          <el-select v-model="rentForm.driverId" placeholder="不选择司机" clearable style="width: 100%;">
            <el-option v-for="d in availableDrivers" :key="d.id" :value="d.id">
              <span>{{ d.name }}</span>
              <span style="float:right;color:#999;font-size:12px">¥150/天 · {{ d.licenseType }} · {{ d.experienceYears }}年 · ★{{ d.rating }}</span>
            </el-option>
          </el-select>
          <div class="driver-price-hint">司机服务费 ¥150/天，按实际租期计算</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rentForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="费用明细">
          <div class="cost-detail">
            <div class="cost-row"><span>车辆租金</span><span>¥{{ car?.pricePerDay }}/天 × {{ estimatedDays }}天 = ¥{{ carCost }}</span></div>
            <div class="cost-row" v-if="rentForm.driverId"><span>司机服务费</span><span class="driver-fee">¥150/天 × {{ estimatedDays }}天 = ¥{{ driverCost }}</span></div>
            <!-- 优惠券行（可点击） -->
            <div v-if="couponStatus.eligible" class="cost-row coupon-row" @click="showCouponPopover = !showCouponPopover">
              <span class="coupon-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2"><path d="M20 12v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6"/><path d="M2 8h20v4H2z"/><path d="M12 2v6"/><path d="M12 2l-3 3"/><path d="M12 2l3 3"/></svg>
                新用户首单优惠
                <span class="coupon-tag-mini">最优</span>
              </span>
              <span class="discount-amount">-¥{{ discount }}</span>
            </div>
            <!-- 优惠券详情展开 -->
            <div v-if="showCouponPopover && couponStatus.eligible" class="coupon-popover">
              <div class="coupon-popover-header">
                <span class="coupon-popover-icon">🎫</span>
                <div>
                  <div class="coupon-popover-title">新用户首单优惠券</div>
                  <div class="coupon-popover-sub">首次租车减免50%，最高减200元</div>
                </div>
                <span class="coupon-popover-amount">-¥{{ discount }}</span>
              </div>
              <div class="coupon-popover-rules">
                <div class="coupon-rule">✓ 注册{{ couponStatus.daysLeft > 0 ? '30天' : '' }}内可用{{ couponStatus.daysLeft > 0 ? `（剩余${couponStatus.daysLeft}天）` : '' }}</div>
                <div class="coupon-rule">✓ 仅限首次下单，全车型通用</div>
                <div class="coupon-rule">✓ 订单金额50%减免，最高200元</div>
              </div>
              <div class="coupon-popover-status">已自动勾选最优方案</div>
            </div>
            <div class="cost-row total"><span>合计</span><span>¥{{ totalCost }}</span></div>
            <div class="cost-row"><span>押金（可退）</span><span>¥{{ car?.deposit }}</span></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRentDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRent" :loading="renting" :disabled="!rentForm.startTime || !rentForm.endTime">
          确认租车并支付押金
        </el-button>
      </template>
    </el-dialog>

    <!-- 预约弹窗 -->
    <el-dialog v-model="showReserveDialog" title="预约租车" width="520px" :close-on-click-modal="false">
      <div class="dialog-tip">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#409eff" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
        <span>预约可提前2个月，最长20天。预约后车辆将被锁定，到期未取自动作废。</span>
      </div>
      <!-- 已占用时间段 -->
      <div class="occupied-slots" v-if="occupiedSlots.length > 0">
        <div class="slots-title">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <span>以下时段不可用（含6小时周转缓冲）</span>
        </div>
        <div v-for="(slot, i) in occupiedSlots" :key="i" class="slot-item">
          <span class="slot-type" :class="slot.type">{{ slot.type === 'rented' ? '在租' : '已预约' }}</span>
          <span class="slot-time">{{ formatSlotTime(slot.bufferedStart) }} ~ {{ formatSlotTime(slot.bufferedEnd) }}</span>
        </div>
      </div>
      <div v-else class="occupied-slots empty">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>当前车辆全部时段可用</span>
      </div>
      <el-form :model="rentForm" label-width="80px">
        <el-form-item label="开始时间">
          <el-date-picker v-model="rentForm.startTime" type="datetime" placeholder="选择开始时间（可提前2个月）" :disabled-date="disableReserveStartDate" :disabled-hours="disabledStartHours" style="width:100%" @change="onStartTimeChange" />
          <div v-if="timeWarning.start" class="time-warning">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            {{ timeWarning.start }}
          </div>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="rentForm.endTime" type="datetime" placeholder="选择结束时间（最长20天）" :disabled-date="disableEndDate" style="width:100%" @change="onEndTimeChange" />
          <div v-if="timeWarning.end" class="time-warning">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            {{ timeWarning.end }}
          </div>
        </el-form-item>
        <el-form-item label="取车门店">
          <el-select v-model="rentForm.pickupStoreId" placeholder="请选择取车门店" style="width: 100%;">
            <el-option v-for="s in pickupStores" :key="s.id" :value="s.id">
              <span>{{ s.name }}</span>
              <span style="float:right;color:#999;font-size:12px">{{ s.city }} · {{ s.address.substring(0, 15) }}...</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="还车门店">
          <el-select v-model="rentForm.returnStoreId" placeholder="请选择还车门店（默认同取车门店）" clearable style="width: 100%;">
            <el-option v-for="s in returnStores" :key="s.id" :value="s.id">
              <span>{{ s.name }}</span>
              <span style="float:right;color:#999;font-size:12px">{{ s.city }} · {{ s.address.substring(0, 15) }}...</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="选择司机">
          <el-select v-model="rentForm.driverId" placeholder="不选择司机" clearable style="width: 100%;">
            <el-option v-for="d in availableDrivers" :key="d.id" :value="d.id">
              <span>{{ d.name }}</span>
              <span style="float:right;color:#999;font-size:12px">¥150/天 · {{ d.licenseType }} · {{ d.experienceYears }}年 · ★{{ d.rating }}</span>
            </el-option>
          </el-select>
          <div class="driver-price-hint">司机服务费 ¥150/天，按实际租期计算</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rentForm.remark" type="textarea" :rows="2" placeholder="选填，如取车地点等" />
        </el-form-item>
        <el-form-item label="费用预览" v-if="estimatedDays > 0">
          <div class="cost-detail">
            <div class="cost-row"><span>车辆租金</span><span>¥{{ car?.pricePerDay }}/天 × {{ estimatedDays }}天 = ¥{{ carCost }}</span></div>
            <div class="cost-row" v-if="rentForm.driverId"><span>司机服务费</span><span>¥150/天 × {{ estimatedDays }}天 = ¥{{ driverCost }}</span></div>
            <!-- 优惠券行（可点击） -->
            <div v-if="couponStatus.eligible" class="cost-row coupon-row" @click="showCouponPopover = !showCouponPopover">
              <span class="coupon-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2"><path d="M20 12v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6"/><path d="M2 8h20v4H2z"/><path d="M12 2v6"/><path d="M12 2l-3 3"/><path d="M12 2l3 3"/></svg>
                新用户首单优惠
                <span class="coupon-tag-mini">最优</span>
              </span>
              <span class="discount-amount">-¥{{ discount }}</span>
            </div>
            <!-- 优惠券详情展开 -->
            <div v-if="showCouponPopover && couponStatus.eligible" class="coupon-popover">
              <div class="coupon-popover-header">
                <span class="coupon-popover-icon">🎫</span>
                <div>
                  <div class="coupon-popover-title">新用户首单优惠券</div>
                  <div class="coupon-popover-sub">首次租车减免50%，最高减200元</div>
                </div>
                <span class="coupon-popover-amount">-¥{{ discount }}</span>
              </div>
              <div class="coupon-popover-rules">
                <div class="coupon-rule">✓ 注册{{ couponStatus.daysLeft > 0 ? '30天' : '' }}内可用{{ couponStatus.daysLeft > 0 ? `（剩余${couponStatus.daysLeft}天）` : '' }}</div>
                <div class="coupon-rule">✓ 仅限首次下单，全车型通用</div>
                <div class="coupon-rule">✓ 订单金额50%减免，最高200元</div>
              </div>
              <div class="coupon-popover-status">已自动勾选最优方案</div>
            </div>
            <div class="cost-row total"><span>预计合计</span><span>¥{{ totalCost }}</span></div>
            <div class="cost-row"><span>押金（可退）</span><span>¥{{ car?.deposit }}</span></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReserveDialog = false">取消</el-button>
        <el-button type="warning" @click="handleReserve" :loading="renting" :disabled="!rentForm.startTime || !rentForm.endTime">
          确认预约
        </el-button>
      </template>
    </el-dialog>

    <!-- 支付弹窗 -->
    <el-dialog v-model="showPayDialog" :title="payDialogTitle" width="420px" :close-on-click-modal="false" :show-close="payStep !== 'processing'">
      <!-- 步骤1：确认支付 -->
      <div v-if="payStep === 'confirm'" class="pay-confirm">
        <div class="pay-amount-wrap">
          <span class="pay-label">{{ payBoth ? '押金 + 租金 一起支付' : (payType === 'deposit' ? '押金支付' : '租金支付') }}</span>
          <span class="pay-amount">¥{{ payBoth ? (parseFloat(payAmount) + parseFloat(payRentalAmount)).toFixed(2) : payAmount }}</span>
        </div>
        <div class="pay-info">
          <div class="pay-info-row"><span>押金</span><span>¥{{ payDepositAmount }}（可退）</span></div>
          <div class="pay-info-row"><span>租金</span><span>¥{{ payRentalAmount }}</span></div>
          <div class="pay-info-row total-row" v-if="payBoth"><span>合计</span><span>¥{{ (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) }}</span></div>
        </div>
        <!-- 押金支付时，可选择一起付租金 -->
        <div class="pay-both-option" v-if="payType === 'deposit' && !payBoth">
          <button class="both-btn" @click="payBoth = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
            同时支付租金 ¥{{ payRentalAmount }}，合计 ¥{{ (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) }}
          </button>
        </div>
        <div class="pay-both-selected" v-if="payBoth && payType === 'deposit'">
          <span class="both-tag">已选择一起支付</span>
          <button class="both-cancel" @click="payBoth = false">取消，只付押金</button>
        </div>
        <div class="pay-card-input">
          <div class="card-label">模拟支付方式</div>
          <div class="card-row">
            <input v-model="cardNum1" maxlength="4" placeholder="6222" class="card-input" />
            <span class="card-dash">-</span>
            <input v-model="cardNum2" maxlength="4" placeholder="8888" class="card-input" />
            <span class="card-dash">-</span>
            <input v-model="cardNum3" maxlength="4" placeholder="6666" class="card-input" />
            <span class="card-dash">-</span>
            <input v-model="cardNum4" maxlength="4" placeholder="0001" class="card-input" />
          </div>
        </div>
      </div>
      <!-- 步骤2：支付中 -->
      <div v-if="payStep === 'processing'" class="pay-processing">
        <div class="pay-spinner"></div>
        <p>正在处理支付...</p>
      </div>
      <!-- 步骤3：支付成功 -->
      <div v-if="payStep === 'success'" class="pay-success">
        <div class="success-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <h3>支付成功</h3>
        <p>¥{{ payBoth && payType === 'deposit' ? (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) : payAmount }} 已支付</p>
      </div>
      <template #footer>
        <el-button v-if="payStep === 'confirm'" @click="showPayDialog = false; payBoth = false">取消</el-button>
        <el-button v-if="payStep === 'confirm'" type="primary" @click="processPayment">
          确认支付 ¥{{ payBoth && payType === 'deposit' ? (parseFloat(payDepositAmount) + parseFloat(payRentalAmount)).toFixed(2) : payAmount }}
        </el-button>
        <el-button v-if="payStep === 'success'" type="primary" @click="onPaySuccess">
          完成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const route = useRoute()
const router = useRouter()
const car = ref(null)
const allCars = ref([]) // 全部车辆列表，用于左右切换
const showRentDialog = ref(false)
const showReserveDialog = ref(false)
const renting = ref(false)
const availableDrivers = ref([])
const isLoggedIn = ref(!!localStorage.getItem('token'))

const rentForm = ref({ startTime: '', endTime: '', driverId: null, remark: '' })
const timeWarning = reactive({ start: '', end: '' })

// 已占用时间段
const occupiedSlots = ref([])

// 支付相关
const showPayDialog = ref(false)

// 优惠券状态
const couponStatus = ref({ eligible: false, daysLeft: 0, expireDate: null })
const showCouponPopover = ref(false)

const loadCouponStatus = async () => {
  try {
    const res = await api.get('/user/coupon-status')
    if (res.code === 200) couponStatus.value = res.data
  } catch (e) { /* 静默处理 */ }
}
const payStep = ref('confirm') // confirm, processing, success
const payType = ref('deposit') // deposit, rental
const payAmount = ref(0)
const payDepositAmount = ref(0)
const payRentalAmount = ref(0)
const payOrderId = ref(null)
const payDialogTitle = ref('')
const payBoth = ref(false) // 是否一起支付押金+租金
const cardNum1 = ref('6222')
const cardNum2 = ref('8888')
const cardNum3 = ref('6666')
const cardNum4 = ref('0001')

onMounted(async () => {
  const res = await api.get(`/cars/detail/${route.params.id}`)
  if (res.code === 200) car.value = res.data
  else { ElMessage.error(res.message); router.push('/') }
  loadDrivers()
  loadAllCars()
  // 加载优惠券状态
  loadCouponStatus()
})

// ====== 左右切换 ======
const loadAllCars = async () => {
  const res = await api.get('/cars/list')
  if (res.code === 200) {
    // 与首页 filteredCars 默认排序一致：排除维护中，小米 > 问界M7/零跑C11 > 其他
    const list = res.data.filter(c => c.status !== 3)
    const priority = (car) => {
      if (car.brand === '小米') return 0
      if ((car.brand === '问界' && car.model.includes('M7')) || (car.brand === '零跑' && car.model.includes('C11'))) return 1
      return 2
    }
    list.sort((a, b) => priority(a) - priority(b))
    allCars.value = list
  }
}

const currentIndex = computed(() => {
  if (!car.value || !allCars.value.length) return -1
  return allCars.value.findIndex(c => c.id === car.value.id)
})

const prevCar = computed(() => {
  if (currentIndex.value <= 0) return null
  return allCars.value[currentIndex.value - 1]
})

const nextCar = computed(() => {
  if (currentIndex.value < 0 || currentIndex.value >= allCars.value.length - 1) return null
  return allCars.value[currentIndex.value + 1]
})

const navigateToCar = (id) => {
  router.push({ name: 'CarDetail', params: { id } })
}

// 返回：如果来自AI助手，直接回AI；否则正常回退
const goBack = () => {
  // 检查来源页面（首页或AI助手）
  const fromPage = sessionStorage.getItem('car_detail_from')
  if (fromPage) {
    sessionStorage.removeItem('car_detail_from')
    router.push(fromPage)
    return
  }
  // 默认回首页
  router.push('/')
}

// 监听路由参数变化，重新加载车辆数据（Vue Router 复用组件实例）
watch(() => route.params.id, async (newId) => {
  if (!newId) return
  const res = await api.get(`/cars/detail/${newId}`)
  if (res.code === 200) {
    car.value = res.data
    // 重置弹窗状态
    showRentDialog.value = false
    showReserveDialog.value = false
    showPayDialog.value = false
    rentForm.value = { startTime: '', endTime: '', driverId: null, remark: '' }
    occupiedSlots.value = []
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
})

// 触摸滑动切换（移动端）
let touchStartX = 0
let touchStartY = 0
const onTouchStart = (e) => {
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
}
const onTouchEnd = (e) => {
  const dx = e.changedTouches[0].clientX - touchStartX
  const dy = e.changedTouches[0].clientY - touchStartY
  // 水平滑动距离需大于50px，且水平距离 > 垂直距离（排除上下滚动）
  if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy)) {
    if (dx > 0 && prevCar.value) {
      navigateToCar(prevCar.value.id)
    } else if (dx < 0 && nextCar.value) {
      navigateToCar(nextCar.value.id)
    }
  }
}

const loadDrivers = async () => {
  const res = await api.get('/drivers/available')
  if (res.code === 200) availableDrivers.value = res.data
}

// ====== 门店相关 ======
const cities = ref([])
const stores = ref([])
const selectedPickupCity = ref('')
const selectedReturnCity = ref('')

const loadCities = async () => {
  const res = await api.get('/stores/cities')
  if (res.code === 200) cities.value = res.data
}

const loadStores = async (city) => {
  const params = city ? `?city=${city}` : ''
  const res = await api.get(`/stores/list${params}`)
  if (res.code === 200) stores.value = res.data
}

const onPickupCityChange = (city) => {
  selectedPickupCity.value = city
  rentForm.value.pickupStoreId = null
  loadStores(city)
}

const onReturnCityChange = (city) => {
  selectedReturnCity.value = city
  rentForm.value.returnStoreId = null
  loadStores(city)
}

const pickupStores = computed(() => {
  if (!selectedPickupCity.value) return stores.value
  return stores.value.filter(s => s.city === selectedPickupCity.value)
})

const returnStores = computed(() => {
  if (!selectedReturnCity.value) return stores.value
  return stores.value.filter(s => s.city === selectedReturnCity.value)
})

// ====== 已占用时间段 ======
const loadOccupiedSlots = async () => {
  if (!car.value) return
  const res = await api.get(`/orders/slots/${car.value.id}`)
  if (res.code === 200) occupiedSlots.value = res.data
}

const formatSlotTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 检查时间是否与占用时间段冲突（含6小时缓冲）
const isTimeConflict = (start, end) => {
  const s = new Date(start).getTime()
  const e = new Date(end).getTime()
  return occupiedSlots.value.some(slot => {
    const bs = new Date(slot.bufferedStart).getTime()
    const be = new Date(slot.bufferedEnd).getTime()
    return s < be && e > bs
  })
}

// ====== 支付流程 ======
const openPayDialog = (type, amount, orderId) => {
  payType.value = type
  payAmount.value = amount
  payOrderId.value = orderId
  payStep.value = 'confirm'
  payBoth.value = false
  payDialogTitle.value = type === 'deposit' ? '支付押金' : '支付租金'
  // 设置押金和租金金额
  payDepositAmount.value = car.value?.deposit || 0
  payRentalAmount.value = totalCost.value || 0
  showPayDialog.value = true
}

const processPayment = async () => {
  payStep.value = 'processing'
  // 模拟支付过程（1.5秒）
  await new Promise(resolve => setTimeout(resolve, 1500))

  try {
    // 先支付押金
    const depositRes = await api.post(`/orders/pay-deposit/${payOrderId.value}`)
    if (depositRes.code !== 200) {
      ElMessage.error(depositRes.message)
      payStep.value = 'confirm'
      return
    }

    // 如果选择一起支付租金
    if (payBoth.value && payType.value === 'deposit') {
      const rentalRes = await api.post(`/orders/pay-rental/${payOrderId.value}`)
      if (rentalRes.code !== 200) {
        ElMessage.error(rentalRes.message)
        payStep.value = 'confirm'
        return
      }
    }

    payStep.value = 'success'
  } catch (e) {
    ElMessage.error('支付失败，请重试')
    payStep.value = 'confirm'
  }
}

const onPaySuccess = () => {
  showPayDialog.value = false
  payBoth.value = false
  if (payType.value === 'deposit') {
    ElMessage.success(payBoth.value ? '押金和租金支付成功！' : '押金支付成功！')
    router.push('/orders')
  } else {
    ElMessage.success('租金支付成功！')
    api.get(`/cars/detail/${route.params.id}`).then(res => {
      if (res.code === 200) car.value = res.data
    })
  }
}

// ====== 时间相关 ======

// 立即租车：2pm前可租当天，2pm后只能从明天起
// 立即租车：只能选今天、明天、后天
const disableStartDate = (date) => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const maxDate = new Date(today)
  maxDate.setDate(maxDate.getDate() + 3)
  return date < today || date >= maxDate
}

// 预约租车：今天及以后都可选（无2pm限制）
const disableReserveStartDate = (date) => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return date < today
}

// 不限制小时，全时段可选
const disabledStartHours = () => []

// 结束日期：不能早于开始日期，立即租车最多15天，预约最多20天
const disableEndDate = (date) => {
  if (!rentForm.value.startTime) return true
  const start = new Date(rentForm.value.startTime)
  const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const maxDays = showReserveDialog.value ? 20 : 15
  const maxEnd = new Date(startDate); maxEnd.setDate(maxEnd.getDate() + maxDays)
  return date < startDate || date > maxEnd
}

const onStartTimeChange = () => {
  timeWarning.start = ''
  timeWarning.end = ''
  if (!rentForm.value.startTime) return

  const now = new Date()
  const start = new Date(rentForm.value.startTime)

  // 立即租车：检查是否超过3天
  const maxStart = new Date(now)
  maxStart.setDate(maxStart.getDate() + 3)
  maxStart.setHours(23, 59, 59, 999)
  if (start > maxStart) {
    timeWarning.start = '立即租车须在3天内开始，超过请选择预约租车'
  }

  // 检查是否早于当前时间
  if (start <= now) {
    timeWarning.start = '开始时间必须晚于当前时间'
  }

  // 清除不合规的结束时间
  if (rentForm.value.endTime) {
    const end = new Date(rentForm.value.endTime)
    const maxEnd = new Date(start)
    maxEnd.setDate(maxEnd.getDate() + 15)
    if (end > maxEnd) {
      rentForm.value.endTime = ''
      timeWarning.end = '已清除结束时间，请重新选择'
    }
    if (end <= start) {
      rentForm.value.endTime = ''
      timeWarning.end = '结束时间必须晚于开始时间'
    }
  }
}

const onEndTimeChange = () => {
  timeWarning.end = ''
  if (!rentForm.value.endTime || !rentForm.value.startTime) return

  const start = new Date(rentForm.value.startTime)
  const end = new Date(rentForm.value.endTime)

  if (end <= start) {
    timeWarning.end = '结束时间必须晚于开始时间'
    return
  }

  // 计算租期
  const days = (end - start) / (1000 * 60 * 60 * 24)
  const maxDays = showReserveDialog.value ? 20 : 15
  if (days > maxDays) {
    timeWarning.end = `最多${maxDays}天，请缩短租期`
  }

  // 检查时间冲突
  if (isTimeConflict(rentForm.value.startTime, rentForm.value.endTime)) {
    timeWarning.end = '该时间段与已有订单冲突（含6小时缓冲）'
  }
}

// ====== 费用计算（精确到0.01天） ======
const estimatedDays = computed(() => {
  if (!rentForm.value.startTime || !rentForm.value.endTime) return 0
  const ms = new Date(rentForm.value.endTime) - new Date(rentForm.value.startTime)
  if (ms <= 0) return 0
  const minutes = ms / 60000
  const days = Math.round((minutes / 1440) * 100) / 100
  return Math.max(1, days)
})

const carCost = computed(() => car.value ? (car.value.pricePerDay * estimatedDays.value).toFixed(2) : '0.00')
const driverCost = computed(() => rentForm.value.driverId ? (150 * estimatedDays.value).toFixed(2) : '0.00')
const originalTotal = computed(() => (parseFloat(carCost.value) + parseFloat(driverCost.value)).toFixed(2))

const isNewUser = computed(() => couponStatus.value.eligible === true)

const discount = computed(() => {
  if (!isNewUser.value) return '0.00'
  const halfDiscount = parseFloat(originalTotal.value) * 0.5
  return Math.min(halfDiscount, 200).toFixed(2)
})

const totalCost = computed(() => (parseFloat(originalTotal.value) - parseFloat(discount.value)).toFixed(2))

// ====== 校验时间 ======
const validateTime = () => {
  if (!rentForm.value.startTime || !rentForm.value.endTime) { ElMessage.warning('请选择租车时间'); return false }
  const now = new Date()
  const start = new Date(rentForm.value.startTime)
  const end = new Date(rentForm.value.endTime)
  if (start <= now) { ElMessage.warning('开始时间必须晚于当前时间'); return false }
  if (end <= start) { ElMessage.warning('结束时间必须晚于开始时间'); return false }
  // 立即租车：必须在3天内开始
  const maxStart = new Date(now)
  maxStart.setDate(maxStart.getDate() + 3)
  maxStart.setHours(23, 59, 59, 999)
  if (start > maxStart) { ElMessage.warning('立即租车必须在3天内开始，超过3天请选择预约租车'); return false }
  if (estimatedDays.value > 15) { ElMessage.warning('立即租车最多15天'); return false }
  if (isTimeConflict(rentForm.value.startTime, rentForm.value.endTime)) {
    ElMessage.warning('所选时间段与已有订单冲突（含6小时周转缓冲），请选择其他时间')
    return false
  }
  return true
}

// 预约校验
const validateReserveTime = () => {
  if (!rentForm.value.startTime || !rentForm.value.endTime) { ElMessage.warning('请选择预约时间'); return false }
  const now = new Date()
  const start = new Date(rentForm.value.startTime)
  const end = new Date(rentForm.value.endTime)
  if (start <= now) { ElMessage.warning('开始时间必须晚于当前时间'); return false }
  if (end <= start) { ElMessage.warning('结束时间必须晚于开始时间'); return false }
  // 预约：最长2个月
  const maxStart = new Date(now)
  maxStart.setMonth(maxStart.getMonth() + 2)
  if (start > maxStart) { ElMessage.warning('预约最早只能提前2个月'); return false }
  if (estimatedDays.value > 20) { ElMessage.warning('预约租车最多20天'); return false }
  if (isTimeConflict(rentForm.value.startTime, rentForm.value.endTime)) {
    ElMessage.warning('所选时间段与已有订单冲突（含6小时周转缓冲），请选择其他时间')
    return false
  }
  return true
}

const openRentDialog = () => {
  if (!isLoggedIn.value) { ElMessage.warning('请先登录'); router.push('/login'); return }
  rentForm.value = { startTime: '', endTime: '', driverId: null, remark: '', pickupStoreId: null, returnStoreId: null }
  loadOccupiedSlots()
  loadStores()
  showRentDialog.value = true
}

const openReserveDialog = () => {
  if (!isLoggedIn.value) { ElMessage.warning('请先登录'); router.push('/login'); return }
  rentForm.value = { startTime: '', endTime: '', driverId: null, remark: '', pickupStoreId: null, returnStoreId: null }
  loadOccupiedSlots()
  loadStores()
  showReserveDialog.value = true
}

// 格式化时间为后端接受的格式 (YYYY-MM-DDTHH:mm:ss)
const formatTimeForBackend = (date) => {
  if (!date) return null
  const d = new Date(date)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const handleRent = async () => {
  if (!validateTime()) return
  renting.value = true
  try {
    const res = await api.post('/orders/rent', {
      carId: car.value.id,
      startTime: formatTimeForBackend(rentForm.value.startTime),
      endTime: formatTimeForBackend(rentForm.value.endTime),
      driverId: rentForm.value.driverId,
      remark: rentForm.value.remark,
      isReservation: false
    })
    if (res.code === 200) {
      showRentDialog.value = false
      // 弹出支付押金弹窗
      openPayDialog('deposit', car.value.deposit, res.data.id)
    } else ElMessage.error(res.message)
  } finally { renting.value = false }
}

const handleReserve = async () => {
  if (!validateReserveTime()) return
  renting.value = true
  try {
    const res = await api.post('/orders/rent', {
      carId: car.value.id,
      startTime: formatTimeForBackend(rentForm.value.startTime),
      endTime: formatTimeForBackend(rentForm.value.endTime),
      driverId: rentForm.value.driverId,
      remark: rentForm.value.remark,
      isReservation: true
    })
    if (res.code === 200) {
      showReserveDialog.value = false
      // 弹出支付押金弹窗
      openPayDialog('deposit', car.value.deposit, res.data.id)
    } else ElMessage.error(res.message)
  } finally { renting.value = false }
}

const statusText = (s) => ({ 0: '空闲', 1: '已租出', 2: '已预约', 3: '维护中' }[s] || '')

// 品牌主题色
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
  const t = brandThemes[brand] || { body: '#667eea', roof: '#5a67d8', accent: '#764ba2' }

  // 根据车型类别选择不同车身轮廓
  const isSUV = category === 'SUV'
  const isMPV = category === 'MPV'

  let bodyPath, roofPath, windowPath1, windowPath2
  if (isSUV) {
    bodyPath = 'M80,195 Q80,175 100,175 L120,130 L200,115 L380,115 L460,130 Q490,140 500,175 Q510,175 510,195 L510,205 L80,205 Z'
    roofPath = 'M120,130 L200,110 L380,110 L460,130 Q475,135 480,145 L120,145 Q115,135 120,130 Z'
    windowPath1 = 'M135,145 L195,132 L195,170 L135,170 Z'
    windowPath2 = 'M205,130 L375,130 L375,170 L205,170 Z'
  } else if (isMPV) {
    bodyPath = 'M70,195 Q70,170 90,170 L105,120 L190,95 L400,95 L470,120 Q500,135 510,170 Q515,170 515,195 L515,205 L70,205 Z'
    roofPath = 'M105,120 L190,90 L400,90 L470,120 Q480,125 485,135 L105,135 Q100,125 105,120 Z'
    windowPath1 = 'M120,135 L185,118 L185,165 L120,165 Z'
    windowPath2 = 'M195,115 L390,115 L390,165 L195,165 Z'
  } else {
    bodyPath = 'M85,195 Q85,180 105,180 L130,150 L195,135 L385,135 L450,150 Q485,160 495,180 Q505,180 505,195 L505,205 L85,205 Z'
    roofPath = 'M130,150 L195,130 L385,130 L450,150 Q460,155 465,160 L130,160 Q125,155 130,150 Z'
    windowPath1 = 'M145,160 L192,148 L192,178 L145,178 Z'
    windowPath2 = 'M202,146 L378,146 L378,178 L202,178 Z'
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 300">
    <defs>
      <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#87CEEB"/>
        <stop offset="60%" stop-color="#B0E0E6"/>
        <stop offset="100%" stop-color="#E0F0FF"/>
      </linearGradient>
      <linearGradient id="ground" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#90A4AE"/>
        <stop offset="100%" stop-color="#78909C"/>
      </linearGradient>
      <linearGradient id="bodyGrad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="${t.body}"/>
        <stop offset="100%" stop-color="${t.roof}"/>
      </linearGradient>
      <linearGradient id="roofGrad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="${t.accent}"/>
        <stop offset="100%" stop-color="${t.body}"/>
      </linearGradient>
      <linearGradient id="winGrad" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="#81D4FA"/>
        <stop offset="50%" stop-color="#B3E5FC"/>
        <stop offset="100%" stop-color="#E1F5FE"/>
      </linearGradient>
      <linearGradient id="wheelGrad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#37474F"/>
        <stop offset="100%" stop-color="#263238"/>
      </linearGradient>
      <radialGradient id="headlight" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="#FFF9C4"/>
        <stop offset="100%" stop-color="#FDD835" stop-opacity="0.6"/>
      </radialGradient>
      <filter id="shadow" x="-5%" y="-5%" width="110%" height="120%">
        <feDropShadow dx="0" dy="4" stdDeviation="6" flood-opacity="0.15"/>
      </filter>
    </defs>

    <!-- 天空 -->
    <rect fill="url(#sky)" width="600" height="300" rx="12"/>

    <!-- 远景山丘 -->
    <ellipse cx="150" cy="210" rx="180" ry="40" fill="#A5D6A7" opacity="0.4"/>
    <ellipse cx="450" cy="215" rx="200" ry="35" fill="#81C784" opacity="0.3"/>

    <!-- 地面 -->
    <rect fill="url(#ground)" y="210" width="600" height="90" rx="0"/>
    <rect fill="#607D8B" y="210" width="600" height="3"/>

    <!-- 车身阴影 -->
    <ellipse cx="300" cy="210" rx="200" ry="12" fill="#000" opacity="0.12"/>

    <!-- 车身主体 -->
    <path d="${bodyPath}" fill="url(#bodyGrad)" filter="url(#shadow)"/>
    <path d="${bodyPath}" fill="none" stroke="${t.roof}" stroke-width="1.5"/>

    <!-- 车顶 -->
    <path d="${roofPath}" fill="url(#roofGrad)" stroke="${t.roof}" stroke-width="1"/>

    <!-- 车窗 -->
    <path d="${windowPath1}" fill="url(#winGrad)" opacity="0.85" rx="3"/>
    <path d="${windowPath2}" fill="url(#winGrad)" opacity="0.85" rx="3"/>
    <!-- 车窗高光 -->
    <path d="${windowPath1}" fill="#fff" opacity="0.15"/>
    <rect x="${isSUV ? 138 : isMPV ? 123 : 148}" y="${isSUV ? 148 : isMPV ? 138 : 163}" width="15" height="8" rx="2" fill="#fff" opacity="0.3"/>

    <!-- 车身装饰线 -->
    <line x1="105" y1="${isSUV ? 180 : isMPV ? 178 : 185}" x2="490" y2="${isSUV ? 180 : isMPV ? 178 : 185}" stroke="${t.accent}" stroke-width="2" opacity="0.5"/>

    <!-- 车门把手 -->
    <rect x="230" y="${isSUV ? 165 : isMPV ? 160 : 172}" width="28" height="5" rx="2.5" fill="${t.roof}" opacity="0.6"/>
    <rect x="330" y="${isSUV ? 165 : isMPV ? 160 : 172}" width="28" height="5" rx="2.5" fill="${t.roof}" opacity="0.6"/>

    <!-- 前大灯 -->
    <ellipse cx="${isSUV ? 92 : isMPV ? 82 : 97}" cy="${isSUV ? 188 : isMPV ? 183 : 190}" rx="14" ry="8" fill="url(#headlight)"/>
    <ellipse cx="${isSUV ? 92 : isMPV ? 82 : 97}" cy="${isSUV ? 188 : isMPV ? 183 : 190}" rx="8" ry="5" fill="#FFFDE7" opacity="0.8"/>

    <!-- 尾灯 -->
    <rect x="${isSUV ? 497 : isMPV ? 502 : 492}" y="${isSUV ? 180 : isMPV ? 175 : 182}" width="10" height="18" rx="3" fill="#E53935" opacity="0.9"/>
    <rect x="${isSUV ? 499 : isMPV ? 504 : 494}" y="${isSUV ? 183 : isMPV ? 178 : 185}" width="6" height="12" rx="2" fill="#EF5350" opacity="0.7"/>

    <!-- 前格栅 -->
    <rect x="${isSUV ? 82 : isMPV ? 72 : 87}" y="${isSUV ? 178 : isMPV ? 173 : 180}" width="20" height="12" rx="3" fill="#263238" opacity="0.7"/>

    <!-- 轮胎 -->
    <g>
      <circle cx="${isSUV ? 155 : isMPV ? 145 : 160}" cy="205" r="22" fill="url(#wheelGrad)"/>
      <circle cx="${isSUV ? 155 : isMPV ? 145 : 160}" cy="205" r="15" fill="#455A64"/>
      <circle cx="${isSUV ? 155 : isMPV ? 145 : 160}" cy="205" r="8" fill="#78909C"/>
      <circle cx="${isSUV ? 155 : isMPV ? 145 : 160}" cy="205" r="3" fill="#B0BEC5"/>
      <!-- 轮毂辐条 -->
      <line x1="${isSUV ? 155 : isMPV ? 145 : 160}" y1="190" x2="${isSUV ? 155 : isMPV ? 145 : 160}" y2="220" stroke="#607D8B" stroke-width="2" opacity="0.5"/>
      <line x1="${isSUV ? 140 : isMPV ? 130 : 145}" y1="205" x2="${isSUV ? 170 : isMPV ? 160 : 175}" y2="205" stroke="#607D8B" stroke-width="2" opacity="0.5"/>
    </g>
    <g>
      <circle cx="${isSUV ? 435 : isMPV ? 445 : 430}" cy="205" r="22" fill="url(#wheelGrad)"/>
      <circle cx="${isSUV ? 435 : isMPV ? 445 : 430}" cy="205" r="15" fill="#455A64"/>
      <circle cx="${isSUV ? 435 : isMPV ? 445 : 430}" cy="205" r="8" fill="#78909C"/>
      <circle cx="${isSUV ? 435 : isMPV ? 445 : 430}" cy="205" r="3" fill="#B0BEC5"/>
      <line x1="${isSUV ? 435 : isMPV ? 445 : 430}" y1="190" x2="${isSUV ? 435 : isMPV ? 445 : 430}" y2="220" stroke="#607D8B" stroke-width="2" opacity="0.5"/>
      <line x1="${isSUV ? 420 : isMPV ? 430 : 415}" y1="205" x2="${isSUV ? 450 : isMPV ? 460 : 445}" y2="205" stroke="#607D8B" stroke-width="2" opacity="0.5"/>
    </g>

    <!-- 后视镜 -->
    <ellipse cx="${isSUV ? 108 : isMPV ? 98 : 113}" cy="${isSUV ? 152 : isMPV ? 142 : 157}" rx="8" ry="5" fill="${t.roof}" stroke="${t.body}" stroke-width="1"/>

    <!-- 品牌标识 -->
    <text x="300" y="60" text-anchor="middle" fill="${t.body}" font-size="26" font-weight="800" font-family="system-ui,-apple-system,sans-serif" opacity="0.85">${brand}</text>
    <text x="300" y="82" text-anchor="middle" fill="${t.body}" font-size="14" font-family="system-ui,-apple-system,sans-serif" opacity="0.5">${category}</text>

    <!-- 路面反光 -->
    <rect x="100" y="240" width="400" height="1" fill="#fff" opacity="0.08" rx="1"/>
  </svg>`
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent(svg)
}
</script>

<style scoped>
.detail-page { min-height: 100vh; background: #f0f2f5; }
.header { background: #fff; box-shadow: 0 1px 0 rgba(0,0,0,0.06); position: sticky; top: 0; z-index: 100; }
.header-content { max-width: 1000px; margin: 0 auto; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.header-content h1 { font-size: 16px; }
.back-btn { display: flex; align-items: center; gap: 6px; background: none; border: none; color: #666; font-size: 14px; cursor: pointer; padding: 8px 12px; border-radius: 8px; transition: all 0.2s; }
.back-btn:hover { background: #f5f7fa; color: #333; }

.main { max-width: 1000px; margin: 24px auto; padding: 0 24px; }
.detail-wrapper { position: relative; }
.detail-card { background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }

/* 左右导航箭头 */
.nav-arrow { position: absolute; top: 50%; transform: translateY(-50%); z-index: 10; width: 48px; height: 48px; border-radius: 50%; background: rgba(255,255,255,0.92); border: none; box-shadow: 0 4px 16px rgba(0,0,0,0.12); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.3s cubic-bezier(0.4,0,0.2,1); color: #333; }
.nav-arrow:hover { background: #fff; box-shadow: 0 6px 24px rgba(0,0,0,0.18); transform: translateY(-50%) scale(1.08); color: #667eea; }
.nav-arrow:active { transform: translateY(-50%) scale(0.95); }

/* 图片两侧的切换按钮 */
.img-nav-left { left: 12px; }
.img-nav-right { right: 12px; }

.img-section { height: 350px; position: relative; background: #f5f5f5; overflow: hidden; }
.car-img { width: 100%; height: 100%; object-fit: cover; }
.status-badge { position: absolute; top: 16px; right: 16px; padding: 6px 14px; border-radius: 8px; font-size: 13px; font-weight: 600; color: #fff; backdrop-filter: blur(8px); }
.status-0 { background: rgba(103,194,58,0.9); }
.status-1 { background: rgba(245,108,108,0.9); }
.status-2 { background: rgba(230,162,60,0.9); }
.status-3 { background: rgba(144,147,153,0.9); }
.usage-badges { position: absolute; top: 16px; left: 16px; display: flex; gap: 8px; }
.badge { padding: 6px 12px; border-radius: 8px; font-size: 12px; font-weight: 600; backdrop-filter: blur(8px); }
.badge.business { background: rgba(102,126,234,0.9); color: #fff; }
.badge.wedding { background: rgba(245,108,108,0.9); color: #fff; }

.info-section { padding: 32px; }
.info-section h2 { font-size: 24px; font-weight: 700; color: #1a1a2e; margin-bottom: 8px; }
.desc { color: #666; margin-bottom: 24px; line-height: 1.6; }

.info-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 32px; padding: 20px; background: #f8f9fa; border-radius: 12px; }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-item .label { font-size: 12px; color: #999; }
.info-item .value { font-size: 15px; color: #333; font-weight: 500; }
.info-item.highlight .price { font-size: 22px; color: #f56c6c; font-weight: 700; }
.info-item.highlight .price small { font-size: 12px; font-weight: normal; color: #999; }

.actions { display: flex; gap: 16px; margin-bottom: 16px; }
.btn { display: inline-flex; align-items: center; gap: 8px; padding: 14px 32px; border-radius: 12px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.3s; border: none; }
.btn-primary { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(102,126,234,0.4); }
.btn-outline { background: #fff; color: #667eea; border: 2px solid #667eea; }
.btn-outline:hover { background: #667eea; color: #fff; }
.btn-disabled { background: #e5e7eb; color: #999; cursor: not-allowed; }
.status-hint { font-size: 13px; color: #e6a23c; margin-bottom: 10px; text-align: center; }

.login-hint { display: flex; align-items: center; gap: 6px; padding: 12px 16px; background: #fffbe6; border-radius: 8px; color: #e6a23c; font-size: 13px; }
.login-hint a { color: #667eea; text-decoration: underline; }

.dialog-tip { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: #ecf5ff; border-radius: 8px; margin-bottom: 20px; color: #409eff; font-size: 13px; }

.cost-detail { width: 100%; }
.cost-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 13px; color: #666; border-bottom: 1px dashed #eee; }
.cost-row:last-child { border-bottom: none; }
.cost-row.total { font-size: 15px; font-weight: 600; color: #f56c6c; padding-top: 12px; border-top: 1px solid #eee; border-bottom: none; }
.cost-row.discount { color: #f56c6c; }
.cost-row.discount span:first-child { display: flex; align-items: center; gap: 4px; }
.driver-fee { color: #e6a23c; font-weight: 600; }
.driver-price-hint { font-size: 12px; color: #e6a23c; margin-top: 4px; padding-left: 2px; }
.discount-amount { font-weight: 600; color: #f56c6c; }

/* 优惠券行 */
.coupon-row { cursor: pointer; padding: 10px 8px; border-radius: 6px; transition: background 0.2s; border-bottom: 1px dashed #eee !important; }
.coupon-row:hover { background: #fef0f0; }
.coupon-label { display: flex; align-items: center; gap: 6px; color: #f56c6c; font-weight: 500; }
.coupon-label svg { flex-shrink: 0; }
.coupon-tag-mini { font-size: 10px; padding: 1px 6px; background: linear-gradient(135deg, #f56c6c, #e74c3c); color: #fff; border-radius: 8px; font-weight: 600; }

/* 优惠券详情弹出 */
.coupon-popover { background: #fff; border: 1px solid #fbc4c4; border-radius: 10px; padding: 14px; margin: 4px 0 8px; animation: couponSlide 0.2s ease; }
@keyframes couponSlide { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
.coupon-popover-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px dashed #f0f0f0; }
.coupon-popover-icon { font-size: 28px; }
.coupon-popover-title { font-size: 14px; font-weight: 600; color: #333; }
.coupon-popover-sub { font-size: 12px; color: #999; margin-top: 2px; }
.coupon-popover-amount { font-size: 18px; font-weight: 800; color: #f56c6c; margin-left: auto; }
.coupon-popover-rules { margin-bottom: 8px; }
.coupon-rule { font-size: 12px; color: #666; line-height: 1.8; }
.coupon-popover-status { font-size: 11px; color: #67c23a; background: #f0f9eb; padding: 6px 10px; border-radius: 6px; text-align: center; border: 1px solid #c2e7b0; }
.time-warning { display: flex; align-items: center; gap: 4px; margin-top: 6px; font-size: 12px; color: #e6a23c; background: #fffbe6; padding: 6px 10px; border-radius: 6px; border: 1px solid #ffe58f; }

/* 已占用时间段 */
.occupied-slots { background: #fff9f5; border: 1px solid #ffe0c0; border-radius: 10px; padding: 12px 16px; margin-bottom: 16px; }
.occupied-slots.empty { background: #f0f9eb; border-color: #c2e7b0; }
.slots-title { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #e6a23c; margin-bottom: 8px; font-weight: 600; }
.occupied-slots.empty .slots-title { color: #67c23a; }
.slot-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
.slot-type { padding: 1px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; color: #fff; }
.slot-type.rented { background: #f56c6c; }
.slot-type.reserved { background: #e6a23c; }
.slot-time { font-size: 13px; color: #666; }

/* 支付弹窗 */
.pay-confirm { display: flex; flex-direction: column; gap: 20px; }
.pay-amount-wrap { text-align: center; padding: 20px; background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 12px; color: #fff; }
.pay-label { font-size: 14px; opacity: 0.9; display: block; margin-bottom: 8px; }
.pay-amount { font-size: 36px; font-weight: 800; }
.pay-info { background: #f8f9fb; border-radius: 10px; padding: 12px 16px; }
.pay-info-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; }
.pay-info-row span:first-child { color: #999; }
.pay-info-row span:last-child { color: #333; font-weight: 500; }
.pay-card-input { background: #f8f9fb; border-radius: 10px; padding: 12px 16px; }
.card-label { font-size: 13px; color: #999; margin-bottom: 10px; }
.card-row { display: flex; align-items: center; gap: 8px; }
.card-input { width: 60px; padding: 8px; text-align: center; border: 1px solid #ddd; border-radius: 6px; font-size: 15px; font-weight: 600; letter-spacing: 2px; outline: none; }
.card-input:focus { border-color: #667eea; }
.card-dash { color: #999; font-weight: 600; }

.pay-processing { text-align: center; padding: 40px 0; }
.pay-spinner { width: 48px; height: 48px; border: 4px solid #e5e7eb; border-top-color: #667eea; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.pay-processing p { color: #666; font-size: 15px; }

.pay-success { text-align: center; padding: 20px 0; }
.success-icon { margin-bottom: 16px; animation: popIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes popIn { 0% { transform: scale(0); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
.pay-success h3 { font-size: 20px; color: #67c23a; margin-bottom: 8px; }
.pay-success p { font-size: 14px; color: #999; }

/* 一起支付选项 */
.pay-both-option { margin-top: 4px; }
.both-btn { display: flex; align-items: center; gap: 8px; width: 100%; padding: 12px 16px; background: linear-gradient(135deg, #fff3e0, #ffe0b2); border: 1px solid #ffb74d; border-radius: 10px; color: #e65100; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.both-btn:hover { background: linear-gradient(135deg, #ffe0b2, #ffcc80); transform: translateY(-1px); }
.pay-both-selected { display: flex; align-items: center; gap: 10px; padding: 10px 16px; background: #e8f5e9; border: 1px solid #81c784; border-radius: 10px; margin-top: 4px; }
.both-tag { font-size: 13px; color: #2e7d32; font-weight: 600; }
.both-cancel { font-size: 12px; color: #999; background: none; border: none; cursor: pointer; text-decoration: underline; margin-left: auto; }
.total-row { font-weight: 600; color: #333; border-top: 1px solid #eee; padding-top: 8px; margin-top: 4px; }

/* 移动端适配 */
@media (max-width: 768px) {
  .main { padding: 0 12px; margin: 12px auto; }
  .nav-arrow { width: 36px; height: 36px; background: rgba(255,255,255,0.8); }
  .nav-arrow svg { width: 18px; height: 18px; }
  .img-nav-left { left: 8px; }
  .img-nav-right { right: 8px; }
  .detail-wrapper { touch-action: pan-y; }
}
</style>
