import { sm3 } from 'sm-crypto'

/**
 * 文件指纹 Worker：国密 SM3。
 * 分块并行计算块摘要后，再对块摘要十六进制串拼接做 SM3（便于大文件）。
 * 完成消息字段：digest。
 */
if (typeof globalThis !== 'undefined') {
  globalThis.addEventListener('message', (event) => {
    const { file, taskId, blockSize } = event.data

    if (file && taskId && blockSize) {
      calculateFileSm3(file, taskId, blockSize).catch((error) => {
        globalThis.postMessage({
          type: 'error',
          taskId,
          error: error instanceof Error ? error.message : String(error),
        })
      })
    } else {
      globalThis.postMessage({
        type: 'error',
        taskId: taskId || 'unknown',
        error: 'Missing required parameters: file, taskId, blockSize',
      })
    }
  })
}

async function calculateFileSm3(file: File, taskId: string, blockSize: number) {
  const totalSize = file.size
  const blocks = Math.max(1, Math.ceil(totalSize / blockSize))
  const blockHashes: string[] = Array.from({ length: blocks })
  let processedBytes = 0

  const maxConcurrency = Math.max(2, navigator.hardwareConcurrency || 2)
  let nextBlockIndex = 0

  async function processBlock(blockIndex: number) {
    const start = blockIndex * blockSize
    const end = Math.min(start + blockSize, totalSize)
    const buffer = await file.slice(start, end).arrayBuffer()
    blockHashes[blockIndex] = sm3(new Uint8Array(buffer))
    processedBytes += buffer.byteLength
    globalThis.postMessage({
      type: 'progress',
      taskId,
      progress: totalSize > 0 ? processedBytes / totalSize : 1,
      processedBytes,
      totalSize,
    })
  }

  async function runPool() {
    const workers: Promise<void>[] = []
    const launch = async () => {
      while (nextBlockIndex < blocks) {
        const index = nextBlockIndex++
        await processBlock(index)
      }
    }
    for (let i = 0; i < Math.min(maxConcurrency, blocks); i++) {
      workers.push(launch())
    }
    await Promise.all(workers)
  }

  await runPool()

  const digest = sm3(blockHashes.join(''))
  globalThis.postMessage({
    type: 'complete',
    taskId,
    digest,
  })
}
