<template>
  <div class="dashboard-page">
    <el-card class="panel" shadow="never">
      <div class="panel-top">
        <div>
          <h2 class="panel-title">管理员工作台</h2>
          <p class="panel-desc">全局状态、趋势、订单与人员管理集中在一页。</p>
        </div>
        <el-space>
          <el-button type="primary" @click="openCourierDialog">新增快递员</el-button>
          <el-button :loading="exportLoading" @click="handleExportOrders">导出Excel</el-button>
          <el-button @click="resetStatusFilter">重置筛选</el-button>
          <el-button @click="loadAll">刷新数据</el-button>
        </el-space>
      </div>

      <div class="stats-grid stats-grid--wide">
        <div class="stat-item"><span>订单总量</span><strong>{{ overview.metrics.totalOrders ?? 0 }}</strong></div>
        <div class="stat-item"><span>今日新增</span><strong>{{ overview.metrics.todayOrders ?? 0 }}</strong></div>
        <div class="stat-item"><span>处理中</span><strong>{{ processingOrders }}</strong></div>
        <div class="stat-item"><span>取消审核中</span><strong>{{ overview.metrics.cancelPendingOrders ?? 0 }}</strong></div>
        <div class="stat-item"><span>已完成</span><strong>{{ overview.metrics.signedOrders ?? 0 }}</strong></div>
        <div class="stat-item"><span>已取消</span><strong>{{ overview.metrics.cancelledOrders ?? 0 }}</strong></div>
      </div>
    </el-card>

    <el-row :gutter="12" class="main-row">
      <el-col :xs="24" :lg="14">
        <el-card class="panel" shadow="never">
          <template #header>
            <div class="card-head">
              <div>
                <h3>订单状态分布</h3>
                <p>可快速识别异常积压状态。</p>
              </div>
              <el-tag type="success" effect="plain">实时</el-tag>
            </div>
          </template>

          <div class="status-bars">
            <div v-for="item in statusBars" :key="item.status" class="status-row">
              <span>{{ item.label }}</span>
              <el-progress :stroke-width="12" :percentage="item.percent" :color="item.color" />
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <div class="task-inline">
            <el-tag effect="plain">任务待接单 {{ stats.taskStatusStats.PENDING ?? 0 }}</el-tag>
            <el-tag type="warning" effect="plain">任务运输中 {{ stats.taskStatusStats.ACCEPTED ?? 0 }}</el-tag>
            <el-tag type="success" effect="plain">任务已完成 {{ stats.taskStatusStats.FINISHED ?? 0 }}</el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card class="panel" shadow="never">
          <template #header>
            <div class="card-head card-head--compact">
              <div>
                <h3>近7日订单趋势</h3>
                <p>用于观察业务波动。</p>
              </div>
            </div>
          </template>

          <div class="trend-grid">
            <div v-for="item in stats.trend" :key="item.date" class="trend-col">
              <div class="trend-bar-wrap">
                <div class="trend-bar" :style="{ height: `${trendPercent(item.count)}%` }" />
              </div>
              <div class="trend-count">{{ item.count }}</div>
              <div class="trend-date">{{ formatDate(item.date) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <h3>全站订单列表</h3>
            <p>支持筛选并查看订单详情。</p>
          </div>
          <el-space>
            <el-select v-model="statusFilter" style="width: 190px" @change="loadOrders">
              <el-option label="全部状态" value="" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="取消审核中" value="CANCEL_PENDING" />
              <el-option label="已完成" value="SIGNED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
            <el-button text @click="loadOrders">刷新</el-button>
          </el-space>
        </div>
      </template>

      <el-table
        class="admin-order-table"
        :data="orders"
        stripe
        border
        row-key="id"
        max-height="520"
        table-layout="fixed"
        :header-cell-style="tableCenterStyle"
        :cell-style="tableCenterStyle"
      >
        <el-table-column label="单号信息" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="cell-stack">
              <div>{{ row.orderNo }}</div>
              <small class="cell-muted">{{ row.waybillNo || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="收寄信息" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="cell-stack">
              <div>寄：{{ row.senderName || '-' }}</div>
              <small class="cell-muted">收：{{ row.receiverName || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CANCELLED' && row.refunded ? 'success' : orderStatusTagType(row.status)">
              {{ orderStatusDisplayText(row.status, row.refunded) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="120">
          <template #default="{ row }">
            <el-space>
              <el-button type="primary" text @click="openOrderDetail(row.id)">订单详情</el-button>
              <el-button
                v-if="row.status === 'CANCELLED' && !row.refunded"
                type="danger"
                text
                @click="openOrderDetail(row.id)"
              >
                去退款
              </el-button>
            </el-space>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无订单数据" :image-size="72" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="courierDialogVisible" width="560px" title="新增快递员" destroy-on-close>
      <el-form ref="formRef" :model="courierForm" :rules="rules" label-position="top">
        <el-form-item label="快递员姓名" prop="username">
          <el-input v-model="courierForm.username" maxlength="50" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="courierForm.phone" maxlength="11" clearable />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="courierForm.email" clearable />
        </el-form-item>
        <el-form-item label="所属网点" prop="stationId">
          <el-select v-model="courierForm.stationId" style="width: 100%" :loading="stationLoading" placeholder="请选择网点">
            <el-option
              v-for="item in stations"
              :key="item.id"
              :label="`${item.province} ${item.city} - ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="courierForm.password" show-password type="password" />
        </el-form-item>
      </el-form>

      <el-alert v-if="lastCreated" type="success" :closable="false" show-icon class="create-result">
        已创建：{{ lastCreated.username }}（工号：{{ lastCreated.workNo }}）
      </el-alert>

      <template #footer>
        <el-button @click="courierDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="createCourierUser">创建账号</el-button>
      </template>
    </el-dialog>

    <order-detail-dialog v-model="detailDialogVisible" :order-id="detailOrderId" role="admin" @refresh="loadAll" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import OrderDetailDialog from '../../components/OrderDetailDialog.vue'
import { createCourier, exportAdminOrders, listStations } from '../../api/admin'
import { fetchAdminOrders, fetchAdminOverview, fetchAdminStats } from '../../api/workbench'
import type { AuthCreateCourierResponse, StationOption } from '../../types/auth'
import type { AdminDashboardStatsResponse, OrderListItem, WorkbenchOverviewResponse } from '../../types/workbench'
import { orderStatusDisplayText, orderStatusTagType } from '../../utils/status'

const formRef = ref<FormInstance>()
const createLoading = ref(false)
const exportLoading = ref(false)
const stationLoading = ref(false)
const statusFilter = ref('')
const orders = ref<OrderListItem[]>([])
const overview = ref<WorkbenchOverviewResponse>({ metrics: {} })
const stations = ref<StationOption[]>([])
const lastCreated = ref<AuthCreateCourierResponse | null>(null)
const detailDialogVisible = ref(false)
const detailOrderId = ref<number | null>(null)
const courierDialogVisible = ref(false)
const tableCenterStyle = { textAlign: 'center' as const }

const stats = ref<AdminDashboardStatsResponse>({
  orderStatusStats: {},
  taskStatusStats: {},
  trend: [],
})

const courierForm = reactive({
  username: '',
  phone: '',
  email: '',
  stationId: undefined as number | undefined,
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入快递员姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  stationId: [{ required: true, message: '请选择所属网点', trigger: 'change' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '密码至少8位且包含字母和数字', trigger: 'blur' },
  ],
}

const statusSummaryOrder = ['PROCESSING', 'CANCEL_PENDING', 'SIGNED', 'CANCELLED']
const statusSummaryColorMap: Record<string, string> = {
  PROCESSING: '#5b8ff9',
  CANCEL_PENDING: '#faad14',
  SIGNED: '#52c41a',
  CANCELLED: '#f56c6c',
}
const statusSummaryLabelMap: Record<string, string> = {
  PROCESSING: '处理中',
  CANCEL_PENDING: '取消审核中',
  SIGNED: '已完成',
  CANCELLED: '已取消',
}

const processingOrders = computed(() => {
  return (
    (overview.value.metrics.createdOrders ?? 0) +
    (overview.value.metrics.inTransitOrders ?? 0) +
    (overview.value.metrics.deliveringOrders ?? 0)
  )
})

function summaryValue(status: string): number {
  if (status === 'PROCESSING') {
    return processingOrders.value
  }
  return stats.value.orderStatusStats[status] ?? 0
}

const statusBars = computed(() => {
  const total = Math.max(
    statusSummaryOrder.reduce((sum, status) => sum + summaryValue(status), 0),
    1,
  )
  return statusSummaryOrder.map((status) => {
    const value = summaryValue(status)
    return {
      status,
      label: statusSummaryLabelMap[status],
      value,
      percent: Math.round((value / total) * 100),
      color: statusSummaryColorMap[status],
    }
  })
})

const trendMax = computed(() => {
  const values = stats.value.trend.map((item) => item.count)
  return Math.max(1, ...values)
})

onMounted(() => {
  void loadAll()
  void loadStations()
})

async function loadAll() {
  await Promise.all([loadOverview(), loadOrders(), loadStats()])
}

async function loadOverview() {
  try {
    overview.value = await fetchAdminOverview()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '概览加载失败')
  }
}

async function loadOrders() {
  try {
    const data = await fetchAdminOrders({ page: 1, size: 50, status: statusFilter.value || undefined })
    orders.value = data.records
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单加载失败')
  }
}

async function loadStats() {
  try {
    stats.value = await fetchAdminStats()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '统计数据加载失败')
  }
}

async function loadStations() {
  stationLoading.value = true
  try {
    stations.value = await listStations()
    if (!courierForm.stationId && stations.value.length > 0) {
      courierForm.stationId = stations.value[0].id
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '网点加载失败')
  } finally {
    stationLoading.value = false
  }
}

function resetStatusFilter() {
  statusFilter.value = ''
  void loadOrders()
}

function openCourierDialog() {
  courierDialogVisible.value = true
  lastCreated.value = null
}

async function handleExportOrders() {
  exportLoading.value = true
  try {
    const blob = await exportAdminOrders(statusFilter.value || undefined)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `订单列表-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '')}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  } finally {
    exportLoading.value = false
  }
}

async function createCourierUser() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  createLoading.value = true
  try {
    lastCreated.value = await createCourier({
      username: courierForm.username.trim(),
      phone: courierForm.phone.trim(),
      email: courierForm.email.trim() || undefined,
      stationId: courierForm.stationId,
      password: courierForm.password,
    })
    ElMessage.success('快递员创建成功')
    formRef.value.resetFields()
    if (stations.value.length > 0) {
      courierForm.stationId = stations.value[0].id
    }
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '快递员创建失败')
  } finally {
    createLoading.value = false
  }
}

function trendPercent(count: number): number {
  return Math.max(6, Math.round((count / trendMax.value) * 100))
}

function formatDate(dateText: string): string {
  const date = new Date(dateText)
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${month}/${day}`
}

function formatTime(timeText: string) {
  return new Date(timeText).toLocaleString('zh-CN', { hour12: false })
}

function openOrderDetail(orderId: number) {
  detailOrderId.value = orderId
  detailDialogVisible.value = true
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

.stats-grid--wide {
  grid-template-columns: repeat(6, minmax(0, 1fr));
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

.status-bars {
  display: grid;
  gap: 10px;
}

.status-row {
  display: grid;
  grid-template-columns: 80px 1fr 48px;
  gap: 10px;
  align-items: center;
}

.status-row span {
  color: #41658a;
  font-size: 13px;
}

.status-row strong {
  text-align: right;
  color: #1d4b78;
}

.task-inline {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.trend-grid {
  height: 260px;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}

.trend-col {
  display: grid;
  gap: 6px;
  align-items: end;
  justify-items: center;
}

.trend-bar-wrap {
  width: 100%;
  height: 170px;
  border-radius: 10px;
  background: linear-gradient(180deg, #e9f2fb, #f7fbff);
  border: 1px solid #e3edf8;
  display: flex;
  align-items: flex-end;
  padding: 4px;
}

.trend-bar {
  width: 100%;
  border-radius: 8px;
  background: linear-gradient(180deg, #5a9bef, #2f77c4);
}

.trend-count {
  font-weight: 700;
  color: #205281;
  font-size: 13px;
}

.trend-date {
  font-size: 12px;
  color: #5e7b99;
}

.create-result {
  margin-top: 8px;
}

:deep(.el-table .cell) {
  white-space: normal;
  word-break: break-word;
}

@media (max-width: 1480px) {
  .stats-grid--wide {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stats-grid,
  .stats-grid--wide {
    grid-template-columns: 1fr;
  }

  .card-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
