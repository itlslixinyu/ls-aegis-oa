import request from '@/utils/request'

/**
 * 系统外观：走平台 SITE 配置（与 PC /system/common/dict/option/site 一致）
 * 无需鉴权（@SaIgnore）
 */
export function getSiteOptions() {
  return request({
    url: '/system/common/dict/option/site',
    method: 'get',
    headers: { isToken: false }
  })
}

/** 公共字典（标签非敏感，@SaIgnore；与 PC listCommonDict 同接口） */
export function listCommonDict(code) {
  return request({
    url: `/system/common/dict/${code}`,
    method: 'get',
    headers: { isToken: false }
  }).then((res) => {
    const list = res?.data ?? res ?? []
    return Array.isArray(list) ? list : []
  }).catch(() => [])
}
