import request from '../utils/request'
import type { ApiResponse } from '../types/auth'
import type {
  CreateOrderPayload,
  OrderCancelPayload,
  OrderCreateResponse,
  OrderDetail,
  OrderListItem,
  OrderPaymentQrcodeResponse,
  OrderReceiverUpdatePayload,
  OrderReceiverUpdateResponse,
  PricingQuoteResponse,
  TrackingProgressResponse,
  TrackingQueryResponse,
} from '../types/workbench'

const SUCCESS_CODE = '00000'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== SUCCESS_CODE) {
    throw new Error(response.message || '请求失败')
  }
  return response.data
}

export async function quotePrice(params: {
  senderAddr: string
  receiverAddr: string
  weight: number
  volume: number
  serviceType: number
  insuredAmount: number
}): Promise<PricingQuoteResponse> {
  const { data } = await request.get<ApiResponse<PricingQuoteResponse>>('/pricing/quote', { params })
  return unwrap(data)
}

export async function createOrder(payload: CreateOrderPayload): Promise<OrderCreateResponse> {
  const { data } = await request.post<ApiResponse<OrderCreateResponse>>('/orders', payload)
  return unwrap(data)
}

export async function cancelOrder(orderId: number, payload: OrderCancelPayload): Promise<void> {
  const { data } = await request.post<ApiResponse<null>>(`/orders/${orderId}/cancel`, payload)
  unwrap(data)
}

export async function updateOrderReceiver(
  orderId: number,
  payload: OrderReceiverUpdatePayload,
): Promise<OrderReceiverUpdateResponse> {
  const { data } = await request.put<ApiResponse<OrderReceiverUpdateResponse>>(`/orders/${orderId}/receiver`, payload)
  return unwrap(data)
}

export async function getOrderPaymentQrcode(orderId: number): Promise<OrderPaymentQrcodeResponse> {
  const { data } = await request.get<ApiResponse<OrderPaymentQrcodeResponse>>(`/orders/${orderId}/payment/qrcode`)
  return unwrap(data)
}

export async function confirmOrderOnlinePayment(orderId: number): Promise<void> {
  const { data } = await request.post<ApiResponse<null>>(`/orders/${orderId}/payment/confirm`)
  unwrap(data)
}

export async function listMyOrders(params: {
  page: number
  size: number
  status?: string
  relation?: 'all' | 'sender' | 'receiver'
}): Promise<{ page: number; size: number; total: number; records: OrderListItem[] }> {
  const { data } = await request.get<ApiResponse<{ page: number; size: number; total: number; records: OrderListItem[] }>>(
    '/orders',
    { params },
  )
  return unwrap(data)
}

export async function getMyOrderDetail(orderId: number): Promise<OrderDetail> {
  const { data } = await request.get<ApiResponse<OrderDetail>>(`/orders/${orderId}`)
  return unwrap(data)
}

export async function queryTracking(waybillNo: string): Promise<TrackingQueryResponse> {
  const { data } = await request.get<ApiResponse<TrackingQueryResponse>>(`/tracking/${waybillNo}`)
  return unwrap(data)
}

export async function queryTrackingProgress(waybillNo: string): Promise<TrackingProgressResponse> {
  const { data } = await request.get<ApiResponse<TrackingProgressResponse>>(`/tracking/${waybillNo}/progress`)
  return unwrap(data)
}
