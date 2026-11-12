// Real TaskDetail/Vant component, simulated API and device capabilities; no platform writes.
import { spawn } from 'node:child_process'
import { mkdir, writeFile, unlink } from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import assert from 'node:assert/strict'

const out = path.resolve('.local/maintain/overnight-delivery-20261111/attendance-browser')
const fixture = path.resolve('ls-aegis-h5/__attendance_preview.html')
const port = 19487
await mkdir(out, { recursive: true })
const mocks = `
window.calls=[];window.geoDenied=false;window.geoDelay=5;window.address='河北省廊坊市合成签到地点';
window.task={id:'123',status:'ACCEPTED',address:'原工单服务地点'};
window.taskRecords=[];
Object.defineProperty(navigator,'geolocation',{value:{getCurrentPosition(ok,fail){
setTimeout(()=>window.geoDenied?fail({code:1}):ok({coords:{longitude:116.4,latitude:39.9}}),window.geoDelay)}}});
window.__LS_AEGIS_NATIVE_GEOCODER__=true;
window.prompt=(message,json)=>{const request=JSON.parse(json);setTimeout(()=>window.dispatchEvent(new CustomEvent('ls-aegis-address-result',{detail:{id:request.id,address:window.address}})),5);return 'accepted'};
export const getTaskMaterials=async()=>({data:[]});
export const getMaterialOptions=async()=>({data:{goods:[],warehouses:[]}});
export const getTaskDetail=async()=>({data:{order:{...window.task},records:[...window.taskRecords]}});
export const acceptTask=async()=>({data:true});
export const startTask=async(...args)=>{window.calls.push({action:'start',args});window.task.status='PROCESSING';window.task.signInAddress=args[4];return {data:true}};
export const finishTask=async(...args)=>{window.calls.push({action:'finish',args});window.task.status='WAIT_SETTLEMENT';window.task.signOutAddress=args[4];return {data:true}};
`
const { createServer } = await import(pathToFileURL(path.resolve('ls-aegis-h5/node_modules/vite/dist/node/index.js')).href)
const plugin = {
  name: 'attendance-contract-fixture', enforce: 'pre',
  resolveId(id) { if (/api[\/]engineer(?:.js)?$/.test(id)) return '\0attendance-api' },
  load(id) { if (id === '\0attendance-api') return mocks }
}
await writeFile(fixture, `<html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="app"></div><script type="module">
import {createApp,h} from 'vue';import {createPinia} from 'pinia';import {createRouter,createMemoryHistory,RouterView} from 'vue-router';import Vant from 'vant';import 'vant/lib/index.css';
import Task from '/src/views/engineer/TaskDetail.vue';
const router=createRouter({history:createMemoryHistory(),routes:[{path:'/engineer/task/:id',component:Task}]});
const app=createApp({render:()=>h(RouterView)});app.use(createPinia());app.use(router);app.use(Vant);await router.push('/engineer/task/123');await router.isReady();app.mount('#app');
window.state=()=>router.currentRoute.value.matched[0].instances.default.$.setupState;window.fixtureApp=app;window.ready=true;
</script></body></html>`, { flag: 'wx' })
let preview, chrome, ws
const pending = new Map()
let nextId = 0
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))
function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++nextId
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(`Timeout ${method}`)) }, 15000)
    pending.set(id, { resolve, reject, timer }); ws.send(JSON.stringify({ id, method, params }))
  })
}
try {
  preview = await createServer({ root: path.resolve('ls-aegis-h5'), configFile: path.resolve('ls-aegis-h5/vite.config.js'), plugins: [plugin], mode: 'engineer', server: { host: '127.0.0.1', port: 5189, strictPort: true, open: false } })
  await preview.listen()
  chrome = spawn(process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', ['--headless=new', '--remote-debugging-address=127.0.0.1', `--remote-debugging-port=${port}`, `--user-data-dir=${path.join(out, 'chrome-profile')}`, '--no-first-run', '--no-default-browser-check', '--disable-background-networking', '--disable-extensions', 'about:blank'], { windowsHide: true, stdio: 'ignore' })
  let target
  for (let i = 0; i < 40; i++) {
    try { target = await (await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, { method: 'PUT' })).json(); break } catch { await sleep(250) }
  }
  assert.ok(target?.webSocketDebuggerUrl, 'Isolated headless browser failed to start')
  ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject })
  ws.onmessage = ({ data }) => {
    const message = JSON.parse(data); const request = pending.get(message.id)
    if (!request) return
    clearTimeout(request.timer); pending.delete(message.id)
    if (message.error) request.reject(new Error(JSON.stringify(message.error))); else request.resolve(message.result)
  }
  const evaluate = async expression => {
    const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
    if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails))
    return result.result.value
  }
  const wait = async expression => { for (let i = 0; i < 80; i++) { if (await evaluate(expression)) return; await sleep(100) } throw new Error(`Condition timed out: ${expression}`) }
  await send('Page.enable')
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true })
  await send('Page.navigate', { url: 'http://127.0.0.1:5189/__attendance_preview.html' })
  await wait('window.ready===true && window.state().order.id==="123"')
  await evaluate('window.state().onStart()')
  await wait('document.querySelector(".van-dialog textarea")!==null')
  await evaluate("window.state().actionRemark='合成签到备注'")
  assert.equal(await evaluate("window.state().beforeRemarkClose('confirm')"), true)
  assert.deepEqual(await evaluate('window.calls[0]'), { action: 'start', args: ['123', '合成签到备注', '116.400000', '39.900000', '河北省廊坊市合成签到地点'] })
  assert.equal(await evaluate('window.state().order.address'), '原工单服务地点')
  assert.equal(await evaluate('window.state().order.signInAddress'), '河北省廊坊市合成签到地点')
  await evaluate('window.state().showRemarkDialog=false;window.state().onFinish()')
  assert.equal(await evaluate('window.state().currentAction'), 'start', 'No repair record must block completion')
  await evaluate("window.state().records=[{id:'456'}];window.taskRecords=[{id:'456'}];window.address='河北省廊坊市合成签退地点';window.state().onFinish();window.state().actionRemark='合成签退备注'")
  assert.equal(await evaluate("window.state().beforeRemarkClose('confirm')"), true)
  assert.deepEqual(await evaluate('window.calls[1]'), { action: 'finish', args: ['123', '合成签退备注', '116.400000', '39.900000', '河北省廊坊市合成签退地点'] })
  assert.equal(await evaluate('window.state().order.signInAddress'), '河北省廊坊市合成签到地点')
  assert.equal(await evaluate('window.state().order.signOutAddress'), '河北省廊坊市合成签退地点')
  await evaluate('window.geoDenied=true;window.state().onStart()')
  assert.equal(await evaluate("window.state().beforeRemarkClose('confirm')"), false)
  assert.equal(await evaluate('window.calls.length'), 2, 'Denied GPS must prevent API write')
  assert.equal(await evaluate('window.state().actionLoading'), false)
  await evaluate('window.geoDenied=false;window.__LS_AEGIS_NATIVE_GEOCODER__=false;window.state().onStart()')
  assert.equal(await evaluate("window.state().beforeRemarkClose('confirm')"), true)
  assert.equal(await evaluate('window.calls[2].args[4]'), '', 'No native resolver must retain coordinates only')
  await evaluate("window.geoDelay=150;window.state().onStart();window.pendingAction=window.state().beforeRemarkClose('confirm');true")
  assert.equal(await evaluate("window.state().beforeRemarkClose('confirm')"), false, 'Pending location must block duplicate confirmation')
  await evaluate('window.fixtureApp.unmount();true')
  assert.equal(await evaluate('window.pendingAction'), false, 'Leaving during location must prevent delayed business writes')
  assert.equal(await evaluate('window.calls.length'), 3)
  const results = { scope: 'Real Vue/Vant component; simulated API/GPS/native resolver; no in-use data writes', cases: ['signin-visible-remark-and-native-address', 'independent-signout-address', 'missing-record-blocks-finish', 'denied-GPS-blocks-write', 'no-native-provider-coordinate-fallback', 'pending-confirmation-and-leave-prevent-write'], status: 'PASS' }
  await writeFile(path.join(out, 'result.json'), JSON.stringify(results, null, 2))
  console.log('PASS 6 attendance component cases; no platform writes')
} finally {
  if (ws?.readyState === WebSocket.OPEN) { try { await send('Browser.close') } catch {} ws.close() }
  if (chrome?.exitCode === null) chrome.kill()
  await preview?.close(); await unlink(fixture)
}
