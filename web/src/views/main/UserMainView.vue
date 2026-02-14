<template>
  <div class="dashboard-page">
    <el-card class="panel" shadow="never">
      <div class="panel-top">
        <div>
          <h2 class="panel-title">用户工作台</h2>
          <p class="panel-desc">下单、查单、追踪都在一个页面完成。</p>
        </div>
        <el-space wrap>
          <el-button type="primary" @click="openOrderDialog">新建订单</el-button>
          <el-button @click="loadOrders">刷新订单</el-button>
        </el-space>
      </div>

      <div class="stats-grid">
        <div class="stat-item">
          <span>我的订单</span>
          <strong>{{ dashboardStats.orderCount }}</strong>
        </div>
        <div class="stat-item">
          <span>运输中</span>
          <strong>{{ dashboardStats.transitCount }}</strong>
        </div>
        <div class="stat-item">
          <span>取消待审核</span>
          <strong>{{ dashboardStats.cancelPendingCount }}</strong>
        </div>
        <div class="stat-item">
          <span>已签收</span>
          <strong>{{ dashboardStats.signedCount }}</strong>
        </div>
      </div>
    </el-card>

    <el-row :gutter="12" class="main-row">
      <el-col :xs="24" :lg="17">
        <el-card class="panel" shadow="never">
          <template #header>
            <div class="card-head">
              <div>
                <h3>订单列表</h3>
                <p>点击订单详情查看完整信息，点击查看进度定位到地图。</p>
              </div>
              <el-space>
                <el-radio-group v-model="relationFilter" size="small" @change="loadOrders">
                  <el-radio-button label="all">全部</el-radio-button>
                  <el-radio-button label="sender">我发出的</el-radio-button>
                  <el-radio-button label="receiver">我收件的</el-radio-button>
                </el-radio-group>
                <el-button text @click="loadOrders">刷新</el-button>
              </el-space>
            </div>
          </template>

          <el-table
            class="order-table"
            :data="orders"
            stripe
            border
            row-key="id"
            max-height="560"
            table-layout="fixed"
            :header-cell-style="tableCenterStyle"
            :cell-style="tableCenterStyle"
          >
            <el-table-column label="单号信息" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="cell-inline" :title="`${row.orderNo} / ${row.waybillNo || '-'}`">
                  <span>{{ row.orderNo }}</span>
                  <span class="cell-muted">/</span>
                  <span class="cell-muted">{{ row.waybillNo || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="收件信息" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="cell-inline">
                  <span>{{ row.receiverName }}</span>
                  <el-tag size="small" :type="normalizeRelationType(row.relationType) === 'RECEIVER' ? 'warning' : 'info'">
                    {{ relationTypeText(row.relationType) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" min-width="150">
              <template #default="{ row }">
                <el-space wrap>
                  <el-tag :type="row.status === 'CANCELLED' && row.refunded ? 'success' : orderStatusTagType(row.status)">
                    {{ orderStatusDisplayText(row.status, row.refunded, row.paid) }}
                  </el-tag>
                  <el-tag v-if="row.status !== 'CANCELLED'" :type="row.paid ? 'success' : 'warning'" effect="plain">
                    {{ paymentStatusText(row.payType, row.paid) }}
                  </el-tag>
                </el-space>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ formatTimeCompact(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="130">
              <template #default="{ row }">
                <el-dropdown @command="(cmd) => handleOrderAction(cmd as string, row)">
                  <el-button type="primary" text>更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="detail">订单详情</el-dropdown-item>
                      <el-dropdown-item command="track">查看进度</el-dropdown-item>
                      <el-dropdown-item command="pay" :disabled="!canOnlinePay(row)">在线支付</el-dropdown-item>
                      <el-dropdown-item command="receiver" :disabled="!canEditReceiver(row.status, row.relationType)">
                        修改收货地
                      </el-dropdown-item>
                      <el-dropdown-item command="cancel" :disabled="!canRequestCancel(row.status, row.relationType)">
                        取消订单
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无订单，点击右上角“新建订单”开始" :image-size="72" />
            </template>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="7">
        <el-card class="panel" shadow="never">
          <template #header>
            <div class="card-head card-head--compact">
              <div>
                <h3>物流追踪</h3>
                <p>输入运单号后自动实时刷新。</p>
              </div>
            </div>
          </template>

          <el-input v-model="trackingWaybillNo" placeholder="请输入运单号" clearable>
            <template #append>
              <el-button :loading="trackingLoading" @click="handleTrackingQuery">查询</el-button>
            </template>
          </el-input>
          <div class="track-tools">
            <el-button text type="primary" :disabled="!trackingResult?.events?.length" @click="timelineDialogVisible = true">
              轨迹详情
            </el-button>
          </div>

          <template v-if="trackingProgress">
            <div class="tracking-meta">
              <span>阶段：{{ phaseText(trackingProgress.phase) }}</span>
              <span>进度：{{ Math.round(trackingProgress.progress * 100) }}%</span>
              <span>里程：{{ trackingProgress.travelledKm.toFixed(1) }}/{{ trackingProgress.distanceKm.toFixed(1) }}km</span>
              <span :class="['live-flag', { active: pollingActive }]">{{ pollingActive ? '实时刷新中' : '已停止刷新' }}</span>
            </div>

            <el-progress
              :percentage="Math.round(trackingProgress.progress * 100)"
              :status="trackingProgress.phase === 'SIGNED' ? 'success' : undefined"
              :stroke-width="14"
            />

            <div class="address-lines">
              <div>发货地：{{ trackingProgress.senderAddr }}</div>
              <div>收货地：{{ trackingProgress.receiverAddr }}</div>
            </div>

            <div class="map-compact">
              <tracking-map :progress="trackingProgress" />
            </div>
          </template>

          <el-empty v-else description="先查询运单号后显示地图与轨迹" :image-size="72" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="orderDialogVisible" width="760px" title="新建订单" destroy-on-close>
      <el-form ref="formRef" :model="orderForm" :rules="orderRules" label-width="112px">
        <el-form-item label="发货常用地址">
          <el-select
            v-model="selectedSenderAddressId"
            clearable
            filterable
            placeholder="从地址簿选择"
            style="width: 100%"
            @change="handleAddressSelect('sender', $event)"
          >
            <el-option
              v-for="item in savedAddresses"
              :key="item.id"
              :label="`${item.contactName} ${item.contactPhone} - ${item.fullAddress}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="发货地区" prop="senderRegion">
          <el-cascader
            v-model="orderForm.senderRegion"
            :options="regionOptions"
            :props="cascaderProps"
            clearable
            filterable
            style="width: 100%"
            placeholder="请选择发货省市区县"
          />
        </el-form-item>

        <el-form-item label="发货详细地址" prop="senderDetail">
          <el-input v-model="orderForm.senderDetail" placeholder="请输入发货详细地址" />
        </el-form-item>

        <el-form-item label="收货常用地址">
          <el-select
            v-model="selectedReceiverAddressId"
            clearable
            filterable
            placeholder="从地址簿选择"
            style="width: 100%"
            @change="handleAddressSelect('receiver', $event)"
          >
            <el-option
              v-for="item in savedAddresses"
              :key="`receiver-${item.id}`"
              :label="`${item.contactName} ${item.contactPhone} - ${item.fullAddress}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="orderForm.receiverName" placeholder="请输入收件人" />
        </el-form-item>

        <el-form-item label="收件手机号" prop="receiverPhone">
          <el-input v-model="orderForm.receiverPhone" maxlength="11" placeholder="请输入收件手机号" />
        </el-form-item>

        <el-form-item label="收货地区" prop="receiverRegion">
          <el-cascader
            v-model="orderForm.receiverRegion"
            :options="regionOptions"
            :props="cascaderProps"
            clearable
            filterable
            style="width: 100%"
            placeholder="请选择收货省市区县"
          />
        </el-form-item>

        <el-form-item label="收货详细地址" prop="receiverDetail">
          <el-input v-model="orderForm.receiverDetail" placeholder="请输入收货详细地址" />
        </el-form-item>

        <el-row :gutter="10">
          <el-col :xs="24" :sm="8">
            <el-form-item label="重量kg" prop="weight" label-width="72px">
              <el-input-number v-model="orderForm.weight" :precision="2" :step="0.1" :min="0.1" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="体积cm³" prop="volume" label-width="80px">
              <el-input-number v-model="orderForm.volume" :precision="0" :step="100" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="保价金额" prop="insuredAmount" label-width="80px">
              <el-input-number v-model="orderForm.insuredAmount" :precision="2" :step="100" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="服务类型">
          <el-radio-group v-model="orderForm.serviceType">
            <el-radio :value="1">标准</el-radio>
            <el-radio :value="2">加急</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="支付方式">
          <el-radio-group v-model="orderForm.payType">
            <el-radio :value="1">在线支付</el-radio>
            <el-radio :value="2">到付</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-alert v-if="quoteResult" :closable="false" type="success" show-icon class="quote-alert">
          试算运费：<strong>¥ {{ Number(quoteResult.totalFee).toFixed(2) }}</strong>
          （计费重 {{ quoteResult.chargeWeight }}kg）
        </el-alert>
      </el-form>

      <template #footer>
        <el-space>
          <el-button @click="resetOrderForm">清空</el-button>
          <el-button :loading="quoteLoading" @click="handleQuote">运费试算</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmitOrder">提交订单</el-button>
        </el-space>
      </template>
    </el-dialog>

    <el-dialog v-model="receiverDialogVisible" width="560px" title="修改收货信息并重计费">
      <el-alert type="info" :closable="false" show-icon class="dialog-tip">
        修改后将按新收货地址重新计算运费。已发起取消审核的订单不可修改。
      </el-alert>
      <el-form ref="receiverFormRef" :model="receiverEditForm" :rules="receiverEditRules" label-width="96px">
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="receiverEditForm.receiverName" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="receiverEditForm.receiverPhone" maxlength="11" />
        </el-form-item>
        <el-form-item label="省市区县" prop="receiverRegion">
          <el-cascader
            v-model="receiverEditForm.receiverRegion"
            :options="regionOptions"
            :props="cascaderProps"
            filterable
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="receiverDetail">
          <el-input v-model="receiverEditForm.receiverDetail" maxlength="255" />
        </el-form-item>
        <el-form-item label="修改原因" prop="reason">
          <el-input v-model="receiverEditForm.reason" maxlength="100" placeholder="可选，如：收件人改到公司地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiverDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="receiverEditLoading" @click="submitReceiverUpdate">保存并重计费</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paymentDialogVisible" width="460px" title="在线支付" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon class="dialog-tip">
        请扫码完成支付，点击“我已支付”后将更新订单状态。
      </el-alert>
      <el-skeleton :loading="paymentLoading" animated :rows="4">
        <template #default>
          <div v-if="paymentInfo" class="payment-body">
            <p class="payment-title">订单号：{{ paymentInfo.orderNo }}</p>
            <p class="payment-title">运单号：{{ paymentInfo.waybillNo }}</p>
            <p class="payment-amount">应付金额：¥{{ Number(paymentInfo.amount).toFixed(2) }}</p>
            <el-image
              class="payment-qr"
              :src="paymentQrImageUrl"
              fit="contain"
              :preview-src-list="[paymentQrImageUrl]"
              preview-teleported
            />
          </div>
        </template>
      </el-skeleton>
      <template #footer>
        <el-space>
          <el-button @click="paymentDialogVisible = false">稍后支付</el-button>
          <el-button type="primary" :loading="paymentConfirmLoading" @click="confirmOnlinePaymentByDialog">我已支付</el-button>
        </el-space>
      </template>
    </el-dialog>

    <el-dialog v-model="timelineDialogVisible" width="680px" title="轨迹详情" destroy-on-close>
      <el-timeline v-if="trackingResult?.events?.length" class="timeline-dialog-list">
        <el-timeline-item
          v-for="item in trackingResult.events"
          :key="`${item.eventType}-${item.eventTime}`"
          :timestamp="formatTime(item.eventTime)"
          placement="top"
        >
          <strong>{{ trackingEventText(item.eventType) }}</strong>
          <p>{{ item.description }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无轨迹事件" :image-size="72" />
    </el-dialog>

    <order-detail-dialog v-model="detailDialogVisible" :order-id="detailOrderId" role="user" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type CascaderOption, type FormInstance, type FormRules } from 'element-plus'
import { pcaTextArr } from 'element-china-area-data'
import TrackingMap from '../../components/TrackingMap.vue'
import OrderDetailDialog from '../../components/OrderDetailDialog.vue'
import {
  cancelOrder,
  confirmOrderOnlinePayment,
  createOrder,
  getOrderPaymentQrcode,
  listMyOrders,
  queryTracking,
  queryTrackingProgress,
  quotePrice,
  updateOrderReceiver,
} from '../../api/order'
import { createProfileAddress, listProfileAddresses } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'
import type { ProfileAddress } from '../../types/auth'
import type {
  OrderListItem,
  OrderPaymentQrcodeResponse,
  PricingQuoteResponse,
  TrackingProgressResponse,
  TrackingQueryResponse,
} from '../../types/workbench'
import { orderStatusDisplayText, orderStatusTagType, paymentStatusText, phaseText, trackingEventText } from '../../utils/status'

const POLL_INTERVAL_MS = 1000
const EVENT_REFRESH_TICK = 5

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const receiverFormRef = ref<FormInstance>()

const orderDialogVisible = ref(false)
const orders = ref<OrderListItem[]>([])
const trackingWaybillNo = ref('')
const trackingResult = ref<TrackingQueryResponse | null>(null)
const trackingProgress = ref<TrackingProgressResponse | null>(null)
const trackingLoading = ref(false)
const pollingActive = ref(false)
const relationFilter = ref<'all' | 'sender' | 'receiver'>('all')

const savedAddresses = ref<ProfileAddress[]>([])
const selectedSenderAddressId = ref<number | null>(null)
const selectedReceiverAddressId = ref<number | null>(null)

const quoteResult = ref<PricingQuoteResponse | null>(null)
const quoteLoading = ref(false)
const submitLoading = ref(false)

const receiverDialogVisible = ref(false)
const receiverEditLoading = ref(false)
const paymentDialogVisible = ref(false)
const paymentLoading = ref(false)
const paymentConfirmLoading = ref(false)
const paymentInfo = ref<OrderPaymentQrcodeResponse | null>(null)
const detailDialogVisible = ref(false)
const detailOrderId = ref<number | null>(null)
const timelineDialogVisible = ref(false)

const receiverEditForm = reactive({
  orderId: 0,
  receiverName: '',
  receiverPhone: '',
  receiverRegion: [] as string[],
  receiverDetail: '',
  reason: '',
})

const receiverEditRules: FormRules = {
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入收件手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  receiverRegion: [{ validator: validateRegion, trigger: 'change' }],
  receiverDetail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

let pollingTimer: number | null = null
let pollingBusy = false
let pollTick = 0
let pollErrorNotified = false

const regionOptions = pcaTextArr as CascaderOption[]
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  emitPath: true,
  checkStrictly: false,
}

const orderForm = reactive({
  senderRegion: [] as string[],
  senderDetail: '',
  receiverName: '',
  receiverPhone: '',
  receiverRegion: [] as string[],
  receiverDetail: '',
  weight: 1.2,
  volume: 8000,
  insuredAmount: 0,
  serviceType: 1,
  payType: 1,
})

const orderRules: FormRules = {
  senderRegion: [{ validator: validateRegion, trigger: 'change' }],
  senderDetail: [{ required: true, message: '请输入发货详细地址', trigger: 'blur' }],
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入收件手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  receiverRegion: [{ validator: validateRegion, trigger: 'change' }],
  receiverDetail: [{ required: true, message: '请输入收货详细地址', trigger: 'blur' }],
}

const dashboardStats = computed(() => {
  const orderCount = orders.value.length
  const transitCount = orders.value.filter((item) => ['IN_TRANSIT', 'DELIVERING'].includes(item.status)).length
  const cancelPendingCount = orders.value.filter((item) => item.status === 'CANCEL_PENDING').length
  const signedCount = orders.value.filter((item) => item.status === 'SIGNED').length
  return { orderCount, transitCount, cancelPendingCount, signedCount }
})

const paymentQrImageUrl = computed(() => {
  if (!paymentInfo.value?.qrCodeText) {
    return ''
  }
  return `https://api.qrserver.com/v1/create-qr-code/?size=260x260&margin=12&data=${encodeURIComponent(paymentInfo.value.qrCodeText)}`
})
const tableCenterStyle = { textAlign: 'center' as const }

void loadOrders()
void loadSavedAddresses()

onBeforeUnmount(() => {
  stopTrackingPolling()
})

function openOrderDialog() {
  orderDialogVisible.value = true
  if (savedAddresses.value.length && !selectedSenderAddressId.value && !selectedReceiverAddressId.value) {
    const defaultAddress = savedAddresses.value.find((item) => item.isDefault) ?? savedAddresses.value[0]
    selectedSenderAddressId.value = defaultAddress.id
    handleAddressSelect('sender', defaultAddress.id)
  }
}

async function loadOrders() {
  try {
    const data = await listMyOrders({ page: 1, size: 50, relation: relationFilter.value })
    orders.value = data.records
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单加载失败')
  }
}

async function loadSavedAddresses() {
  try {
    savedAddresses.value = await listProfileAddresses()
  } catch (error) {
    savedAddresses.value = []
    console.warn('地址簿加载失败，已降级为空列表', error)
  }
}

function handleAddressSelect(target: 'sender' | 'receiver', addressId: number | null) {
  if (addressId == null) {
    return
  }
  const address = savedAddresses.value.find((item) => item.id === addressId)
  if (!address) {
    return
  }

  const region = [address.province, address.city, address.district]
  if (target === 'sender') {
    orderForm.senderRegion = region
    orderForm.senderDetail = address.detail
    return
  }

  orderForm.receiverName = address.contactName
  orderForm.receiverPhone = address.contactPhone
  orderForm.receiverRegion = region
  orderForm.receiverDetail = address.detail
}

function validateRegion(_rule: unknown, value: string[], callback: (err?: Error) => void) {
  if (!Array.isArray(value) || value.length < 3) {
    callback(new Error('请选择省市区县'))
    return
  }
  callback()
}

function formatRegion(regions: string[]): string {
  return regions.slice(0, 3).join(' ')
}

function buildAddress(regions: string[], detail: string): string {
  return `${formatRegion(regions)} ${detail.trim()}`.trim()
}

function resetOrderForm() {
  orderForm.senderRegion = []
  orderForm.senderDetail = ''
  orderForm.receiverName = ''
  orderForm.receiverPhone = ''
  orderForm.receiverRegion = []
  orderForm.receiverDetail = ''
  orderForm.weight = 1.2
  orderForm.volume = 8000
  orderForm.insuredAmount = 0
  orderForm.serviceType = 1
  orderForm.payType = 1
  selectedSenderAddressId.value = null
  selectedReceiverAddressId.value = null
  quoteResult.value = null
}

async function handleQuote() {
  if (orderForm.senderRegion.length < 3 || orderForm.receiverRegion.length < 3) {
    ElMessage.warning('请先选择发货地区和收货地区')
    return
  }
  if (!orderForm.senderDetail.trim() || !orderForm.receiverDetail.trim()) {
    ElMessage.warning('请填写详细地址')
    return
  }

  quoteLoading.value = true
  try {
    quoteResult.value = await quotePrice({
      senderAddr: buildAddress(orderForm.senderRegion, orderForm.senderDetail),
      receiverAddr: buildAddress(orderForm.receiverRegion, orderForm.receiverDetail),
      weight: orderForm.weight,
      volume: orderForm.volume,
      serviceType: orderForm.serviceType,
      insuredAmount: orderForm.insuredAmount,
    })
    ElMessage.success('试算完成')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试算失败')
  } finally {
    quoteLoading.value = false
  }
}

async function handleSubmitOrder() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    const senderName = authStore.user?.username || '用户'
    const senderPhone = authStore.user?.phone || '13800000000'

    const result = await createOrder({
      senderName,
      senderPhone,
      senderAddr: buildAddress(orderForm.senderRegion, orderForm.senderDetail),
      receiverName: orderForm.receiverName,
      receiverPhone: orderForm.receiverPhone,
      receiverAddr: buildAddress(orderForm.receiverRegion, orderForm.receiverDetail),
      serviceType: orderForm.serviceType,
      payType: orderForm.payType,
      weight: orderForm.weight,
      volume: orderForm.volume,
      insuredAmount: orderForm.insuredAmount,
      remark: '用户在线下单',
    })

    ElMessage.success(`下单成功，运单号：${result.waybillNo}`)
    orderDialogVisible.value = false
    trackingWaybillNo.value = result.waybillNo
    await handleTrackingQuery()
    await loadOrders()
    await suggestSaveNewAddresses(senderName, senderPhone)
    if (result.payType === 1) {
      await openOnlinePaymentDialog(result.orderId)
    }
    resetOrderForm()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下单失败')
  } finally {
    submitLoading.value = false
  }
}

function isAddressExists(payload: {
  contactName: string
  contactPhone: string
  province: string
  city: string
  district: string
  detail: string
}): boolean {
  const normalize = (value: string) => value.trim()
  return savedAddresses.value.some((item) => {
    return (
      normalize(item.contactName) === normalize(payload.contactName) &&
      normalize(item.contactPhone) === normalize(payload.contactPhone) &&
      normalize(item.province) === normalize(payload.province) &&
      normalize(item.city) === normalize(payload.city) &&
      normalize(item.district) === normalize(payload.district) &&
      normalize(item.detail) === normalize(payload.detail)
    )
  })
}

async function suggestSaveNewAddresses(senderName: string, senderPhone: string) {
  const senderPayload = {
    contactName: senderName.trim(),
    contactPhone: senderPhone.trim(),
    province: orderForm.senderRegion[0]?.trim() || '',
    city: orderForm.senderRegion[1]?.trim() || '',
    district: orderForm.senderRegion[2]?.trim() || '',
    detail: orderForm.senderDetail.trim(),
    isDefault: false,
  }
  const receiverPayload = {
    contactName: orderForm.receiverName.trim(),
    contactPhone: orderForm.receiverPhone.trim(),
    province: orderForm.receiverRegion[0]?.trim() || '',
    city: orderForm.receiverRegion[1]?.trim() || '',
    district: orderForm.receiverRegion[2]?.trim() || '',
    detail: orderForm.receiverDetail.trim(),
    isDefault: false,
  }

  const candidates = [
    { label: '发货地址', payload: senderPayload },
    { label: '收货地址', payload: receiverPayload },
  ].filter((item) => {
    return (
      item.payload.contactName &&
      item.payload.contactPhone &&
      item.payload.province &&
      item.payload.city &&
      item.payload.district &&
      item.payload.detail &&
      !isAddressExists(item.payload)
    )
  })

  if (!candidates.length) {
    return
  }

  const labels = candidates.map((item) => item.label).join('、')
  try {
    await ElMessageBox.confirm(`检测到新的${labels}，是否添加到常用地址？`, '保存常用地址', {
      confirmButtonText: '添加',
      cancelButtonText: '不添加',
      type: 'info',
    })
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    throw error
  }

  try {
    for (const item of candidates) {
      await createProfileAddress(item.payload)
    }
    await loadSavedAddresses()
    ElMessage.success('已添加到常用地址')
  } catch (error) {
    ElMessage.warning(error instanceof Error ? `地址保存失败：${error.message}` : '地址保存失败')
  }
}

async function requestCancel(orderId: number) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      confirmButtonText: '提交',
      cancelButtonText: '放弃',
      inputPlaceholder: '例如：收件信息有误',
      inputValidator: (text) => {
        if (!text || !text.trim()) {
          return '取消原因不能为空'
        }
        if (text.trim().length > 100) {
          return '取消原因不能超过100字'
        }
        return true
      },
    })
    await cancelOrder(orderId, { reason: value.trim() })
    ElMessage.success('取消申请已提交')
    await loadOrders()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '取消订单失败')
  }
}

function openReceiverDialog(row: OrderListItem) {
  receiverEditForm.orderId = row.id
  receiverEditForm.receiverName = row.receiverName
  receiverEditForm.receiverPhone = row.receiverPhone
  receiverEditForm.receiverRegion = []
  receiverEditForm.receiverDetail = ''
  receiverEditForm.reason = ''
  receiverDialogVisible.value = true
}

async function submitReceiverUpdate() {
  if (!receiverFormRef.value) {
    return
  }
  const valid = await receiverFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  receiverEditLoading.value = true
  try {
    const result = await updateOrderReceiver(receiverEditForm.orderId, {
      receiverName: receiverEditForm.receiverName.trim(),
      receiverPhone: receiverEditForm.receiverPhone.trim(),
      receiverAddr: buildAddress(receiverEditForm.receiverRegion, receiverEditForm.receiverDetail),
      reason: receiverEditForm.reason.trim() || undefined,
    })
    ElMessage.success(
      `收货地已更新，运费：¥${Number(result.oldFeeTotal).toFixed(2)} -> ¥${Number(result.newFeeTotal).toFixed(2)}`,
    )
    receiverDialogVisible.value = false
    await loadOrders()
    await quickTrack(result.waybillNo)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改收货地失败')
  } finally {
    receiverEditLoading.value = false
  }
}

function canRequestCancel(status: string, relationType?: string): boolean {
  if (normalizeRelationType(relationType) !== 'SENDER') {
    return false
  }
  const normalizedStatus = normalizeStatus(status)
  return ['CREATED', 'IN_TRANSIT', 'DELIVERING'].includes(normalizedStatus)
}

function canEditReceiver(status: string, relationType?: string): boolean {
  if (normalizeRelationType(relationType) !== 'SENDER') {
    return false
  }
  const normalizedStatus = normalizeStatus(status)
  return ['CREATED', 'IN_TRANSIT', 'DELIVERING'].includes(normalizedStatus)
}

function canOnlinePay(row: OrderListItem): boolean {
  if (normalizeRelationType(row.relationType) !== 'SENDER') {
    return false
  }
  if (row.payType !== 1 || row.paid) {
    return false
  }
  return !['CANCELLED', 'SIGNED'].includes(normalizeStatus(row.status))
}

function normalizeStatus(status?: string): string {
  return (status || '').trim().toUpperCase()
}

function normalizeRelationType(relationType?: string): string {
  return (relationType || '').trim().toUpperCase()
}

function relationTypeText(relationType?: string): string {
  return normalizeRelationType(relationType) === 'RECEIVER' ? '收件' : '发件'
}

function openOrderDetail(orderId: number) {
  detailOrderId.value = orderId
  detailDialogVisible.value = true
}

async function handleTrackingQuery() {
  if (!trackingWaybillNo.value.trim()) {
    ElMessage.warning('请输入运单号')
    return
  }
  trackingLoading.value = true
  try {
    const waybillNo = trackingWaybillNo.value.trim()
    await refreshTracking(waybillNo, true)
    startTrackingPolling(waybillNo)
  } catch (error) {
    stopTrackingPolling()
    trackingResult.value = null
    trackingProgress.value = null
    ElMessage.error(error instanceof Error ? error.message : '轨迹查询失败')
  } finally {
    trackingLoading.value = false
  }
}

async function quickTrack(waybillNo: string) {
  if (!waybillNo) {
    ElMessage.warning('当前订单暂无运单号')
    return
  }
  trackingWaybillNo.value = waybillNo
  await handleTrackingQuery()
}

function handleOrderAction(command: string, row: OrderListItem) {
  if (command === 'detail') {
    openOrderDetail(row.id)
    return
  }
  if (command === 'track') {
    void quickTrack(row.waybillNo)
    return
  }
  if (command === 'pay') {
    if (!canOnlinePay(row)) {
      ElMessage.warning('当前订单不可在线支付')
      return
    }
    void openOnlinePaymentDialog(row.id)
    return
  }
  if (command === 'receiver') {
    if (!canEditReceiver(row.status, row.relationType)) {
      ElMessage.warning('当前状态不可修改收货地')
      return
    }
    openReceiverDialog(row)
    return
  }
  if (command === 'cancel') {
    if (!canRequestCancel(row.status, row.relationType)) {
      ElMessage.warning('当前状态不可取消')
      return
    }
    void requestCancel(row.id)
  }
}

async function openOnlinePaymentDialog(orderId: number) {
  paymentDialogVisible.value = true
  paymentLoading.value = true
  paymentInfo.value = null
  try {
    paymentInfo.value = await getOrderPaymentQrcode(orderId)
  } catch (error) {
    paymentDialogVisible.value = false
    ElMessage.error(error instanceof Error ? error.message : '获取支付二维码失败')
  } finally {
    paymentLoading.value = false
  }
}

async function confirmOnlinePaymentByDialog() {
  if (!paymentInfo.value) {
    return
  }
  paymentConfirmLoading.value = true
  try {
    await confirmOrderOnlinePayment(paymentInfo.value.orderId)
    ElMessage.success('支付成功')
    paymentDialogVisible.value = false
    await loadOrders()
    await quickTrack(paymentInfo.value.waybillNo)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '支付确认失败')
  } finally {
    paymentConfirmLoading.value = false
  }
}

function startTrackingPolling(waybillNo: string) {
  stopTrackingPolling()
  pollTick = 0
  pollErrorNotified = false
  pollingActive.value = true

  pollingTimer = window.setInterval(async () => {
    if (pollingBusy) {
      return
    }
    pollingBusy = true
    try {
      await refreshTracking(waybillNo, false)
      pollErrorNotified = false
      if (trackingProgress.value?.phase === 'SIGNED' || trackingProgress.value?.phase === 'CANCELLED') {
        stopTrackingPolling()
      }
    } catch {
      if (!pollErrorNotified) {
        ElMessage.warning('实时更新中断，正在重试')
        pollErrorNotified = true
      }
    } finally {
      pollingBusy = false
    }
  }, POLL_INTERVAL_MS)
}

function stopTrackingPolling() {
  if (pollingTimer != null) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
  pollingActive.value = false
}

async function refreshTracking(waybillNo: string, forceEventRefresh: boolean) {
  trackingProgress.value = await queryTrackingProgress(waybillNo)
  pollTick += 1
  if (forceEventRefresh || pollTick % EVENT_REFRESH_TICK === 0 || !trackingResult.value) {
    trackingResult.value = await queryTracking(waybillNo)
  }
}

function formatTime(timeText: string) {
  return new Date(timeText).toLocaleString('zh-CN', { hour12: false })
}

function formatTimeCompact(timeText: string) {
  const date = new Date(timeText)
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${year}/${month}/${day} ${hour}:${minute}`
}
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 12px;
}

.panel {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.92);
}

.panel-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.panel-title {
  margin: 0;
  color: #1f4368;
}

.panel-desc {
  margin: 4px 0 0;
  color: #6d849b;
  font-size: 13px;
}

.stats-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.stat-item {
  border-radius: 10px;
  padding: 10px 12px;
  background: #f6f9fe;
  border: 1px solid #e4ecf8;
  display: grid;
  gap: 4px;
}

.stat-item span {
  color: #62809b;
  font-size: 12px;
}

.stat-item strong {
  color: #205281;
  font-size: 22px;
}

.main-row {
  width: 100%;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-head--compact {
  align-items: flex-start;
}

.card-head h3 {
  margin: 0;
  color: #1f4368;
}

.card-head p {
  margin: 4px 0 0;
  color: #6d849b;
  font-size: 12px;
}

.cell-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 0;
  width: 100%;
}

.cell-muted {
  color: #6e849b;
  font-size: 12px;
}

.tracking-meta {
  margin: 10px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #365f86;
  font-size: 13px;
}

.live-flag {
  color: #6b7280;
  font-weight: 600;
}

.live-flag.active {
  color: #16a34a;
}

.address-lines {
  margin: 10px 0;
  display: grid;
  gap: 4px;
  color: #41698f;
  font-size: 13px;
}

.quote-alert {
  margin-top: 4px;
}

.dialog-tip {
  margin-bottom: 12px;
}

.payment-body {
  display: grid;
  justify-items: center;
  gap: 8px;
}

.payment-title {
  margin: 0;
  color: #3f6284;
  font-size: 13px;
}

.payment-amount {
  margin: 2px 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #205281;
}

.payment-qr {
  width: 260px;
  height: 260px;
  border-radius: 12px;
  border: 1px solid #e4ecf8;
  background: #fff;
}

:deep(.el-table .cell) {
  white-space: normal;
  word-break: break-word;
}

.order-table :deep(.el-tag) {
  vertical-align: middle;
}

.order-table :deep(.el-table__body-wrapper) {
  overflow-x: hidden;
}

.track-tools {
  margin-top: 4px;
  display: flex;
  justify-content: flex-end;
}

.map-compact {
  margin-top: 6px;
}

.map-compact :deep(.tracking-map) {
  height: 280px;
}

.timeline-dialog-list {
  max-height: 420px;
  overflow: auto;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .card-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
