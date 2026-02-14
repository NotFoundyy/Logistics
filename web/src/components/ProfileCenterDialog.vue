<template>
  <el-dialog
    :model-value="modelValue"
    width="960px"
    top="6vh"
    class="profile-dialog"
    title="个人中心"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <section class="profile-summary">
      <div>
        <h3>{{ authStore.user?.username || '用户' }}</h3>
        <p>{{ roleText }} · 可在这里维护手机号、邮箱、密码和常用地址</p>
      </div>
      <el-tag effect="plain" type="success">资料实时生效</el-tag>
    </section>

    <el-tabs v-model="activeTab" type="border-card" class="profile-tabs">
      <el-tab-pane label="资料设置" name="contact">
        <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="96px" class="inner-form">
          <el-form-item label="用户名">
            <el-input :model-value="authStore.user?.username || '-'" disabled />
          </el-form-item>
          <el-form-item label="角色">
            <el-input :model-value="roleText" disabled />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="contactForm.phone" maxlength="11" placeholder="请输入11位手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="contactForm.email" placeholder="可选，如 demo@example.com" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="contactSubmitting" @click="submitContact">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="修改密码" name="password">
        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="96px" class="inner-form">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="passwordSubmitting" @click="submitPassword">更新密码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="常用地址" name="address">
        <div class="address-toolbar">
          <el-button type="primary" @click="openCreateAddress">新增地址</el-button>
          <el-button @click="loadAddresses">刷新</el-button>
        </div>

        <el-table
          class="profile-address-table"
          :data="addresses"
          stripe
          border
          max-height="360"
          empty-text="暂无地址，点击“新增地址”创建"
          row-key="id"
          table-layout="fixed"
          :header-cell-style="tableCenterStyle"
          :cell-style="tableCenterStyle"
        >
          <el-table-column prop="contactName" label="联系人" min-width="100" show-overflow-tooltip />
          <el-table-column prop="contactPhone" label="联系电话" min-width="130" show-overflow-tooltip />
          <el-table-column prop="fullAddress" label="地址" min-width="260" show-overflow-tooltip />
          <el-table-column label="默认" min-width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isDefault" type="success">默认</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="220">
            <template #default="{ row }">
              <el-space>
                <el-button text type="primary" @click="openEditAddress(row)">编辑</el-button>
                <el-button text type="success" :disabled="row.isDefault" @click="submitSetDefault(row.id)">设为默认</el-button>
                <el-popconfirm title="确认删除该地址？" @confirm="submitDeleteAddress(row.id)">
                  <template #reference>
                    <el-button text type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </el-space>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无常用地址，请先新增一条" :image-size="64" />
          </template>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      :model-value="addressDialogVisible"
      width="620px"
      :title="addressForm.id ? '编辑地址' : '新增地址'"
      append-to-body
      @update:model-value="addressDialogVisible = $event"
    >
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="94px">
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="addressForm.contactName" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="addressForm.contactPhone" maxlength="11" />
        </el-form-item>
        <el-form-item label="省市区县" prop="region">
          <el-cascader
            v-model="addressForm.region"
            :options="regionOptions"
            :props="cascaderProps"
            clearable
            filterable
            style="width: 100%"
            placeholder="请选择省市区县"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addressForm.detail" maxlength="255" placeholder="如：金开大道88号 A栋" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="addressForm.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addressSubmitting" @click="submitAddress">
          {{ addressForm.id ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type CascaderOption, type FormInstance, type FormRules } from 'element-plus'
import { pcaTextArr } from 'element-china-area-data'
import {
  createProfileAddress,
  deleteProfileAddress,
  listProfileAddresses,
  setDefaultProfileAddress,
  updateProfileAddress,
  updateProfileContact,
  updateProfilePassword,
} from '../api/auth'
import { useAuthStore } from '../stores/auth'
import type { ProfileAddress } from '../types/auth'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const authStore = useAuthStore()

const contactFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const addressFormRef = ref<FormInstance>()

const activeTab = ref('contact')
const contactSubmitting = ref(false)
const passwordSubmitting = ref(false)
const addressSubmitting = ref(false)
const addressDialogVisible = ref(false)
const tableCenterStyle = { textAlign: 'center' as const }

const addresses = ref<ProfileAddress[]>([])

const contactForm = reactive({
  phone: '',
  email: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const addressForm = reactive({
  id: 0,
  contactName: '',
  contactPhone: '',
  region: [] as string[],
  detail: '',
  isDefault: false,
})

const regionOptions = pcaTextArr as CascaderOption[]
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  emitPath: true,
  checkStrictly: false,
}

const roleText = computed(() => {
  const roles = authStore.user?.roles || []
  if (roles.includes('ROLE_ADMIN')) {
    return '管理员'
  }
  if (roles.includes('ROLE_COURIER')) {
    return '快递员'
  }
  return '用户'
})

const contactRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '密码至少8位且包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const addressRules: FormRules = {
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  region: [{ validator: validateRegion, trigger: 'change' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      return
    }
    activeTab.value = 'contact'
    contactForm.phone = authStore.user?.phone || ''
    contactForm.email = authStore.user?.email || ''
    resetPasswordForm()
    void loadAddresses()
  },
  { immediate: true },
)

function validateRegion(_rule: unknown, value: string[], callback: (error?: Error) => void) {
  if (!Array.isArray(value) || value.length < 3) {
    callback(new Error('请选择省市区县'))
    return
  }
  callback()
}

async function loadAddresses() {
  try {
    addresses.value = await listProfileAddresses()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '地址加载失败')
  }
}

async function submitContact() {
  if (!contactFormRef.value) {
    return
  }
  const valid = await contactFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  contactSubmitting.value = true
  try {
    const user = await updateProfileContact({
      phone: contactForm.phone.trim(),
      email: contactForm.email.trim() || undefined,
    })
    authStore.setUser(user)
    ElMessage.success('资料保存成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资料保存失败')
  } finally {
    contactSubmitting.value = false
  }
}

async function submitPassword() {
  if (!passwordFormRef.value) {
    return
  }
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  passwordSubmitting.value = true
  try {
    await updateProfilePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请使用新密码登录')
    resetPasswordForm()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码修改失败')
  } finally {
    passwordSubmitting.value = false
  }
}

function openCreateAddress() {
  addressForm.id = 0
  addressForm.contactName = authStore.user?.username || ''
  addressForm.contactPhone = authStore.user?.phone || ''
  addressForm.region = []
  addressForm.detail = ''
  addressForm.isDefault = addresses.value.length === 0
  addressDialogVisible.value = true
}

function openEditAddress(address: ProfileAddress) {
  addressForm.id = address.id
  addressForm.contactName = address.contactName
  addressForm.contactPhone = address.contactPhone
  addressForm.region = [address.province, address.city, address.district]
  addressForm.detail = address.detail
  addressForm.isDefault = address.isDefault
  addressDialogVisible.value = true
}

async function submitAddress() {
  if (!addressFormRef.value) {
    return
  }
  const valid = await addressFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  addressSubmitting.value = true
  try {
    const payload = {
      contactName: addressForm.contactName.trim(),
      contactPhone: addressForm.contactPhone.trim(),
      province: addressForm.region[0],
      city: addressForm.region[1],
      district: addressForm.region[2],
      detail: addressForm.detail.trim(),
      isDefault: addressForm.isDefault,
    }

    if (addressForm.id > 0) {
      await updateProfileAddress(addressForm.id, payload)
      ElMessage.success('地址更新成功')
    } else {
      await createProfileAddress(payload)
      ElMessage.success('地址新增成功')
    }

    addressDialogVisible.value = false
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '地址保存失败')
  } finally {
    addressSubmitting.value = false
  }
}

async function submitSetDefault(addressId: number) {
  try {
    await setDefaultProfileAddress(addressId)
    ElMessage.success('已设为默认地址')
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '设置默认地址失败')
  }
}

async function submitDeleteAddress(addressId: number) {
  try {
    await deleteProfileAddress(addressId)
    ElMessage.success('地址删除成功')
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '地址删除失败')
  }
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}
</script>

<style scoped>
.profile-tabs {
  border-radius: 12px;
  overflow: hidden;
}

.profile-summary {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #e3edf8;
  background: linear-gradient(135deg, #f8fbff, #f2f8ff);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.profile-summary h3 {
  margin: 0;
  color: #1f4467;
}

.profile-summary p {
  margin: 4px 0 0;
  color: #6d849b;
  font-size: 13px;
}

.inner-form {
  max-width: 520px;
  padding-top: 8px;
}

.address-toolbar {
  margin-bottom: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.profile-address-table .el-table__cell .cell) {
  text-align: center;
  white-space: normal;
  word-break: break-word;
}

:deep(.profile-dialog .el-dialog__body) {
  padding-top: 8px;
}

@media (max-width: 768px) {
  .profile-summary {
    flex-direction: column;
    align-items: flex-start;
  }

  :deep(.profile-dialog) {
    width: calc(100vw - 16px) !important;
    margin: 0 8px;
  }
}
</style>
