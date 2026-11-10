<template>
  <GiPageLayout>
    <BaseTable
      ref="tableRef"
      row-key="id"
      table-id="system-menu-v4"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="false"
      :disabled-column-keys="['title']"
      @refresh="refresh"
    >
      <template #expand-icon="{ expanded }">
        <IconDown v-if="expanded" />
        <IconRight v-else />
      </template>
      <template #toolbar-left>
        <a-input v-model="title" placeholder="搜索菜单标题" allow-clear>
          <template #prefix><icon-search /></template>
        </a-input>
        <a-input v-model="path" placeholder="搜索路由地址" allow-clear>
          <template #prefix><icon-search /></template>
        </a-input>
        <a-input v-model="permission" placeholder="搜索权限标识" allow-clear>
          <template #prefix><icon-search /></template>
        </a-input>
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          <template #default>重置</template>
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['system:menu:create']" type="primary" @click="onAdd()">
          <template #icon><icon-plus /></template>
          <template #default>新增</template>
        </a-button>
        <a-button v-permission="['system:menu:clearCache']" type="outline" status="warning" @click="onClearCache">
          <template #icon><icon-delete /></template>
          <template #default>清除缓存</template>
        </a-button>
        <a-button @click="onExpanded">
          <template #icon>
            <icon-list v-if="isExpanded" />
            <icon-mind-mapping v-else />
          </template>
          <template #default>
            <span v-if="!isExpanded">展开</span>
            <span v-else>折叠</span>
          </template>
        </a-button>
      </template>
      <template #title="{ record }">
        <GiSvgIcon :name="record.icon" :size="15" />
        <span style="margin-left: 5px; vertical-align: middle">{{ record.title }}</span>
      </template>
      <template #type="{ record }">
        <a-tag v-if="record.type === 1" color="arcoblue">目录</a-tag>
        <a-tag v-if="record.type === 2" color="green">菜单</a-tag>
        <a-tag v-if="record.type === 3">按钮</a-tag>
      </template>
      <template #sort="{ record }">
        <a-tag v-if="record.type === 1" color="arcoblue">{{ record.sort }}</a-tag>
        <a-tag v-else-if="record.type === 2" color="green">{{ record.sort }}</a-tag>
        <a-tag v-else>{{ record.sort }}</a-tag>
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #isExternal="{ record }">
        <a-tag v-if="record.isExternal" color="arcoblue" size="small">是</a-tag>
        <a-tag v-else color="red" size="small">否</a-tag>
      </template>
      <template #isHidden="{ record }">
        <a-tag v-if="record.isHidden" color="arcoblue" size="small">是</a-tag>
        <a-tag v-else color="red" size="small">否</a-tag>
      </template>
      <template #isCache="{ record }">
        <a-tag v-if="record.isCache" color="arcoblue" size="small">是</a-tag>
        <a-tag v-else color="red" size="small">否</a-tag>
      </template>
      <template #action="{ record }">
        <div class="table-action">
          <a-space :size="0" :wrap="false">
            <a-link v-permission="['system:menu:update']" title="修改" @click="onUpdate(record)">修改</a-link>
            <a-link
              v-permission="['system:menu:create']"
              :disabled="![1, 2].includes(record.type)"
              :title="![1, 2].includes(record.type) ? '按钮类型不支持新增下级' : '新增下级'"
              @click="onAdd(record.id)"
            >
              新增下级
            </a-link>
            <a-link v-permission="['system:menu:delete']" status="danger" title="删除" @click="onDelete(record)">
              删除
            </a-link>
          </a-space>
        </div>
      </template>
    </BaseTable>

    <AddModal ref="AddModalRef" :menus="dataList" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import AddModal from './AddModal.vue'
import { type MenuResp, clearMenuCache, deleteMenu, listMenu } from '@/apis/system/menu'
import { TableCol } from '@/constant/table-col'
import { useTable } from '@/hooks'
import has from '@/utils/has'

defineOptions({ name: 'SystemMenu' })

const {
  tableData,
  loading,
  search,
  refresh,
  handleDelete,
} = useTable(() => listMenu(), { immediate: true })

const searchData = (menuTitle: string, menuPath: string, menuPermission: string) => {
  const loop = (data: MenuResp[]) => {
    const result = [] as MenuResp[]
    data.forEach((item: MenuResp) => {
      if (
        (!menuTitle || item.title?.toLowerCase().includes(menuTitle.toLowerCase()))
        && (!menuPath || item.path?.toLowerCase().includes(menuPath.toLowerCase()))
        && (!menuPermission || item.permission?.toLowerCase().includes(menuPermission.toLowerCase()))
      ) {
        result.push({ ...item })
      } else if (item.children) {
        const filterData = loop(item.children)
        if (filterData.length) {
          result.push({
            ...item,
            children: filterData,
          })
        }
      }
    })
    return result
  }
  return loop(tableData.value)
}

const title = ref('')
const path = ref('')
const permission = ref('')
const dataList = computed(() => {
  if (!title.value && !path.value && !permission.value) return tableData.value
  return searchData(title.value, path.value, permission.value)
})

const columns: TableInstance['columns'] = [
  { title: '菜单标题', dataIndex: 'title', slotName: 'title', width: TableCol.name, fixed: 'left' },
  { title: '类型', dataIndex: 'type', slotName: 'type', width: TableCol.type },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: TableCol.status },
  { title: '排序', dataIndex: 'sort', slotName: 'sort', width: TableCol.qty },
  { title: '路由地址', dataIndex: 'path', width: TableCol.codeMin },
  { title: '组件名称', dataIndex: 'name', width: TableCol.codeMinSm },
  { title: '组件路径', dataIndex: 'component', width: TableCol.name },
  { title: '权限标识', dataIndex: 'permission', width: TableCol.name },
  { title: '外链', dataIndex: 'isExternal', slotName: 'isExternal', width: TableCol.typeSm, show: false },
  { title: '隐藏', dataIndex: 'isHidden', slotName: 'isHidden', width: TableCol.typeSm },
  { title: '缓存', dataIndex: 'isCache', slotName: 'isCache', width: TableCol.typeSm, show: false },
  { title: '创建人', dataIndex: 'createUserString', width: 100, show: false },
  { title: '创建时间', dataIndex: 'createTime', width: TableCol.date },
  { title: '修改人', dataIndex: 'updateUserString', width: 100, show: false },
  { title: '修改时间', dataIndex: 'updateTime', width: TableCol.date, show: false },
  {
    title: '操作',
    dataIndex: 'action',
    slotName: 'action',
    width: TableCol.actionL,
    show: has.hasPermOr(['system:menu:update', 'system:menu:create', 'system:menu:delete']),
  },
]

const reset = () => {
  title.value = ''
  path.value = ''
  permission.value = ''
}

const onDelete = (record: MenuResp) => {
  return handleDelete(() => deleteMenu(record.id), {
    content: `是否确定菜单「${record.title}」？`,
    showModal: true,
  })
}

const onClearCache = () => {
  Modal.warning({
    title: '提示',
    content: `是否确定清除全部菜单缓存？`,
    hideCancel: false,
    maskClosable: false,
    onOk: async () => {
      await clearMenuCache()
      Message.success('清除成功')
    },
  })
}

const isExpanded = ref(false)
const tableRef = ref<{ tableRef?: { expandAll: (expanded: boolean) => void } }>()
const onExpanded = () => {
  isExpanded.value = !isExpanded.value
  tableRef.value?.tableRef?.expandAll(isExpanded.value)
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const onAdd = (parentId?: string) => {
  AddModalRef.value?.onAdd(parentId)
}

const onUpdate = (record: MenuResp) => {
  AddModalRef.value?.onUpdate(record.id)
}
</script>

<style scoped lang="scss">
.table-action {
  display: inline-flex;
  justify-content: center;
  white-space: nowrap;

  :deep(.arco-link) {
    padding: 0 6px;
  }
}
</style>
