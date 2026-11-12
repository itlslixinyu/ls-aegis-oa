<template>
  <section class="material-section" aria-label="使用配件">
    <header><h2><van-icon name="apps-o" />使用配件</h2><span>选填</span></header>
    <p class="section-hint">按实际使用情况登记，费用由后台核对。</p>
    <van-radio-group :model-value="used" direction="horizontal" class="material-choice" @update:model-value="changeUsed">
      <van-radio :name="false">未使用配件</van-radio><van-radio :name="true">使用了配件</van-radio>
    </van-radio-group>
    <template v-if="used">
      <article v-for="(line,index) in modelValue" :key="line.key" class="material-line">
        <div class="line-head"><strong>配件 {{ index + 1 }}</strong><van-button native-type="button" size="mini" plain type="danger" @click="remove(index)">移除</van-button></div>
        <van-field :model-value="line.materialName" label="配件" placeholder="搜索并选择配件" readonly is-link :rules="[{required:true,message:'请选择配件'}]" @click="openGoods(index)" />
        <van-field :model-value="line.warehouseName" label="出库仓库" placeholder="选择仓库" readonly is-link :rules="[{required:true,message:'请选择仓库'}]" @click="openWarehouses(index)" />
        <van-field :model-value="String(line.quantity || '')" label="使用数量" :rules="[{validator:()=>validQuantity(line.quantity),message:'数量需大于0，最多两位小数'}]">
          <template #input><van-stepper v-model="line.quantity" :min="0.01" :max="99999999.99" :decimal-length="2" :integer="false" /><span class="unit">{{ line.unit || '个' }}</span></template>
        </van-field>
        <van-field v-model="line.remark" label="配件备注" placeholder="选填，如安装位置" maxlength="500" />
      </article>
      <van-button block plain round native-type="button" type="primary" icon="plus" :disabled="modelValue.length >= 20" @click="add">添加配件</van-button>
    </template>
    <van-popup v-model:show="showGoods" position="bottom" round :style="{height:'65%'}" closeable>
      <div class="picker-heading">选择配件</div><van-search v-model="keyword" placeholder="输入配件名称或编号" @update:model-value="search" />
      <div v-if="fetching" class="picker-message"><van-loading size="24">加载中</van-loading></div>
      <div v-else-if="failed" class="picker-message"><p>配件暂未加载</p><van-button native-type="button" size="small" @click="fetchOptions">重试</van-button></div>
      <van-empty v-else-if="!goods.length" description="未找到配件，请更换关键词或由后台维护商品档案" />
      <van-cell v-for="item in goods" :key="item.id" :title="item.name" :label="[item.spec,item.unit].filter(Boolean).join(' · ')" is-link @click="chooseGood(item)" />
    </van-popup>
    <van-action-sheet v-model:show="showWarehouses" :actions="warehouseActions" title="选择出库仓库" cancel-text="取消" close-on-click-action @select="chooseWarehouse" />
  </section>
</template>
<script setup>
import { computed, ref, onBeforeUnmount } from 'vue'
import { showConfirmDialog, showFailToast, closeDialog } from 'vant'
import { getMaterialOptions } from '@/api/engineer'
const props = defineProps({ orderId: { type:String, default:'' }, modelValue:{type:Array,required:true} })
const emit = defineEmits(['update:modelValue'])
const used=ref(props.modelValue.length>0),showGoods=ref(false),showWarehouses=ref(false),keyword=ref(''),goods=ref([]),warehouses=ref([]),fetching=ref(false),failed=ref(false)
const warehouseActions=computed(()=>warehouses.value.map(item=>({name:item.name,id:item.id})))
let selected=-1,sequence=0,active=true,timer,key=0,dialogOpen=false
const validQuantity=value=>/^\d{1,8}(\.\d{1,2})?$/.test(String(value)) && Number(value)>0
function add(){if(props.modelValue.length<20)emit('update:modelValue',[...props.modelValue,{key:++key,materialId:'',materialName:'',warehouseId:'',warehouseName:'',quantity:1,unit:'个',remark:''}])}
async function changeUsed(next){
  if(!next && props.modelValue.length){try{dialogOpen=true;await showConfirmDialog({title:'不使用配件',message:'清除本次尚未提交的配件填写内容？'})}catch{return}finally{dialogOpen=false}}
  if(!active)return
  used.value=next
  if(next && !props.modelValue.length)add()
  if(!next)emit('update:modelValue',[])
}
function remove(index){emit('update:modelValue',props.modelValue.filter((_,i)=>i!==index));if(props.modelValue.length===1)used.value=false}
async function fetchOptions(){
  if(!props.orderId || !active)return
  const current=++sequence;fetching.value=true;failed.value=false
  try{const res=await getMaterialOptions(props.orderId,keyword.value);if(!active || current!==sequence)return;goods.value=res?.data?.goods||[];warehouses.value=res?.data?.warehouses||[]}
  catch{if(active && current===sequence)failed.value=true}
  finally{if(active && current===sequence)fetching.value=false}
}
function search(){clearTimeout(timer);sequence++;fetching.value=true;timer=setTimeout(fetchOptions,300)}
function openGoods(index){selected=index;showGoods.value=true;keyword.value='';fetchOptions()}
async function openWarehouses(index){selected=index;await fetchOptions();if(!active)return;if(failed.value)return;if(!warehouses.value.length){showFailToast('暂无启用仓库，请由后台维护');return}showWarehouses.value=true}
function chooseGood(item){const line=props.modelValue[selected];if(!line)return;line.materialId=item.id;line.materialName=item.name;line.unit=item.unit||'个';showGoods.value=false;if(warehouses.value.length===1){line.warehouseId=warehouses.value[0].id;line.warehouseName=warehouses.value[0].name}}
function chooseWarehouse(item){const line=props.modelValue[selected];if(line){line.warehouseId=item.id;line.warehouseName=item.name}}
onBeforeUnmount(()=>{active=false;sequence++;clearTimeout(timer);if(dialogOpen)closeDialog()})
</script>
<style scoped>
.material-section { margin:16px; padding:16px; border:1px solid #e7ecf3; border-radius:16px; background:#fff; }
header,.line-head { display:flex; align-items:center; justify-content:space-between; gap:12px; }
h2 { display:flex;align-items:center;gap:8px;margin:0;color:#17243a;font-size:16px;line-height:24px; } h2 .van-icon{color:#4775d5;font-size:20px}
header>span,.section-hint,.unit{font-size:12px;color:#8290a5}.section-hint{line-height:1.7;margin:8px 0 16px}.material-choice{font-size:14px;gap:10px}
.material-line{margin:16px 0;padding:12px 0;border-top:1px solid #edf1f7}.line-head{padding:0 4px 8px;font-size:14px}.unit{margin-left:8px}
.material-line :deep(.van-field__label){width:70px}.material-line :deep(.van-cell){padding-left:4px;padding-right:4px}.material-line :deep(.van-stepper__input){width:60px}
.picker-heading{text-align:center;padding:20px 44px 12px;font-size:16px;font-weight:600}.picker-message{text-align:center;padding:32px;color:#8290a5}
</style>
