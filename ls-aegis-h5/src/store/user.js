import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态管理
 * 管理客户H5令牌和工程师JWT令牌
 */
export const useUserStore = defineStore('user', () => {
  // 客户H5令牌
  const h5Token = ref(localStorage.getItem('h5_token') || '')
  // 客户信息
  const customerInfo = ref(JSON.parse(localStorage.getItem('customer_info') || 'null'))
  // 工程师JWT令牌（复用ITLS平台token）
  const engineerToken = ref(localStorage.getItem('engineer_token') || '')
  // 工程师用户信息
  const engineerInfo = ref(JSON.parse(localStorage.getItem('engineer_info') || 'null'))
  // In-memory identity generations invalidate responses/timers from previous logins.
  const h5SessionVersion = ref(0)
  const engineerSessionVersion = ref(0)

  // 是否已登录客户身份
  const isCustomerAuthed = computed(() => !!h5Token.value)
  // 是否已登录工程师身份
  const isEngineerAuthed = computed(() => !!engineerToken.value)

  /** 设置客户H5令牌 */
  function setH5Token(token, info) {
    h5SessionVersion.value++
    h5Token.value = token
    customerInfo.value = info
    localStorage.setItem('h5_token', token)
    localStorage.setItem('customer_info', JSON.stringify(info))
  }

  /** 清除客户H5令牌 */
  function clearH5Token() {
    h5SessionVersion.value++
    h5Token.value = ''
    customerInfo.value = null
    localStorage.removeItem('h5_token')
    localStorage.removeItem('customer_info')
  }

  /** 设置工程师令牌 */
  function setEngineerToken(token, info) {
    engineerSessionVersion.value++
    engineerToken.value = token
    engineerInfo.value = info
    localStorage.setItem('engineer_token', token)
    localStorage.setItem('engineer_info', JSON.stringify(info))
  }

  /** 清除工程师令牌 */
  function clearEngineerToken() {
    engineerSessionVersion.value++
    engineerToken.value = ''
    engineerInfo.value = null
    localStorage.removeItem('engineer_token')
    localStorage.removeItem('engineer_info')
  }

  /** Update this identity's metadata without replacing its authenticated session. */
  function updateEngineerInfo(info) {
    if (!engineerToken.value) return
    engineerInfo.value = info
    localStorage.setItem('engineer_info', JSON.stringify(info))
  }

  return {
    h5Token,
    customerInfo,
    engineerToken,
    engineerInfo,
    h5SessionVersion,
    engineerSessionVersion,
    isCustomerAuthed,
    isEngineerAuthed,
    setH5Token,
    clearH5Token,
    setEngineerToken,
    updateEngineerInfo,
    clearEngineerToken
  }
})
