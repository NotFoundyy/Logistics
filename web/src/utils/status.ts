export function orderStatusText(status: string): string {
  const map: Record<string, string> = {
    CREATED: '待处理',
    IN_TRANSIT: '运输中',
    DELIVERING: '运输中',
    SIGNED: '已完成',
    CANCEL_PENDING: '取消审核中',
    CANCELLED: '已取消',
  }
  return map[status] ?? status
}

export function taskStatusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待接单',
    ACCEPTED: '运输中',
    FINISHED: '已完成',
  }
  return map[status] ?? status
}

export function trackingEventText(eventType: string): string {
  const map: Record<string, string> = {
    CREATED: '订单创建',
    PICKED_UP: '快递员接单',
    IN_TRANSIT: '干线运输',
    ARRIVED_PROVINCE_HUB: '到达省会站',
    ARRIVED_CITY_HUB: '到达地级市站',
    DELIVERING: '派送中',
    SIGNED: '已签收',
    CANCEL_REQUESTED: '用户申请取消',
    CANCEL_REJECTED: '取消申请被拒绝',
    RECEIVER_UPDATED: '收件信息已修改',
    PAYMENT_CONFIRMED: '在线支付完成',
    COD_PAID_CONFIRMED: '到付已收款',
    WAITING_PAYMENT: '待支付',
    CANCELLED_BY_COURIER: '快递员取消订单',
    CANCELLED: '订单已取消',
    REFUND_SUCCESS: '退款成功',
  }
  return map[eventType] ?? eventType
}

export function phaseText(phase: string): string {
  const map: Record<string, string> = {
    WAITING_ACCEPT: '待接单',
    IN_TRANSIT: '运输中',
    DELIVERING: '派送中',
    SIGNED: '已签收',
    CANCELLED: '已取消',
  }
  return map[phase] ?? phase
}

export function orderStatusTagType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'SIGNED') {
    return 'success'
  }
  if (status === 'CANCELLED') {
    return 'danger'
  }
  if (status === 'CANCEL_PENDING') {
    return 'warning'
  }
  return 'info'
}

export function taskStatusTagType(status: string): 'success' | 'warning' | 'info' {
  if (status === 'FINISHED') {
    return 'success'
  }
  if (status === 'ACCEPTED') {
    return 'warning'
  }
  return 'info'
}

export function orderStatusDisplayText(status: string, refunded?: boolean, paid?: boolean): string {
  if (status === 'CANCELLED') {
    if (paid === false) {
      return '已取消(无需退款)'
    }
    return refunded ? '已取消(已退款)' : '已取消(待退款)'
  }
  return orderStatusText(status)
}

export function paymentStatusText(payType: number, paid: boolean): string {
  if (payType === 2) {
    return paid ? '到付已收款' : '到付待收款'
  }
  return paid ? '在线已支付' : '在线待支付'
}
