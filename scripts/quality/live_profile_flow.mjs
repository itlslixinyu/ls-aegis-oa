/** Own disposable identity only; normal CAPTCHA/SM2 login is provisioned separately. */
import assert from 'node:assert/strict'
import {readFileSync,writeFileSync} from 'node:fs'
import {resolve} from 'node:path'
import {spawnSync} from 'node:child_process'
const run=process.argv.find(x=>x.startsWith('--run='))?.slice(6);assert.match(run||'',/^R\d+$/)
const root=resolve(import.meta.dirname,'../..'),local=n=>resolve(root,'.local',run.toLowerCase()+'-'+n),ledger=local('ledger.json'),privateFile=local('profile.private.json')
const state=JSON.parse(readFileSync(ledger,'utf8'));assert.equal(state.run,run);assert.ok(!state.closed)
const admin=JSON.parse(readFileSync(local('session.json'),'utf8')).token,base='http://127.0.0.1:8000',prefix='ITLS-TEST-'+run
const save=()=>writeFileSync(ledger,JSON.stringify(state,null,2)),reg=(key,kind,id,marker)=>{assert.ok(!state.objects[key]);state.objects[key]={kind,id:String(id),marker,status:'ACTIVE',cleanup:'delete own user then role; purge avatar'};save()}
async function call(p,{method='GET',data,token=admin}={}){const r=await fetch(base+p,{method,headers:{Authorization:'Bearer '+token,...(data instanceof FormData?{}:{'Content-Type':'application/json'})},body:data===undefined?undefined:data instanceof FormData?data:JSON.stringify(data),signal:AbortSignal.timeout(30000)});const b=await r.json();assert.equal(b.success,true,p+': '+b.msg);return b.data}
function sql(q){assert.match(q,/^SELECT\b/);assert.ok(!q.includes(';'));const r=spawnSync('docker',['exec','ls-aegis-mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B ls_aegis -e "$1"','sh',q],{encoding:'utf8',timeout:20000});assert.equal(r.status,0);return r.stdout.trim()}
const phase=process.argv[2]
if(phase==='prepare'){
 assert.ok(!state.objects.profileUser);const r=await call('/system/role',{method:'POST',data:{name:prefix+'-PROFILE',code:run.toLowerCase()+'_profile',dataScope:4,sort:999,description:prefix}});const roleId=String(typeof r==='object'?r.id:r);reg('profileRole','role',roleId,prefix+'-PROFILE')
 const username=run.toLowerCase()+'_profile',u=await call('/system/user',{method:'POST',data:{username,nickname:prefix+'-PROFILE',gender:0,deptId:'1',roleIds:[roleId],status:1,description:prefix}});assert.ok(u.generated&&u.initialPassword);reg('profileUser','user',u.id,username)
 writeFileSync(privateFile,JSON.stringify({id:String(u.id),username,password:u.initialPassword,initial:true}),{mode:0o600});console.log('Own no-business-permission profile identity prepared; initial credential not printed')
}else if(phase==='test'){
 const account=JSON.parse(readFileSync(privateFile,'utf8'));assert.equal(account.username,run.toLowerCase()+'_profile');assert.equal(account.initial,false);assert.ok(account.token)
 const own=(p,method='GET',data)=>call(p,{method,data,token:account.token});const info=await own('/auth/user/info');assert.equal(String(info.id),account.id)
 await own('/user/profile/basic/info','PATCH',{nickname:prefix+'-PROFILE-EDIT',gender:0});assert.equal((await own('/auth/user/info')).nickname,prefix+'-PROFILE-EDIT')
 const prefs={theme:'light',menu:true,topMenu:false,sidebar:true};await own('/user/profile/ui-settings','PUT',prefs);assert.deepEqual(await own('/user/profile/ui-settings'),prefs)
 const form=new FormData();form.set('avatarFile',new Blob([readFileSync(resolve(root,'.local/r39-ui/ITLS-TEST-R39-UI-seal.png'))],{type:'image/png'}),prefix+'-avatar.png');const image=await own('/user/profile/avatar','PATCH',form);assert.ok(image.avatar)
 const avatar=sql('SELECT id FROM sys_file WHERE create_user='+account.id+" AND parent_path LIKE '%user/avatar%' AND deleted=0 ORDER BY create_time DESC LIMIT 1");assert.match(avatar,/^\d+$/);reg('profileAvatar','sys_file',avatar,prefix+'-avatar.png');assert.equal((await call('/system/user/'+account.id)).avatar,image.avatar)
 const social=await own('/user/profile/social');assert.ok(Array.isArray(social));const proof={nicknameSaved:true,uiPreferencesRoundTrip:true,avatarSaved:true,externalSocialCalls:false,boundSocialCount:social.length,normalOwnIdentity:true};state.results.push({id:run+'-PROFILE-BASIC-PREFERENCES-AVATAR',at:new Date().toISOString(),status:'PASS',backendSha256:state.currentBackendSha256,evidence:proof});save();console.log(JSON.stringify(proof))
}else if(phase==='cleanup'){
 const user=state.objects.profileUser,role=state.objects.profileRole,account=JSON.parse(readFileSync(privateFile,'utf8'));assert.equal(user.id,account.id);assert.equal((await call('/system/user/'+user.id)).username,user.marker)
 if(account.token)await call('/auth/logout',{method:'POST',token:account.token})
 await call('/system/user',{method:'DELETE',data:{ids:[user.id]}});assert.equal(Number(sql('SELECT COUNT(*) FROM sys_user WHERE id='+user.id+' AND deleted=0')),0);user.status='CLEANED'
 await call('/system/role',{method:'DELETE',data:{ids:[role.id]}});role.status='CLEANED'
 if(state.objects.profileAvatar){const id=state.objects.profileAvatar.id;await call('/system/file',{method:'DELETE',data:{ids:[id]}});await call('/system/file/recycle/'+id,{method:'DELETE'});state.objects.profileAvatar.status='CLEANED'}save()
 const preferences=Number(sql('SELECT COUNT(*) FROM sys_user_preference WHERE user_id='+user.id));assert.equal(preferences,0,'Deleting an own test user must also remove its UI preferences');state.profileCleanup={userDeleted:true,roleDeleted:true,avatarPurged:true,preferencesRemaining:preferences};save();console.log('Own profile identity, role, avatar and preferences cleaned')
}else throw new Error('Explicit prepare/test/cleanup phase required')
