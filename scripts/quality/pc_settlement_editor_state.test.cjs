const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const { createRequire } = require('node:module')
const req = createRequire(path.resolve('ls-aegis-web/package.json'))
const ts = req('typescript'), vue = req('vue')
const deferred = () => {let resolve; const promise = new Promise(r => {resolve=r}); return {promise,resolve}}
function fixture(get,validate=async()=>undefined) {
 const text=fs.readFileSync('ls-aegis-web/src/views/eoms/repair/SettlementAddModal.vue','utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
 const ast=ts.createSourceFile('editor.ts',text,ts.ScriptTarget.Latest,true,ts.ScriptKind.TS)
 const names=['editRequest','reset','save','onUpdate']
 const source=ast.statements.filter(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>names.includes(d.name.getText(ast)))).map(s=>s.getText(ast)).join('\n')
 const form=vue.reactive({}),dataId=vue.ref(''),visible=vue.ref(false),writes=[]
 const globals={...vue,form,dataId,visible,formRef:vue.ref({formRef:{validate,resetFields(){}}}),resetForm(){for(const k of Object.keys(form))delete form[k]},Message:{warning(){},success(){}},emit(){},getSettlement:get,updateSettlement:async data=>writes.push(JSON.parse(JSON.stringify(data))),exports:{}}
 vm.runInNewContext(ts.transpileModule(source+'\nexports.api={reset,save,onUpdate}',{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,globals)
 return {...globals.exports.api,form,dataId,visible,writes}
}
const row=(id,status='DRAFT')=>({id,status,repairOrderId:'909000000000000099',orderNo:'GD-TEST-'+id,customerName:'Synthetic',laborAmount:20,totalIncome:20})
test('settlement edits the generated draft and preserves string relation IDs',async()=>{const f=fixture(async id=>({data:row(id)}));await f.onUpdate('909000000000000001');await f.save();assert.equal(f.writes.length,1);assert.equal(f.writes[0].id,'909000000000000001');assert.equal(f.writes[0].repairOrderId,'909000000000000099')})
test('settlement refuses a missing, mismatched or final draft',async()=>{for(const data of [null,row('other'),row('A','CONFIRMED')]){const f=fixture(async()=>({data}));await f.onUpdate('A');assert.equal(await f.save(),false);assert.equal(f.writes.length,0)}})
test('settlement late detail A cannot overwrite current draft B',async()=>{const a=deferred(),b=deferred(),f=fixture(id=>id==='A'?a.promise:b.promise);const pa=f.onUpdate('A'),pb=f.onUpdate('B');b.resolve({data:row('B')});await pb;a.resolve({data:row('A')});await pa;await f.save();assert.equal(f.writes[0].id,'B');assert.equal(f.writes[0].orderNo,'GD-TEST-B')})
test('settlement close invalidates a pending detail',async()=>{const d=deferred(),f=fixture(()=>d.promise);const p=f.onUpdate('A');f.reset();d.resolve({data:row('A')});await p;assert.equal(f.visible.value,false);assert.equal(await f.save(),false);assert.equal(f.writes.length,0)})
test('settlement validation completing after a draft switch cannot write either draft',async()=>{const d=deferred(),f=fixture(async id=>({data:row(id)}),()=>d.promise);await f.onUpdate('A');const p=f.save();await f.onUpdate('B');d.resolve();await p;assert.equal(f.writes.length,0)})
