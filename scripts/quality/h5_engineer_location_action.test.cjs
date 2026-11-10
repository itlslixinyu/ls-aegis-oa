const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const source = fs.readFileSync('ls-aegis-h5/src/views/engineer/TaskDetail.vue', 'utf8')
const action = source.slice(source.indexOf('async function beforeRemarkClose('), source.indexOf('/** 跳转维修记录提交 */'))
function fixture(mode, getLocation) {
  const sent = [], messages = [], loading = {value:false}
  const handler = new Function('actionLoading','currentAction','order','actionRemark','getLocation','acceptTask','startTask','finishTask','showFailToast','showSuccessToast','loadDetail', action + '\nreturn beforeRemarkClose')(
    loading,{value:mode},{value:{id:'909415870873207363'}},{value:'synthetic'},getLocation,
    async(...args)=>{sent.push(['accept',...args]);return {}},
    async(...args)=>{sent.push(['start',...args]);return {}},
    async(...args)=>{sent.push(['finish',...args]);return {}},msg=>messages.push(msg),()=>{},async()=>{})
  return {handler,sent,messages,loading}
}
for(const mode of ['start','finish']) {
  test(`engineer ${mode}: denied GPS blocks network write and releases loading`,async()=>{
    const f=fixture(mode,async()=>{throw Error('permission denied')})
    assert.equal(await f.handler('confirm'),false);assert.equal(f.sent.length,0)
    assert.equal(f.loading.value,false);assert.equal(f.messages.length,1)
  })
  test(`engineer ${mode}: successful location forwards exact coordinates and string order ID`,async()=>{
    const f=fixture(mode,async()=>({longitude:'0.000000',latitude:'0.000000'}))
    assert.equal(await f.handler('confirm'),true)
    assert.equal(f.sent.length,1);assert.equal(f.sent[0][1],'909415870873207363')
    assert.deepEqual(f.sent[0].slice(-2),['0.000000','0.000000']);assert.equal(f.loading.value,false)
  })
}
test('canceling engineer location dialog performs no location lookup or write',async()=>{
  const f=fixture('start',async()=>assert.fail('GPS should not be requested'))
  assert.equal(await f.handler('cancel'),true);assert.equal(f.sent.length,0)
})
