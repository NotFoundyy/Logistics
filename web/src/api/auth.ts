import request from '../utils/request'
import type {
  ApiResponse,
  AuthLoginResponse,
  AuthRegisterRequest,
  AuthUserProfile,
  ForgotPasswordAddressOption,
  ForgotPasswordOptionsPayload,
  ForgotPasswordResetPayload,
  ProfileAddress,
  ProfileAddressUpsertPayload,
  ProfileContactUpdatePayload,
  ProfilePasswordUpdatePayload,
} from '../types/auth'

const SUCCESS_CODE = '00000'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== SUCCESS_CODE) {
    throw new Error(response.message || '请求失败')
  }
  return response.data
}

export async function login(payload: { account: string; password: string }): Promise<AuthLoginResponse> {
  const { data } = await request.post<ApiResponse<AuthLoginResponse>>('/auth/login', payload)
  return unwrap(data)
}

export async function register(payload: AuthRegisterRequest): Promise<AuthUserProfile> {
  const { data } = await request.post<ApiResponse<AuthUserProfile>>('/auth/register', payload)
  return unwrap(data)
}

export async function forgotPasswordOptions(payload: ForgotPasswordOptionsPayload): Promise<ForgotPasswordAddressOption[]> {
  const { data } = await request.post<ApiResponse<ForgotPasswordAddressOption[]>>('/auth/forgot-password/options', payload)
  return unwrap(data)
}

export async function forgotPasswordReset(payload: ForgotPasswordResetPayload): Promise<void> {
  const { data } = await request.post<ApiResponse<null>>('/auth/forgot-password/reset', payload)
  unwrap(data)
}

export async function fetchMe(): Promise<AuthUserProfile> {
  const { data } = await request.get<ApiResponse<AuthUserProfile>>('/profile/me')
  return unwrap(data)
}

export async function updateProfileContact(payload: ProfileContactUpdatePayload): Promise<AuthUserProfile> {
  const { data } = await request.put<ApiResponse<AuthUserProfile>>('/profile/contact', payload)
  return unwrap(data)
}

export async function updateProfilePassword(payload: ProfilePasswordUpdatePayload): Promise<void> {
  const { data } = await request.put<ApiResponse<null>>('/profile/password', payload)
  unwrap(data)
}

export async function listProfileAddresses(): Promise<ProfileAddress[]> {
  const { data } = await request.get<ApiResponse<ProfileAddress[]>>('/profile/addresses')
  return unwrap(data)
}

export async function createProfileAddress(payload: ProfileAddressUpsertPayload): Promise<ProfileAddress> {
  const { data } = await request.post<ApiResponse<ProfileAddress>>('/profile/addresses', payload)
  return unwrap(data)
}

export async function updateProfileAddress(addressId: number, payload: ProfileAddressUpsertPayload): Promise<ProfileAddress> {
  const { data } = await request.put<ApiResponse<ProfileAddress>>(`/profile/addresses/${addressId}`, payload)
  return unwrap(data)
}

export async function setDefaultProfileAddress(addressId: number): Promise<void> {
  const { data } = await request.put<ApiResponse<null>>(`/profile/addresses/${addressId}/default`)
  unwrap(data)
}

export async function deleteProfileAddress(addressId: number): Promise<void> {
  const { data } = await request.delete<ApiResponse<null>>(`/profile/addresses/${addressId}`)
  unwrap(data)
}
