// 分片上传通用 hooks，支持多文件/多分片并发、暂停、恢复、取消、重试等
import { computed, onUnmounted, ref } from 'vue'
import { throttle } from 'lodash-es'
import {
  cancelUpload,
  completeMultipartUpload,
  initMultipartUpload,
  uploadPart,
} from '@/apis/system/multipart-upload'

// 文件上传任务对象类型
export interface FileTask {
  uid: string // 唯一标识
  file: File // 文件对象
  relativePath: string // 相对路径（支持文件夹结构）
  parentPath: string // 文件夹根路径
  status: 'waiting' | 'uploading' | 'paused' | 'completed' | 'failed' | 'cancelled' // 状态
  progress: number // 上传进度（0-1）
  uploadedChunks: number[] // 已上传分片编号
  totalChunks: number // 总分片数
  chunkSize: number // 分片大小（由后端返回）
  fileName: string // 文件名
  fileType: string // 文件类型
  fileSize: number // 文件大小
  fileDigest?: string // 文件指纹（国密 SM3）
  uploadId?: string // 分片上传ID
  path?: string // 文件路径（由后端返回）
  errorMessage?: string // 错误信息
  abortController?: AbortController // 请求中断控制器
  _uploading?: boolean // 标记是否正在上传（内部控制）
  _pause?: () => void // 暂停方法
  _resume?: () => void // 继续方法
  _cancel?: () => void // 取消方法
  _retryCount?: Map<number, number> // 分片重试次数记录
}

/**
 * useMultipartUploader - 通用分片上传 hooks
 * @param props.maxConcurrentFiles   最大同时上传文件数（全局并发）
 * @param props.maxConcurrentChunks  每个文件分片上传最大并发数
 * @param props.maxUploadWorkers     最大上传Worker数量（基于CPU核心数）
 * @returns 上传相关响应式状态与操作方法
 */
export function useMultipartUploader(props: {
  maxConcurrentFiles?: number
  maxConcurrentChunks?: number
  maxUploadWorkers?: number
  rootPath?: string
}) {
  // 获取CPU核心数，用于控制Worker数量
  const getCpuCores = () => {
    if (typeof navigator !== 'undefined' && navigator.hardwareConcurrency) {
      return navigator.hardwareConcurrency
    }
    return 2 // 默认2个核心
  }

  // 所有上传任务列表
  const fileTasks = ref<FileTask[]>([])
  // 当前正在上传的文件数量
  const uploadingCount = computed(() => fileTasks.value.filter((t) => t.status === 'uploading').length)
  // 最大并发上传文件数
  const maxConcurrent = computed(() => props.maxConcurrentFiles ?? getCpuCores())
  // 每个文件分片上传最大并发数
  const maxChunkConcurrent = computed(() => props.maxConcurrentChunks ?? getCpuCores())
  // 最大上传Worker数量
  const maxUploadWorkers = computed(() => props.maxUploadWorkers ?? getCpuCores() / 2)

  // 本地队列管理
  const uploadQueue = ref<Array<{ task: FileTask, chunkNumber: number }>>([])
  const activeUploads = ref<Set<string>>(new Set()) // 正在上传的分片ID集合

  const digestCalculatingTaskUid = ref<string | null>(null) // 文件指纹（SM3）计算中的任务 ID
  const performanceStats = ref<{
    digestStartTime: number
    digestEndTime: number
    uploadStartTime: number
    uploadEndTime: number
    totalTime: number
  } | null>(null)

  // 文件指纹 Worker（SM3）
  let digestWorker: Worker | null = null

  /** 节流的进度更新函数 */
  const updateTaskProgress = throttle((task: FileTask, totalChunks: number) => {
    const currentFinishedChunks = task.uploadedChunks.length
    if (totalChunks > 0) {
      task.progress = Number(Math.min(currentFinishedChunks / totalChunks, 1).toFixed(2))
    } else {
      task.progress = 0
    }
  }, 150)

  /**
   * 初始化文件指纹 Worker（SM3）
   */
  function initDigestWorker() {
    if (typeof Worker !== 'undefined' && !digestWorker) {
      digestWorker = new Worker(new URL('../../utils/file-digest-worker.ts', import.meta.url), { type: 'module' })
      digestWorker.onmessage = function (e) {
        const { type, taskId, digest, error } = e.data
        const value = digest

        if (type === 'complete' && value) {
          const task = fileTasks.value.find((t) => t.uid === taskId)
          if (task) {
            task.fileDigest = value
            digestCalculatingTaskUid.value = null
          }
        } else if (type === 'error') {
          console.error('文件指纹计算失败:', error)
          digestCalculatingTaskUid.value = null
        }
      }
    }
  }

  /**
   * 计算文件指纹（国密 SM3，Web Worker）
   */
  function calcFileDigest(file: File, taskUid: string): Promise<string> {
    return new Promise((resolve, reject) => {
      if (!digestWorker) {
        initDigestWorker()
      }

      if (digestWorker) {
        digestCalculatingTaskUid.value = taskUid

        performanceStats.value = {
          digestStartTime: Date.now(),
          digestEndTime: 0,
          uploadStartTime: 0,
          uploadEndTime: 0,
          totalTime: 0,
        }

        let blockSize: number
        if (file.size > 500 * 1024 * 1024) {
          blockSize = 50 * 1024 * 1024
        } else if (file.size > 100 * 1024 * 1024) {
          blockSize = 30 * 1024 * 1024
        } else if (file.size > 10 * 1024 * 1024) {
          blockSize = 10 * 1024 * 1024
        } else {
          blockSize = 5 * 1024 * 1024
        }

        digestWorker.postMessage({
          file,
          taskId: taskUid,
          blockSize,
        })

        const handleComplete = (e: MessageEvent) => {
          const { type, taskId, digest, error } = e.data
          if (type === 'error' && taskId === taskUid) {
            digestWorker?.removeEventListener('message', handleComplete)
            reject(error || new Error('文件指纹计算失败'))
            return
          }
          if (type === 'complete' && taskId === taskUid) {
            digestWorker?.removeEventListener('message', handleComplete)
            if (performanceStats.value) {
              performanceStats.value.digestEndTime = Date.now()
            }
            resolve(digest)
          }
        }

        digestWorker.addEventListener('message', handleComplete)
      } else {
        reject(new Error('Web Worker not supported'))
      }
    })
  }

  /**
   * 添加分片到上传队列
   */
  function addChunkToQueue(task: FileTask, chunkNumber: number) {
    const chunkId = `${task.uid}-${chunkNumber}`
    if (!activeUploads.value.has(chunkId)) {
      uploadQueue.value.push({ task, chunkNumber })
      processUploadQueue()
    }
  }

  /**
   * 处理上传队列
   */
  function processUploadQueue() {
    const sortedQueue = [...uploadQueue.value].sort((a, b) => {
      const aProgress = a.task.uploadedChunks.length / a.task.totalChunks
      const bProgress = b.task.uploadedChunks.length / b.task.totalChunks
      return bProgress - aProgress
    })

    while (sortedQueue.length > 0 && activeUploads.value.size < maxUploadWorkers.value) {
      const { task, chunkNumber } = sortedQueue.shift()!
      const chunkId = `${task.uid}-${chunkNumber}`

      if (task.status === 'uploading' && !activeUploads.value.has(chunkId)) {
        activeUploads.value.add(chunkId)
        uploadChunk(task, chunkNumber)

        const index = uploadQueue.value.findIndex((item) =>
          item.task.uid === task.uid && item.chunkNumber === chunkNumber,
        )
        if (index > -1) {
          uploadQueue.value.splice(index, 1)
        }
      }
    }
  }

  /**
   * 上传单个分片
   */
  async function uploadChunk(task: FileTask, chunkNumber: number) {
    const chunkId = `${task.uid}-${chunkNumber}`

    try {
      const start = (chunkNumber - 1) * task.chunkSize
      const end = Math.min(start + task.chunkSize, task.fileSize)
      const chunkBlob = task.file.slice(start, end)

      if (!task.abortController) {
        task.abortController = new AbortController()
      }

      const res = await uploadPart({
        uploadId: task.uploadId!,
        partNumber: chunkNumber,
        file: chunkBlob,
        path: task.path!,
      }, task.abortController.signal)

      if (res.data && res.data.success) {
        if (!task.uploadedChunks.includes(chunkNumber)) {
          task.uploadedChunks.push(chunkNumber)
        }

        updateTaskProgress(task, task.totalChunks)

        if (task.uploadedChunks.length >= task.totalChunks) {
          await completeMultipartUpload({
            uploadId: task.uploadId!,
          })
          task.status = 'completed'
          task.progress = 1
          startNextTasks()
        }
      } else {
        const errorMessage = res.data?.errorMessage || '分片上传失败'
        throw new Error(`分片${chunkNumber}上传失败: ${errorMessage}`)
      }
    } catch (error) {
      if (
        (error instanceof Error && error.name === 'AbortError')
        || task.status === 'cancelled'
        || task.status === 'paused'
      ) {
        return
      }

      console.error(`分片上传失败: ${task.fileName} - 分片${chunkNumber}`, error)

      const isNetworkError = error instanceof TypeError
        || (error as any)?.message?.includes('Network')
        || (error as any)?.message?.includes('fetch')

      const isServerError = (error as any)?.response?.status >= 500
        || (error as any)?.response?.status === 429

      if (isNetworkError || isServerError) {
        if (!task._retryCount) {
          task._retryCount = new Map()
        }

        const currentRetryCount = task._retryCount.get(chunkNumber) || 0
        const maxRetries = 3

        if (currentRetryCount < maxRetries) {
          task._retryCount.set(chunkNumber, currentRetryCount + 1)
          setTimeout(() => {
            if (task.status === 'uploading' && !activeUploads.value.has(chunkId)) {
              addChunkToQueue(task, chunkNumber)
            }
          }, 2000 * (currentRetryCount + 1))
        } else {
          task.status = 'failed'
          task.errorMessage = `分片 ${chunkNumber} 重试次数超过限制`
          uploadQueue.value = uploadQueue.value.filter((item) => item.task.uid !== task.uid)
          const activeChunkIds = Array.from(activeUploads.value).filter((id) => id.startsWith(task.uid))
          activeChunkIds.forEach((id) => activeUploads.value.delete(id))
          startNextTasks()
        }
      } else {
        task.status = 'failed'
        task.errorMessage = (error as Error)?.message || '上传失败'
        uploadQueue.value = uploadQueue.value.filter((item) => item.task.uid !== task.uid)
        const activeChunkIds = Array.from(activeUploads.value).filter((id) => id.startsWith(task.uid))
        activeChunkIds.forEach((id) => activeUploads.value.delete(id))
        startNextTasks()
      }
    } finally {
      activeUploads.value.delete(chunkId)
      processUploadQueue()
    }
  }

  /**
   * 分片上传核心逻辑
   */
  async function uploadFileTask(task: FileTask) {
    try {
      if (!task.uploadId) {
        if (!task.fileDigest) {
          task.fileDigest = await calcFileDigest(task.file, task.uid)
        }

        const parentPath = task.parentPath && task.parentPath !== '' ? task.parentPath : '/'

        const res = await initMultipartUpload({
          fileName: task.fileName,
          fileSize: task.fileSize,
          fileDigest: task.fileDigest,
          parentPath,
          metaData: {
            contentType: task.fileType,
            originalName: task.fileName,
          },
        })

        if (res && res.data) {
          task.uploadId = res.data.uploadId
          task.chunkSize = res.data.partSize
          task.path = res.data.path

          if (res.data.uploadedPartNumbers && res.data.uploadedPartNumbers.length > 0) {
            task.uploadedChunks = [...res.data.uploadedPartNumbers]
            const totalChunks = Math.ceil(task.fileSize / task.chunkSize)
            updateTaskProgress(task, totalChunks)
          }
        } else {
          task.status = 'failed'
          return
        }
      }

      const totalChunks = Math.ceil(task.fileSize / task.chunkSize)
      task.totalChunks = totalChunks

      if (task.uploadedChunks.length > 0) {
        updateTaskProgress(task, task.totalChunks)
      } else {
        task.uploadedChunks = []
        task.progress = 0
      }

      for (let i = 1; i <= totalChunks; i++) {
        if (!task.uploadedChunks.includes(i)) {
          addChunkToQueue(task, i)
        }
      }

      task._pause = () => {
        task.status = 'paused'
        uploadQueue.value = uploadQueue.value.filter((item) => item.task.uid !== task.uid)
        const activeChunkIds = Array.from(activeUploads.value).filter((id) => id.startsWith(task.uid))
        activeChunkIds.forEach((id) => activeUploads.value.delete(id))
      }

      task._resume = () => {
        if (task.status === 'paused') {
          task.status = 'uploading'
          for (let i = 1; i <= task.totalChunks; i++) {
            if (!task.uploadedChunks.includes(i)) {
              addChunkToQueue(task, i)
            }
          }
        }
      }

      task._cancel = () => {
        task.status = 'cancelled'
        if (task.abortController) {
          task.abortController.abort()
        }
        uploadQueue.value = uploadQueue.value.filter((item) => item.task.uid !== task.uid)
        const activeChunkIds = Array.from(activeUploads.value).filter((id) => id.startsWith(task.uid))
        activeChunkIds.forEach((id) => activeUploads.value.delete(id))
        if (task.uploadId) {
          cancelUpload({ uploadId: task.uploadId })
        }
      }
    } catch {
      task.status = 'failed'
      startNextTasks()
    }
  }

  /**
   * 启动下一个可用的上传任务
   */
  function startNextTasks() {
    let available = maxConcurrent.value - uploadingCount.value
    for (const task of fileTasks.value) {
      if (available <= 0) break
      if ((task.status === 'waiting' || task.status === 'uploading') && !task._uploading) {
        task.status = 'uploading'
        task._uploading = true
        available--
        uploadFileTask(task)
      }
    }
  }

  /**
   * 全部开始上传
   */
  function startAllUpload() {
    for (const task of fileTasks.value) {
      if (task.status === 'waiting' || task.status === 'paused') {
        task._uploading = false
        if (task.status === 'paused') {
          task.status = 'uploading'
        }
      }
    }
    startNextTasks()
  }

  /**
   * 添加文件/文件夹到上传队列
   */
  function addFiles(files: File[], parentPath: string, isFolder = false) {
    const validFiles = files.filter((file) => {
      if (!file) {
        return false
      }
      if (file.size === 0) {
        return false
      }
      if (!file.name || file.name.trim() === '') {
        return false
      }
      return true
    })

    if (validFiles.length === 0) {
      return
    }

    for (const file of validFiles) {
      const relativePath = (file as any).webkitRelativePath || '/'
      let parent = ''

      if (isFolder) {
        if (relativePath && relativePath !== '/') {
          parent = props.rootPath || parentPath || '/'
          if (parent.length > 1 && parent.endsWith('/')) {
            parent = parent.slice(0, -1)
          }

          const pathParts = relativePath.split('/')
          pathParts.pop()
          const folderPath = pathParts.join('/')

          if (folderPath) {
            parent = `${parent}/${folderPath}`
          }
        } else {
          parent = props.rootPath || parentPath || '/'
        }
      } else {
        parent = props.rootPath || parentPath || '/'
      }

      if (parent.length > 1 && parent.endsWith('/')) {
        parent = parent.slice(0, -1)
      }

      if (parent.startsWith('//')) {
        parent = parent.substring(1)
      }

      const task: FileTask = {
        uid: `${file.name}-${file.size}-${Date.now()}-${Math.random()}`,
        file,
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size,
        relativePath,
        parentPath: parent,
        status: 'waiting',
        progress: 0,
        uploadedChunks: [],
        totalChunks: 0,
        chunkSize: 0,
        fileDigest: '',
        path: '',
        errorMessage: '',
        abortController: new AbortController(),
        _retryCount: new Map(),
      }

      calcFileDigest(file, task.uid).then((digest) => {
        task.fileDigest = digest
      }).catch(() => {
        task.status = 'failed'
        task.errorMessage = '文件指纹（SM3）计算失败'
      })

      fileTasks.value.push(task)
    }
  }

  function pauseTask(task: FileTask) {
    task._pause?.()
  }

  function resumeTask(task: FileTask) {
    task._resume?.()
    if (task.status === 'paused') {
      task._uploading = false
      startNextTasks()
    }
  }

  function cancelTask(task: FileTask) {
    if (task.abortController) {
      task.abortController.abort()
    }
    task._cancel?.()
  }

  function startTask(task: FileTask) {
    if (task.status === 'waiting') {
      task.status = 'uploading'
      task._uploading = false
      startNextTasks()
    }
  }

  function retryTask(task: FileTask) {
    if (task.status === 'failed') {
      task.status = 'uploading'
      task.progress = 0
      task._uploading = false
      task._retryCount = new Map()
      task.errorMessage = ''
      task.abortController = new AbortController()
      startNextTasks()
    }
  }

  function clearAllTasks() {
    fileTasks.value = []
  }

  function removeTask(task: FileTask) {
    if (task.status === 'uploading' || task.status === 'waiting' || task.status === 'paused') {
      task._cancel?.()
    }
    uploadQueue.value = uploadQueue.value.filter((item) => item.task.uid !== task.uid)
    const activeChunkIds = Array.from(activeUploads.value).filter((id) => id.startsWith(task.uid))
    activeChunkIds.forEach((id) => activeUploads.value.delete(id))
    fileTasks.value = fileTasks.value.filter((t) => t.uid !== task.uid)
  }

  function formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return `${Number.parseFloat((bytes / k ** i).toFixed(2))} ${sizes[i]}`
  }

  onUnmounted(() => {
    fileTasks.value.forEach((task) => {
      if (task.status === 'uploading' || task.status === 'waiting' || task.status === 'paused') {
        pauseTask(task)
      }
    })

    if (digestWorker) {
      digestWorker.terminate()
      digestWorker = null
    }
  })

  return {
    fileTasks,
    uploadingCount,
    maxConcurrent,
    maxChunkConcurrent,
    uploadFileTask,
    startNextTasks,
    startAllUpload,
    addFiles,
    pauseTask,
    resumeTask,
    cancelTask,
    startTask,
    retryTask,
    clearAllTasks,
    removeTask,
    formatFileSize,
    digestCalculatingTaskUid,
  }
}
