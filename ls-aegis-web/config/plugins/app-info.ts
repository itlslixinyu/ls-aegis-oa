import boxen from 'boxen'
import picocolors from 'picocolors'
import type { Plugin } from 'vite'
import { version } from '../../package.json'

export default function appInfo(): Plugin {
  return {
    name: 'appInfo',
    apply: 'serve',
    async buildStart() {
      const { bold, green, cyan, bgGreen } = picocolors
      // eslint-disable-next-line no-console
      console.log(
        boxen(
          `${bold(green(bgGreen(`LS·Aegis OA v${version}`)))}\n${cyan('前端：Vue3 + Arco Design')}\n${cyan('后端：ls-aegis-server')}\n${cyan('OA统一业务平台')}`,
          {
            padding: 1,
            margin: 1,
            borderStyle: 'double',
            textAlignment: 'center',
          },
        ),
      )
    },
  }
}
