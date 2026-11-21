import { version } from '../../package.json'

// Display the installed shell version; release builds keep platform and both apps synchronized.
export const APP_VERSION = globalThis.navigator?.userAgent?.match(/\bLSAegisApp\/(\d+\.\d+\.\d+)(?=\s|$)/)?.[1] || version
