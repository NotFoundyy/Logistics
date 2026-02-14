import type { ApiResponse } from './auth'

export type ApiPageResult<T> = ApiResponse<{
  page: number
  size: number
  total: number
  records: T[]
}>

export interface WorkbenchOverviewResponse {
  metrics: Record<string, number>
}

export interface OrderListItem {
  id: number
  orderNo: string
  waybillNo: string
  relationType?: 'SENDER' | 'RECEIVER'
  status: string
  payType: number
  paid: boolean
  refunded?: boolean
  senderName?: string
  senderPhone?: string
  receiverName: string
  receiverPhone: string
  feeTotal: number
  createdAt: string
}

export interface CreateOrderPayload {
  senderName: string
  senderPhone: string
  senderAddr: string
  receiverName: string
  receiverPhone: string
  receiverAddr: string
  serviceType: number
  payType: number
  weight: number
  volume: number
  insuredAmount: number
  remark?: string
}

export interface OrderCancelPayload {
  reason: string
}

export interface OrderReceiverUpdatePayload {
  receiverName: string
  receiverPhone: string
  receiverAddr: string
  reason?: string
}

export interface OrderReceiverUpdateResponse {
  orderNo: string
  waybillNo: string
  oldFeeTotal: number
  newFeeTotal: number
  feeDelta: number
}

export interface OrderCreateResponse {
  orderId: number
  orderNo: string
  waybillNo: string
  status: string
  feeTotal: number
  payType: number
  paid: boolean
}

export interface OrderDetail {
  id: number
  orderNo: string
  waybillNo: string
  status: string
  waybillStatus: string
  senderName: string
  senderPhone: string
  senderAddr: string
  receiverName: string
  receiverPhone: string
  receiverAddr: string
  serviceType: number
  payType: number
  paid: boolean
  refunded?: boolean
  weight: number
  volume: number
  chargeWeight: number
  feeTotal: number
  remark: string | null
  createdAt: string
}

export interface PricingQuoteResponse {
  actualWeight: number
  volumeWeight: number
  chargeWeight: number
  baseFee: number
  continueFee: number
  serviceFee: number
  remoteFee: number
  insuredFee: number
  totalFee: number
  ruleDesc: string
}

export interface TrackingEvent {
  eventTime: string
  eventType: string
  stationId: number | null
  courierId: number | null
  description: string
}

export interface TrackingQueryResponse {
  waybillNo: string
  currentStatus: string
  events: TrackingEvent[]
}

export interface TrackingGeoPoint {
  lat: number
  lng: number
}

export interface TrackingProgressResponse {
  waybillNo: string
  phase: string
  progress: number
  distanceKm: number
  travelledKm: number
  speedKmPerHour: number
  speedKmPerSecond: number
  elapsedSeconds: number
  totalSeconds: number
  acceptedAt: string | null
  arriveProvinceHubAt: string
  arriveCityHubAt: string
  signedAt: string
  senderProvince: string
  senderCity: string
  senderAddr: string
  receiverProvince: string
  receiverCity: string
  receiverDistrict: string
  receiverAddr: string
  receiverCapitalCity: string
  routeNodeNames: string[]
  startPoint: TrackingGeoPoint
  endPoint: TrackingGeoPoint
  currentPoint: TrackingGeoPoint
  route: TrackingGeoPoint[]
}

export interface CourierTaskItem {
  taskId: number
  waybillNo: string
  orderId: number
  orderNo: string
  orderStatus: string
  refunded?: boolean
  receiverName: string
  receiverPhone: string
  receiverAddr: string
  payType: number
  paid: boolean
  taskType: number
  taskStatus: string
  plannedTime: string | null
  acceptedAt: string | null
}

export interface AdminOrderTrendItem {
  date: string
  count: number
}

export interface AdminDashboardStatsResponse {
  orderStatusStats: Record<string, number>
  taskStatusStats: Record<string, number>
  trend: AdminOrderTrendItem[]
}

export interface CourierCancelReviewPayload {
  approved: boolean
  reason?: string
}

export interface CourierCancelOrderPayload {
  reason: string
}

export interface CourierSignPayload {
  paidConfirmed?: boolean
  remark?: string
}

export interface OrderPaymentQrcodeResponse {
  orderId: number
  orderNo: string
  waybillNo: string
  amount: number
  qrCodeText: string
  expireSeconds: number
}
