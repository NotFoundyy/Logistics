import request from '../utils/request'
import type {
  ApiResponse,
  AuthCreateCourierRequest,
  AuthCreateCourierResponse,
  StationOption,
} from '../types/auth'

const SUCCESS_CODE = '00000'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== SUCCESS_CODE) {
    throw new Error(response.message || '请求失败')
  }
  return response.data
}

export async function createCourier(payload: AuthCreateCourierRequest): Promise<AuthCreateCourierResponse> {
  const { data } = await request.post<ApiResponse<AuthCreateCourierResponse>>('/admin/courier', payload)
  return unwrap(data)
}

export async function listStations(): Promise<StationOption[]> {
  const { data } = await request.get<ApiResponse<StationOption[]>>('/admin/stations')
  return unwrap(data)
}

export async function exportAdminOrders(status?: string): Promise<Blob> {
  const { data } = await request.get('/admin/orders/export', {
    params: { status },
    responseType: 'blob',
  })
  return data as Blob
}

export async function approveAdminOrderRefund(orderId: number): Promise<void> {
  try {
    const { data } = await request.post<ApiResponse<null>>(`/admin/orders/${orderId}/refund`)
    unwrap(data)
  } catch {
    const { data } = await request.post<ApiResponse<null>>(`/admin/orders/refund/${orderId}`)
    unwrap(data)
  }
}
