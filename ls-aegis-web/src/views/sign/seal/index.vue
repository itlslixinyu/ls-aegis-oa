<template>
  <GiPageLayout>
    <BaseTable
      row-key="sealId"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-input-search
          v-model="queryForm.sealName"
          placeholder="搜索印章名称"
          allow-clear
          @search="search"
        />
        <a-select
          v-model="queryForm.sealCategory"
          placeholder="印章类型"
          allow-clear
          style="width: 160px"
          :options="sealCategoryOptions"
          @change="search"
        />
        <a-select
          v-model="queryForm.status"
          placeholder="状态"
          allow-clear
          style="width: 120px"
          :options="statusOptions"
          @change="search"
        />
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['sign:seal:add']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          新增印章
        </a-button>
        <a-button v-permission="['sign:seal:export']" @click="onExport">
          <template #icon><icon-download /></template>
          导出
        </a-button>
      </template>
      <template #category="{ record }">
        <a-tag>{{ record.categoryLabel || categoryLabel(record.sealCategory) }}</a-tag>
      </template>
      <template #status="{ record }">
        <GiCellTag :value="record.status" :dict="sign_seal_status" />
      </template>
      <template #certStatus="{ record }">
        <a-tag :color="certColor(record.certStatus)">{{ record.certStatus || '未绑定' }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['sign:seal:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-permission="['sign:seal:bind']" @click="onBindCert(record)">绑定证书</a-link>
          <a-link v-permission="['sign:seal:auth']" @click="onAuth(record)">授权</a-link>
          <a-link @click="onPreview(record)">预览</a-link>
          <a-link v-permission="['sign:seal:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>

    <AddModal ref="AddModalRef" @save-success="search" />

    <a-modal v-model:visible="previewVisible" title="印章预览" :width="400" :footer="false" @close="onPreviewClose">
      <div style="text-align: center">
        <img v-if="previewUrl" :src="previewUrl" style="max-width: 300px; max-height: 300px" />
        <a-empty v-else description="暂无印章图片" />
      </div>
    </a-modal>

    <a-modal
      v-model:visible="authVisible"
      title="印章授权"
      :width="520"
      @before-ok="submitAuth"
      @close="authVisible = false"
    >
      <a-alert type="info" style="margin-bottom: 12px">选择可使用此印章的用户</a-alert>
      <a-select
        v-model="selectedUserIds"
        multiple
        allow-search
        placeholder="选择用户"
        style="width: 100%"
        :options="userOptions"
      />
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { SignCol } from '../constants/table-col'
import AddModal from './AddModal.vue'
import {
  type SealResp,
  bindSealAuth,
  bindSealCert,
  delSeal,
  exportSeal,
  fetchSignImageObjectUrl,
  getSealAuthUsers,
  getSealCertInfo,
  getSealImageResource,
  listSeal,
} from '@/apis/sign'
import { listAllUser } from '@/apis/system/user'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions, findDictLabel } from '@/utils/dict'
import { ensureSelectOpt, toBizIdList, toUserSelectOpts } from '@/utils/biz-id'

defineOptions({ name: 'SignSeal' })

const { sign_seal_category, sign_seal_status } = useDict('sign_seal_category', 'sign_seal_status')
const sealCategoryOptions = computed(() => dictSelectOptions(sign_seal_category.value))
const statusOptions = computed(() => dictSelectOptions(sign_seal_status.value))

const queryForm = reactive<Record<string, any>>({})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable(async (page) => {
  const res = await listSeal({ ...queryForm, ...page })
  const list = await Promise.all(
    (res.data?.list || []).map(async (row) => {
      try {
        const cert = await getSealCertInfo(row.sealId)
        return { ...row, ...cert.data, certStatus: cert.data?.certStatus || (row.certId ? '已绑定' : '未绑定') }
      } catch {
        return { ...row, certStatus: row.certId ? '已绑定' : '未绑定' }
      }
    }),
  )
  return { ...res, data: { ...res.data, list } }
}, { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '印章名称', dataIndex: 'sealName', width: SignCol.nameLg },
  { title: '印章编号', dataIndex: 'sealCode', minWidth: SignCol.codeMin, ellipsis: false },
  { title: '印章类型', dataIndex: 'sealCategory', slotName: 'category', width: SignCol.type },
  { title: '所属部门', dataIndex: 'deptName', width: SignCol.name },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: SignCol.status },
  { title: '平台证书', dataIndex: 'certStatus', slotName: 'certStatus', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: SignCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: SignCol.actionXL },
]

const categoryLabel = (v?: string) => findDictLabel(sign_seal_category.value, v)
const certColor = (status?: string) => {
  const map: Record<string, string> = {
    已绑定: 'green',
    未绑定: 'gray',
    已过期: 'orangered',
    已吊销: 'red',
    绑定异常: 'red',
    已禁用: 'orangered',
    未生效: 'orangered',
  }
  return map[status || ''] || 'gray'
}

const reset = () => {
  Object.keys(queryForm).forEach((k) => {
    queryForm[k] = undefined
  })
  search()
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const onAdd = () => AddModalRef.value?.onAdd()
const onUpdate = (record: SealResp) => AddModalRef.value?.onUpdate(record.sealId)
const onDelete = (record: SealResp) =>
  handleDelete(() => delSeal(record.sealId), { content: `是否删除印章「${record.sealName}」？` })
const onExport = () => useDownload(() => exportSeal(queryForm))

const onBindCert = async (record: SealResp) => {
  try {
    await bindSealCert({ sealId: record.sealId })
    Message.success('平台证书绑定成功')
    search()
  } catch (e: any) {
    Message.error(e?.msg || e?.message || '平台证书绑定失败')
  }
}

const previewVisible = ref(false)
const previewUrl = ref('')
const onPreview = async (record: SealResp) => {
  previewVisible.value = true
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  try {
    previewUrl.value = await fetchSignImageObjectUrl(getSealImageResource(record.sealId))
  } catch {
    Message.error('印章图片加载失败')
  }
}
const onPreviewClose = () => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

const authVisible = ref(false)
const currentSealId = ref<string>()
const selectedUserIds = ref<string[]>([])
const userOptions = ref<{ label: string, value: string }[]>([])

const onAuth = async (record: SealResp) => {
  currentSealId.value = record.sealId
  authVisible.value = true
  try {
    const [usersRes, authRes] = await Promise.all([
      listAllUser({}),
      getSealAuthUsers(record.sealId),
    ])
    const users = Array.isArray(usersRes.data) ? usersRes.data : []
    userOptions.value = toUserSelectOpts(users)
    selectedUserIds.value = toBizIdList(authRes.data)
    selectedUserIds.value.forEach((uid) => ensureSelectOpt(userOptions.value, uid, '已删除用户'))
  } catch {
    selectedUserIds.value = []
  }
}

const submitAuth = async () => {
  if (!currentSealId.value) return false
  try {
    await bindSealAuth(currentSealId.value, selectedUserIds.value)
    Message.success('授权成功')
    return true
  } catch {
    return false
  }
}

onBeforeUnmount(() => onPreviewClose())
</script>
