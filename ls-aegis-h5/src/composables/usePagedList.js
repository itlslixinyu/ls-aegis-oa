import { ref, onBeforeUnmount } from 'vue'

/** Page generation prevents old filter responses replacing the current list. */
export function usePagedList(fetchPage, size = 10) {
  const list=ref([]),loading=ref(false),refreshing=ref(false),finished=ref(false),failed=ref(false)
  let page=1,generation=0,pending=-1,active=true
  async function load() {
    if(!active || pending===generation || finished.value)return
    const current=generation;pending=current;loading.value=true;failed.value=false
    try {
      const response=await fetchPage({page,size})
      if(!active || current!==generation)return
      const rows=response?.data?.list
      if(!Array.isArray(rows))throw Error('Invalid page response')
      list.value.push(...rows)
      const total=response?.data?.total
      finished.value=rows.length<size || (total!=null && Number.isFinite(Number(total)) && list.value.length>=Number(total))
      if(!finished.value)page++
    } catch {
      if(active && current===generation)failed.value=true
    } finally {
      if(active && current===generation){pending=-1;loading.value=false;refreshing.value=false}
    }
  }
  function refresh(){generation++;pending=-1;page=1;list.value=[];finished.value=false;return load()}
  onBeforeUnmount(()=>{active=false;generation++})
  return {list,loading,refreshing,finished,failed,load,refresh}
}
