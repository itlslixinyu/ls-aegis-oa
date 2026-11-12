// Real Vue/Vant password form and actual router; isolated browser and simulated API only.
import { spawn } from 'node:child_process'
import { mkdir,writeFile,unlink } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { pathToFileURL } from 'node:url'
import path from 'node:path'
import assert from 'node:assert/strict'
const out=path.resolve('.local/maintain/engineer-password-20261111/browser'),fixture=path.resolve('ls-aegis-h5/__password_preview.html'),port=19489
await mkdir(out,{recursive:true})
const {sm2}=createRequire(path.resolve('ls-aegis-web/package.json'))('sm-crypto'),keys=sm2.generateKeyPairHex()
const mocks=`window.passwordCalls=[];window.failUpdate=true;
export const updatePassword=async data=>{window.passwordCalls.push(data);await new Promise(r=>setTimeout(r,80));if(window.failUpdate)throw new Error('当前密码不正确');return {data:true}};
export const logout=async()=>({data:true});export const login=async()=>({data:{token:'fixture-only'}});
export const getInfo=async()=>({data:{username:'fixture',nickname:'合成工程师',pwdExpired:true}});
export const getCodeImg=async()=>({data:{isEnabled:false}});export const getProfile=async()=>({data:{total:1,accepted:1}});
export const getTaskList=async()=>({data:{list:[],total:0}});`
const {createServer}=await import(pathToFileURL(path.resolve('ls-aegis-h5/node_modules/vite/dist/node/index.js')).href)
const plugin={name:'password-form-fixture',enforce:'pre',resolveId(id){if(/api[\/]engineer(?:.js)?$/.test(id))return '\0password-api';if(/api[\/]auth(?:.js)?$/.test(id))return '\0password-key'},load(id){if(id==='\0password-api')return mocks;if(id==='\0password-key')return `export const getGmPublicKey=async()=>({data:{enable:true,publicKeyHex:'${keys.publicKey}'}})`}}
await writeFile(fixture,`<html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="app"></div><script type="module">
import {createApp,h} from 'vue';import {createPinia} from 'pinia';import {RouterView} from 'vue-router';import Vant from 'vant';import 'vant/lib/index.css';import router from '/src/router/index.js';import {useUserStore} from '/src/store/user.js';
const app=createApp({render:()=>h(RouterView)}),pinia=createPinia();app.use(pinia);const user=useUserStore(pinia);user.setEngineerToken('synthetic-password-session',{pwdExpired:true,user:{nickName:'合成工程师'}});app.use(router);app.use(Vant);await router.push('/engineer/workbench');await router.isReady();app.mount('#app');window.user=user;window.fixtureRouter=router;window.state=()=>router.currentRoute.value.matched.at(-1).instances.default.$.setupState;window.ready=true;
</script></body></html>`,{flag:'wx'})
let preview,chrome,ws,id=0;const pending=new Map(),sleep=ms=>new Promise(r=>setTimeout(r,ms))
function send(method,params={}){return new Promise((resolve,reject)=>{const n=++id,timer=setTimeout(()=>{pending.delete(n);reject(Error('Timeout '+method))},15000);pending.set(n,{resolve,reject,timer});ws.send(JSON.stringify({id:n,method,params}))})}
try{
 preview=await createServer({root:path.resolve('ls-aegis-h5'),configFile:path.resolve('ls-aegis-h5/vite.config.js'),plugins:[plugin],mode:'engineer',define:{__H5_ROLE__:JSON.stringify('engineer')},resolve:{alias:{'#h5-routes':path.resolve('ls-aegis-h5/src/router/engineer-routes.js')}},optimizeDeps:{noDiscovery:true,include:['vue','vue-router','pinia','vant','sm-crypto']},server:{host:'127.0.0.1',port:5192,strictPort:true,open:false,hmr:false,watch:{ignored:['**/dist/**','**/dist-engineer/**']}}});await preview.listen()
 chrome=spawn(process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--remote-debugging-address=127.0.0.1',`--remote-debugging-port=${port}`,`--user-data-dir=${path.join(out,'chrome-profile')}`,'--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-extensions','about:blank'],{windowsHide:true,stdio:'ignore'})
 let target;for(let i=0;i<40;i++){try{target=await(await fetch(`http://127.0.0.1:${port}/json/new?about:blank`,{method:'PUT',signal:AbortSignal.timeout(1000)})).json();break}catch{await sleep(250)}}assert.ok(target?.webSocketDebuggerUrl)
 ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});ws.onmessage=({data})=>{const m=JSON.parse(data),p=pending.get(m.id);if(!p)return;pending.delete(m.id);clearTimeout(p.timer);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result)}
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value}
 const wait=async expression=>{for(let i=0;i<100;i++){if(await evaluate(expression))return;await sleep(100)}throw Error('Timed out '+expression)}
 await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true})
 await send('Page.navigate',{url:'http://127.0.0.1:5192/__password_preview.html'});await wait('window.ready===true');await sleep(1500)
 await wait('location.pathname==="/engineer/password" && document.querySelectorAll("input[type=password]").length===3')
 await evaluate("window.fixtureRouter.push('/engineer/todos')");assert.equal(await evaluate('location.pathname'),'/engineer/password')
 await evaluate("Object.assign(window.state().form,{oldPassword:'Old1!Synthetic',newPassword:'Old1!Synthetic',confirmPassword:'Old1!Synthetic'});window.state().submit()")
 assert.equal(await evaluate('window.passwordCalls.length'),0)
 await evaluate("Object.assign(window.state().form,{newPassword:'New2!Synthetic',confirmPassword:'mismatch'});window.state().submit()")
 assert.equal(await evaluate('window.passwordCalls.length'),0)
 await evaluate("window.state().form.confirmPassword='New2!Synthetic';document.querySelector('.van-button--primary').click()")
 await wait('window.passwordCalls.length===1 && window.state().saving===false')
 assert.equal(await evaluate('window.state().form.oldPassword'),'Old1!Synthetic');assert.ok(await evaluate('window.user.engineerToken'))
 await evaluate("window.failUpdate=false;Promise.all([window.state().submit(),window.state().submit()])")
 await wait('location.pathname==="/engineer/login"')
 assert.equal(await evaluate('window.passwordCalls.length'),2);assert.equal(await evaluate('window.user.engineerToken'),'')
 const calls=await evaluate('window.passwordCalls')
 for(const call of calls){assert.notEqual(call.oldPassword,'Old1!Synthetic');assert.equal(sm2.doDecrypt(call.oldPassword.slice(2),keys.privateKey,1),'Old1!Synthetic');assert.equal(sm2.doDecrypt(call.newPassword.slice(2),keys.privateKey,1),'New2!Synthetic')}
 assert.equal(await evaluate("Object.values(localStorage).some(value=>value.includes('Old1!Synthetic')||value.includes('New2!Synthetic'))"),false)
 await evaluate("window.user.setEngineerToken('synthetic-next-session',{pwdExpired:false,user:{nickName:'合成工程师'}});window.fixtureRouter.push('/engineer/profile')")
 await wait("document.body.innerText.includes('修改密码') && location.pathname==='/engineer/profile'")
 await evaluate("[...document.querySelectorAll('.van-cell')].find(cell=>cell.innerText.includes('修改密码')).click()")
 await wait("location.pathname==='/engineer/password'");assert.equal(await evaluate('window.state().requiredChange'),false)
 await writeFile(path.join(out,'result.json'),JSON.stringify({status:'PASS',scope:'Real Vue/Vant form and router; simulated account API; ephemeral synthetic SM2 keypair only',cases:['expired-workbench-and-todo-route-guard','same-password-and-confirmation-validation','failed-update-preserves-input-and-session','encrypted-update-and-repeat-submit-guard','successful-update-clears-session-and-returns-login','existing-profile-password-entry'],plaintextCredentialsPersisted:false},null,2))
 console.log('PASS 6 password UI flows, real SM2 transport encryption, route guards and usable profile entry')
}catch(error){
 if(ws?.readyState===WebSocket.OPEN){try{const state=await send('Runtime.evaluate',{expression:'JSON.stringify({path:location.pathname,ready:window.ready,passwordInputs:document.querySelectorAll("input[type=password]").length,hasForm:!!document.querySelector(".password-form"),hasApp:!!document.querySelector("#app")})',returnByValue:true});console.error('Fixture diagnostics: '+state.result.value)}catch{}}
 throw error
}finally{if(ws?.readyState===WebSocket.OPEN){try{await send('Browser.close')}catch{}ws.close()}if(chrome?.exitCode===null)chrome.kill();await preview?.close();await unlink(fixture)}
