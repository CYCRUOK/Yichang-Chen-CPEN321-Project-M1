import { formatGmtOffset, formatLocalTime, localOffsetMinutes } from '../../src/services/time';

const TIME_FORMAT = /^\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}$/;

describe('formatGmtOffset', () => {
  test('positive whole-hour offset', () => {
    expect(formatGmtOffset(480)).toBe('GMT+08:00');
  });

  test('negative whole-hour offset', () => {
    expect(formatGmtOffset(-420)).toBe('GMT-07:00');
  });

  test('zero offset is rendered as +00:00', () => {
    expect(formatGmtOffset(0)).toBe('GMT+00:00');
  });

  test('half-hour and quarter-hour offsets', () => {
    expect(formatGmtOffset(330)).toBe('GMT+05:30');
    expect(formatGmtOffset(345)).toBe('GMT+05:45');
    expect(formatGmtOffset(-210)).toBe('GMT-03:30');
  });
});

describe('formatLocalTime', () => {
  // 2026-09-17T23:59:58Z
  const instant = new Date(Date.UTC(2026, 8, 17, 23, 59, 58));

  test('formats as hh:mm:ss GMT+hh:mm in 24-hour clock', () => {
    const out = formatLocalTime(instant, 0);
    expect(out).toBe('23:59:58 GMT+00:00');
    expect(out).toMatch(TIME_FORMAT);
  });

  test('shifts wall-clock time by the offset and wraps past midnight', () => {
    expect(formatLocalTime(instant, 480)).toBe('07:59:58 GMT+08:00');
  });

  test('negative offset shifts backwards', () => {
    expect(formatLocalTime(instant, -420)).toBe('16:59:58 GMT-07:00');
  });

  test('zero-pads single-digit hours', () => {
    const early = new Date(Date.UTC(2026, 0, 1, 3, 4, 5));
    expect(formatLocalTime(early, 0)).toBe('03:04:05 GMT+00:00');
  });

  test('half-hour offset', () => {
    expect(formatLocalTime(instant, 330)).toBe('05:29:58 GMT+05:30');
  });
});

describe('localOffsetMinutes', () => {
  test('is the negation of Date#getTimezoneOffset', () => {
    const d = new Date();
    expect(localOffsetMinutes(d)).toBe(-d.getTimezoneOffset());
  });
});
