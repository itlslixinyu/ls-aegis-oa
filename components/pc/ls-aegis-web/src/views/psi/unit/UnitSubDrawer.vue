<template>
  <a-drawer
    v-model:visible="visible"
    :title="`子资料 · ${unitName || ''}`"
    :width="width >= 720 ? 720 : '100%'"
    unmount-on-close
    @close="onClose"
  >
    <a-tabs v-model:active-key="activeTab">
      <a-tab-pane key="contact" title="联系人">
        <div class="sub-toolbar">
          <a-button type="primary" size="mini" @click="openForm('contact')">新增联系人</a-button>
        </div>
        <a-table row-key="id" size="small" :loading="loading" :data="contacts" :pagination="false" :columns="contactCols">
          <template #isDefault="{ record }">
            <a-tag v-if="Number(record.isDefault) === 1" color="green">默认</a-tag>
            <span v-else>—</span>
          </template>
          <template #action="{ record }">
            <a-space>
              <a-link @click="openForm('contact', record)">修改</a-link>
              <a-link status="danger" @click="onRemove('contact', record)">删除</a-link>
            </a-space>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="invoice" title="开票信息">
        <div class="sub-toolbar">
          <a-button type="primary" size="mini" @click="openForm('invoice')">新增开票</a-button>
        </div>
        <a-table row-key="id" size="small" :loading="loading" :data="invoices" :pagination="false" :columns="invoiceCols">
          <template #isDefault="{ record }">
            <a-tag v-if="Number(record.isDefault) === 1" color="green">默认</a-tag>
            <span v-else>—</span>
          </template>
          <template #action="{ record }">
            <a-space>
              <a-link @click="openForm('invoice', record)">修改</a-link>
              <a-link status="danger" @click="onRemove('invoice', record)">删除</a-link>
            </a-space>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="address" title="收货地址">
        <div class="sub-toolbar">
          <a-button type="primary" size="mini" @click="openForm('address')">新增地址</a-button>
        </div>
        <a-table row-key="id" size="small" :loading="loading" :data="addresses" :pagination="false" :columns="addressCols">
          <template #isDefault="{ record }">
            <a-tag v-if="Number(record.isDefault) === 1" color="green">默认</a-tag>
            <span v-else>—</span>
          </template>
          <template #action="{ record }">
            <a-space>
              <a-link @click="openForm('address', record)">修改</a-link>
              <a-link status="danger" @click="onRemove('address', record)">删除</a-link>
            </a-space>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="pay" title="收款信息">
        <div class="sub-toolbar">
          <a-button type="primary" size="mini" @click="openForm('pay')">新增收款账户</a-button>
        </div>
        <a-table row-key="id" size="small" :loading="loading" :data="pays" :pagination="false" :columns="payCols">
          <template #isDefault="{ record }">
            <a-tag v-if="Number(record.isDefault) === 1" color="green">默认</a-tag>
            <span v-else>—</span>
          </template>
          <template #action="{ record }">
            <a-space>
              <a-link @click="openForm('pay', record)">修改</a-link>
              <a-link status="danger" @click="onRemove('pay', record)">删除</a-link>
            </a-space>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:visible="formVisible"
      :title="formTitle"
      :width="520"
      :mask-closable="false"
      draggable
      @before-ok="submitForm"
      @close="resetForm"
    >
      <a-form :model="form" layout="vertical">
        <template v-if="formKind === 'contact'">
          <a-form-item label="联系人" required>
            <a-input v-model="form.contactName" :max-length="64" />
          </a-form-item>
          <a-form-item label="手机"><a-input v-model="form.mobile" :max-length="32" /></a-form-item>
          <a-form-item label="电话"><a-input v-model="form.phone" :max-length="32" /></a-form-item>
          <a-form-item label="邮箱"><a-input v-model="form.email" :max-length="128" /></a-form-item>
          <a-form-item label="职务"><a-input v-model="form.position" :max-length="64" /></a-form-item>
        </template>
        <template v-else-if="formKind === 'invoice'">
          <a-form-item label="发票抬头" required>
            <a-input v-model="form.title" :max-length="128" />
          </a-form-item>
          <a-form-item label="税号"><a-input v-model="form.taxNo" :max-length="64" /></a-form-item>
          <a-form-item label="开户行"><a-input v-model="form.bankName" :max-length="128" /></a-form-item>
          <a-form-item label="账号"><a-input v-model="form.accountNo" :max-length="64" /></a-form-item>
          <a-form-item label="地址"><a-input v-model="form.address" :max-length="256" /></a-form-item>
          <a-form-item label="电话"><a-input v-model="form.phone" :max-length="32" /></a-form-item>
        </template>
        <template v-else-if="formKind === 'address'">
          <a-form-item label="收货人"><a-input v-model="form.contactName" :max-length="64" /></a-form-item>
          <a-form-item label="手机"><a-input v-model="form.mobile" :max-length="32" /></a-form-item>
          <a-form-item label="省市区"><a-input v-model="form.region" :max-length="128" /></a-form-item>
          <a-form-item label="详细地址" required>
            <a-input v-model="form.address" :max-length="256" />
          </a-form-item>
        </template>
        <template v-else>
          <a-form-item label="户名" required>
            <a-input v-model="form.accountName" :max-length="128" />
          </a-form-item>
          <a-form-item label="开户行"><a-input v-model="form.bankName" :max-length="128" /></a-form-item>
          <a-form-item label="账号" required>
            <a-input v-model="form.accountNo" :max-length="64" />
          </a-form-item>
        </template>
        <a-form-item label="默认">
          <a-switch v-model="form.isDefault" :checked-value="1" :unchecked-value="0" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model="form.remark" :max-length="500" :auto-size="{ minRows: 2, maxRows: 4 }" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import {
  addUnitAddress,
  addUnitContact,
  addUnitInvoice,
  addUnitPay,
  deleteUnitAddress,
  deleteUnitContact,
  deleteUnitInvoice,
  deleteUnitPay,
  listUnitAddresses,
  listUnitContacts,
  listUnitInvoices,
  listUnitPays,
  updateUnitAddress,
  updateUnitContact,
  updateUnitInvoice,
  updateUnitPay,
} from '@/apis/psi'

type Kind = 'contact' | 'invoice' | 'address' | 'pay'

const { width } = useWindowSize()
const visible = ref(false)
const loading = ref(false)
const unitId = ref('')
const unitName = ref('')
const activeTab = ref<Kind>('contact')

const contacts = ref<any[]>([])
const invoices = ref<any[]>([])
const addresses = ref<any[]>([])
const pays = ref<any[]>([])

const formVisible = ref(false)
const formKind = ref<Kind>('contact')
const formId = ref('')
const form = reactive<any>({ isDefault: 0 })

const formTitle = computed(() => {
  const map: Record<Kind, string> = {
    contact: '联系人',
    invoice: '开票信息',
    address: '收货地址',
    pay: '收款信息',
  }
  return `${formId.value ? '修改' : '新增'}${map[formKind.value]}`
})

const contactCols: TableInstance['columns'] = [
  { title: '联系人', dataIndex: 'contactName' },
  { title: '手机', dataIndex: 'mobile', width: 120 },
  { title: '职务', dataIndex: 'position', width: 100 },
  { title: '默认', dataIndex: 'isDefault', width: 70, slotName: 'isDefault' },
  { title: '操作', slotName: 'action', width: 120 },
]
const invoiceCols: TableInstance['columns'] = [
  { title: '抬头', dataIndex: 'title' },
  { title: '税号', dataIndex: 'taxNo', width: 140 },
  { title: '默认', dataIndex: 'isDefault', width: 70, slotName: 'isDefault' },
  { title: '操作', slotName: 'action', width: 120 },
]
const addressCols: TableInstance['columns'] = [
  { title: '收货人', dataIndex: 'contactName', width: 100 },
  { title: '手机', dataIndex: 'mobile', width: 120 },
  { title: '地址', dataIndex: 'address', ellipsis: true, tooltip: true },
  { title: '默认', dataIndex: 'isDefault', width: 70, slotName: 'isDefault' },
  { title: '操作', slotName: 'action', width: 120 },
]
const payCols: TableInstance['columns'] = [
  { title: '户名', dataIndex: 'accountName' },
  { title: '开户行', dataIndex: 'bankName', width: 120 },
  { title: '账号', dataIndex: 'accountNo', width: 140 },
  { title: '默认', dataIndex: 'isDefault', width: 70, slotName: 'isDefault' },
  { title: '操作', slotName: 'action', width: 120 },
]

const loadAll = async () => {
  if (!unitId.value) return
  loading.value = true
  try {
    const [c, i, a, p] = await Promise.all([
      listUnitContacts(unitId.value),
      listUnitInvoices(unitId.value),
      listUnitAddresses(unitId.value),
      listUnitPays(unitId.value),
    ])
    contacts.value = c.data || []
    invoices.value = i.data || []
    addresses.value = a.data || []
    pays.value = p.data || []
  } finally {
    loading.value = false
  }
}

const open = async (id: string, name: string) => {
  unitId.value = id
  unitName.value = name
  activeTab.value = 'contact'
  visible.value = true
  await loadAll()
}

const onClose = () => {
  unitId.value = ''
  unitName.value = ''
}

const resetForm = () => {
  formId.value = ''
  Object.keys(form).forEach((k) => delete form[k])
  form.isDefault = 0
}

const openForm = (kind: Kind, row?: any) => {
  resetForm()
  formKind.value = kind
  if (row) {
    formId.value = String(row.id)
    Object.assign(form, row)
    form.isDefault = Number(row.isDefault) || 0
  }
  formVisible.value = true
}

const submitForm = async () => {
  try {
    const id = unitId.value
    const kind = formKind.value
    const payload = { ...form, isDefault: Number(form.isDefault) || 0 }
    if (kind === 'contact') {
      if (!payload.contactName) {
        Message.warning('请填写联系人')
        return false
      }
      if (formId.value) await updateUnitContact(id, formId.value, payload)
      else await addUnitContact(id, payload)
    } else if (kind === 'invoice') {
      if (!payload.title) {
        Message.warning('请填写发票抬头')
        return false
      }
      if (formId.value) await updateUnitInvoice(id, formId.value, payload)
      else await addUnitInvoice(id, payload)
    } else if (kind === 'address') {
      if (!payload.address) {
        Message.warning('请填写详细地址')
        return false
      }
      if (formId.value) await updateUnitAddress(id, formId.value, payload)
      else await addUnitAddress(id, payload)
    } else {
      if (!payload.accountName || !payload.accountNo) {
        Message.warning('请填写户名与账号')
        return false
      }
      if (formId.value) await updateUnitPay(id, formId.value, payload)
      else await addUnitPay(id, payload)
    }
    Message.success('保存成功')
    await loadAll()
    return true
  } catch {
    return false
  }
}

const onRemove = (kind: Kind, row: any) => {
  Modal.warning({
    title: '提示',
    content: '确认删除该条记录？',
    hideCancel: false,
    onOk: async () => {
      const id = unitId.value
      if (kind === 'contact') await deleteUnitContact(id, row.id)
      else if (kind === 'invoice') await deleteUnitInvoice(id, row.id)
      else if (kind === 'address') await deleteUnitAddress(id, row.id)
      else await deleteUnitPay(id, row.id)
      Message.success('删除成功')
      await loadAll()
    },
  })
}

defineExpose({ open })
</script>

<style scoped>
.sub-toolbar {
  margin-bottom: 8px;
  text-align: right;
}
</style>
