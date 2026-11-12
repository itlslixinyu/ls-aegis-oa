// Actual Vue/Vant component regression with simulated transport; no authenticated platform writes.
import { spawn } from 'node:child_process'
import { writeFile, unlink, mkdir } from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import assert from 'node:assert/strict'
const out=path.resolve('.local/image-upload/browser-components'); const port=19479
const fixture=path.resolve('ls-aegis-h5/__photo_workflow_preview.html')
await mkdir(out,{recursive:true});await writeFile(path.join(out,'invalid.jpg'),'not an image');
const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWXkAAAAASUVORK5CYII=','base64');for(const name of ['first.png','second.png'])await writeFile(path.join(out,name),png)
const mocks=`
window.calls=[]; window.business=[]; window.geoCalls=0; window.activeUploads=0; window.maxUploads=0; window.failOnce=true;
Object.defineProperty(navigator,'geolocation',{value:{getCurrentPosition(ok,fail){window.geoCalls++;setTimeout(()=>fail({code:1}),10)}}});
export async function upload(file,context,options={}) {
 const call={name:file.name,context,aborted:false,reporter:options.executor,remark:options.remark};window.calls.push(call); window.activeUploads++;window.maxUploads=Math.max(window.maxUploads,window.activeUploads);
 try { await new Promise((resolve,reject)=>{const t=setTimeout(resolve,350);options.signal?.addEventListener('abort',()=>{clearTimeout(t);call.aborted=true;reject(new Error('cancelled'))},{once:true})});
 options.onUploadProgress?.({loaded:1,total:1});if(window.failOnce){window.failOnce=false;throw new Error('模拟网络中断')};return {data:{fileId:String(10000+window.calls.indexOf(call))}};
 }finally{window.activeUploads--}
}
export const uploadWithWatermark=(file,options)=>upload(file,'customer',options);
export const uploadRepairPhoto=(file,orderId,photoType,lon,lat,options)=>upload(file,photoType,options);
export const getTaskMaterials=async()=>({data:[]});
export const getMaterialOptions=async()=>({data:{goods:[],warehouses:[]}});
export const getTaskDetail=async()=>({data:{order:{status:'PROCESSING'}}});
export const submitApply=async(data)=>{window.business.push(JSON.parse(JSON.stringify(data)));return {data:true}};
export const submitRecord=submitApply;
`
const {createServer}=await import(pathToFileURL(path.resolve('ls-aegis-h5/node_modules/vite/dist/node/index.js')).href)
const plugin={name:'photo-contract-fixture',enforce:'pre',resolveId(id,importer){if((id==='./request'||id==='./request.js')&&importer?.replaceAll('\\','/').endsWith('/src/utils/eomsFile.js'))return '\0photo-preview';if(/api[\/]customer(?:.js)?$|api[\/]engineer(?:.js)?$/.test(id))return '\0photo-mocks'; if(/utils[\/]request(?:.js)?$/.test(id))return '\0photo-preview'},load(id){if(id==='\0photo-mocks')return mocks;if(id==='\0photo-preview')return `export default async()=>({data:new Blob([Uint8Array.from(atob('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWXkAAAAASUVORK5CYII='),c=>c.charCodeAt(0))],{type:'image/png'})})`}}
await writeFile(fixture,`<html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head><body><div id="app"></div><script type="module">
import {createApp,h} from 'vue';import {createPinia} from 'pinia';import {createRouter,createMemoryHistory,RouterView} from 'vue-router';import Vant from 'vant';import 'vant/lib/index.css';
import Apply from '/src/views/customer/Apply.vue';import Record from '/src/views/engineer/Record.vue';
const engineer=location.search.includes('engineer');const router=createRouter({history:createMemoryHistory(),routes:[{path:'/form/:orderId',component:engineer?Record:Apply},{path:'/:pathMatch(.*)*',component:{render:()=>h('p','done')}}]});
const app=createApp({render:()=>h(RouterView)});app.use(createPinia());app.use(router);app.use(Vant);await router.push('/form/123');await router.isReady();app.mount('#app');window.fixtureApp=app;window.state=()=>router.currentRoute.value.matched[0].instances.default.$.setupState;window.ready=true;
</script></body></html>`,{flag:'wx'})
const previewServer=await createServer({root:path.resolve('ls-aegis-h5'),configFile:path.resolve('ls-aegis-h5/vite.config.js'),plugins:[plugin],mode:'customer',server:{host:'127.0.0.1',port:5188,strictPort:true,open:false}})
await previewServer.listen()
if(process.argv.includes('--native-fixture')) {
 console.log('Native component fixture ready on 127.0.0.1:5188; all API and geolocation mocked')
 await new Promise(resolve=>{process.once('SIGINT',resolve);process.once('SIGTERM',resolve)})
 await previewServer.close();await unlink(fixture);process.exit(0)
}
const chrome = spawn(process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', '--remote-debugging-address=127.0.0.1', `--remote-debugging-port=${port}`,
  `--user-data-dir=${path.join(out, 'chrome-profile')}`, '--no-first-run', '--no-default-browser-check',
  '--disable-background-networking', '--disable-extensions', 'about:blank',
], { windowsHide: true, stdio: 'ignore' })
let ws
const pending = new Map()
let nextId = 0
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++nextId
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(`Timeout ${method}`)) }, 15000)
    pending.set(id, { resolve, reject, timer })
    ws.send(JSON.stringify({ id, method, params }))
  })
}
try {
  let target
  for (let attempt = 0; attempt < 40; attempt++) {
    try {
      const res = await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, { method: 'PUT' })
      target = await res.json()
      break
    } catch { await sleep(250) }
  }
  assert.ok(target?.webSocketDebuggerUrl, 'Isolated headless browser failed to start')
  ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject })
  ws.onmessage = ({ data }) => {
    const message = JSON.parse(data)
    const request = pending.get(message.id)
    if (!request) return
    clearTimeout(request.timer); pending.delete(message.id)
    if (message.error) request.reject(new Error(JSON.stringify(message.error)))
    else request.resolve(message.result)
  }
  await send('Page.enable')
  const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw new Error(JSON.stringify(r.exceptionDetails));return r.result.value}
  const wait=async expression=>{for(let i=0;i<80;i++){if(await evaluate(expression))return;await sleep(100)}throw new Error('Condition timed out: '+expression)}
  const results=[]
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true})
  for(const role of ['customer','engineer']){
    await send('Page.navigate',{url:'http://127.0.0.1:5188/__photo_workflow_preview.html?'+role});await wait('window.ready===true');
    await sleep(1800);await wait('document.querySelectorAll("input[type=file]").length>0');
    const select=async index=>{const root=await send('DOM.getDocument');const inputs=await send('DOM.querySelectorAll',{nodeId:root.root.nodeId,selector:'input[type=file]'});assert.ok(inputs.nodeIds[index]);await send('DOM.setFileInputFiles',{nodeId:inputs.nodeIds[index],files:[path.join(out,'first.png'),path.join(out,'second.png')]})}
    if(role==='customer')await evaluate("window.state().form.contactName='格式检查人'");
    {const root=await send('DOM.getDocument');const input=await send('DOM.querySelector',{nodeId:root.root.nodeId,selector:'input[type=file]'});await send('DOM.setFileInputFiles',{nodeId:input.nodeId,files:[path.join(out,'invalid.jpg')]});await sleep(200);assert.equal(await evaluate('window.calls.length'),0);assert.equal(await evaluate('document.querySelectorAll(".van-uploader__preview").length'),0)}
    assert.equal(await evaluate("document.querySelector('input[type=file]').closest('[data-photo-remaining]').dataset.photoRemaining"),role==='customer'?'9':'6');
    if(role==='customer'){await evaluate("window.state().form.contactName=''");await select(0);await sleep(100);assert.equal(await evaluate('window.calls.length'),0,'Reporter is required before choosing photos');await evaluate("window.state().form.contactName='合成报修人'")}
    await evaluate("window.state().form.remark='合成备注'");
    await select(0);
    await wait('window.calls.length===2');await evaluate('window.state().onSubmit()');assert.equal(await evaluate('window.business.length'),0,'pending photos block submit');
    await wait('window.calls.length===2 && window.activeUploads===0');
    assert.equal(await evaluate('window.state().'+(role==='customer'?'fileList':'processingFileList')+'.length'),2);
    await evaluate('window.state().onSubmit()');assert.equal(await evaluate('window.business.length'),0,'failed photo must block business submit');
    await evaluate('document.querySelector(".photo-upload-feedback button").click()');
    await wait('window.calls.length===3 && window.activeUploads===0');
    if(role==='engineer'){await select(1);await wait('window.calls.length===5 && window.activeUploads===0')}
    await evaluate('window.state().onSubmit()');assert.equal(await evaluate('window.business.length'),1);await evaluate('window.state().onSubmit()');assert.equal(await evaluate('window.business.length'),1,'completed submission cannot repeat while navigation is pending');
    const data=await evaluate('({calls:window.calls,body:window.business[0],geoCalls:window.geoCalls,max:window.maxUploads})');
    assert.equal(data.geoCalls,1);assert.ok(data.max<=2);assert.ok(data.calls.every(call=>call.remark==='合成备注'));if(role==='customer')assert.ok(data.calls.every(call=>call.reporter==='合成报修人'));
    if(role==='customer')assert.equal(data.body.photos.split(',').length,2);else {assert.equal(data.body.processingPhotos.split(',').length,2);assert.equal(data.body.completedPhotos.split(',').length,2);assert.deepEqual(data.calls.map(c=>c.context),['processing','processing','processing','completed','completed'])}
    results.push({role,pass:true,uploads:data.calls.length,locationDenials:data.geoCalls,maxConcurrent:data.max,photoGroups:role==='customer'?1:2});
  }
  await send('Page.navigate',{url:'http://127.0.0.1:5188/__photo_workflow_preview.html?customer'});await wait('window.ready===true');await sleep(500);await evaluate("window.state().form.contactName='离页测试人'");
  {const root=await send('DOM.getDocument');const input=await send('DOM.querySelector',{nodeId:root.root.nodeId,selector:'input[type=file]'});await send('DOM.setFileInputFiles',{nodeId:input.nodeId,files:[path.join(out,'first.png')]});await wait('window.calls.length===1');await evaluate('window.fixtureApp.unmount()');await wait('window.activeUploads===0');assert.equal(await evaluate('window.calls[0].aborted'),true);assert.equal(await evaluate('window.business.length'),0)}
  results.push({case:'leave-during-upload',pass:true,transportAborted:true});
  await writeFile(path.join(out,'browser-workflow.json'),JSON.stringify({scope:'real Vue and Vant components; mocked API and denied geolocation; no production writes',results},null,2));console.log('PASS customer and engineer real form selection, failed-submit gate, retry, categories, IDs, concurrency and location denial reuse')
}finally{
 if(ws?.readyState===WebSocket.OPEN){try{await send('Browser.close')}catch{}ws.close()}
 if(chrome.exitCode===null)chrome.kill();await previewServer.close();await unlink(fixture)
}
