<template>
  <div class="gi_page h5-auth-config">
    <a-spin :loading="configLoading">
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        auto-label-width
        label-align="left"
        :layout="width >= 500 ? 'horizontal' : 'vertical'"
        :disabled="!isUpdate"
        scroll-to-first-error
      >
        <a-alert type="info" style="margin-bottom: 16px">
          每位成员使用独立邀请码激活个人账号，之后使用个人用户名密码登录。员工只看自己提交的报修，公司管理员看本公司全部报修。新增成员和密码重置请到往来单位的「App成员」操作；重置码只针对指定成员。访问码十分钟内有效且只能使用一次；管理列表只提供会话摘要和强退句柄。
        </a-alert>

        <h4 class="section-title">令牌策略</h4>
        <a-form-item
          field="H5_TOKEN_EXPIRE_MINUTES"
          :label="labelOf('H5_TOKEN_EXPIRE_MINUTES')"
          :help="descOf('H5_TOKEN_EXPIRE_MINUTES')"
        >
          <a-input-number v-model="form.H5_TOKEN_EXPIRE_MINUTES" :min="30" :max="480" :precision="0">
            <template #append>分钟</template>
          </a-input-number>
        </a-form-item>
        <a-form-item
          field="H5_TOKEN_AUTO_REFRESH"
          :label="labelOf('H5_TOKEN_AUTO_REFRESH')"
          :help="descOf('H5_TOKEN_AUTO_REFRESH')"
        >
          <a-switch
            :model-value="form.H5_TOKEN_AUTO_REFRESH === '1'"
            :disabled="!isUpdate"
            checked-text="开启"
            unchecked-text="关闭"
            @change="(v) => form.H5_TOKEN_AUTO_REFRESH = v === true ? '1' : '0'"
          />
        </a-form-item>

        <a-space style="margin-bottom: 24px">
          <a-button v-if="!isUpdate" v-permission="['system:h5Auth:update']" type="primary" @click="onUpdate">
            <template #icon><icon-edit /></template>修改
          </a-button>
          <a-button v-if="isUpdate" v-permission="['system:h5Auth:update']" type="primary" @click="handleSave">
            <template #icon><icon-save /></template>保存
          </a-button>
          <a-button v-if="isUpdate" @click="resetConfig">
            <template #icon><icon-refresh /></template>重置
          </a-button>
          <a-button v-if="isUpdate" @click="handleCancel">
            <template #icon><icon-undo /></template>取消
          </a-button>
          <a-button v-permission="['system:h5Auth:update']" status="warning" @click="onResetValue">
            <template #icon><icon-sync /></template>恢复默认
          </a-button>
        </a-space>
      </a-form>
    </a-spin>

    <h4 class="section-title">在线客户</h4>
    <p class="section-hint">授权数据来源：进销存 → 往来单位（客户类型且已启用）。停用单位后无法获取新令牌，成员停用或密码重置后其旧会话立即失效；也可在此单独强退会话。</p>

    <BaseTable
      row-key="sessionId"
      :data="dataList"
      :columns="columns"
      :loading="tableLoading"
      :pagination="pagination"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-input-search
          v-model="queryForm.customerNo"
          placeholder="客户编号"
          allow-clear
          style="width: 160px"
          @search="search"
        />
        <a-input-search
          v-model="queryForm.customerName"
          placeholder="客户名称"
          allow-clear
          style="width: 160px"
          @search="search"
        />
        <a-button @click="resetQuery">
          <template #icon><icon-refresh /></template>重置
        </a-button>
      </template>
      <template #expireSeconds="{ record }">
        {{ formatExpire(record.expireSeconds) }}
      </template>
      <template #action="{ record }">
        <a-popconfirm
          type="warning"
          content="确定退出该成员的这次App登录？"
          :ok-button-props="{ status: 'danger' }"
          @ok="handleKickout(record.sessionId)"
        >
          <a-link v-permission="['system:h5Auth:kickout']" status="danger">强退</a-link>
        </a-popconfirm>
      </template>
    </BaseTable>
  </div>
</template>

<script setup lang="tsx">
import { useWindowSize } from '@vueuse/core'
import type { FormInstance, TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { type H5OnlineTokenQuery, kickoutH5Token, listH5OnlineToken } from '@/apis/eoms/h5Auth'
import { type OptionResp, listOption, resetOptionValue, updateOption } from '@/apis/system'
import { TableCol } from '@/constant/table-col'
import { useTable } from '@/hooks'
import { isMobile } from '@/utils'
import has from '@/utils/has'

defineOptions({ name: 'SystemH5AuthConfig' })

const H5_OPTION_CODES = ['H5_TOKEN_EXPIRE_MINUTES', 'H5_TOKEN_AUTO_REFRESH'] as const

const { width } = useWindowSize()
const configLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({
  H5_TOKEN_EXPIRE_MINUTES: 120,
  H5_TOKEN_AUTO_REFRESH: '1',
})
const optionMap = ref<Record<string, OptionResp>>({})
const isUpdate = ref(false)

const rules: FormInstance['rules'] = {
  H5_TOKEN_EXPIRE_MINUTES: [{
    validator: (value: number, callback: (error?: string) => void) => {
      if (value >= 30 && value <= 480) {
        callback()
        return
      }
      callback('有效期须在 30～480 分钟之间')
    },
  }],
}

const labelOf = (code: string) => optionMap.value[code]?.name || code
const descOf = (code: string) => optionMap.value[code]?.description || ''

const applyFormFromConfig = () => {
  H5_OPTION_CODES.forEach((code) => {
    const option = optionMap.value[code]
    if (!option) return
    const raw = option.value ?? option.defaultValue
    if (code === 'H5_TOKEN_EXPIRE_MINUTES') {
      form[code] = Number(raw ?? 120)
    } else {
      form[code] = String(raw ?? '1')
    }
  })
}

const resetConfig = () => {
  formRef.value?.resetFields()
  applyFormFromConfig()
}

const onUpdate = () => {
  isUpdate.value = true
}

const handleCancel = () => {
  resetConfig()
  isUpdate.value = false
}

const queryForm = reactive<Partial<H5OnlineTokenQuery>>({
  sort: ['loginTime,desc'],
})

const {
  tableData: dataList,
  loading: tableLoading,
  pagination,
  search,
  refresh,
} = useTable((page) => listH5OnlineToken({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  {
    title: '序号',
    width: 66,
    align: 'center',
    render: ({ rowIndex }) => h('span', {}, rowIndex + 1 + (pagination.current - 1) * pagination.pageSize),
  },
  { title: '客户编号', dataIndex: 'customerNo', minWidth: TableCol.codeMinSm, ellipsis: false },
  { title: '成员姓名', dataIndex: 'displayName', width: 120 },
  { title: '用户名', dataIndex: 'loginName', width: 150 },
  { title: '客户名称', dataIndex: 'customerName', width: TableCol.name },
  { title: '令牌摘要', dataIndex: 'tokenDigest', minWidth: TableCol.codeMinSm, ellipsis: false },
  { title: '登录时间', dataIndex: 'loginTime', width: TableCol.date },
  { title: '剩余有效', dataIndex: 'expireSeconds', slotName: 'expireSeconds', width: 120 },
  {
    title: '操作',
    dataIndex: 'action',
    slotName: 'action',
    width: TableCol.actionS,
    align: 'center',
    fixed: !isMobile() ? 'right' : undefined,
    show: has.hasPermOr(['system:h5Auth:kickout']),
  },
]

const formatExpire = (seconds?: number) => {
  if (seconds == null || seconds <= 0) return '已过期'
  if (seconds < 60) return `${seconds} 秒`
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return sec > 0 ? `${min} 分 ${sec} 秒` : `${min} 分钟`
}

const resetQuery = () => {
  queryForm.customerNo = undefined
  queryForm.customerName = undefined
  search()
}

const handleKickout = async (sessionId: string) => {
  await kickoutH5Token(sessionId)
  Message.success('强退成功')
  search()
}

const getConfig = async () => {
  try {
    configLoading.value = true
    const { data } = await listOption({ category: 'EOMS' })
    const next: Record<string, OptionResp> = {}
    ;(data || []).forEach((option: OptionResp) => {
      if (H5_OPTION_CODES.includes(option.code as typeof H5_OPTION_CODES[number])) {
        next[option.code] = option
      }
    })
    optionMap.value = next
    handleCancel()
  } finally {
    configLoading.value = false
  }
}

const handleSave = async () => {
  const isInvalid = await formRef.value?.validate()
  if (isInvalid) return
  const payload = H5_OPTION_CODES.map((code) => ({
    id: optionMap.value[code]?.id,
    code,
    value: code === 'H5_TOKEN_EXPIRE_MINUTES' ? String(form[code]) : form[code],
  }))
  await updateOption(payload)
  await getConfig()
  Message.success('保存成功')
  isUpdate.value = false
}

const handleResetValue = async () => {
  await resetOptionValue({ code: [...H5_OPTION_CODES] })
  Message.success('已恢复默认')
  await getConfig()
}

const onResetValue = () => {
  Modal.warning({
    title: '警告',
    content: '确认将 H5 客户授权参数恢复为默认值吗？',
    hideCancel: false,
    maskClosable: false,
    onOk: handleResetValue,
  })
}

onMounted(() => {
  getConfig()
})
</script>

<style scoped lang="scss">
.h5-auth-config {
  padding: 16px 20px 24px;
}

.section-title {
  margin: 8px 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.section-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--color-text-3);
}
</style>
