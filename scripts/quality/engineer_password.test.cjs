const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm')
const {createRequire}=require('node:module')
const ts=createRequire(path.resolve('ls-aegis-web/package.json'))('typescript')
const transpile=code=>ts.transpileModule(code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
const helper={exports:{}};vm.runInNewContext(transpile(fs.readFileSync('ls-aegis-h5/src/utils/engineerPassword.js','utf8')),{exports:helper.exports})
function fixture(){
 const redirects=[],toasts=[],handlers={}
 const store={engineerToken:'engineer-fixture',engineerSessionVersion:2,engineerInfo:{pwdExpired:false},h5Token:'customer-fixture',h5SessionVersion:4,
  updateEngineerInfo(info){this.engineerInfo=info},clearEngineerToken(){this.engineerToken='';this.engineerSessionVersion++},clearH5Token(){this.h5Token='';this.h5SessionVersion++}}
 const service={interceptors:{request:{use(ok,fail){handlers.request=ok}},response:{use(ok,fail){handlers.response=ok;handlers.error=fail}}}}
 const axios={create:()=>service,isCancel:()=>false}
 const code=fs.readFileSync('ls-aegis-h5/src/utils/request.js','utf8').replaceAll('import.meta.env',"({VITE_API_BASE_URL:''})")
 vm.runInNewContext(transpile(code),{exports:{},require(name){if(name==='axios')return {default:axios,...axios};if(name==='vant')return {showToast:m=>toasts.push(m),showFailToast:m=>toasts.push(m)};if(name==='@/store/user')return {useUserStore:()=>store};if(name==='@/utils/engineerPassword')return helper.exports;throw Error(name)},
  FormData,Blob,AbortController,Map,JSON,Promise,Error,Date,Math,setTimeout,clearTimeout,console,window:{location:{pathname:'/engineer/todos',replace:p=>redirects.push(p)}}})
 return {store,handlers,redirects,toasts,request:(url,method='get')=>handlers.request({url,method,headers:{}})}
}
test('password and ordinary engineer requests carry the current authenticated identity',()=>{
 const f=fixture();for(const url of ['/user/profile/password','/eoms/engineer/tasks']){const req=f.request(url,'patch');assert.equal(req.headers.Authorization,'Bearer engineer-fixture');assert.equal(req.authSession.kind,'engineer');assert.equal(req.authSession.version,2)}
})
test('expired business response preserves login and redirects once to usable password route',async()=>{
 const f=fixture(),config=f.request('/eoms/engineer/tasks');const response={config,data:{code:'400',success:false,msg:'密码已过期，请修改密码'}}
 await assert.rejects(f.handlers.response(response));await assert.rejects(f.handlers.response(response));assert.equal(f.store.engineerToken,'engineer-fixture');assert.equal(f.store.engineerInfo.pwdExpired,true);assert.deepEqual(f.redirects,['/engineer/password']);assert.equal(f.toasts.length,0);assert.equal(f.store.h5Token,'customer-fixture')
})
test('HTTP error and silent request also recover expired engineer login',async()=>{
 const f=fixture(),config=f.request('/eoms/engineer/profile');config.skipErrorToast=true
 await assert.rejects(f.handlers.error({config,response:{status:400,data:{msg:'密码已过期，请修改密码'}}}));assert.deepEqual(f.redirects,['/engineer/password'])
})
test('late expiry response cannot redirect or mark a different signed-in identity',async()=>{
 const f=fixture(),config=f.request('/eoms/engineer/tasks');f.store.engineerToken='next-fixture';f.store.engineerSessionVersion++
 await assert.rejects(f.handlers.response({config,data:{code:400,success:false,msg:'密码已过期，请修改密码'}}));assert.equal(f.store.engineerInfo.pwdExpired,false);assert.equal(f.store.engineerToken,'next-fixture');assert.equal(f.redirects.length,0)
})
test('customer errors cannot change engineer password metadata',async()=>{
 const f=fixture(),config=f.request('/eoms/h5/repair/list')
 await assert.rejects(f.handlers.response({config,data:{code:400,success:false,msg:'密码已过期，请修改密码'}}));assert.equal(f.store.engineerInfo.pwdExpired,false);assert.equal(f.redirects.length,0)
})
test('login destinations and password detection keep normal credentials and auth failures separate',()=>{
 assert.equal(helper.exports.engineerLandingPath({pwdExpired:true}),'/engineer/password');assert.equal(helper.exports.engineerLandingPath({pwdExpired:false}),'/engineer/workbench')
 assert.equal(helper.exports.isEngineerPasswordExpired({msg:'当前密码不正确'}),false);assert.equal(helper.exports.isEngineerPasswordExpired({code:401,msg:'登录已过期'}),false)
})
