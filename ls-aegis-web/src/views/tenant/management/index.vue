<template>
  <GiPageLayout>
    <BaseTable
      row-key="id"
      table-id="tenant-management-v2"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      :disabled-column-keys="['name']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-input-search v-model="queryForm.description" placeholder="搜索名称/描述" allow-clear @search="search" />
        <a-select
          v-model="queryForm.packageId"
          :options="packageList"
          placeholder="请选择套餐"
          style="width: 200px"
          allow-clear
          @change="search"
        />
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          <template #default>重置</template>
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['tenant:management:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          <template #default>新增</template>
        </a-button>
      </template>

      <template #code="{ record }">
        <CellCopy :content="record.code" />
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #expireTime="{ record }">
        <span v-if="!record.expireTime">
          <span>永不过期</span>
        </span>
        <span v-else>{{ record.expireTime }}</span>
      </template>
      <template #domain="{ record }">
        <a
          v-if="record.domain"
          style="color: rgb(var(--arcoblue-7))"
          :href="/^https?:\/\//i.test(record.domain) ? record.domain : `https://${record.domain}`"
          target="_blank"
          rel="noopener noreferrer"
        >{{ record.domain }}</a>
        <span v-else style="color: red" class="text-red-4">未设置</span>
      </template>
      <template #action="{ record }">
        <div class="table-action">
          <a-space :size="0" :wrap="false">
            <a-link v-permission="['tenant:management:get']" title="详情" @click="onDetail(record)">详情</a-link>
            <a-link v-permission="['tenant:management:update']" title="修改" @click="onUpdate(record)">修改</a-link>
            <a-dropdown>
              <a-button v-if="has.hasPermOr(['tenant:management:updateAdminUserPwd', 'tenant:management:delete'])" type="text" size="mini" title="更多">
                <template #icon>
                  <icon-more :size="16" />
                </template>
              </a-button>
              <template #content>
                <a-doption v-permission="['tenant:management:updateAdminUserPwd']" title="修改管理员密码" @click="onUpdateAdminUserPwd(record)">修改管理员密码</a-doption>
                <a-doption
                  v-permission="['tenant:management:delete']"
                  :disabled="record.disabled"
                  :title="record.disabled ? '禁止删除' : '删除'"
                  @click="onDelete(record)"
                >
                  删除
                </a-doption>
              </template>
            </a-dropdown>
          </a-space>
        </div>
      </template>
    </BaseTable>

    <AddModal ref="AddModalRef" @save-success="search" />
    <DetailDrawer ref="DetailDrawerRef" />
    <AdminUserPwdUpdateModal ref="AdminUserPwdUpdateModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import AddModal from './AddModal.vue'
import AdminUserPwdUpdateModal from './AdminUserPwdUpdateModal.vue'
import DetailDrawer from './DetailDrawer.vue'
import { type TenantQuery, type TenantResp, deleteTenant, listTenant } from '@/apis/tenant/management'
import { TableCol } from '@/constant/table-col'
import { useTable } from '@/hooks'
import has from '@/utils/has'
import { listTenantPackageDict } from '@/apis/tenant'
import { toLabelValueOpts } from '@/utils/biz-id'

defineOptions({ name: 'TenantManagement' })

const queryForm = reactive<TenantQuery>({
  description: undefined,
  packageId: undefined,
  status: undefined,
  sort: ['createTime,desc'],
})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listTenant({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  {
    title: '序号',
    width: TableCol.index,
    align: 'center',
    render: ({ rowIndex }) => h('span', {}, rowIndex + 1 + (pagination.current - 1) * pagination.pageSize),
    fixed: 'left',
  },
  { title: '编码', dataIndex: 'code', slotName: 'code', minWidth: TableCol.codeMinSm, ellipsis: false },
  { title: '名称', dataIndex: 'name', slotName: 'name' },
  { title: '套餐', dataIndex: 'packageName', slotName: 'packageName', width: TableCol.name },
  { title: '域名', dataIndex: 'domain', slotName: 'domain' },
  { title: '过期时间', dataIndex: 'expireTime', slotName: 'expireTime', width: TableCol.date },
  { title: '管理员用户', dataIndex: 'adminUsername', slotName: 'adminUsername', width: TableCol.codeMinSm },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: TableCol.status },
  { title: '描述', dataIndex: 'description', width: TableCol.name },
  { title: '创建人', dataIndex: 'createUserString', width: 100, show: false },
  { title: '创建时间', dataIndex: 'createTime', width: TableCol.date },
  { title: '修改人', dataIndex: 'updateUserString', width: 100, show: false },
  { title: '修改时间', dataIndex: 'updateTime', width: TableCol.date, show: false },
  {
    title: '操作',
    dataIndex: 'action',
    slotName: 'action',
    width: TableCol.actionM,
    show: has.hasPermOr(['tenant:management:get', 'tenant:management:update', 'tenant:management:delete', 'tenant:management:updateAdminUserPwd']),
  },
]

const reset = () => {
  queryForm.description = undefined
  queryForm.packageId = undefined
  queryForm.status = undefined
  search()
}

const onDelete = (record: TenantResp) => {
  return handleDelete(() => deleteTenant(record.id), {
    content: `是否确定删除租户「${record.name}(${record.code})」？`,
    showModal: true,
  })
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const onAdd = () => {
  AddModalRef.value?.onAdd()
}

const onUpdate = (record: TenantResp) => {
  AddModalRef.value?.onUpdate(record.id)
}

const DetailDrawerRef = ref<InstanceType<typeof DetailDrawer>>()
const onDetail = (record: TenantResp) => {
  DetailDrawerRef.value?.onOpen(record.id)
}

const AdminUserPwdUpdateModalRef = ref<InstanceType<typeof AdminUserPwdUpdateModal>>()
const onUpdateAdminUserPwd = (record: TenantResp) => {
  AdminUserPwdUpdateModalRef.value?.open(record.id)
}

const packageList = ref<{ label: string, value: string }[]>([])
const getPackageList = async () => {
  const { data } = await listTenantPackageDict()
  packageList.value = toLabelValueOpts(data)
}

onMounted(() => {
  getPackageList()
})
</script>

<style scoped lang="scss">
.table-action {
  display: inline-flex;
  justify-content: center;
  white-space: nowrap;
}
</style>
