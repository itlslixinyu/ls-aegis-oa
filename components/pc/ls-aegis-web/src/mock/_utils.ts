import Mock from 'mockjs'
import XEUtils from 'xe-utils'

/** 返回成功数据 */
export const resultSuccess = (data: unknown) => {
  return Mock.mock({
    code: 200,
    data,
    msg: '请求成功',
    success: true,
  })
}

/** 获取 50-300ms 的随机延时 */
export const getDelayTime = () => {
  return XEUtils.random(50, 300)
}
