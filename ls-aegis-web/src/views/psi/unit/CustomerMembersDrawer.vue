<template>
  <a-drawer v-model:visible="visible" :title="`客户App成员 · ${customerName}`" :width="width >= 800 ? 800 : '100%'" :footer="false" unmount-on-close @close="close">
    <a-alert>员工只看自己提交的报修；公司管理员可查看本公司全部报修。每人独立设置用户名和密码。</a-alert>
    <a-alert v-if="loadFailed" type="error" class="toolbar">成员加载失败，请点击刷新重试。</a-alert>
    <a-space class="toolbar" wrap>
      <a-select v-model="inviteRole" style="width: 160px" :disabled="issuing">
        <a-option value="MEMBER">普通员工</a-option><a-option value="ADMIN">公司管理员</a-option>
      </a-select>
      <a-button type="primary" :loading="issuing" :disabled="loading || loadFailed" @click="issue()">签发新增成员码</a-button>
      <a-button :loading="loading" @click="load">刷新成员</a-button>
    </a-space>
    <a-table row-key="id" size="small" :data="members" :loading="loading" :columns="columns" :pagination="false" :scroll="{ x: 680 }">
      <template #role="{ record }"><a-tag :color="record.accountRole === 'ADMIN' ? 'blue' : 'gray'">{{ record.accountRole === 'ADMIN' ? '公司管理员' : '普通员工' }}</a-tag></template>
      <template #status="{ record }"><a-tag :color="record.status === 1 ? 'green' : 'gray'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag></template>
      <template #action="{ record }"><a-space><a-link @click="edit(record)">修改</a-link><a-link :disabled="record.status !== 1 || issuing" @click="confirmReset(record)">重置密码</a-link></a-space></template>
    </a-table>
    <a-modal v-model:visible="codeVisible" :title="codeTitle" :footer="false" @close="clearCode">
      <a-alert type="warning">仅通过可信方式交给指定成员。此码十分钟内有效，只能使用一次；不要让多人共用。</a-alert>
      <p>{{ codeDescription }}</p><p>到期时间：{{ expiresAt }}</p>
      <a-input-password :model-value="code" readonly autocomplete="off" />
      <a-button type="primary" :disabled="!code" @click="copyText(code)">复制访问码</a-button>
    </a-modal>
    <a-modal v-model:visible="editVisible" title="修改App成员" :mask-closable="!saving" :esc-to-close="!saving" :ok-loading="saving" @before-ok="save">
      <a-form :model="form" layout="vertical">
        <a-form-item label="用户名"><a-input :model-value="form.loginName" disabled /></a-form-item>
        <a-form-item label="姓名" required><a-input v-model="form.displayName" :max-length="50" /></a-form-item>
        <a-form-item label="角色"><a-select v-model="form.accountRole"><a-option value="MEMBER">普通员工</a-option><a-option value="ADMIN">公司管理员</a-option></a-select></a-form-item>
        <a-form-item label="状态"><a-radio-group v-model="form.status"><a-radio :value="1">启用</a-radio><a-radio :value="0">停用</a-radio></a-radio-group></a-form-item>
      </a-form>
      <a-alert>角色或状态修改后，该成员需重新登录；不影响其他成员。</a-alert>
    </a-modal>
  </a-drawer>
</template>
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { Message, Modal } from '@arco-design/web-vue'
import type { TableInstance } from '@arco-design/web-vue'
import { issueH5Invitation, listH5CustomerMembers, updateH5CustomerMember } from '@/apis/eoms/h5Auth'
import type { H5CustomerMember } from '@/apis/eoms/h5Auth'
import { copyText } from '@/utils'
const { width } = useWindowSize()
const visible = ref(false), customerId = ref(''), customerName = ref(''), loading = ref(false), issuing = ref(false)
const loadFailed = ref(false)
const members = ref<H5CustomerMember[]>([]), inviteRole = ref<'MEMBER' | 'ADMIN'>('MEMBER')
const codeVisible = ref(false), code = ref(''), codeTitle = ref(''), codeDescription = ref(''), expiresAt = ref('')
const editVisible = ref(false), saving = ref(false)
const form = reactive<H5CustomerMember>({ id: '', customerId: '', loginName: '', displayName: '', accountRole: 'MEMBER', status: 1 })
let generation = 0
const columns: TableInstance['columns'] = [
  { title: '用户名', dataIndex: 'loginName', width: 150 }, { title: '姓名', dataIndex: 'displayName', width: 130 },
  { title: '角色', slotName: 'role', width: 120 }, { title: '状态', slotName: 'status', width: 80 }, { title: '操作', slotName: 'action', width: 170 },
]
function clearCode() { code.value = ''; codeDescription.value = ''; expiresAt.value = '' }
function close() { generation++; clearCode(); codeVisible.value = false; editVisible.value = false; members.value = []; loading.value = false; issuing.value = false }
async function load() {
  if (!visible.value) return
  const current = generation; loading.value = true; loadFailed.value = false
  try { const { data } = await listH5CustomerMembers(customerId.value); if (current === generation) members.value = data || [] }
  catch { if (current === generation) loadFailed.value = true }
  finally { if (current === generation) loading.value = false }
}
async function open(id: string, name: string) {
  close(); customerId.value = id; customerName.value = name; visible.value = true
  await load(); inviteRole.value = members.value.length ? 'MEMBER' : 'ADMIN'
}
async function issue(member?: H5CustomerMember) {
  if (issuing.value || loading.value || loadFailed.value) return
  const current = generation; issuing.value = true; clearCode()
  try {
    const { data } = await issueH5Invitation(customerId.value, { accountRole: member?.accountRole || inviteRole.value, resetAccountId: member?.id })
    if (current !== generation || !visible.value) return
    code.value = data.invitationCode; expiresAt.value = new Date(Date.now() + data.expiresInSeconds * 1000).toLocaleString()
    codeTitle.value = member ? '重置指定成员密码' : '新增App成员'
    codeDescription.value = member ? `仅用于 ${member.displayName}（${member.loginName}），请沿用该用户名设置新密码。` : `角色：${inviteRole.value === 'ADMIN' ? '公司管理员' : '普通员工'}。请在客户服务App选择账号激活，自行设置个人用户名、姓名和密码。`
    codeVisible.value = true
  } finally { if (current === generation) issuing.value = false }
}
function confirmReset(member: H5CustomerMember) {
  Modal.warning({ title: '签发成员密码重置码', content: `仅为「${member.displayName}（${member.loginName}）」签发重置码？其他成员密码不变。`, hideCancel: false, onOk: () => issue(member) })
}
function edit(member: H5CustomerMember) { Object.assign(form, member); editVisible.value = true }
async function save() {
  if (!form.displayName.trim()) { Message.warning('请输入姓名'); return false }
  if (saving.value) return false
  saving.value = true
  try { await updateH5CustomerMember(customerId.value, { ...form, displayName: form.displayName.trim() }); await load(); Message.success('成员已更新'); return true }
  catch { return false } finally { saving.value = false }
}
defineExpose({ open })
</script>
<style scoped>.toolbar { margin:16px 0; }</style>
