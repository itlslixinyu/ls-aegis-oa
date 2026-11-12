const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm')
const req=require('node:module').createRequire(path.resolve('ls-aegis-web/package.json')),ts=req('typescript'),vue=req('vue')
const tick=()=>new Promise(resolve=>setImmediate(resolve))
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return{resolve,reject,promise}}
function load(file,names,imports,extra={}){
 const source=fs.readFileSync(file,'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
 const code=ts.transpileModule(source+'\nexport { '+names+' };',{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
 const exports={},lifecycle=[]
 vm.runInNewContext(code,{...vue,defineOptions(){},onBeforeUnmount:fn=>lifecycle.push(fn),exports,AbortController,FormData,File,Blob,URL,console,require:name=>{if(name in imports)return imports[name];throw Error(name)},...extra})
 return{...exports,unmount:()=>lifecycle.forEach(fn=>fn())}
}
function avatar(){
 const readers=[],requests=[],messages=[],store={userInfo:vue.reactive({avatar:'old'}),getInfo:async()=>{}}
 const f=load('ls-aegis-web/src/views/user/profile/BasicInfo.vue','onBeforeUpload,handleUpload,reset,cropperRef,uploading,options,visible',{
 '@vueuse/core':{useWindowSize:()=>({width:vue.ref(1000)})},'@arco-design/web-vue':{Message:{error:msg=>messages.push(msg),success:msg=>messages.push(msg)}},'vue-cropper':{},'./BasicInfoUpdateModal.vue':{},'vue-cropper/dist/index.css':{},'@/stores':{useUserStore:()=>store},'@/apis/system':{uploadAvatar:(data,options)=>{const pending=deferred();requests.push({data,options,...pending});return pending.promise}},
 },{FileReader:class{constructor(){readers.push(this)}readAsDataURL(){} }})
 return{...f,readers,requests,messages,store}
}
test('avatar rejects empty and oversized files before FileReader and ignores stale reader callbacks',()=>{
 const f=avatar();f.onBeforeUpload(new File([],'a.png',{type:'image/png'}));f.onBeforeUpload(new File([new Uint8Array(10*1024*1024+1)],'a.png',{type:'image/png'}));assert.equal(f.readers.length,0)
 f.onBeforeUpload(new File(['x'],'a.png',{type:'image/png'}));f.onBeforeUpload(new File(['x'],'b.png',{type:'image/png'}));f.readers[1].result='data:B';f.readers[1].onload();f.readers[0].result='data:A';f.readers[0].onload();assert.equal(f.options.img,'data:B');f.reset();f.readers[1].onload();assert.equal(f.options.img,'')
})
test('avatar crop confirm is single flight and cancel aborts with no late avatar mutation',async()=>{
 const f=avatar();f.options.img='data:synthetic';let crops=0;f.cropperRef.value={getCropBlob:callback=>{crops++;callback(new Blob(['png'],{type:'image/png'}))}}
 await f.handleUpload();await f.handleUpload();assert.equal(crops,1);assert.equal(f.requests.length,1);assert.equal(f.uploading.value,true)
 f.reset();assert.equal(f.requests[0].options.signal.aborted,true);f.requests[0].resolve({data:{avatar:'late'}});await tick();assert.equal(f.store.userInfo.avatar,'old');assert.equal(f.uploading.value,false)
})
test('seal extraction freezes source during processing and releases a preview resolved after close',async()=>{
 const pending=deferred(),preview=deferred(),revoked=[];const readers=[]
 const f=load('ls-aegis-web/src/views/sign/seal/extract.vue','onFileChange,doExtract,selectedFile,extractPreview,previewExtract,onPreviewClose,previewUrl',{
 '@arco-design/web-vue':{Message:{error(){},warning(){},success(){}}},'../constants/table-col':{SEAL_EXTRACT_SIZE_MM:{},SignCol:{}},'@/hooks':{useDownload(){},useTable:()=>({tableData:vue.ref([]),loading:vue.ref(false),pagination:{},search(){},refresh(){},handleDelete(){}})},'@/hooks/app':{useDict:()=>({sign_seal_extract_category:vue.ref([])})},'@/utils/dict':{dictSelectOptions:()=>[]},'@/apis/sign':{extractSealPreview:()=>pending.promise,getExtractImageResource:id=>id,fetchSignImageObjectUrl:()=>preview.promise}
 },{FileReader:class{constructor(){readers.push(this)}readAsDataURL(){}},URL:{revokeObjectURL:url=>revoked.push(url)}})
 const a=new File(['a'],'a.png',{type:'image/png'}),b=new File(['b'],'b.png',{type:'image/png'});f.onFileChange([{file:a}]);const extracting=f.doExtract();f.onFileChange([{file:b}]);assert.equal(f.selectedFile.value,a);pending.resolve({data:{extractImageBase64:'synthetic'}});await extracting;assert.equal(f.extractPreview.value,'data:image/png;base64,synthetic');const showing=f.previewExtract({id:'123'});f.onPreviewClose();preview.resolve('blob:late');await showing;assert.equal(f.previewUrl.value,'');assert.deepEqual(revoked,['blob:late'])
})
