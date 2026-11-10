/** ??????? */
export interface ImageCaptchaResp {
  uuid: string
  img: string
  expireTime: number
  isEnabled: boolean
}

/** ??????? */
export interface DashboardNoticeResp {
  /** ?? ID????????? Long ToStringSerializer? */
  id: string
  title: string
  /** ????? notice_type?1?????9????? */
  type: string
  isTop: boolean
  /** ???? */
  publishTime?: string
}

/** ????????? */
export interface DashboardAccessTrendResp {
  date: string
  pvCount: number
  ipCount: number
}

/** ????????? */
export interface DashboardOverviewCommonResp {
  total: number
  today: number
  growth: number
  dataList: DashboardChartCommonResp[]
}

/** ????????? */
export interface DashboardChartCommonResp {
  name: string
  value: number
}

/* ??????? */
export interface BehaviorCaptchaResp {
  originalImageBase64: string
  point: {
    x: number
    y: number
  }
  jigsawImageBase64: string
  token: string
  secretKey: string
  wordList: string[]
}

export interface BehaviorCaptchaReq {
  captchaType?: string
  captchaVerification?: string
  clientUid?: string
}

export interface CheckBehaviorCaptchaResp {
  repCode: string
  repMsg: string
}
