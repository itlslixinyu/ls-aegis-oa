<template>
  <section class="material-summary">
    <h2>使用配件</h2>
    <div v-if="loading" class="hint">正在加载配件…</div>
    <van-cell v-else-if="failed" title="配件暂未加载" value="重试" is-link @click="load" />
    <p v-else-if="!items.length" class="hint">未登记使用配件</p>
    <article v-for="item in items" :key="item.id"><strong>{{ item.materialName }}</strong><span>× {{ item.quantity }}</span><p>{{ [item.specification,item.warehouseName].filter(Boolean).join(' · ') }}</p><p v-if="item.remark">{{ item.remark }}</p></article>
  </section>
</template>
<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { getTaskMaterials } from '@/api/engineer'
const props=defineProps({orderId:{type:[String,Number],required:true}})
const items=ref([]),loading=ref(false),failed=ref(false)
let active=true,sequence=0
async function load(){const current=++sequence;loading.value=true;failed.value=false;try{const r=await getTaskMaterials(String(props.orderId));if(active && current===sequence)items.value=r?.data||[]}catch{if(active && current===sequence)failed.value=true}finally{if(active && current===sequence)loading.value=false}}
watch(()=>props.orderId,()=>{items.value=[];load()},{immediate:true})
onBeforeUnmount(()=>{active=false;sequence++})
</script>
<style scoped>
.material-summary{margin:16px;padding:16px;background:#fff;border:1px solid #e7ecf3;border-radius:16px}h2{margin:0 0 12px;font-size:16px;color:#17243a}.hint{font-size:13px;color:#8290a5}article{border-top:1px solid #edf1f7;padding:12px 0;font-size:14px;overflow-wrap:anywhere}article>span{float:right;color:#4775d5}article p{font-size:12px;color:#8290a5;margin:8px 0 0;white-space:pre-wrap}
</style>
