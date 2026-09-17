const MS_PER_MINUTE = 60_000;

function pad2(n: number): string {
  return n.toString().padStart(2, '0');
}

/**
 * Formats a UTC offset in minutes as `GMT+hh:mm` / `GMT-hh:mm`.
 * `offsetMinutes` is minutes EAST of GMT (e.g. +480 for GMT+08:00, -420 for GMT-07:00).
 */
export function formatGmtOffset(offsetMinutes: number): string {
  const sign = offsetMinutes < 0 ? '-' : '+';
  const abs = Math.abs(offsetMinutes);
  return `GMT${sign}${pad2(Math.floor(abs / 60))}:${pad2(abs % 60)}`;
}

/**
 * Formats `date` as local wall-clock time in the given offset: `hh:mm:ss GMT+hh:mm` (24-hour).
 * Pure: does not depend on the process time zone.
 */
export function formatLocalTime(date: Date, offsetMinutes: number): string {
  const shifted = new Date(date.getTime() + offsetMinutes * MS_PER_MINUTE);
  const hh = pad2(shifted.getUTCHours());
  const mm = pad2(shifted.getUTCMinutes());
  const ss = pad2(shifted.getUTCSeconds());
  return `${hh}:${mm}:${ss} ${formatGmtOffset(offsetMinutes)}`;
}

/** Minutes east of GMT for the process's local time zone at `date`. */
export function localOffsetMinutes(date: Date): number {
  // Date#getTimezoneOffset returns minutes WEST of GMT, so flip the sign.
  return -date.getTimezoneOffset();
}
