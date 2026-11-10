<template>
  <GiPageLayout>
    <a-card title="签章管理" :bordered="false">
      <a-row :gutter="16">
        <a-col v-for="item in menus" :key="item.title" :span="6" style="margin-bottom: 16px">
          <a-card hoverable class="menu-card" @click="go(item)">
            <div class="menu-title">{{ item.title }}</div>
            <div class="menu-desc">{{ item.desc }}</div>
          </a-card>
        </a-col>
      </a-row>
    </a-card>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { LocationQueryRaw, RouteLocationRaw } from 'vue-router'

defineOptions({ name: 'SignHome' })

const router = useRouter()

type SignMenuItem = {
  title: string
  desc: string
  to: RouteLocationRaw
}

const menus: SignMenuItem[] = [
  { title: '仪表盘', to: '/sign/dashboard', desc: '项目与签章概览统计' },
  { title: '项目管理', to: '/sign/project', desc: '签章项目；可选用合同号作快照，签署不必进合同台账' },
  { title: '印章管理', to: '/sign/seal', desc: '印章维护、授权与证书' },
  { title: '印章提取', to: '/sign/seal/extract', desc: '从图片提取印章素材' },
  { title: '发起签章', to: '/sign/execute', desc: '选择项目绑定文件并执行签章' },
  { title: '签章日志', to: '/sign/log', desc: '签章记录查询与下载' },
  { title: '指纹校验', to: '/sign/verify', desc: '签后文件指纹与 CA 校验' },
  {
    title: '审计日志',
    to: { path: '/monitor/log', query: { tabKey: '3' } satisfies LocationQueryRaw },
    desc: '操作审计追踪（系统监控 / 系统日志）',
  },
]

const go = (item: SignMenuItem) => {
  router.push(item.to)
}
</script>

<style scoped>
.menu-card {
  cursor: pointer;
  min-height: 100px;
}
.menu-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}
.menu-desc {
  color: var(--color-text-3);
  font-size: 13px;
}
</style>
