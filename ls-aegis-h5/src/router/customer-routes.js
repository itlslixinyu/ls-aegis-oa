// 客户端构建专用页面，身份及接口仍使用平台共用底座。
export default [
  {
    path: '/customer/auth',
    name: 'CustomerAuth',
    component: () => import('@/views/customer/Auth.vue'),
    meta: { title: '客户登录', requireH5Token: false, role: 'customer' }
  },
  {
    path: '/customer/workbench',
    name: 'CustomerWorkbench',
    component: () => import('@/views/customer/Workbench.vue'),
    meta: { title: '工作台', requireH5Token: true, role: 'customer' }
  },
  {
    path: '/customer/apply',
    name: 'CustomerApply',
    component: () => import('@/views/customer/Apply.vue'),
    meta: { title: '在线报修', requireH5Token: true, role: 'customer' }
  },
  {
    path: '/customer/repairs/all',
    name: 'CustomerAllRepairs',
    component: () => import('@/views/customer/AllRepairs.vue'),
    meta: { title: '我的报修', requireH5Token: true, role: 'customer' }
  },
  {
    path: '/customer/repairs/status/:status',
    name: 'CustomerRepairStatus',
    component: () => import('@/views/customer/RepairStatus.vue'),
    meta: { title: '报修状态', requireH5Token: true, role: 'customer' }
  },
  {
    path: '/customer/repairs',
    name: 'CustomerRepairs',
    component: () => import('@/views/customer/Repairs.vue'),
    meta: { title: '我的报修', requireH5Token: true, role: 'customer' }
  },
  {
    path: '/customer/profile',
    name: 'CustomerProfile',
    component: () => import('@/views/customer/Profile.vue'),
    meta: { title: '我的', requireH5Token: true, role: 'customer' }
  },
  {
    path: '/customer/repair/:id',
    name: 'CustomerRepairDetail',
    component: () => import('@/views/customer/RepairDetail.vue'),
    meta: { title: '维修详情', requireH5Token: true, role: 'customer' }
  },
]
