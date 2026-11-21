import components from 'unplugin-vue-components/vite'
import { ArcoResolver } from 'unplugin-vue-components/resolvers'

export default function createComponents() {
  return components({
    include: [/\.vue($|\?)/, /\.[jt]sx$/],
    // 继续使用平台既有主题样式，避免按需CSS覆盖定制样式。
    resolvers: [ArcoResolver({ importStyle: false })],
    // 指定组件位置，默认是 src/components 自动导入自定义组件
    dirs: ['src/components'],
    extensions: ['vue', 'tsx'],
    // 配置文件生成位置
    dts: './src/types/components.d.ts',
  })
}
