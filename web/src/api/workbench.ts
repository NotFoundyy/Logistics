import request from '../utils/request'
import type { ApiResponse } from '../types/auth'
import type {
  AdminDashboardStatsResponse,
  CourierCancelOrderPayload,
  CourierCancelReviewPayload,
  CourierSignPayload,
  CourierTaskItem,
  OrderDetail,
  OrderListItem,
  WorkbenchOverviewResponse,
} from '../types/workbench'

const SUCCESS_CODE = '00000'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== SUCCESS_CODE) {
    throw new Error(response.message || '请求失败')
  }
  return response.data
}

export async function fetchAdminOverview(): Promise<WorkbenchOverviewResponse> {
  const { data } = await request.get<ApiResponse<WorkbenchOverviewResponse>>('/admin/overview')
  return unwrap(data)
}

export async function fetchAdminStats(): Promise<AdminDashboardStatsResponse> {
  const { data } = await request.get<ApiResponse<AdminDashboardStatsResponse>>('/admin/stats')
  return unwrap(data)
}

export async function fetchAdminOrders(params: { page: number; size: number; status?: string }): Promise<{
  page: number
  size: number
  total: number
  records: OrderListItem[]
}> {
  const { data } = await request.get<ApiResponse<{ page: number; size: number; total: number; records: OrderListItem[] }>>(
    '/admin/orders',
    { params },
  )
  return unwrap(data)
}

export async function fetchAdminOrderDetail(orderId: number): Promise<OrderDetail> {
  const { data } = await request.get<ApiResponse<OrderDetail>>(`/admin/orders/${orderId}`)
  return unwrap(data)
}

export async function fetchCourierOverview(): Promise<WorkbenchOverviewResponse> {
  const { data } = await request.get<ApiResponse<WorkbenchOverviewResponse>>('/courier/overview')
  return unwrap(data)
}

export async function fetchCourierTasks(params: { page: number; size: number; status?: string }): Promise<{
  page: number
  size: number
  total: number
  records: CourierTaskItem[]
}> {
  const { data } = await request.get<ApiResponse<{ page: number; size: number; total: number; records: CourierTaskItem[] }>>(
    '/courier/tasks',
    { params },
  )
  return unwrap(data)
}

export async function fetchCourierOrderDetail(orderId: number): Promise<OrderDetail> {
  const { data } = await request.get<ApiResponse<OrderDetail>>(`/courier/orders/${orderId}`)
  return unwrap(data)
}

export async function acceptCourierTask(taskId: number): Promise<void> {
  const { data } = await request.post<ApiResponse<null>>(`/courier/tasks/${taskId}/accept`)
  unwrap(data)
}

export async function reviewCancelRequest(orderId: number, payload: CourierCancelReviewPayload): Promise<void> {
  const { data } = await request.post<ApiResponse<null>>(`/courier/orders/${orderId}/cancel-review`, payload)
  unwrap(data)
}

export async function cancelOrderByCourier(taskId: number, payload: CourierCancelOrderPayload): Promise<void> {
  const { data } = await request.post<ApiResponse<null>>(`/courier/tasks/${taskId}/cancel-order`, payload)
  unwrap(data)
}

export async function signCourierOrder(taskId: number, payload: CourierSignPayload): Promise<void> {
  const { data } = await request.post<ApiResponse<null>>(`/courier/tasks/${taskId}/sign`, payload)
  unwrap(data)
}
