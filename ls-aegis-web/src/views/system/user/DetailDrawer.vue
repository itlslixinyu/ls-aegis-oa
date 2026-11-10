<template>
  <a-drawer v-model:visible="visible" title="用户详情" :width="width >= 560 ? 560 : '100%'" :footer="false">
    <a-form class="user-detail-form" layout="horizontal" auto-label-width :model="form">
      <a-form-item label="ID">
        <a-input :model-value="form.id" readonly>
          <template #suffix>
            <icon-copy class="copy-icon" @click="onCopyId" />
          </template>
        </a-input>
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="用户名">
            <a-input :model-value="form.username" readonly />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="昵称">
            <a-input :model-value="form.nickname" readonly />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="性别">
            <a-input :model-value="genderText" readonly />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态">
            <a-input :model-value="statusText" readonly />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="手机号">
            <a-input :model-value="form.phone || '暂无'" readonly />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="邮箱">
            <a-input :model-value="form.email || '暂无'" readonly />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="所属部门">
            <a-input :model-value="form.deptName" readonly />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="角色">
            <a-input :model-value="roleText" readonly />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="创建人">
            <a-input :model-value="form.createUserString || '—'" readonly />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="创建时间">
            <a-input :model-value="form.createTime || '—'" readonly />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="修改人">
            <a-input :model-value="form.updateUserString || '—'" readonly />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="修改时间">
            <a-input :model-value="form.updateTime || '—'" readonly />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="描述">
        <a-textarea :model-value="form.description || '—'" :auto-size="{ minRows: 2, maxRows: 4 }" readonly />
      </a-form-item>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useClipboard, useWindowSize } from '@vueuse/core'
import { type UserDetailResp, getUser as getDetail } from '@/apis/system/user'

defineOptions({ name: 'UserDetailDrawer' })

const { width } = useWindowSize()
const { copy } = useClipboard({ legacy: true })

const dataId = ref('')
const form = ref<Partial<UserDetailResp>>({})
const visible = ref(false)

const genderText = computed(() => {
  if (form.value.gender === 1) return '男'
  if (form.value.gender === 2) return '女'
  return '未知'
})

const statusText = computed(() => (form.value.status === 1 ? '启用' : '禁用'))

const roleText = computed(() => {
  const names = form.value.roleNames
  return names?.length ? names.join('、') : '—'
})

const onCopyId = async () => {
  if (!form.value.id) return
  await copy(String(form.value.id))
  Message.success('已复制 ID')
}

const getDataDetail = async () => {
  const { data } = await getDetail(dataId.value)
  form.value = data
}

const onOpen = async (id: string) => {
  dataId.value = id
  await getDataDetail()
  visible.value = true
}

defineExpose({ onOpen })
</script>

<style scoped lang="scss">
.user-detail-form {
  :deep(.arco-input-wrapper),
  :deep(.arco-textarea-wrapper) {
    background-color: var(--color-fill-2);
  }

  :deep(.arco-input[readonly]),
  :deep(.arco-textarea[readonly]) {
    cursor: default;
    color: var(--color-text-1);
  }
}

.copy-icon {
  cursor: pointer;
  color: var(--color-text-3);

  &:hover {
    color: rgb(var(--primary-6));
  }
}
</style>
