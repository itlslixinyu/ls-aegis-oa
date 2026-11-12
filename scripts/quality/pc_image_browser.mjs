// Real PC components with synthetic identity, simulated transport and no live API access.
import {spawn} from 'node:child_process'
import {deflateSync} from 'node:zlib'
import {writeFile,unlink,mkdir,readFile} from 'node:fs/promises'
import path from 'node:path'
import {pathToFileURL} from 'node:url'
import assert from 'node:assert/strict'
const out=path.resolve('.local/image-upload/pc-components'),port=19481
await mkdir(out,{recursive:true})
await unlink(path.join(out,'pc-browser.json')).catch(()=>{});
const fixture=path.resolve('ls-aegis-web/__pc_image_preview.html')
const crc32=data=>{let crc=0xffffffff;for(const value of data){crc^=value;for(let i=0;i<8;i++)crc=crc&1?(crc>>>1)^0xedb88320:crc>>>1}return (crc^0xffffffff)>>>0}
const chunk=(name,data)=>{const type=Buffer.from(name),size=Buffer.alloc(4),crc=Buffer.alloc(4);size.writeUInt32BE(data.length);crc.writeUInt32BE(crc32(Buffer.concat([type,data])));return Buffer.concat([size,type,data,crc])}
const header=Buffer.alloc(13);header.writeUInt32BE(64,0);header.writeUInt32BE(48,4);header[8]=8;header[9]=2;
const pixels=Buffer.alloc(48*(64*3+1));for(let y=0;y<48;y++)for(let x=0;x<64;x++){const at=y*(64*3+1)+1+x*3;pixels[at]=x*4;pixels[at+1]=y*5;pixels[at+2]=128}
const bytes=Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',deflateSync(pixels)),chunk('IEND',Buffer.alloc(0))]);const imageFile=path.join(out,'synthetic.png');await writeFile(imageFile,bytes)
const mockHttp=`window.transports=[];window.activeUploads=0;
const bytes=Uint8Array.from(atob('${bytes.toString('base64')}'),c=>c.charCodeAt(0));
async function request(method,url,data,options={}){
 if(options.responseType==='blob')return {data:new Blob([bytes],{type:'image/png'})};
 if(data instanceof FormData){const call={url,method,aborted:false};window.transports.push(call);window.activeUploads++;
 try{await new Promise((resolve,reject)=>{const timer=setTimeout(resolve,600);options.signal?.addEventListener('abort',()=>{clearTimeout(timer);call.aborted=true;reject(new Error('cancelled'))},{once:true})});options.onUploadProgress?.({loaded:1,total:1});return {data:{id:String(1000+window.transports.length),url:'/brand-logo.png',avatar:'/brand-logo.png',recordId:'3001',extractImageBase64:btoa(String.fromCharCode(...bytes))}}}finally{window.activeUploads--}}
 if(url.includes('/option'))return {data:['SITE_LOGO','SITE_FAVICON','SITE_BEIAN_GONGAN_ICON','SITE_TITLE','SITE_TITLE_SHORT','SITE_COMPANY','SITE_COMPANY_SHORT'].map(code=>({id:code,code,value:code.includes('LOGO')?'/brand-logo.png':'test'}))};
 if(url.includes('/user/info'))return {data:{avatar:'/brand-logo.png'}};
 if(url.includes('/next'))return {data:'FIXTURE-SEAL'};
 if(/common\\/file\\//.test(url))return {data:{originalName:'fixture.jpg',extension:'jpg'}};
 return {data:[]};
}
const http={get:(u,p,o)=>request('get',u,p,o||p||{}),post:(u,d,o)=>request('post',u,d,o),patch:(u,d,o)=>request('patch',u,d,o),put:(u,d,o)=>request('put',u,d,o),del:(u,d,o)=>request('delete',u,d,o)};export default http;`
const mockStores=`import {reactive} from 'vue';import {createPinia} from 'pinia';
const user=reactive({roles:['admin'],permissions:['*:*:*'],userInfo:{nickname:'合成测试',roleNames:[],roles:[],permissions:['*:*:*'],avatar:'',gender:0},nickname:'合成测试',getInfo:async()=>{}});
const app=reactive({tableSize:'large',tableBordered:true,settingConfig:{tableSize:'large',tableBordered:true},initSiteConfig:async()=>{}});const dict=new Map();
export const useUserStore=()=>user;export const useAppStore=()=>app;export const useDictStore=()=>({getDict:k=>dict.get(k),setDict:(k,v)=>dict.set(k,v)});export const useTenantStore=()=>({tenantId:''});export const useTabsStore=()=>({tabs:[]});export const useRouteStore=()=>({routes:[],menus:[]});export default createPinia();`
const {createServer}=await import(pathToFileURL(path.resolve('ls-aegis-web/node_modules/vite/dist/node/index.js')).href)
const plugin={name:'pc-image-fixture',enforce:'pre',configureServer(server){server.middlewares.use((req,res,next)=>{if(/^\/api(?:\/|$)/.test(req.url||'')){res.writeHead(503,{'Content-Type':'application/json'});res.end(JSON.stringify({success:false,msg:'Component fixture has no live API access'}));return}next()})},resolveId(id){id=id.replaceAll('\\','/');if(/\/src\/utils\/http(?:\.ts)?$/.test(id))return '\0fixture-http';if(/\/src\/stores(?:\/index\.ts)?$/.test(id))return '\0fixture-stores'},load(id){if(id==='\0fixture-http')return mockHttp;if(id==='\0fixture-stores')return mockStores}}
await writeFile(fixture,`<html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="app"></div><script type="module">
import {createApp,h,ref,nextTick} from 'vue';import {createRouter,createMemoryHistory} from 'vue-router';import Arco from '@arco-design/web-vue';import '@arco-design/web-vue/dist/arco.css';
import Field from '/src/components/PlatformFileField/index.vue';import Avatar from '/src/views/user/profile/BasicInfo.vue';import Seal from '/src/views/sign/seal/AddModal.vue';import Extract from '/src/views/sign/seal/extract.vue';import Site from '/src/views/system/config/site/index.vue';
import Eoms from '/src/views/eoms/contract/EditModal.vue';import Sale from '/src/views/psi/contract-sale/EditModal.vue';import Purchase from '/src/views/psi/contract-purchase/EditModal.vue';import Repair from '/src/views/eoms/repair/ApplyAddModal.vue';import FileMain from '/src/views/system/file/main/FileMain/index.vue';
const modes={field:Field,avatar:Avatar,seal:Seal,extract:Extract,site:Site,eoms:Eoms,sale:Sale,purchase:Purchase,repair:Repair,fileMain:FileMain};const mode=ref('field'),instance=ref(),ids=ref();const router=createRouter({history:createMemoryHistory(),routes:[{path:'/',component:{render:()=>null}}]});const app=createApp({render:()=>h(modes[mode.value],{key:mode.value,ref:instance,modelValue:ids.value,'onUpdate:modelValue':v=>ids.value=v,multiple:true})});app.use(router);app.use(Arco);app.directive('permission',()=>{});await router.push('/');await router.isReady();app.mount('#app');
window.state=()=>instance.value.$.setupState;window.model=()=>ids.value;window.selectCase=async name=>{mode.value=name;await nextTick();await nextTick();if(window.state().onAdd)await window.state().onAdd();window.caseReady=name};window.ready=true;
</script></body></html>`,{flag:'wx'})
const previewServer=await createServer({root:path.resolve('ls-aegis-web'),configFile:path.resolve('ls-aegis-web/vite.config.ts'),plugins:[plugin],mode:'development',server:{host:'127.0.0.1',port:5191,strictPort:true,open:false}});await previewServer.listen()
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
  const errors=[];ws.addEventListener('message',({data})=>{const m=JSON.parse(data);if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.text)})
  const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value}
  const wait=async expression=>{for(let i=0;i<160;i++){if(await evaluate(expression))return;await sleep(200)}throw Error('Timed out '+expression)}
  await send('Runtime.enable');await send('Page.navigate',{url:'http://127.0.0.1:5191/__pc_image_preview.html'});await wait('window.ready===true');await sleep(2000)
  const selectFile=async(index=-1)=>{const root=await send('DOM.getDocument');const inputs=await send('DOM.querySelectorAll',{nodeId:root.root.nodeId,selector:'input[type=file]'});assert.ok(inputs.nodeIds.length,'file input not mounted');await send('DOM.setFileInputFiles',{nodeId:inputs.nodeIds.at(index),files:[imageFile]})}
  const results=[]
  for(const mode of ['field','eoms','sale','purchase','repair','fileMain','avatar','seal','extract','site']){
   await evaluate(`window.selectCase('${mode}')`);await wait(`window.caseReady==='${mode}'`)
   if(mode==='fileMain'){await evaluate("document.querySelector('.file-main__search .arco-btn-primary').click()");await sleep(350)}
   if(mode==='site')await evaluate('window.state().editing=true')
   await sleep(350);const before=await evaluate('window.transports.length');await selectFile()
   if(['field','eoms','sale','purchase','repair','fileMain','site'].includes(mode)){
    await wait('window.activeUploads>0');if(['eoms','sale','purchase','repair'].includes(mode)){assert.equal(await evaluate('(window.state().attachmentRef||window.state().photosRef).isUploading()'),true);assert.equal(await evaluate('window.state().save()'),false)}
    await wait('window.activeUploads===0');assert.equal(await evaluate('window.transports.length'),before+1)
    if(mode==='field')assert.ok(await evaluate('window.model()'));else if(mode==='site')assert.ok(await evaluate("window.state().form.SITE_BEIAN_GONGAN_ICON"));else if(mode==='repair')assert.ok(await evaluate('window.state().form.photos'));else if(mode!=='fileMain')assert.ok(await evaluate('window.state().form.fileId'));
    if(mode==='site'){for(const index of [0,1]){await selectFile(index);await wait('window.activeUploads>0');await wait('window.activeUploads===0')}assert.equal(await evaluate('window.transports.length'),before+3)}
   }else if(mode==='avatar'){
    await wait('window.state().visible===true');await sleep(500);await evaluate('window.state().handleUpload()');await wait('window.transports.length>'+before+' && window.activeUploads===0');
   }else if(mode==='seal'){
    await wait('window.state().uploadFile?.size>0');assert.equal(await evaluate('document.body.innerText.includes("最大 50MB")'),false);await evaluate("Object.assign(window.state().form,{sealName:'ITLS-TEST-SEAL',sealCategory:'公章'});window.state().save()");assert.equal(await evaluate('window.transports.length'),before+1)
   }else {
    await wait('window.state().selectedFile?.size>0');await evaluate('window.state().doExtract()');assert.ok(await evaluate('window.state().extractPreview'));await evaluate("window.state().customFileName='ITLS-TEST-EXTRACT';window.state().doSave()");assert.equal(await evaluate('window.state().currentExtractRecordId'),'3001')
   }
   results.push({case:mode,pass:true,scope:'real Vue/Arco component, simulated server transport'});const screenshot=await send('Page.captureScreenshot',{format:'png'});await writeFile(path.join(out,'pc-'+mode+'.png'),Buffer.from(screenshot.data,'base64'))
  }
  await writeFile(path.join(out,'pc-browser.json'),JSON.stringify({results,errors},null,2));console.log('PASS '+results.length+' PC image entry component workflows')
}finally{if(ws?.readyState===WebSocket.OPEN){try{await send('Browser.close')}catch{}ws.close()}if(chrome.exitCode===null)chrome.kill();await previewServer.close();await unlink(fixture)}
