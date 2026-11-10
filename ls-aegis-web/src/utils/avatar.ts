import Unknown from '../assets/images/avatar/unknown.png'
import Male from '../assets/images/avatar/male.png'
import Female from '../assets/images/avatar/female.png'

/** 将不可达的旧存储域名改写为经 Nginx /api 反代的同源路径，避免跨主机/跨端口加载失败 */
function normalizeAvatarUrl(url: string) {
  return url
    .replace(/^https?:\/\/[^/]+\/api\/file\//i, '/api/file/')
    .replace(/^https?:\/\/(?:localhost|127\.0\.0\.1):8000\/file\//i, '/api/file/')
    .replace(/^https?:\/\/(?:localhost|127\.0\.0\.1):18000\/file\//i, '/api/file/')
}

export default function getAvatar(avatar: string | undefined, gender: number | undefined) {
  if (avatar) {
    return normalizeAvatarUrl(avatar)
  }
  if (gender === 1) {
    return Male
  }
  if (gender === 2) {
    return Female
  }
  return Unknown
}
