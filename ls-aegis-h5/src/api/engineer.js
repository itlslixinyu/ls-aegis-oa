/**
 * 工程师端业务 API（登录见 @/api/auth）
 */
import request from '@/utils/request'
import { normalizeUploadImageFile } from '@/utils/uploadImage'
import { accountLogin, logout as authLogout, getUserInfo, getImageCaptcha } from '@/api/auth'

/** @deprecated 使用 accountLogin；保留别名减少页面改动面 */
export function login(data) {
  return accountLogin(data)
}

export function getCodeImg() {
  return getImageCaptcha()
}

export function getInfo() {
  return getUserInfo()
}

/** Platform self-service password update; transport values must be SM2 ciphertext. */
export function updatePassword(data) {
  return request({ url: '/user/profile/password', method: 'patch', data })
}

export function logout() {
  return authLogout()
}

/** 工程师任务列表（平台分页 page/size） */
export function getTaskList(params) {
  if (params?.status === 'COMPLETED') {
    params = { ...params, status: undefined, serviceCompleted: true }
  }
  return request({
    url: '/eoms/engineer/tasks',
    method: 'get',
    params
  })
}

export function getTaskDetail(id) {
  return request({
    url: `/eoms/engineer/task/${id}`,
    method: 'get'
  })
}

export function acceptTask(id, remark) {
  return request({
    url: `/eoms/engineer/accept/${id}`,
    method: 'post',
    params: { remark }
  })
}

export function startTask(id, remark, longitude, latitude, location) {
  return request({
    url: `/eoms/engineer/start/${id}`,
    method: 'post',
    params: { remark, longitude, latitude, location }
  })
}

export function uploadRepairPhoto(file, orderId, photoType, longitude, latitude, options = {}) {
  const formData = new FormData()
  formData.append('file', normalizeUploadImageFile(file))
  formData.append('orderId', orderId)
  formData.append('photoType', photoType)
  if (longitude) formData.append('longitude', longitude)
  if (latitude) formData.append('latitude', latitude)
  if (options.location) formData.append('location', options.location)
  if (options.remark) formData.append('remark', options.remark)
  return request({
    url: '/eoms/engineer/upload',
    method: 'post',
    data: formData,
    signal: options.signal,
    onUploadProgress: options.onUploadProgress
  })
}

export function submitRecord(data) {
  return request({
    url: '/eoms/engineer/record',
    method: 'post',
    data
  })
}

export function finishTask(id, remark, longitude, latitude, location) {
  return request({
    url: `/eoms/engineer/finish/${id}`,
    method: 'post',
    params: { remark, longitude, latitude, location }
  })
}

export function getProfile() {
  return request({
    url: '/eoms/engineer/profile',
    method: 'get'
  })
}

/** Tenant-wide open reports; claiming atomically creates or takes over the unaccepted task. */
export function getPoolList(params) {
  return request({ url: '/eoms/engineer/pool', method: 'get', params })
}
export function claimPoolTask(applyId) {
  return request({ url: `/eoms/engineer/pool/${applyId}/claim`, method: 'post' })
}

export function getMaterialOptions(orderId, keyword = '') {
  return request({ url: `/eoms/engineer/task/${orderId}/material-options`, method: 'get', params: { keyword } })
}
export function getTaskMaterials(orderId) {
  return request({ url: `/eoms/engineer/task/${orderId}/materials`, method: 'get' })
}
