// 工程师构建专用页面，身份及接口仍使用平台共用底座。
export default [
  {
    path: '/engineer/pool',
    name: 'EngineerPool',
    component: () => import('@/views/engineer/Pool.vue'),
    meta: { title: '工单池', requireAuth: true, role: 'engineer' }
  },
  {
    path: '/engineer/password',
    name: 'EngineerPassword',
    component: () => import('@/views/engineer/Password.vue'),
    meta: { title: '修改密码', requireAuth: true, role: 'engineer' }
  },
  {
    path: '/engineer/todos',
    name: 'EngineerTodos',
    component: () => import('@/views/engineer/Todos.vue'),
    meta: { title: '我的待办', requireAuth: true, role: 'engineer' }
  },
  {
    path: '/engineer/login',
    name: 'EngineerLogin',
    component: () => import('@/views/engineer/Login.vue'),
    meta: { title: '工程师登录', requireAuth: false, role: 'engineer' }
  },
  {
    path: '/engineer/workbench',
    name: 'EngineerWorkbench',
    component: () => import('@/views/engineer/Workbench.vue'),
    meta: { title: '工作台', requireAuth: true, role: 'engineer' }
  },
  {
    path: '/engineer/tasks/status/:status',
    name: 'EngineerTaskStatus',
    component: () => import('@/views/engineer/TaskStatus.vue'),
    meta: { title: '任务状态', requireAuth: true, role: 'engineer' }
  },
  {
    path: '/engineer/tasks',
    name: 'EngineerTasks',
    component: () => import('@/views/engineer/Tasks.vue'),
    meta: { title: '我的任务', requireAuth: true, role: 'engineer' }
  },
  {
    path: '/engineer/task/:id',
    name: 'EngineerTaskDetail',
    component: () => import('@/views/engineer/TaskDetail.vue'),
    meta: { title: '任务详情', requireAuth: true, role: 'engineer' }
  },
  {
    path: '/engineer/record/:orderId',
    name: 'EngineerRecord',
    component: () => import('@/views/engineer/Record.vue'),
    meta: { title: '维修记录', requireAuth: true, role: 'engineer' }
  },
  {
    path: '/engineer/profile',
    name: 'EngineerProfile',
    component: () => import('@/views/engineer/Profile.vue'),
    meta: { title: '个人中心', requireAuth: true, role: 'engineer' }
  },
]
