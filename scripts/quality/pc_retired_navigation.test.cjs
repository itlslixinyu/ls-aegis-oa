const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const {createRequire}=require('node:module'),req=createRequire(path.resolve('ls-aegis-web/package.json')),ts=req('typescript'),vue=req('vue');
function moduleCode(file,extra={}) {const box={exports:{},Set,JSON,...extra};vm.runInNewContext(ts.transpileModule(fs.readFileSync(file,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,box);return box.exports}
const policy=moduleCode('ls-aegis-web/src/utils/retired-navigation.ts');
const plain=v=>JSON.parse(JSON.stringify(v)),gate=()=>{let resolve,reject;const promise=new Promise((r,j)=>{resolve=r;reject=j});return{promise,resolve,reject}};
const retired={path:'/code',name:'Code',children:[{path:'/code/generator',name:'CodeGenerator'}]},goods={path:'/psi/goods',name:'Goods',meta:{keepAlive:true}};
function fixture(loader) {
 const text=fs.readFileSync('ls-aegis-web/src/stores/modules/route.ts','utf8'),ast=ts.createSourceFile('route.ts',text,ts.ScriptTarget.Latest,true);
 const source=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(ast)==='storeSetup')).getText(ast);
 let clock=1000,token='own-session',calls=0;const registered=new Map();
 const router={currentRoute:{value:{path:'/psi/goods'}},getRoutes:()=>[...registered.values()],hasRoute:n=>registered.has(n),removeRoute:n=>registered.delete(n),addRoute:r=>{registered.set(r.name,r);return()=>registered.delete(r.name)},replace:async p=>{router.currentRoute.value={path:p}}};
 const box={...vue,...policy,exports:{},Date:{now:()=>clock},getToken:()=>token,getUserRoute:()=>{calls++;return loader(calls)},constantRoutes:[],systemRoutes:[],formatAsyncRoutes:policy.activeNavigation,flatMultiLevelRoutes:r=>r,cloneDeep:plain};
 vm.runInNewContext(ts.transpileModule(source+';exports.store=storeSetup()', {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,box);
 return{store:box.exports.store,router,registered,tick:n=>clock+=n,identity:t=>token=t,calls:()=>calls};
}
test('retired trees are removed recursively without dropping similarly named business routes',()=>{
 const original=[retired,{path:'/open/app?old=1'},{path:'/workspace',children:[{id:'9010',path:'/renamed'},goods]},{path:'/system/config?tab=client'},{path:'/code-review'},{path:'/open-orders'}];
 const result=plain(policy.activeNavigation(original));assert.deepEqual(result.map(x=>x.path),['/workspace','/system/config?tab=client','/code-review','/open-orders']);assert.deepEqual(result[0].children,[goods]);assert.equal(original[2].children.length,2);
});
test('old tab/cache/search snapshots are migrated precisely and cleanup is idempotent',()=>{
 const data=new Map([['tabs',JSON.stringify({tabList:[retired,goods,{path:'/open/app'}],cacheList:['CodeGenerator','Goods','OpenApp'],reloadFlag:true})],['layout-search-history',JSON.stringify([{path:'/code/generator',title:'old'},{path:'/psi/goods',title:'goods'}])],['user','OWN-SESSION-UNTOUCHED'],['draft','BUSINESS-DRAFT']]);let writes=0;
 const storage={getItem:k=>data.get(k),setItem:(k,v)=>{writes++;data.set(k,v)}};
 policy.cleanRetiredNavigationStorage(storage,storage);const tabs=JSON.parse(data.get('tabs'));assert.deepEqual(tabs.tabList,[goods]);assert.deepEqual(tabs.cacheList,['Goods']);assert.equal(tabs.reloadFlag,true);assert.equal(data.get('user'),'OWN-SESSION-UNTOUCHED');assert.equal(data.get('draft'),'BUSINESS-DRAFT');assert.equal(JSON.parse(data.get('layout-search-history')).length,1);assert.equal(writes,2);policy.cleanRetiredNavigationStorage(storage,storage);assert.equal(writes,2);
});
test('malformed or inaccessible storage never destroys saved data or blocks bootstrap',()=>{
 let writes=0;const malformed={getItem:()=>'{invalid',setItem:()=>writes++},unavailable={getItem:()=>{throw Error('denied')},setItem:()=>writes++};assert.doesNotThrow(()=>policy.cleanRetiredNavigationStorage(malformed,unavailable));assert.equal(writes,0);
});
test('already-open navigation refresh replaces old registered routes with fresh menus',async()=>{
 const f=fixture(async n=>({data:n===1?[retired,goods]:[{path:'/quote',name:'Quote'}]}));const first=await f.store.generateRoutes();first.forEach(f.router.addRoute);assert.equal(f.store.routes.value.length,1);f.registered.set('Code',retired);await f.store.refreshRoutes(f.router);assert.equal(f.registered.has('Code'),false);assert.equal(f.calls(),1);f.tick(61000);await f.store.refreshRoutes(f.router);assert.deepEqual([...f.registered.keys()],['Quote']);assert.deepEqual(plain(f.store.routes.value).map(x=>x.path),['/quote']);
});
test('simultaneous focus/navigation coalesce, and network failures do not empty working menus',async()=>{
 const d=gate(),f=fixture(n=>n===1?Promise.resolve({data:[goods]}):d.promise);await f.store.generateRoutes();f.tick(61000);const a=f.store.refreshRoutes(f.router),b=f.store.refreshRoutes(f.router);assert.equal(a,b);assert.equal(f.calls(),2);d.reject(Error('temporary'));await assert.rejects(a);assert.deepEqual(plain(f.store.routes.value),[goods]);await f.store.refreshRoutes(f.router);assert.equal(f.calls(),2);
});
test('late response after logout cannot restore the previous identity menu or register routes',async()=>{
 const d=gate(),f=fixture(n=>n===1?Promise.resolve({data:[goods]}):d.promise);await f.store.generateRoutes();f.tick(61000);const pending=f.store.refreshRoutes(f.router);f.identity('');f.store.clearRoutes();d.resolve({data:[{path:'/quote',name:'Quote'}]});await assert.rejects(pending);assert.equal(f.store.routes.value.length,0);assert.equal(f.registered.size,0);
});
test('event synchronization listens only once and removes all listeners when disposed',()=>{
 const listeners=new Map(),docListeners=new Map();let callback,pruned=0,refreshed=0,tabPruned=0,unregistered=0;
 const window={addEventListener:(k,v)=>listeners.set(k,v),removeEventListener:k=>listeners.delete(k)},document={visibilityState:'visible',addEventListener:(k,v)=>docListeners.set(k,v),removeEventListener:k=>docListeners.delete(k)};
 const store={pruneRetiredRoutes:()=>pruned++,refreshRoutes:()=>{refreshed++;return Promise.resolve()}},tabs={pruneRetiredNavigation:()=>tabPruned++};
 const api=moduleCode('ls-aegis-web/src/router/menu-sync.ts',{window,document,require:n=>n==='@/stores'?{useRouteStore:()=>store,useTabsStore:()=>tabs}:policy});
 const stop=api.setupMenuSynchronization({afterEach:c=>{callback=c;return()=>unregistered++},currentRoute:{value:{path:'/psi/goods'}}});assert.equal(refreshed,1);listeners.get('focus')();callback();assert.equal(refreshed,3);assert.equal(tabPruned,3);document.visibilityState='hidden';docListeners.get('visibilitychange')();assert.equal(refreshed,3);stop();assert.equal(listeners.size,0);assert.equal(docListeners.size,0);assert.equal(unregistered,1);
});

test('persisted tab restore prunes retired tabs and keep-alive names; later adds cannot revive them',()=>{
 let options;const router={currentRoute:{value:{path:'/psi/goods'}},push(){}};
 const api=moduleCode('ls-aegis-web/src/stores/modules/tabs.ts',{sessionStorage:{},require:n=>{
  if(n==='pinia')return{defineStore:(id,setup,config)=>{options=config;return()=>vue.reactive(setup())}};
  if(n==='vue')return vue;if(n==='@/router')return{default:router};if(n==='@/stores')return{useRouteStore:()=>({routes:[]})};if(n==='@/utils/retired-navigation')return policy;if(n==='xe-utils/eachTree')return{default:require(path.resolve('ls-aegis-web/node_modules/xe-utils/eachTree'))};throw Error(n);
 }});
 const store=api.useTabsStore();store.tabList=[retired,goods,{path:'/open/app',name:'OpenApp'}];store.cacheList=['CodeGenerator','Goods','OpenApp'];options.persist.afterRestore({store});assert.deepEqual(plain(store.tabList),[goods]);assert.deepEqual(plain(store.cacheList),['Goods']);store.addTabItem(retired);store.addCacheItem({...retired,meta:{keepAlive:true}});assert.equal(store.tabList.length,1);assert.equal(store.cacheList.length,1);
});
