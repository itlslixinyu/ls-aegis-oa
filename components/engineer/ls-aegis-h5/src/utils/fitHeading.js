/** Keep a readable minimum; exceptionally long names remain horizontally scrollable. */
export function fitHeadingSize(textWidth, currentSize, availableWidth, min = 12, max = 23) {
  if (![textWidth, currentSize, availableWidth].every(Number.isFinite) || textWidth <= 0 || availableWidth <= 0) return max
  return Math.max(min, Math.min(max, Math.floor(currentSize * Math.max(0, availableWidth - 1) / textWidth * 10) / 10))
}
