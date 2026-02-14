<template>
  <el-dialog
    :model-value="modelValue"
    width="760px"
    top="6vh"
    title="订单详情"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <div v-if="detail" class="detail-toolbar">
          <el-space wrap>
            <el-tag effect="plain">订单号：{{ detail.orderNo }}</el-tag>
            <el-tag effect="plain" type="info">运单号：{{ detail.waybillNo || '-' }}</el-tag>
            <el-button
              v-if="canApproveRefund(detail)"
              type="danger"
              plain
              :loading="refundLoading"
              @click="approveRefundByAdmin"
            >
              同意退款
            </el-button>
            <el-button text type="primary" @click="copyText(detail.orderNo)">复制订单号</el-button>
            <el-button v-if="detail.waybillNo" text type="primary" @click="copyText(detail.waybillNo)">复制运单号</el-button>
          </el-space>
        </div>

        <el-descriptions v-if="detail" :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ detail.waybillNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="orderStatusTagType(detail.status)">{{ orderStatusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="运单状态">
            <el-tag :type="orderStatusTagType(detail.waybillStatus)">{{ orderStatusText(detail.waybillStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ detail.payType === 2 ? '到付' : '在线支付' }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ detail.paid ? '已支付' : '未支付' }}</el-descriptions-item>
          <el-descriptions-item label="退款状态">{{ refundStatusText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="服务类型">{{ detail.serviceType === 2 ? '加急' : '标准' }}</el-descriptions-item>
          <el-descriptions-item label="寄件人">{{ detail.senderName }}（{{ detail.senderPhone }}）</el-descriptions-item>
          <el-descriptions-item label="收件人">{{ detail.receiverName }}（{{ detail.receiverPhone }}）</el-descriptions-item>
          <el-descriptions-item label="寄件地址" :span="2">{{ detail.senderAddr }}</el-descriptions-item>
          <el-descriptions-item label="收件地址" :span="2">{{ detail.receiverAddr }}</el-descriptions-item>
          <el-descriptions-item label="重量/体积">
            {{ Number(detail.weight).toFixed(2) }}kg / {{ Number(detail.volume).toFixed(0) }}cm³
          </el-descriptions-item>
          <el-descriptions-item label="计费重">{{ Number(detail.chargeWeight).toFixed(2) }}kg</el-descriptions-item>
          <el-descriptions-item label="费用总计">¥ {{ Number(detail.feeTotal).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-skeleton>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveAdminOrderRefund } from '../api/admin'
import { getMyOrderDetail } from '../api/order'
import { fetchAdminOrderDetail, fetchCourierOrderDetail } from '../api/workbench'
import type { OrderDetail } from '../types/workbench'
import { orderStatusTagType, orderStatusText } from '../utils/status'

const props = defineProps<{
  modelValue: boolean
  orderId: number | null
  role: 'user' | 'courier' | 'admin'
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'refresh'): void
}>()

const loading = ref(false)
const detail = ref<OrderDetail | null>(null)
const refundLoading = ref(false)

watch(
  () => [props.modelValue, props.orderId, props.role] as const,
  ([visible, orderId]) => {
    if (!visible || !orderId) {
      return
    }
    void loadDetail(orderId)
  },
  { immediate: true },
)

async function loadDetail(orderId: number) {
  loading.value = true
  try {
    if (props.role === 'admin') {
      detail.value = await fetchAdminOrderDetail(orderId)
      return
    }
    if (props.role === 'courier') {
      detail.value = await fetchCourierOrderDetail(orderId)
      return
    }
    detail.value = await getMyOrderDetail(orderId)
  } catch (error) {
    detail.value = null
    ElMessage.error(error instanceof Error ? error.message : '订单详情加载失败')
  } finally {
    loading.value = false
  }
}

function formatTime(timeText: string) {
  return new Date(timeText).toLocaleString('zh-CN', { hour12: false })
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function canApproveRefund(order: OrderDetail): boolean {
  return props.role === 'admin' && order.status === 'CANCELLED' && order.paid && !order.refunded
}

function refundStatusText(order: OrderDetail): string {
  if (order.status !== 'CANCELLED') {
    return order.refunded ? '已退款' : '未触发退款'
  }
  if (!order.paid) {
    return '无需退款'
  }
  return order.refunded ? '已退款' : '待管理员退款'
}

async function approveRefundByAdmin() {
  if (!detail.value || !canApproveRefund(detail.value)) {
    return
  }
  try {
    await ElMessageBox.confirm('确认同意该订单退款？', '退款确认', {
      confirmButtonText: '确认退款',
      cancelButtonText: '取消',
      type: 'warning',
    })
    refundLoading.value = true
    await approveAdminOrderRefund(detail.value.id)
    ElMessage.success('退款成功')
    emit('refresh')
    await loadDetail(detail.value.id)
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '退款失败')
  } finally {
    refundLoading.value = false
  }
}
</script>

<style scoped>
.detail-toolbar {
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f5f9ff;
  border: 1px solid #e4ecf8;
}
</style>
