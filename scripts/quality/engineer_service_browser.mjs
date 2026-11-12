// Actual engineer router and Vue/Vant service pages, isolated browser and synthetic transport only.
import { spawn } from 'node:child_process'
import { mkdir,writeFile,unlink } from 'node:fs/promises'
import { pathToFileURL } from 'node:url'
import path from 'node:path'
import assert from 'node:assert/strict'
const out=path.resolve('.local/maintain/service-pool-20261112/browser'),fixture=path.resolve('ls-aegis-h5/__engineer_service_preview.html'),port=19493
await mkdir(out,{recursive:true})
const mocks=`window.recordCalls=[];window.claimCalls=[];window.task={id:'123',orderNo:'合成工单123',status:'PROCESSING',customerName:'合成客户',reportAddress:'合成报修地址'};window.taskRecords=[];window.savedMaterials=[];window.failRecord=true;
export const getTaskDetail=async()=>({data:{order:{...window.task},records:[...window.taskRecords]}});
export const getTaskMaterials=async()=>({data:window.savedMaterials});
export const getMaterialOptions=async()=>({data:{goods:[{id:'9007199254740993',name:'合成电源配件',spec:'12V',unit:'个'}],warehouses:[{id:'9007199254740995',name:'合成仓库'}]}});
export const submitRecord=async data=>{window.recordCalls.push(JSON.parse(JSON.stringify(data)));await new Promise(r=>setTimeout(r,80));if(window.failRecord)throw Error('模拟保存失败');window.taskRecords=[{...data,id:'321',createBy:'合成工程师'}];window.savedMaterials=[{id:'555',materialName:'合成电源配件',quantity:data.materials[0]?.quantity||1,warehouseName:'合成仓库'}];return {data:true}};
export const uploadRepairPhoto=async()=>({data:{fileId:'444'}});
export const getPoolList=async()=>({data:{list:[{applyId:'9007199254740997',repairNo:'合成报修123',faultDesc:'合成等待领取的报修',applyTime:'2026-11-11 12:00:00'}],total:1}});
export const claimPoolTask=async id=>{window.claimCalls.push(id);await new Promise(r=>setTimeout(r,100));window.task.status='ACCEPTED';return {data:{orderId:'123'}}};
export const acceptTask=async()=>({data:true});export const startTask=async()=>({data:true});export const finishTask=async()=>({data:true});
export const getTaskList=async params=>({data:{list:params.status==='COMPLETED'?[{id:'124',orderNo:'合成已完工A',status:'WAIT_SETTLEMENT'},{id:'125',orderNo:'合成已完工B',status:'FINISHED'}]:[],total:params.status==='COMPLETED'?2:0}});
export const getProfile=async()=>({data:{total:2,waitSettlement:1,finished:1}});export const logout=async()=>({data:true});`
const {createServer}=await import(pathToFileURL(path.resolve('ls-aegis-h5/node_modules/vite/dist/node/index.js')).href)
const plugin={name:'engineer-service-fixture',enforce:'pre',resolveId(id){if(/api[\/]engineer(?:.js)?$/.test(id))return '\0engineer-service-api'},load(id){if(id==='\0engineer-service-api')return mocks}}
await writeFile(fixture,`<html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="app"></div><script type="module">
import {createApp,h} from 'vue';import {createPinia} from 'pinia';import {RouterView} from 'vue-router';import Vant from 'vant';import 'vant/lib/index.css';import router from '/src/router/index.js';import {useUserStore} from '/src/store/user.js';
const app=createApp({render:()=>h(RouterView)}),pinia=createPinia();app.use(pinia);const user=useUserStore(pinia);user.setEngineerToken('synthetic-engineer-session',{pwdExpired:false,user:{nickName:'合成工程师'}});app.use(router);app.use(Vant);await router.push('/engineer/task/123');await router.isReady();app.mount('#app');window.user=user;window.fixtureRouter=router;window.state=()=>router.currentRoute.value.matched.at(-1).instances.default.$.setupState;window.ready=true;
</script></body></html>`,{flag:'wx'})
let preview,chrome,ws,id=0;const pending=new Map(),sleep=ms=>new Promise(r=>setTimeout(r,ms))
function send(method,params={}){return new Promise((resolve,reject)=>{const n=++id,timer=setTimeout(()=>{pending.delete(n);reject(Error('Timeout '+method))},15000);pending.set(n,{resolve,reject,timer});ws.send(JSON.stringify({id:n,method,params}))})}
try{
 preview=await createServer({root:path.resolve('ls-aegis-h5'),configFile:path.resolve('ls-aegis-h5/vite.config.js'),plugins:[plugin],mode:'engineer',define:{__H5_ROLE__:JSON.stringify('engineer')},resolve:{alias:{'#h5-routes':path.resolve('ls-aegis-h5/src/router/engineer-routes.js')}},optimizeDeps:{noDiscovery:true,include:['vue','vue-router','pinia','vant','sm-crypto']},server:{host:'127.0.0.1',port:5196,strictPort:true,open:false,hmr:false,watch:{ignored:['**/dist/**','**/dist-engineer/**']}}});await preview.listen()
 chrome=spawn(process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--remote-debugging-address=127.0.0.1',`--remote-debugging-port=${port}`,`--user-data-dir=${path.join(out,'chrome-profile')}`,'--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-extensions','about:blank'],{windowsHide:true,stdio:'ignore'})
 let target;for(let i=0;i<40;i++){try{target=await(await fetch(`http://127.0.0.1:${port}/json/new?about:blank`,{method:'PUT',signal:AbortSignal.timeout(1000)})).json();break}catch{await sleep(250)}}assert.ok(target?.webSocketDebuggerUrl)
 ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});ws.onmessage=({data})=>{const m=JSON.parse(data),p=pending.get(m.id);if(!p)return;pending.delete(m.id);clearTimeout(p.timer);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result)}
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value}
 const wait=async expression=>{for(let i=0;i<100;i++){if(await evaluate(expression))return;await sleep(100)}throw Error('Timed out '+expression)}
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true})
 await send('Page.navigate',{url:'http://127.0.0.1:5196/__engineer_service_preview.html'});await wait('window.ready===true');await sleep(1500)
 const cases=[];
 await wait("location.pathname==='/engineer/task/123' && document.body.innerText.includes('填写维修记录')");
 await evaluate("document.body.style.minHeight='3000px';window.scrollTo(0,900);[...document.querySelectorAll('button')].find(b=>b.textContent.includes('填写维修记录')).click()");
 await wait("location.pathname==='/engineer/record/123' && !!document.querySelector('.record-intro') && window.scrollY===0");
 cases.push('task-to-record-opens-at-top');
 await evaluate("document.body.style.minHeight='';Object.assign(window.state().form,{faultReason:'合成故障原因',checkProcess:'合成检测过程',solution:'更换合成配件',result:'合成维修结果'})");
 await evaluate("[...document.querySelectorAll('.van-radio')].find(e=>e.textContent.includes('使用了配件')).click()");
 await wait('window.state().form.materials.length===1');
 await evaluate("[...document.querySelectorAll('.material-line .van-field')].find(e=>e.textContent.includes('配件')).click()");
 await wait("[...document.querySelectorAll('.van-popup .van-cell')].some(e=>e.textContent.includes('合成电源配件'))");
 await evaluate("[...document.querySelectorAll('.van-popup .van-cell')].find(e=>e.textContent.includes('合成电源配件')).click()");
 await wait("window.state().form.materials[0].materialId==='9007199254740993'");
 assert.equal(await evaluate('window.state().form.materials[0].warehouseId'),'9007199254740995');
 await evaluate('window.state().form.materials[0].quantity=2');
 await send('Emulation.setDeviceMetricsOverride',{width:320,height:740,deviceScaleFactor:1,mobile:true});await sleep(300);
 assert.ok(await evaluate('document.documentElement.scrollWidth<=window.innerWidth+1'));
 const shot=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});await writeFile(path.join(out,'record-320.png'),Buffer.from(shot.data,'base64'));
 cases.push('catalog-parts-selection-keeps-string-ids-and-small-screen-fit');
 await evaluate('window.state().onSubmit()');await wait('window.recordCalls.length===1 && window.state().loading===false');
 assert.equal(await evaluate('window.state().form.faultReason'),'合成故障原因');assert.equal(await evaluate('window.state().form.materials.length'),1);
 await evaluate('window.failRecord=false;Promise.all([window.state().onSubmit(),window.state().onSubmit()])');
 await wait("location.pathname==='/engineer/task/123' && document.body.innerText.includes('合成电源配件')");
 const calls=await evaluate('window.recordCalls');assert.equal(calls.length,2);assert.deepEqual(Object.keys(calls[1].materials[0]).sort(),['materialId','quantity','remark','warehouseId']);assert.equal(Number(calls[1].materials[0].quantity),2);
 cases.push('failed-save-preserves-input-and-success-submits-record-and-parts-once');
 assert.equal(await evaluate("document.body.innerText.includes('维修地址')"),false);assert.equal(await evaluate("document.body.innerText.includes('合成报修地址')"),true);
 assert.ok(await evaluate("document.querySelector('.finish-section').getBoundingClientRect().top>document.querySelector('.material-summary').getBoundingClientRect().top"));
 cases.push('reported-site-and-parts-summary-before-bottom-finish-button');
 await evaluate("window.fixtureRouter.push('/engineer/tasks/status/COMPLETED')");await wait("document.body.innerText.includes('合成已完工B')");assert.equal(await evaluate("document.body.innerText.includes('待结算')"),false);
 cases.push('completed-services-hide-financial-status');
 await evaluate("window.fixtureRouter.push('/engineer/pool')");await wait("document.body.innerText.includes('合成报修123')");
 assert.equal(await evaluate("document.querySelectorAll('.van-tabbar-item')[0].textContent.trim()"),'工单池');
 await evaluate("[...document.querySelectorAll('button')].find(b=>b.textContent.includes('领取工单')).click()");await wait("document.querySelector('.van-dialog__confirm')");
 await evaluate("document.querySelector('.van-dialog__confirm').click()");await wait("location.pathname==='/engineer/task/123' && document.body.innerText.includes('开始维修')");
 assert.deepEqual(await evaluate('window.claimCalls'),['9007199254740997']);cases.push('pool-first-tab-and-confirmed-claim-opens-owned-task');
 await writeFile(path.join(out,'result.json'),JSON.stringify({status:'PASS',scope:'Actual Vue/Vant engineer pages and router; simulated API, no production writes',cases},null,2));console.log('PASS '+cases.length+' engineer service UI flows');
}catch(error){
 throw error
}finally{if(ws?.readyState===WebSocket.OPEN){try{await send('Browser.close')}catch{}ws.close()}if(chrome?.exitCode===null)chrome.kill();await preview?.close();await unlink(fixture)}
