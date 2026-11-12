/**
 * H5客户报修API
 */
import request from '@/utils/request'
import { normalizeUploadImageFile } from '@/utils/uploadImage'

/** 获取 H5 专用图形验证码 */
export function getImageCaptcha() {
  return request({
    url: '/eoms/h5/auth/captcha',
    method: 'get'
  })
}

/** 客户首次激活或使用管理员新邀请码重置密码 */
export function activateAccount(data) {
  return request({
    url: '/eoms/h5/auth/activate',
    method: 'post',
    data
  })
}

/** 客户用户名密码登录（需图形验证码） */
export function passwordLogin(data) {
  return request({
    url: '/eoms/h5/auth/password',
    method: 'post',
    data
  })
}

/** 提交报修申请 */
export function submitApply(data) {
  return request({
    url: '/eoms/h5/repair/apply',
    method: 'post',
    data
  })
}

/** H5端带水印的图片上传
 * @param {File} file 图片文件
 * @param {Object} options 水印元数据（可选）
 * @param {string} options.location 定位地址（兼容旧参数）
 * @param {string} options.longitude 经度
 * @param {string} options.latitude 纬度
 * @param {string} options.title 水印标题（默认"现场照片"）
 * @param {string} options.taskName 任务名称
 * @param {string} options.executor 执行人
 * @param {string} options.remark 备注
 * @param {string} options.serviceAddress 服务地址
 * @param {string} options.projectName 项目名称
 * @param {string} options.workTime 报修时间（不传则服务端用当前时间）
 */
export function uploadWithWatermark(file, options = {}) {
  const formData = new FormData()
  formData.append('file', normalizeUploadImageFile(file))
  const { location, longitude, latitude, title, taskName, executor, remark, serviceAddress, projectName, workTime } = options
  if (location) formData.append('location', location)
  if (longitude) formData.append('longitude', longitude)
  if (latitude) formData.append('latitude', latitude)
  if (title) formData.append('title', title)
  if (taskName) formData.append('taskName', taskName)
  if (executor) formData.append('executor', executor)
  if (remark) formData.append('remark', remark)
  if (serviceAddress) formData.append('serviceAddress', serviceAddress)
  if (projectName) formData.append('projectName', projectName)
  if (workTime) formData.append('workTime', workTime)
  return request({
    url: '/eoms/h5/repair/upload',
    method: 'post',
    data: formData,
    signal: options.signal,
    onUploadProgress: options.onUploadProgress
  })
}

/** 查询我的报修列表 */
export function getRepairList(params) {
  return request({
    url: '/eoms/h5/repair/list',
    method: 'get',
    params
  })
}

/** 查询维修详情 */
export function getRepairDetail(id) {
  return request({
    url: `/eoms/h5/repair/detail/${id}`,
    method: 'get'
  })
}

/** 客户确认完成 */
export function confirmRepair(data) {
  return request({
    url: '/eoms/h5/repair/confirm',
    method: 'post',
    data
  })
}

/** 退出登录 */
export function logout(token) {
  return request({
    url: '/eoms/h5/auth/logout',
    method: 'post',
    headers: { 'h5-token': token }
  })
}

/** 当前客户尚未确认的维修结果，服务端按会话归属分页。 */
export function getPendingConfirmations(params) {
  return request({ url: '/eoms/h5/repair/todos', method: 'get', params })
}
