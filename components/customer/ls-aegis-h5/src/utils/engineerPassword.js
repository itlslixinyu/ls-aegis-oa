export function isEngineerPasswordExpired(body) {
  const message = body?.msg || body?.message
  return typeof message === 'string' && message.includes('密码已过期')
}

export function engineerLandingPath(info) {
  return info?.pwdExpired ? '/engineer/password' : '/engineer/workbench'
}
