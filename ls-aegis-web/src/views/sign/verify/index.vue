<template>
  <GiPageLayout>
    <a-card title="签章指纹校验" :bordered="false">
      <div class="upload-area">
        <a-upload
          :auto-upload="false"
          accept=".pdf,application/pdf"
          :show-file-list="false"
          draggable
          @change="onFileChange"
        >
          <template #upload-button>
            <div class="upload-inner">
              <icon-upload :size="28" />
              <div class="upload-title">上传签章后的 PDF 文件</div>
              <div class="upload-sub">系统会读取指纹并显示元数据校验结果</div>
            </div>
          </template>
        </a-upload>
        <div v-if="fileName" class="upload-file">
          <span>{{ fileName }}</span>
          <a-button v-if="!verifying && !hasResult" type="primary" size="mini" :loading="verifying" @click="startVerify">
            开始校验
          </a-button>
        </div>
      </div>
      <a-space style="margin-top: 16px; justify-content: center; width: 100%">
        <a-input v-model="receiptRecordId" placeholder="输入签章记录ID查询回单" allow-clear style="width: 260px" />
        <a-button type="primary" :disabled="!receiptRecordId" @click="loadReceipt(receiptRecordId)">查询回单</a-button>
      </a-space>
    </a-card>

    <a-card title="校验结果" :bordered="false" style="margin-top: 12px">
      <template #extra>
        <a-space>
          <a-button v-if="receiptRecordId" size="small" @click="loadReceipt(receiptRecordId)">查看回单</a-button>
          <a-button v-if="hasResult" size="small" @click="resetAll">重新校验</a-button>
        </a-space>
      </template>
      <a-row :gutter="16">
        <a-col :span="12">
          <div class="verify-card">
            <div class="verify-card-header">指纹校验</div>
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item v-for="row in fingerprintTable" :key="row.label" :label="row.label">
                <span :class="['kv-value', row.status]">{{ row.value }}</span>
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </a-col>
        <a-col :span="12">
          <div class="verify-card">
            <div class="verify-card-header">CA 校验</div>
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item v-for="row in caTable" :key="row.label" :label="row.label">
                <span :class="['kv-value', row.status]">{{ row.value }}</span>
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <a-modal v-model:visible="receiptOpen" title="验签回单" :width="720" :footer="false">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="回单编号">{{ receipt.receiptNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="记录ID">{{ receipt.recordId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="原文件ID">{{ receipt.originFileId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="签后文件ID">{{ receipt.signedFileId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="证书序列号">{{ receipt.serialNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="证书指纹">{{ receipt.fingerprintSha256 || '-' }}</a-descriptions-item>
        <a-descriptions-item label="验签状态">{{ receipt.verifyStatus || '-' }}</a-descriptions-item>
        <a-descriptions-item label="回单时间">{{ receipt.verifiedAt || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { getVerifyReceipt, verifyFingerprintUpload } from '@/apis/sign'

defineOptions({ name: 'FingerprintVerify' })

const file = ref<File | null>(null)
const fileName = ref('')
const receiptRecordId = ref('')
const receiptOpen = ref(false)
const receipt = ref<Record<string, any>>({})
const verifying = ref(false)
const hasResult = ref(false)
let verifyRequest = 0
let receiptRequest = 0

const fingerprint = reactive({
  result: '等待中',
  resultType: 'warning',
  user: '-',
  timestamp: '-',
  originalPdfHash: '-',
  sealHash: '-',
  currentFileHash: '-',
  pageFingerprint: '-',
  fingerprintSource: '-',
  compareTip: '-',
})

const ca = reactive({
  result: '缺少对比数据',
  resultType: 'warning',
  signer: '-',
  signTime: '-',
  currentFileHash: '-',
  certSerial: '-',
  certIssuer: '-',
  certValid: '-',
  certFingerprint: '-',
  signCertFingerprint: '-',
})

const fingerprintTable = computed(() => [
  { label: '校验结果', value: fingerprint.result, status: fingerprint.resultType },
  { label: '用户', value: fingerprint.user },
  { label: '时间戳', value: fingerprint.timestamp },
  { label: '原PDF Hash', value: fingerprint.originalPdfHash },
  { label: '签章Hash', value: fingerprint.sealHash },
  { label: '当前文件Hash', value: fingerprint.currentFileHash },
  { label: '页面指纹', value: fingerprint.pageFingerprint },
  { label: '指纹来源', value: fingerprint.fingerprintSource },
  { label: '对比提示', value: fingerprint.compareTip },
])

const caTable = computed(() => [
  { label: '校验结果', value: ca.result, status: ca.resultType },
  { label: '签名人', value: ca.signer },
  { label: '签名时间', value: ca.signTime },
  { label: '当前文件Hash', value: ca.currentFileHash },
  { label: '证书序列号', value: ca.certSerial },
  { label: '证书颁发者', value: ca.certIssuer },
  { label: '证书有效期', value: ca.certValid },
  { label: '证书指纹', value: ca.certFingerprint },
  { label: '签章时证书指纹', value: ca.signCertFingerprint },
])

const resetResult = () => {
  Object.assign(fingerprint, {
    result: '等待中',
    resultType: 'warning',
    user: '-',
    timestamp: '-',
    originalPdfHash: '-',
    sealHash: '-',
    currentFileHash: '-',
    pageFingerprint: '-',
    fingerprintSource: '-',
    compareTip: '-',
  })
  Object.assign(ca, {
    result: '缺少对比数据',
    resultType: 'warning',
    signer: '-',
    signTime: '-',
    currentFileHash: '-',
    certSerial: '-',
    certIssuer: '-',
    certValid: '-',
    certFingerprint: '-',
    signCertFingerprint: '-',
  })
}

const onFileChange = (fileList: any[]) => {
  const fileItem = fileList?.[fileList.length - 1]
  const raw = fileItem?.file as File | undefined
  if (!raw) return
  if (raw.type !== 'application/pdf' && !raw.name.toLowerCase().endsWith('.pdf')) {
    Message.warning('请上传 PDF 文件')
    return
  }
  file.value = raw
  verifyRequest += 1
  receiptRequest += 1
  verifying.value = false
  fileName.value = raw.name
  receiptRecordId.value = ''
  receiptOpen.value = false
  receipt.value = {}
  hasResult.value = false
  resetResult()
}

const resetAll = () => {
  verifyRequest += 1
  receiptRequest += 1
  verifying.value = false
  file.value = null
  fileName.value = ''
  receiptRecordId.value = ''
  receiptOpen.value = false
  receipt.value = {}
  hasResult.value = false
  resetResult()
}

const startVerify = async () => {
  if (verifying.value) return
  if (!file.value) {
    Message.warning('请先上传签章后的 PDF 文件')
    return
  }
  const selectedFile = file.value
  const request = ++verifyRequest
  verifying.value = true
  resetResult()
  fingerprint.result = '校验中...'
  fingerprint.resultType = 'warning'
  ca.result = '校验中...'
  ca.resultType = 'warning'
  try {
    const res = await verifyFingerprintUpload(selectedFile)
    if (request !== verifyRequest || selectedFile !== file.value) return
    hasResult.value = true
    fillResult(res.data || {})
  } catch {
    if (request !== verifyRequest || selectedFile !== file.value) return
    hasResult.value = false
    fingerprint.result = '校验未完成'
    fingerprint.resultType = 'warning'
    fingerprint.compareTip = '请求未完成，请恢复登录或网络后重试'
    ca.result = '校验未完成'
    ca.resultType = 'warning'
  } finally {
    if (request === verifyRequest) verifying.value = false
  }
}

const fillResult = (data: any) => {
  if (data.fingerprint) {
    const f = data.fingerprint
    Object.assign(fingerprint, {
      result: f.result || '校验通过',
      resultType: f.resultType || (f.valid === false ? 'danger' : 'success'),
      user: f.user || '-',
      timestamp: f.timestamp || '-',
      originalPdfHash: f.originalPdfHash || '-',
      sealHash: f.sealHash || '-',
      currentFileHash: f.currentFileHash || '-',
      pageFingerprint: f.pageFingerprint || '-',
      fingerprintSource: f.fingerprintSource || '-',
      compareTip: f.compareTip || '-',
    })
  }
  if (data.ca) {
    const c = data.ca
    Object.assign(ca, {
      result: c.result || (c.valid === false ? '校验失败' : '校验通过'),
      resultType: c.resultType || (c.valid === false ? 'danger' : 'success'),
      signer: c.signer || '-',
      signTime: c.signTime || '-',
      currentFileHash: c.currentFileHash || '-',
      certSerial: c.certSerial || '-',
      certIssuer: c.certIssuer || '-',
      certValid: c.certValid || '-',
      certFingerprint: c.certFingerprint || '-',
      signCertFingerprint: c.signCertFingerprint || '-',
    })
  }
  if (data.recordId) receiptRecordId.value = String(data.recordId)
}

const loadReceipt = async (recordId: string) => {
  if (!recordId) {
    Message.warning('请输入签章记录ID')
    return
  }
  const request = ++receiptRequest
  receiptOpen.value = false
  receipt.value = {}
  try {
    const res = await getVerifyReceipt(recordId)
    if (request !== receiptRequest) return
    receipt.value = res.data || {}
    receiptOpen.value = true
  } catch (e: any) {
    if (request !== receiptRequest) return
    Message.error(e?.msg || e?.message || '回单查询失败')
  }
}
</script>

<style scoped>
.upload-area {
  border: 1px dashed rgb(var(--primary-5));
  border-radius: 4px;
  padding: 28px 20px;
  text-align: center;
  background: var(--color-fill-1);
}
.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-text-2);
}
.upload-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-1);
}
.upload-sub {
  font-size: 13px;
  color: var(--color-text-3);
}
.upload-file {
  margin-top: 14px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.verify-card {
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  overflow: hidden;
}
.verify-card-header {
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--color-border-2);
  background: var(--color-fill-1);
}
.kv-value {
  font-family: Consolas, Monaco, monospace;
  word-break: break-all;
  font-size: 13px;
}
.kv-value.success { color: rgb(var(--success-6)); }
.kv-value.warning { color: rgb(var(--warning-6)); }
.kv-value.danger { color: rgb(var(--danger-6)); }
.kv-value.info { color: var(--color-text-3); }
</style>
