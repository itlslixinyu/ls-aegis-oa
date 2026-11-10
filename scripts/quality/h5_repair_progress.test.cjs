const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs')
const source=fs.readFileSync('ls-aegis-h5/src/utils/repair-progress.js','utf8').replace('export function','function')
const build=new Function(source+';return buildRepairProgress')()
const apply={applyTime:'apply'},order={status:'FINISHED',acceptTime:'accept',startTime:'start',finishTime:'finish'}
const fmt=v=>v||'-'
test('finished settlement does not imply customer confirmation',()=>{const s=build(apply,order,null,fmt);assert.deepEqual(s.map(i=>i.title),['提交报修','工程师接单','开始维修','维修完成']);assert.equal(s.at(-1).text,'finish')})
test('customer confirmation uses the separately recorded event time',()=>{const s=build(apply,order,{confirmed:true,confirmedAt:'customer-time'},fmt);assert.deepEqual(s.at(-1),{title:'客户确认',text:'customer-time'})})
test('closed without customer confirmation is only a closure event',()=>{const s=build(apply,{status:'CLOSED'},null,fmt);assert.deepEqual(s,[{title:'提交报修',text:'apply'},{title:'工单关闭',text:'已关闭'}])})
test('confirmed and closed events both remain visible',()=>{const s=build(apply,{...order,status:'CLOSED'},{confirmed:true,confirmedAt:'confirmed'},fmt);assert.equal(s.at(-2).title,'客户确认');assert.equal(s.at(-1).title,'工单关闭')})
test('missing order has no fabricated progress and missing timestamps add no extra steps',()=>{assert.deepEqual(build(apply,null,null,fmt),[]);assert.deepEqual(build(apply,{status:'WAIT_ACCEPT'},null,fmt),[{title:'提交报修',text:'apply'}])})
