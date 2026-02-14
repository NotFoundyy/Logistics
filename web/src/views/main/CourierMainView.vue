<template>
  <div class="dashboard-page">
    <el-card class="panel" shadow="never">
      <div class="panel-top">
        <div>
          <h2 class="panel-title">快递员工作台</h2>
          <p class="panel-desc">接单、取消审核、查看轨迹在同一页面完成。</p>
        </div>
        <el-space>
          <el-select v-model="statusFilter" style="width: 150px" @change="loadTasks">
            <el-option label="全部任务" value="" />
            <el-option label="待接单" value="PENDING" />
            <el-option label="运输中" value="ACCEPTED" />
            <el-option label="已完成" value="FINISHED" />
          </el-select>
          <el-button @click="resetTaskFilter">重置</el-button>
          <el-button type="primary" @click="loadAll">刷新</el-button>
        </el-space>
      </div>

      <div class="stats-grid">
        <div class="stat-item">
          <span>待接单</span>
          <strong>{{ overview.metrics.pendingTasks ?? 0 }}</strong>
        </div>
        <div class="stat-item">
          <span>运输中</span>
          <strong>{{ overview.metrics.acceptedTasks ?? 0 }}</strong>
        </div>
        <div class="stat-item">
          <span>已完成</span>
          <strong>{{ overview.metrics.finishedTasks ?? 0 }}</strong>
        </div>
        <div class="stat-item">
          <span>任务总量</span>
          <strong>{{ overview.metrics.allTasks ?? 0 }}</strong>
        </div>
      </div>
    </el-card>

    <el-row :gutter="12" class="main-row">
      <el-col :xs="24" :lg="14">
        <el-card class="panel" shadow="never">
          <template #header>
            <div class="card-head">
              <div>
                <h3>任务列表</h3>
                <p>优先接“待接单”，取消审核可在操作列直接处理。</p>
              </div>
            </div>
          </template>

          <el-table
            class="task-table"
            :data="tasks"
            stripe
            border
            row-key="taskId"
            max-height="560"
            table-layout="fixed"
            :header-cell-style="tableCenterStyle"
            :cell-style="tableCenterStyle"
          >
            <el-table-column label="任务信息" min-width="230" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="cell-stack">
                  <div>#{{ row.taskId }} · {{ row.orderNo }}</div>
                  <small class="cell-muted">{{ row.waybillNo }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="150">
              <template #default="{ row }">
                <div class="cell-stack">
                  <el-tag :type="row.orderStatus === 'CANCELLED' && row.refunded ? 'success' : orderStatusTagType(row.orderStatus)">
                    {{ orderStatusDisplayText(row.orderStatus, row.refunded, row.paid) }}
                  </el-tag>
                  <el-tag v-if="row.orderStatus !== 'CANCELLED'" :type="row.paid ? 'success' : 'warning'" effect="plain">
                    {{ paymentStatusText(row.payType, row.paid) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="收件信息" min-width="146" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="cell-stack">
                  <div>{{ row.receiverName || '-' }}</div>
                  <small class="cell-muted">{{ row.receiverPhone || '-' }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="252">
              <template #default="{ row }">
                <el-space wrap>
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="row.taskStatus !== 'PENDING'"
                    :loading="acceptingId === row.taskId"
                    @click="acceptTask(row.taskId, row.waybillNo)"
                  >
                    接单
                  </el-button>
                  <el-button
                    size="small"
                    type="warning"
                    plain
                    :disabled="!canSign(row)"
                    @click="signOrder(row)"
                  >
                    签收
                  </el-button>
                  <el-button size="small" type="success" plain @click="openProgress(row.waybillNo)">查看进度</el-button>
                  <el-dropdown @command="(cmd) => handleTaskAction(cmd as string, row)">
                    <el-button size="small" text type="primary">更多</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="detail">订单详情</el-dropdown-item>
                        <el-dropdown-item command="approveCancel" :disabled="!canApproveCancel(row)">同意取消</el-dropdown-item>
                        <el-dropdown-item command="rejectCancel" :disabled="!canApproveCancel(row)">拒绝取消</el-dropdown-item>
                        <el-dropdown-item command="cancelByCourier" :disabled="!canCourierCancel(row)">主动取消</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </el-space>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无任务" :image-size="72" />
            </template>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card class="panel" shadow="never">
          <template #header>
            <div class="card-head card-head--compact">
              <div>
                <h3>运单追踪</h3>
                <p>点击任务“查看进度”后展示实时轨迹。</p>
              </div>
              <el-tag v-if="selectedWaybillNo" type="info" effect="plain">{{ selectedWaybillNo }}</el-tag>
            </div>
          </template>

          <el-empty v-if="!selectedWaybillNo" description="在任务列表中选择运单查看" :image-size="70" />

          <template v-else>
            <div v-if="progress" class="tracking-meta">
              <span>阶段：{{ phaseText(progress.phase) }}</span>
              <span>进度：{{ Math.round(progress.progress * 100) }}%</span>
              <span>里程：{{ progress.travelledKm.toFixed(1) }}/{{ progress.distanceKm.toFixed(1) }}km</span>
              <span :class="['live-flag', { active: pollingActive }]">{{ pollingActive ? '实时刷新中' : '已停止刷新' }}</span>
            </div>

            <el-progress
              v-if="progress"
              :percentage="Math.round(progress.progress * 100)"
              :status="progress.phase === 'SIGNED' ? 'success' : undefined"
              :stroke-width="14"
            />

            <div v-if="progress" class="address-lines">
              <div>发货地：{{ progress.senderAddr }}</div>
              <div>收货地：{{ progress.receiverAddr }}</div>
            </div>

            <tracking-map v-if="progress" :progress="progress" />

            <el-timeline v-if="events.length" class="timeline-list">
              <el-timeline-item
                v-for="item in events"
                :key="`${item.eventType}-${item.eventTime}`"
                :timestamp="formatTime(item.eventTime)"
              >
                <strong>{{ trackingEventText(item.eventType) }}</strong>
                <p>{{ item.description }}</p>
              </el-timeline-item>
            </el-timeline>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <order-detail-dialog v-model="detailDialogVisible" :order-id="detailOrderId" role="courier" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TrackingMap from '../../components/TrackingMap.vue'
import OrderDetailDialog from '../../components/OrderDetailDialog.vue'
import { queryTracking, queryTrackingProgress } from '../../api/order'
import {
  acceptCourierTask,
  cancelOrderByCourier,
  fetchCourierOverview,
  fetchCourierTasks,
  reviewCancelRequest,
  signCourierOrder,
} from '../../api/workbench'
import type { CourierTaskItem, TrackingEvent, TrackingProgressResponse, WorkbenchOverviewResponse } from '../../types/workbench'
import { orderStatusDisplayText, orderStatusTagType, paymentStatusText, phaseText, trackingEventText } from '../../utils/status'

const POLL_INTERVAL_MS = 1000
const EVENT_REFRESH_TICK = 2

const overview = ref<WorkbenchOverviewResponse>({ metrics: {} })
const tasks = ref<CourierTaskItem[]>([])
const statusFilter = ref('')
const acceptingId = ref<number | null>(null)

const selectedWaybillNo = ref('')
const progress = ref<TrackingProgressResponse | null>(null)
const events = ref<TrackingEvent[]>([])
const pollingActive = ref(false)
const detailDialogVisible = ref(false)
const detailOrderId = ref<number | null>(null)
const tableCenterStyle = { textAlign: 'center' as const }

let pollingTimer: number | null = null
let pollingBusy = false
let pollTick = 0
let pollErrorNotified = false

void loadAll()

onBeforeUnmount(() => {
  stopPolling()
})

async function loadAll() {
  await Promise.all([loadOverview(), loadTasks()])
}

async function loadOverview() {
  try {
    overview.value = await fetchCourierOverview()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '概览加载失败')
  }
}

async function loadTasks() {
  try {
    const data = await fetchCourierTasks({ page: 1, size: 50, status: statusFilter.value || undefined })
    tasks.value = data.records
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务加载失败')
  }
}

function resetTaskFilter() {
  statusFilter.value = ''
  void loadTasks()
}

async function acceptTask(taskId: number, waybillNo: string) {
  acceptingId.value = taskId
  try {
    await acceptCourierTask(taskId)
    ElMessage.success('接单成功，已进入运输流程')
    await loadAll()
    await openProgress(waybillNo)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '接单失败')
  } finally {
    acceptingId.value = null
  }
}

async function reviewCancel(orderId: number, approved: boolean) {
  try {
    const { value } = await ElMessageBox.prompt(
      approved ? '可填写说明（可选）' : '请输入拒绝原因',
      approved ? '同意取消申请' : '拒绝取消申请',
      {
        inputPlaceholder: approved ? '例如：已联系用户确认' : '例如：已到派送阶段，不建议取消',
        inputValidator: (text) => {
          if (!approved && (!text || !text.trim())) {
            return '拒绝时请填写原因'
          }
          if (text && text.trim().length > 100) {
            return '说明不能超过100字'
          }
          return true
        },
      },
    )
    await reviewCancelRequest(orderId, {
      approved,
      reason: value?.trim() || undefined,
    })
    ElMessage.success(approved ? '已同意取消并通知用户' : '已拒绝取消申请')
    await loadAll()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '取消审核失败')
  }
}

async function cancelByCourier(taskId: number) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因（将通知用户）', '快递员主动取消', {
      inputPlaceholder: '例如：地址不可达，已联系用户',
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
    await cancelOrderByCourier(taskId, { reason: value.trim() })
    ElMessage.success('订单已取消并通知用户')
    await loadAll()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '取消订单失败')
  }
}

function canApproveCancel(row: CourierTaskItem): boolean {
  return normalizeStatus(row.taskStatus) === 'ACCEPTED' && normalizeStatus(row.orderStatus) === 'CANCEL_PENDING'
}

function canCourierCancel(row: CourierTaskItem): boolean {
  return normalizeStatus(row.taskStatus) === 'ACCEPTED' && !['SIGNED', 'CANCELLED'].includes(normalizeStatus(row.orderStatus))
}

function canSign(row: CourierTaskItem): boolean {
  if (normalizeStatus(row.taskStatus) !== 'ACCEPTED') {
    return false
  }
  if (row.payType === 1 && !row.paid) {
    return false
  }
  return !['SIGNED', 'CANCELLED', 'CANCEL_PENDING'].includes(normalizeStatus(row.orderStatus))
}

function openOrderDetail(orderId: number) {
  detailOrderId.value = orderId
  detailDialogVisible.value = true
}

async function openProgress(waybillNo: string) {
  selectedWaybillNo.value = waybillNo
  try {
    await refreshProgress(waybillNo, true)
    startPolling(waybillNo)
  } catch (error) {
    stopPolling()
    events.value = []
    progress.value = null
    ElMessage.error(error instanceof Error ? error.message : '进度查询失败')
  }
}

function handleTaskAction(command: string, row: CourierTaskItem) {
  if (command === 'detail') {
    openOrderDetail(row.orderId)
    return
  }
  if (command === 'approveCancel') {
    if (!canApproveCancel(row)) {
      ElMessage.warning('当前任务不可审核取消')
      return
    }
    void reviewCancel(row.orderId, true)
    return
  }
  if (command === 'rejectCancel') {
    if (!canApproveCancel(row)) {
      ElMessage.warning('当前任务不可审核取消')
      return
    }
    void reviewCancel(row.orderId, false)
    return
  }
  if (command === 'cancelByCourier') {
    if (!canCourierCancel(row)) {
      ElMessage.warning('当前任务不可主动取消')
      return
    }
    void cancelByCourier(row.taskId)
  }
}

async function signOrder(row: CourierTaskItem) {
  try {
    if (row.payType === 2 && !row.paid) {
      await ElMessageBox.confirm('该订单为到付，请确认已收款后再签收。', '到付签收确认', {
        confirmButtonText: '已收款并签收',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } else {
      await ElMessageBox.confirm('确认已完成签收？', '签收确认', {
        confirmButtonText: '确认签收',
        cancelButtonText: '取消',
        type: 'info',
      })
    }
    await signCourierOrder(row.taskId, {
      paidConfirmed: row.payType === 2 ? true : undefined,
    })
    ElMessage.success('签收成功')
    await loadAll()
    await openProgress(row.waybillNo)
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '签收失败')
  }
}

function startPolling(waybillNo: string) {
  stopPolling()
  pollTick = 0
  pollErrorNotified = false
  pollingActive.value = true

  pollingTimer = window.setInterval(async () => {
    if (pollingBusy) {
      return
    }
    pollingBusy = true
    try {
      await refreshProgress(waybillNo, false)
      pollErrorNotified = false
      if (progress.value?.phase === 'SIGNED' || progress.value?.phase === 'CANCELLED') {
        stopPolling()
        await loadAll()
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

function stopPolling() {
  if (pollingTimer != null) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
  pollingActive.value = false
}

async function refreshProgress(waybillNo: string, forceEventRefresh: boolean) {
  progress.value = await queryTrackingProgress(waybillNo)
  pollTick += 1
  if (forceEventRefresh || pollTick % EVENT_REFRESH_TICK === 0 || !events.value.length) {
    const tracking = await queryTracking(waybillNo)
    events.value = tracking.events
  }
}

function formatTime(timeText: string) {
  return new Date(timeText).toLocaleString('zh-CN', { hour12: false })
}

function normalizeStatus(status?: string): string {
  return (status || '').trim().toUpperCase()
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

.cell-stack {
  display: grid;
  gap: 4px;
  justify-items: center;
  text-align: center;
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

.timeline-list {
  margin-top: 10px;
  max-height: 240px;
  overflow: auto;
}

:deep(.el-table .cell) {
  white-space: normal;
  word-break: break-word;
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
