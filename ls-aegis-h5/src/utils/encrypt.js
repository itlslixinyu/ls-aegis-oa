/**
 * 国密传输加密（与 PC ls-aegis-web/src/utils/encrypt.ts 对齐）
 * sm-crypto ^0.5.5；SM2 C1C3C2（cipherMode=1）；密文补 04 以兼容 BouncyCastle；公钥 GET /auth/gm/public-key。
 */
import { sm2 } from 'sm-crypto'
import { getGmPublicKey } from '@/api/auth'

const SM2_CIPHER_MODE_C1C3C2 = 1

let sm2PublicKeyHex = ''
let gmEnabled = false

export async function loadGmPublicKey() {
  try {
    const res = await getGmPublicKey()
    const payload = res?.data ?? res
    gmEnabled = !!payload?.enable
    if (payload?.enable && payload.publicKeyHex) {
      sm2PublicKeyHex = payload.publicKeyHex
      return true
    }
    sm2PublicKeyHex = ''
  } catch {
    sm2PublicKeyHex = ''
    gmEnabled = false
  }
  return false
}

export function isGmEnabled() {
  return gmEnabled
}

export async function ensureGmPublicKey() {
  if (sm2PublicKeyHex) {
    return
  }
  const ok = await loadGmPublicKey()
  if (!ok || !sm2PublicKeyHex) {
    throw new Error('国密公钥加载失败，请刷新页面重试')
  }
}

export function encryptBySm2(txt) {
  if (!sm2PublicKeyHex) {
    console.error('国密 SM2 公钥未加载，无法加密传输')
    return ''
  }
  const cipher = sm2.doEncrypt(txt, sm2PublicKeyHex, SM2_CIPHER_MODE_C1C3C2)
  if (!cipher) {
    return ''
  }
  return cipher.startsWith('04') ? cipher : `04${cipher}`
}

/** 登录/改密：先拉公钥再 SM2 加密 */
export async function encryptTransport(txt) {
  await ensureGmPublicKey()
  const cipher = encryptBySm2(txt)
  if (!cipher) {
    throw new Error('国密加密失败')
  }
  return cipher
}
