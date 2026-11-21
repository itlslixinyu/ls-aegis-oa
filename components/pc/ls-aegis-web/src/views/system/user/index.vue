<template>
  <GiPageLayout>
    <template #left>
      <DeptTree @node-click="handleSelectDept" />
    </template>
    <BaseTable
      row-key="id"
      table-id="system-user-v6"
      class="user-table"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      :disabled-column-keys="['nickname']"
      @refresh="refresh"
    >
      <template #top>
        <div class="user-filter">
          <a-space wrap align="center" :size="[8, 8]">
            <a-input-search
              v-model="queryForm.description"
              placeholder="用户名 / 昵称 / 描述"
              allow-clear
              style="width: 220px"
              @search="search"
            />
            <a-select
              v-model="queryForm.status"
              :options="DisEnableStatusList"
              placeholder="状态"
              allow-clear
              style="width: 120px"
              @change="search"
            />
            <a-range-picker
              v-model="queryForm.createTime"
              show-time
              :format="DATETIME_FORMAT"
              :value-format="DATETIME_FORMAT"
              style="width: 360px"
              @change="search"
            />
            <a-button type="primary" @click="search">
              <template #icon><icon-search /></template>
              查询
            </a-button>
            <a-button @click="reset">
              <template #icon><icon-refresh /></template>
              重置
            </a-button>
          </a-space>
        </div>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['system:user:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          新增
        </a-button>
        <a-button v-permission="['system:user:import']" @click="onImport">
          <template #icon><icon-upload /></template>
          导入
        </a-button>
        <a-button v-permission="['system:user:export']" @click="onExport">
          <template #icon><icon-download /></template>
          导出
        </a-button>
      </template>
      <template #nickname="{ record }">
        <GiCellAvatar :avatar="record.avatar" :name="record.nickname" />
      </template>
      <template #gender="{ record }">
        <GiCellGender :gender="record.gender" />
      </template>
      <template #roleNames="{ record }">
        <GiCellTags :data="record.roleNames" />
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #isSystem="{ record }">
        <a-tag v-if="record.isSystem" color="red" size="small">是</a-tag>
        <a-tag v-else color="arcoblue" size="small">否</a-tag>
      </template>
      <template #action="{ record }">
        <div class="table-action">
          <a-space :size="0" :wrap="false">
            <a-link v-permission="['system:user:get']" title="详情" @click="onDetail(record)">详情</a-link>
            <a-link v-permission="['system:user:update']" title="修改" @click="onUpdate(record)">修改</a-link>
            <a-dropdown>
              <a-button
                v-if="has.hasPermOr(['system:user:resetPwd', 'system:user:updateRole', 'system:user:delete'])"
                type="text"
                size="mini"
                title="更多"
              >
                <template #icon>
                  <icon-more :size="16" />
                </template>
              </a-button>
              <template #content>
                <a-doption v-permission="['system:user:resetPwd']" @click="onResetPwd(record)">重置密码</a-doption>
                <a-doption
                  v-permission="['system:user:updateRole']"
                  :disabled="record.isSystem"
                  @click="onUpdateRole(record)"
                >
                  分配角色
                </a-doption>
                <a-doption v-permission="['system:user:delete']" :disabled="record.isSystem">
                  <a-link
                    status="danger"
                    :disabled="record.isSystem"
                    :title="record.isSystem ? '系统内置数据不能删除' : '删除'"
                    @click="onDelete(record)"
                  >
                    删除
                  </a-link>
                </a-doption>
              </template>
            </a-dropdown>
          </a-space>
        </div>
      </template>
    </BaseTable>

    <AddDrawer ref="AddDrawerRef" @save-success="search" />
    <ImportDrawer ref="ImportDrawerRef" @save-success="search" />
    <DetailDrawer ref="DetailDrawerRef" />
    <PwdResetModal ref="PwdResetModalRef" />
    <RoleUpdateModal ref="RoleUpdateModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import DeptTree from './dept/index.vue'
import AddDrawer from './AddDrawer.vue'
import ImportDrawer from './ImportDrawer.vue'
import DetailDrawer from './DetailDrawer.vue'
import PwdResetModal from './PwdResetModal.vue'
import RoleUpdateModal from './RoleUpdateModal.vue'
import { type UserResp, deleteUser, exportUser, listUser } from '@/apis/system/user'
import { DisEnableStatusList } from '@/constant/common'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { TableCol } from '@/constant/table-col'
import { useDownload, useResetReactive, useTable } from '@/hooks'
import has from '@/utils/has'

defineOptions({ name: 'SystemUser' })

const [queryForm, resetForm] = useResetReactive({
  sort: ['t1.id,desc'],
})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listUser({ ...queryForm, ...page }), { immediate: false })

const columns: TableInstance['columns'] = [
  {
    title: '序号',
    width: 64,
    align: 'center',
    render: ({ rowIndex }) => h('span', {}, rowIndex + 1 + (pagination.current - 1) * pagination.pageSize),
  },
  {
    title: '昵称',
    dataIndex: 'nickname',
    slotName: 'nickname',
    width: 148,
    align: 'center',
    ellipsis: true,
    tooltip: true,
  },
  {
    title: '用户名',
    dataIndex: 'username',
    width: 100,
    align: 'center',
    ellipsis: true,
    tooltip: true,
  },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: 88, align: 'center' },
  { title: '性别', dataIndex: 'gender', slotName: 'gender', width: 80, align: 'center' },
  {
    title: '所属部门',
    dataIndex: 'deptName',
    width: 120,
    align: 'center',
    ellipsis: true,
    tooltip: true,
  },
  { title: '角色', dataIndex: 'roleNames', slotName: 'roleNames', width: 140, align: 'center' },
  {
    title: '手机号',
    dataIndex: 'phone',
    width: 120,
    align: 'center',
    ellipsis: true,
    tooltip: true,
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    width: 180,
    align: 'center',
    ellipsis: true,
    tooltip: true,
  },
  { title: '系统内置', dataIndex: 'isSystem', slotName: 'isSystem', width: 96, align: 'center', show: false },
  {
    title: '描述',
    dataIndex: 'description',
    width: 140,
    ellipsis: true,
    tooltip: true,
    show: false,
  },
  { title: '创建人', dataIndex: 'createUserString', width: 100, ellipsis: true, tooltip: true, show: false },
  { title: '创建时间', dataIndex: 'createTime', width: 178, show: false },
  { title: '修改人', dataIndex: 'updateUserString', width: 100, ellipsis: true, tooltip: true, show: false },
  { title: '修改时间', dataIndex: 'updateTime', width: 178, show: false },
  {
    title: '操作',
    dataIndex: 'action',
    slotName: 'action',
    width: TableCol.actionM,
    align: 'center',
    show: has.hasPermOr([
      'system:user:get',
      'system:user:update',
      'system:user:resetPwd',
      'system:user:updateRole',
      'system:user:delete',
    ]),
  },
]
// 重置
const reset = () => {
  resetForm()
  search()
}

// 删除
const onDelete = (record: UserResp) => {
  return handleDelete(() => deleteUser(record.id), {
    content: `是否确定删除用户「${record.nickname}(${record.username})」？`,
    showModal: true,
  })
}

// 导出
const onExport = () => {
  useDownload(() => exportUser(queryForm))
}

// 根据选中部门查询
const handleSelectDept = (keys: Array<any>) => {
  queryForm.deptId = keys.length === 1 ? keys[0] : undefined
  search()
}

const ImportDrawerRef = ref<InstanceType<typeof ImportDrawer>>()
// 导入
const onImport = () => {
  ImportDrawerRef.value?.onOpen()
}

const AddDrawerRef = ref<InstanceType<typeof AddDrawer>>()
// 新增
const onAdd = () => {
  AddDrawerRef.value?.onAdd()
}

// 修改
const onUpdate = (record: UserResp) => {
  AddDrawerRef.value?.onUpdate(record.id)
}

const DetailDrawerRef = ref<InstanceType<typeof DetailDrawer>>()
// 详情
const onDetail = (record: UserResp) => {
  DetailDrawerRef.value?.onOpen(record.id)
}

const PwdResetModalRef = ref<InstanceType<typeof PwdResetModal>>()
// 重置密码
const onResetPwd = (record: UserResp) => {
  PwdResetModalRef.value?.onOpen(record.id, record.username)
}

const RoleUpdateModalRef = ref<InstanceType<typeof RoleUpdateModal>>()
// 分配角色
const onUpdateRole = (record: UserResp) => {
  RoleUpdateModalRef.value?.onOpen(record.id)
}
</script>

<style scoped lang="scss">
.user-table {
  :deep(.gi-table__toolbar) {
    margin-bottom: 8px;
  }
}

.table-action {
  display: inline-flex;
  justify-content: center;
  white-space: nowrap;

  :deep(.arco-link) {
    padding: 0 6px;
  }
}

.user-filter {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-2);

  :deep(.arco-space) {
    width: 100%;
  }

  :deep(.arco-space-item) {
    display: inline-flex;
    align-items: center;
  }

  :deep(.arco-select),
  :deep(.arco-picker),
  :deep(.arco-input-wrapper),
  :deep(.arco-input-search) {
    width: 100%;
  }
}
</style>
