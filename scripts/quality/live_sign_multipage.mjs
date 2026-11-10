/** Positive three-page signing flow, restricted to an explicitly owned open test ledger. */
import assert from 'node:assert/strict'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { createRequire } from 'node:module'
import { pathToFileURL } from 'node:url'
import { spawnSync } from 'node:child_process'
const run=process.argv.find(x=>x.startsWith('--run='))?.slice(6);assert.match(run||'',/^R\d+$/)
const root=resolve(import.meta.dirname,'../..'),local=n=>resolve(root,'.local',`${run.toLowerCase()}-${n}`)
const state=JSON.parse(readFileSync(local('ledger.json'),'utf8'));assert.equal(state.run,run);assert.ok(!state.closed);assert.ok(!state.objects.multiSource,'Do not replay this positive signing phase')
assert.equal(state.prefix,'ITLS-TEST-'+run)
for(const key of ['seal','signProject','signSource'])assert.ok(state.objects[key].marker.startsWith(state.prefix))
const runtime=spawnSync('docker',['exec','ls-aegis-mcs-backend','sha256sum','/app/jars/ls-aegis-server.jar'],{encoding:'utf8',timeout:20000})
assert.equal(runtime.status,0);assert.equal(runtime.stdout.trim().split(/\s+/)[0],state.currentBackendSha256)
const token=JSON.parse(readFileSync(local('session.json'),'utf8')).token,base='http://127.0.0.1:8000',marker=`ITLS-TEST-${run}-自签测试`
const save=()=>writeFileSync(local('ledger.json'),JSON.stringify(state,null,2))
const register=(key,kind,id)=>{state.objects[key]={kind,id:String(id),marker,status:'AUDIT_RETAINED',cleanup:'immutable synthetic signature reference audit'};save()}
async function api(p,method='GET',data){const r=await fetch(base+p,{method,headers:{Authorization:'Bearer '+token,...(data instanceof FormData?{}:{'Content-Type':'application/json'})},body:data===undefined?undefined:data instanceof FormData?data:JSON.stringify(data),signal:AbortSignal.timeout(30000)});const b=await r.json();assert.equal(b.success,true,p+': '+b.msg);return b.data}
function pdf(){const objects=['<< /Type /Catalog /Pages 2 0 R >>','<< /Type /Pages /Kids [3 0 R 4 0 R 5 0 R] /Count 3 >>'];for(let i=1;i<=3;i++)objects.push(`<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 9 0 R >> >> /Contents ${5+i} 0 R >>`);for(let i=1;i<=3;i++){const t=`BT /F1 18 Tf 50 750 Td (ITLS TEST ${run} SYNTHETIC PAGE ${i}) Tj ET\n`;objects.push(`<< /Length ${Buffer.byteLength(t)} >>\nstream\n${t}endstream`)}objects.push('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');let out='%PDF-1.4\n';const offsets=[];objects.forEach((o,i)=>{offsets.push(Buffer.byteLength(out));out+=`${i+1} 0 obj\n${o}\nendobj\n`});const xref=Buffer.byteLength(out);return Buffer.from(out+`xref\n0 10\n0000000000 65535 f \n${offsets.map(v=>`${String(v).padStart(10,'0')} 00000 n \n`).join('')}trailer\n<< /Size 10 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`)}
const sealId=state.objects.seal.id,projectId=state.objects.signProject.id
const project=await api('/sign/project/'+projectId);assert.equal(project.projectName,marker)
const seal=await api('/sign/seal/'+sealId);assert.equal(seal.sealName,marker);assert.equal(String(seal.status),'1')
try{
 const form=new FormData();form.set('file',new Blob([pdf()],{type:'application/pdf'}),`${run}-three-pages.pdf`)
 const source=await api('/system/common/file','POST',form);register('multiSource','sys_file',source.id)
 await api('/sign/project','PUT',{...project,otherFileIds:[state.objects.signSource.id,String(source.id)].join(','),sealIds:[sealId],userIds:['1']})
 await api('/sign/seal','PUT',{sealId,sealName:marker,sealCategory:'TEST',status:'0',deptId:'1'});await api('/sign/seal/'+sealId+'/auth/bind','POST',['1'])
 const result=await api('/sign/execute/unified','POST',{projectId,sysFileId:String(source.id),sealId,generalSeals:[{sealId,mode:'custom',positions:[1,2,3].map(page=>({page,x:100,y:100,width:60,height:60}))}],spanSeals:[{sealId,startPage:1,endPage:3,xPosition:0,yPosition:400,sealSize:90}],hasTimestamp:false})
 register('multiRecord','sign_record',result.recordId);register('multiSigned','sign_file',result.signedFileId)
 const receipt=await api('/sign/execute/receipt/'+result.recordId);assert.equal(receipt.verifyStatus,'1')
 const response=await fetch(base+'/sign/file/download/'+result.signedFileId,{headers:{Authorization:'Bearer '+token},signal:AbortSignal.timeout(30000)});assert.equal(response.status,200);const bytes=Buffer.from(await response.arrayBuffer());assert.equal(bytes.subarray(0,4).toString(),'%PDF');writeFileSync(local('multi-signed.pdf'),bytes)
 const verifyForm=new FormData();verifyForm.set('file',new Blob([bytes],{type:'application/pdf'}),'multi-signed.pdf');const verified=await api('/sign/verify/fingerprint/upload','POST',verifyForm);assert.equal(verified.valid,true);assert.equal(verified.trustVerified,false);assert.equal(verified.recordId,result.recordId)
 const req=createRequire(resolve(root,'ls-aegis-web/package.json')),pdfjs=await import(pathToFileURL(req.resolve('pdfjs-dist/legacy/build/pdf.mjs')).href)
 const doc=await pdfjs.getDocument({data:new Uint8Array(bytes),useSystemFonts:true,disableFontFace:true,isEvalSupported:false}).promise
 const pageImageOperations=[]
 try{assert.equal(doc.numPages,3);for(let i=1;i<=3;i++){const page=await doc.getPage(i);const text=(await page.getTextContent()).items.map(x=>x.str||'').join(' ');assert.ok(text.includes(`ITLS TEST ${run} SYNTHETIC PAGE ${i}`));const ops=await page.getOperatorList();const images=ops.fnArray.filter(x=>[pdfjs.OPS.paintImageXObject,pdfjs.OPS.paintInlineImageXObject,pdfjs.OPS.paintImageMaskXObject].includes(x)).length;assert.ok(images>=2,'Each source page must contain both general and span image draws');pageImageOperations.push(images)}}finally{await doc.destroy()}
 const download=await fetch(base+'/sign/execute/receipt/'+result.recordId+'/download',{headers:{Authorization:'Bearer '+token},signal:AbortSignal.timeout(30000)});assert.equal(download.status,200);const receiptBytes=Buffer.from(await download.arrayBuffer());assert.equal(receiptBytes.subarray(0,4).toString(),'%PDF');writeFileSync(local('multi-receipt.pdf'),receiptBytes)
 const evidence={pages:3,generalPlacements:3,spanRange:[1,3],pageImageOperations,recordId:result.recordId,verifyStatus:'1',uploadValid:true,trustVerified:false,allSourcePageTextsPreserved:true,signedBytes:bytes.length,receiptBytes:receiptBytes.length,externalTSA:false}
 state.results.push({id:run+'-SIGN-MULTIPAGE-GENERAL-SPAN-UPLOAD',at:new Date().toISOString(),status:'PASS',backendSha256:state.currentBackendSha256,evidence});save();console.log(JSON.stringify(evidence))
}catch(e){state.results.push({id:run+'-SIGN-MULTIPAGE-GENERAL-SPAN-UPLOAD',at:new Date().toISOString(),status:'FAIL',error:e.message,backendSha256:state.currentBackendSha256});save();throw e}
finally{await api('/sign/seal','PUT',{sealId,sealName:marker,sealCategory:'TEST',status:'1',deptId:'1'});await api('/sign/seal/'+sealId+'/auth/1','DELETE');assert.equal(String((await api('/sign/seal/'+sealId)).status),'1');save()}
