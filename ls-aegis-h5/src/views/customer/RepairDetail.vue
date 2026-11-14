<template>
  <div class="page">
    <van-nav-bar
      title="维修详情"
      left-text="返回"
      left-arrow
      @click-left="onBack"
    />

    <!-- 报修信息 -->
    <van-cell-group inset title="报修信息" class="section">
      <van-cell title="报修编号">
        <template #value>
          <span class="primary-text">{{ apply.repairNo || '-' }}</span>
        </template>
      </van-cell>
      <van-cell title="报修状态">
        <template #value>
          <van-tag :type="getApplyTagType(apply.status)" size="medium">
            {{ getApplyStatusText(apply.status) }}
          </van-tag>
        </template>
      </van-cell>
      <van-cell title="提交人" :value="apply.reporterName || '历史记录'" />
      <van-cell title="联系人" :value="apply.contactName || '-'" />
      <van-cell title="联系电话" :value="apply.contactPhone || '-'" />
      <van-cell v-if="apply.reportAddress" title="报修地址" :value="apply.reportAddress" />
      <van-cell title="故障类型" :value="apply.faultType || '-'" />
      <van-cell title="紧急程度">
        <template #value>
          <span :style="{ color: getUrgentColor(apply.urgentLevel) }">
            {{ getUrgentText(apply.urgentLevel) }}
          </span>
        </template>
      </van-cell>
      <van-cell title="报修来源">
        <template #value>{{ sourceMap[apply.source] || apply.source || '-' }}</template>
      </van-cell>
      <van-cell title="报修时间" :value="formatDateTime(apply.applyTime) || '-'" />
      <van-cell title="故障描述">
        <template #value>
          <div class="desc-text">{{ apply.faultDesc || '-' }}</div>
        </template>
      </van-cell>
      <van-cell v-if="apply.remark" title="备注">
        <template #value>
          <div class="desc-text">{{ apply.remark }}</div>
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 现场照片 -->
    <div v-if="applyPhotoList.length" class="section">
      <div class="block-title">现场照片</div>
      <div class="photo-grid">
        <EomsAuthImage
          v-for="(photo, index) in applyPhotoList"
          :key="index"
          :src="photo"
          width="100"
          height="100"
          fit="cover"
          radius="6"
          @click="previewApplyPhotos(index)"
        />
      </div>
    </div>

    <!-- 工单信息（已转工单时展示） -->
    <van-cell-group v-if="order" inset title="维修工单" class="section">
      <van-cell title="工单编号">
        <template #value>
          <span class="primary-text">{{ order.orderNo || '-' }}</span>
        </template>
      </van-cell>
      <van-cell title="工单状态">
        <template #value>
          <van-tag :type="getOrderTagType(order.status)" size="medium">
            {{ getOrderStatusText(order.status) }}
          </van-tag>
        </template>
      </van-cell>
      <van-cell title="接单时间" :value="formatDateTime(order.acceptTime)" />
      <van-cell title="开始时间" :value="formatDateTime(order.startTime)" />
      <van-cell title="完成时间" :value="formatDateTime(order.finishTime)" />
    </van-cell-group>

    <!-- 维修进度（工单已存在时展示） -->
    <div v-if="order" class="section">
      <div class="block-title">维修进度</div>
      <van-steps direction="vertical" :active="Math.max(0, progressSteps.length - 1)">
        <van-step v-for="step in progressSteps" :key="step.title">
          <h4>{{ step.title }}</h4>
          <p>{{ step.text }}</p>
        </van-step>
      </van-steps>
    </div>

    <!-- 维修记录 -->
    <div v-if="records && records.length" class="section">
      <div class="block-title">维修记录（{{ records.length }}条）</div>
      <div
        v-for="record in records"
        :key="record.id"
        class="record-card"
      >
        <div class="record-card-header">
          <van-tag type="primary" plain size="mini">{{ record.createBy || '工程师' }}</van-tag>
          <span class="record-time">{{ formatDateTime(record.createTime) }}</span>
        </div>
        <div class="record-card-body">
          <div v-if="record.faultReason"><span class="rec-label">故障原因：</span>{{ record.faultReason }}</div>
          <div v-if="record.checkProcess"><span class="rec-label">检测过程：</span>{{ record.checkProcess }}</div>
          <div v-if="record.solution"><span class="rec-label">解决方案：</span>{{ record.solution }}</div>
          <div v-if="record.result"><span class="rec-label">维修结果：</span>{{ record.result }}</div>
          <!-- 维修中照片 -->
          <div v-if="getProcessingPhotos(record).length" class="record-photos">
            <div class="photo-label">维修中照片：</div>
            <div class="photo-grid record-photo-grid">
              <EomsAuthImage
                v-for="(photo, pi) in getProcessingPhotos(record)"
                :key="'p' + pi"
                :src="photo"
                width="80"
                height="80"
                fit="cover"
                radius="4"
                @click="previewPhotos(getProcessingPhotos(record), pi)"
              />
            </div>
          </div>
          <!-- 维修完成照片 -->
          <div v-if="getCompletedPhotos(record).length" class="record-photos">
            <div class="photo-label">维修完成照片：</div>
            <div class="photo-grid record-photo-grid">
              <EomsAuthImage
                v-for="(photo, ci) in getCompletedPhotos(record)"
                :key="'c' + ci"
                :src="photo"
                width="80"
                height="80"
                fit="cover"
                radius="4"
                @click="previewPhotos(getCompletedPhotos(record), ci)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 待确认完成提示 -->
    <van-cell-group v-if="customerConfirmation?.confirmed" inset title="客户已确认维修结果" class="section">
      <van-cell title="确认时间" :value="formatDateTime(customerConfirmation.confirmedAt)" />
      <van-cell title="确认备注" :value="customerConfirmation.reason || '未填写'" />
    </van-cell-group>
    <div v-if="canConfirm" class="confirm-tip">
      <van-notice-bar
        wrapable
        :scrollable="false"
        text="工程师已完成维修，请核实并确认结果。确认后可在本页查看确认时间和备注。"
      />
    </div>

    <!-- 底部操作按钮 -->
    <div v-if="canConfirm" class="bottom-bar">
      <van-button round block type="primary" :disabled="confirmSubmitting" @click="showConfirm = true">
        确认维修结果
      </van-button>
    </div>

    <!-- 确认弹窗 -->
    <van-dialog
      v-model:show="showConfirm"
      title="确认维修完成"
      show-cancel-button
      :before-close="beforeConfirmClose"
    >
      <div class="dialog-content">
        <van-field
          v-model="confirmRemark"
          type="textarea"
          label="确认备注"
          placeholder="可填写确认备注（选填）"
          rows="2"
          autosize
          maxlength="200"
          show-word-limit
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import EomsAuthImage from '@/components/EomsAuthImage.vue'
import { openEomsImagePreview } from '@/utils/eomsFile'
import { getRepairDetail, confirmRepair } from '@/api/customer'
import { toBizId } from '@/utils/biz-id'
import { buildRepairProgress } from '@/utils/repair-progress'
import {
  getApplyStatusText,
  getOrderStatusText,
  getUrgentText,
  getUrgentColor,
  formatDateTime,
  applyStatusMap,
  orderStatusMap,
  sourceMap
} from '@/utils/dict'

const route = useRoute()
const router = useRouter()

const apply = ref({})
const order = ref(null)
const records = ref([])
const loading = ref(false)
const showConfirm = ref(false)
const confirmRemark = ref('')
const confirmSubmitting = ref(false)
const customerConfirmation = ref(null)
const canConfirm = computed(() => order.value && ['WAIT_SETTLEMENT', 'FINISHED'].includes(order.value.status) && !customerConfirmation.value?.confirmed)

/** 报修现场照片列表（逗号分隔字符串转数组） */
const applyPhotoList = computed(() => {
  const src = apply.value.photoUrls || apply.value.photos
  if (!src) return []
  return src.split(',').filter(Boolean)
})

/** 点击预览报修现场照片 */
async function previewApplyPhotos(startIndex) {
  await openEomsImagePreview(applyPhotoList.value, startIndex)
}

/** 将逗号分隔的照片字符串转为数组（统一处理URL） */
function parsePhotos(photosStr) {
  if (!photosStr) return []
  return String(photosStr).split(',').filter(Boolean).map(url => url.trim())
}

/** 获取维修中照片 */
function getProcessingPhotos(record) {
  return parsePhotos(record?.processingPhotoUrls || record?.processingPhotos)
}

/** 获取维修完成照片 */
function getCompletedPhotos(record) {
  return parsePhotos(record?.completedPhotoUrls || record?.completedPhotos)
}

/** 预览通用照片 */
async function previewPhotos(photos, startIndex) {
  await openEomsImagePreview(photos, startIndex)
}

const progressSteps = computed(() => buildRepairProgress(apply.value, order.value, customerConfirmation.value, formatDateTime))

/** 获取报修状态Tag类型 */
function getApplyTagType(status) {
  return applyStatusMap[status]?.tagType || 'default'
}

/** 获取工单状态Tag类型 */
function getOrderTagType(status) {
  return orderStatusMap[status]?.tagType || 'default'
}

/** 加载维修详情 */
async function loadDetail() {
  const id = toBizId(route.params.id)
  if (!id) {
    showFailToast('缺少报修ID')
    return
  }
  loading.value = true
  try {
    const res = await getRepairDetail(id)
    if (res.data) {
      apply.value = res.data.apply || {}
      order.value = res.data.order || null
      records.value = res.data.records || []
      customerConfirmation.value = res.data.customerConfirmation || null
    }
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

/** 确认完成弹窗关闭前回调 */
async function beforeConfirmClose(action) {
  if (action !== 'confirm') return true
  if (confirmSubmitting.value) return false
  if (!order.value) {
    showFailToast('工单信息缺失')
    return false
  }
  confirmSubmitting.value = true
  try {
    const res = await confirmRepair({
      orderId: toBizId(order.value.id),
      remark: confirmRemark.value || '客户确认完成'
    })
    if (res?.data?.confirmed === true) {
      customerConfirmation.value = res.data
      showSuccessToast('已确认维修结果')
      // 刷新详情
      await loadDetail()
      return true
    }
    showFailToast('确认尚未保存，请稍后重试')
    return false
  } catch (e) {
    return false
  } finally {
    confirmSubmitting.value = false
  }
}

function onBack() {
  router.back()
}

// 初始化加载
loadDetail()
</script>

<style scoped>
.section {
  margin-top: 12px;
}

.block-title {
  font-size: 14px;
  color: #969799;
  padding: 16px 16px 8px;
  font-weight: 500;
}

.primary-text {
  color: #1989fa;
  font-weight: 600;
}

.desc-text {
  color: #323233;
  word-break: break-all;
  white-space: pre-wrap;
  line-height: 1.5;
}

/* 维修记录卡片样式 */
.record-card {
  margin: 0 16px 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.record-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f7f8fa 0%, #eef2f7 100%);
  border-bottom: 1px solid #ebedf0;
}

.record-time {
  font-size: 12px;
  color: #969799;
}

.record-card-body {
  padding: 12px 16px;
  font-size: 13px;
  color: #646566;
  line-height: 1.8;
}

.record-card-body > div {
  margin-bottom: 6px;
}

.rec-label {
  color: #969799;
}

.record-photos {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #ebedf0;
}

.photo-label {
  font-size: 12px;
  color: #969799;
  margin-bottom: 6px;
}

.confirm-tip {
  margin-top: 12px;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px env(safe-area-inset-bottom);
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
  z-index: 10;
}

.dialog-content {
  padding: 12px 0;
}

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 16px;
}

/* 维修记录内的照片网格去掉多余的padding（卡片body已自带padding） */
.record-photo-grid {
  padding: 4px 0 0;
}
</style>
