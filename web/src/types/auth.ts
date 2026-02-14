export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  timestamp: number
}

export interface AuthUserProfile {
  userId: number
  username: string
  phone: string
  email: string | null
  roles: string[]
}

export interface AuthLoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: AuthUserProfile
}

export interface AuthRegisterRequest {
  username: string
  phone: string
  email?: string
  password: string
}

export interface ForgotPasswordOptionsPayload {
  account: string
  username: string
}

export interface ForgotPasswordAddressOption {
  addressId: number
  label: string
}

export interface ForgotPasswordResetPayload {
  account: string
  username: string
  addressId: number
  newPassword: string
}

export interface AuthCreateCourierRequest {
  username: string
  phone: string
  email?: string
  password: string
  stationId?: number
}

export interface AuthCreateCourierResponse {
  userId: number
  username: string
  phone: string
  workNo: string
  stationId: number
  roles: string[]
}

export interface StationOption {
  id: number
  name: string
  province: string
  city: string
}

export interface ProfileContactUpdatePayload {
  phone: string
  email?: string
}

export interface ProfilePasswordUpdatePayload {
  oldPassword: string
  newPassword: string
}

export interface ProfileAddress {
  id: number
  contactName: string
  contactPhone: string
  province: string
  city: string
  district: string
  detail: string
  fullAddress: string
  isDefault: boolean
}

export interface ProfileAddressUpsertPayload {
  contactName: string
  contactPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: boolean
}
