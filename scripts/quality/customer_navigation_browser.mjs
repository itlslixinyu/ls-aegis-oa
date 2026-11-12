// Actual customer router and Vue/Vant pages, isolated browser and synthetic transport only.
import { spawn } from 'node:child_process'
import { mkdir,writeFile,unlink } from 'node:fs/promises'
import { pathToFileURL } from 'node:url'
import path from 'node:path'
import assert from 'node:assert/strict'
const out=path.resolve('.local/maintain/customer-navigation-20261112/browser'),fixture=path.resolve('ls-aegis-h5/__customer_navigation_preview.html'),port=19491
await mkdir(out,{recursive:true})
const mocks=`window.listCalls=[];window.applyCalls=[];window.listRows=[];
export const getRepairList=async params=>{window.listCalls.push(params);return {data:{list:[...window.listRows],total:window.listRows.length}}};
export const getRepairDetail=async()=>({data:{apply:{id:'123',repairNo:'合成报修123',status:'ORDER',reportAddress:'合成报修地址',contactName:'合成联系人',contactPhone:'13800000000'},order:{id:'456',status:'PROCESSING'},records:[]}});export const confirmRepair=async()=>({data:{confirmed:true}});
export const submitApply=async data=>{window.applyCalls.push({...data});return {data:true}};
export const uploadWithWatermark=async()=>({data:{fileId:'synthetic-photo'}});
export const logout=async()=>({data:true});
export const activateAccount=async()=>({data:true});export const passwordLogin=async()=>({data:true});export const getImageCaptcha=async()=>({data:{isEnabled:false}});`
const {createServer}=await import(pathToFileURL(path.resolve('ls-aegis-h5/node_modules/vite/dist/node/index.js')).href)
const plugin={name:'customer-navigation-fixture',enforce:'pre',resolveId(id){if(/api[\/]customer(?:.js)?$/.test(id))return '\0customer-navigation-api'},load(id){if(id==='\0customer-navigation-api')return mocks}}
await writeFile(fixture,`<html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="app"></div><script type="module">
import {createApp,h} from 'vue';import {createPinia} from 'pinia';import {RouterView} from 'vue-router';import Vant from 'vant';import 'vant/lib/index.css';import router from '/src/router/index.js';import {useUserStore} from '/src/store/user.js';
const app=createApp({render:()=>h(RouterView)}),pinia=createPinia();app.use(pinia);const user=useUserStore(pinia);user.setH5Token('synthetic-customer-session',{customerName:'合成客户'});app.use(router);app.use(Vant);await router.push('/customer/repairs/all');await router.isReady();app.mount('#app');window.user=user;window.fixtureRouter=router;window.state=()=>router.currentRoute.value.matched.at(-1).instances.default.$.setupState;window.ready=true;
</script></body></html>`,{flag:'wx'})
let preview,chrome,ws,id=0;const pending=new Map(),sleep=ms=>new Promise(r=>setTimeout(r,ms))
function send(method,params={}){return new Promise((resolve,reject)=>{const n=++id,timer=setTimeout(()=>{pending.delete(n);reject(Error('Timeout '+method))},15000);pending.set(n,{resolve,reject,timer});ws.send(JSON.stringify({id:n,method,params}))})}
try{
 preview=await createServer({root:path.resolve('ls-aegis-h5'),configFile:path.resolve('ls-aegis-h5/vite.config.js'),plugins:[plugin],mode:'customer',define:{__H5_ROLE__:JSON.stringify('customer')},resolve:{alias:{'#h5-routes':path.resolve('ls-aegis-h5/src/router/customer-routes.js')}},optimizeDeps:{noDiscovery:true,include:['vue','vue-router','pinia','vant','sm-crypto']},server:{host:'127.0.0.1',port:5194,strictPort:true,open:false,hmr:false,watch:{ignored:['**/dist/**','**/dist-engineer/**']}}});await preview.listen()
 chrome=spawn(process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--remote-debugging-address=127.0.0.1',`--remote-debugging-port=${port}`,`--user-data-dir=${path.join(out,'chrome-profile')}`,'--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-extensions','about:blank'],{windowsHide:true,stdio:'ignore'})
 let target;for(let i=0;i<40;i++){try{target=await(await fetch(`http://127.0.0.1:${port}/json/new?about:blank`,{method:'PUT',signal:AbortSignal.timeout(1000)})).json();break}catch{await sleep(250)}}assert.ok(target?.webSocketDebuggerUrl)
 ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});ws.onmessage=({data})=>{const m=JSON.parse(data),p=pending.get(m.id);if(!p)return;pending.delete(m.id);clearTimeout(p.timer);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result)}
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value}
 const wait=async expression=>{for(let i=0;i<100;i++){if(await evaluate(expression))return;await sleep(100)}throw Error('Timed out '+expression)}
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true})
 await send('Page.navigate',{url:'http://127.0.0.1:5194/__customer_navigation_preview.html'});await wait('window.ready===true');await sleep(1500)
 const cases=[];
 await wait("location.pathname==='/customer/repairs/all' && document.querySelector('.all-head')");
 for(const status of ['', 'WAIT','CONFIRM','ORDER','CANCEL','unknown']) {
   await evaluate(`window.fixtureRouter.push({path:'/customer/repairs',query:${JSON.stringify(status?{status,source:'legacy'}:{source:'legacy'})}})`);
   const expected=['WAIT','CONFIRM','ORDER','CANCEL'].includes(status)?`/customer/repairs/status/${status}`:'/customer/repairs/all';
   await wait(`location.pathname===${JSON.stringify(expected)}`);
   assert.equal(await evaluate('window.fixtureRouter.currentRoute.value.query.source'),'legacy');
   assert.equal(await evaluate('!!document.querySelector(".page-hero")'),false);
 }
 cases.push('old-bookmarks-all-and-statuses-open-current-pages');
 await evaluate("window.fixtureRouter.push('/customer/apply')");await wait("!!document.querySelector('.repair-photo-trigger')");
 await evaluate("window.state().fileList.push({fileId:'synthetic-photo',status:'done',url:'data:image/png;base64,iVBORw0KGgo='});Object.assign(window.state().form,{contactName:'合成联系人',contactPhone:'13800000000',faultType:'测试',faultDesc:'合成报修说明'});window.state().onSubmit()");
 await wait("window.applyCalls.length===1 && location.pathname==='/customer/repairs/all'");
 assert.equal(await evaluate('!!document.querySelector(".all-head")'),true);
 cases.push('successful-apply-returns-current-list');
 await evaluate("window.fixtureRouter.push('/customer/repairs/status/WAIT')");await wait("!!document.querySelector('.status-head')");
 await evaluate("document.body.style.minHeight='3000px';window.scrollTo(0,900)");assert.ok(await evaluate('window.scrollY')>500);
 await evaluate("window.fixtureRouter.push('/customer/repairs/all')");await wait('window.scrollY===0');
 cases.push('new-page-starts-at-top');
 await evaluate("document.body.style.minHeight='3000px';window.scrollTo(0,700)");await sleep(100);
 await evaluate("window.fixtureRouter.push('/customer/repairs/status/ORDER')");await wait('window.scrollY===0');
 await evaluate('window.fixtureRouter.back()');await wait("location.pathname==='/customer/repairs/all' && window.scrollY>=690");
 cases.push('back-restores-list-scroll');
 await evaluate("window.fixtureRouter.push('/customer/repairs/status/WAIT')");await wait("location.pathname==='/customer/repairs/status/WAIT'");
 await evaluate("window.user.clearH5Token();window.fixtureRouter.push('/customer/repairs')");
 await wait("location.pathname==='/customer/auth'");cases.push('legacy-entry-preserves-customer-auth-guard');
 await evaluate("window.user.setH5Token('synthetic-customer-session',{customerName:'合成客户'});document.body.style.minHeight='';window.listRows=[{id:'123',repairNo:'合成报修123',status:'ORDER',faultDesc:'合成故障说明'}]");
 for(let round=0;round<20;round++){
   const entry=round%2===0?'/customer/repairs':'/customer/repairs?status=ORDER';
   await evaluate(`window.fixtureRouter.push(${JSON.stringify(entry)})`);
   const expected=round%2===0?'/customer/repairs/all':'/customer/repairs/status/ORDER';
   await wait(`location.pathname===${JSON.stringify(expected)} && !!document.querySelector('.item-card')`);
   assert.equal(await evaluate('!!document.querySelector(".page-hero")'),false);
   await evaluate("document.querySelector('.item-card').click()");
   await wait("location.pathname==='/customer/repair/123' && document.body.innerText.includes('合成报修地址')");
   await evaluate("document.querySelector('.van-nav-bar__left').click()");await wait(`location.pathname===${JSON.stringify(expected)} && !!document.querySelector('.item-card')`);
 }
 cases.push('twenty-rounds-legacy-entry-to-repair-detail-and-back-never-mount-old-list');
 await writeFile(path.join(out,'result.json'),JSON.stringify({status:'PASS',scope:'Actual Vue/Vant customer routes and submit handler; mocked APIs; no real platform writes',cases},null,2));
 console.log('PASS '+cases.length+' customer navigation flows');
}catch(error){
 throw error
}finally{if(ws?.readyState===WebSocket.OPEN){try{await send('Browser.close')}catch{}ws.close()}if(chrome?.exitCode===null)chrome.kill();await preview?.close();await unlink(fixture)}
