import request from 'supertest';

import { createApp } from '../../src/app';

// Interface GET /api/server-ip
describe('Mocked: GET /api/server-ip', () => {
  // Input: GET request; public-IP provider resolves with an IPv4 address
  // Expected status code: 200
  // Expected behavior: provider is called once and its value is returned
  // Expected output: { ip: "142.250.217.110" }
  test('Provider succeeds with IPv4', async () => {
    const publicIpProvider = jest.fn<Promise<string>, []>().mockResolvedValue('142.250.217.110');
    const response = await request(createApp({ publicIpProvider })).get('/api/server-ip');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ ip: '142.250.217.110' });
    expect(publicIpProvider).toHaveBeenCalledTimes(1);
  });

  // Input: GET request; provider resolves with an IPv6 address
  // Expected status code: 200
  // Expected output: { ip: "2001:db8::1" }
  test('Provider succeeds with IPv6', async () => {
    const publicIpProvider = jest.fn<Promise<string>, []>().mockResolvedValue('2001:db8::1');
    const response = await request(createApp({ publicIpProvider })).get('/api/server-ip');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ ip: '2001:db8::1' });
  });

  // Input: GET request; provider rejects (e.g. ipify unreachable / timeout)
  // Expected status code: 502
  // Expected behavior: error is caught and reported; server does not crash
  // Expected output: { error: string containing the provider message }
  test('Provider fails', async () => {
    const publicIpProvider = jest.fn<Promise<string>, []>().mockRejectedValue(new Error('ipify timeout'));
    const response = await request(createApp({ publicIpProvider })).get('/api/server-ip');

    expect(response.status).toBe(502);
    expect(response.body).toEqual({ error: expect.stringContaining('ipify timeout') });
  });
});

// Interface GET /api/server-time (clock injected)
describe('Mocked: GET /api/server-time', () => {
  // Input: GET request with a fixed clock
  // Expected status code: 200
  // Expected behavior: returned time is the injected instant in the server local zone
  // Expected output: { time: "hh:mm:ss GMT+hh:mm" } matching the fixed instant
  test('Uses injected clock', async () => {
    const fixed = new Date(Date.UTC(2026, 8, 17, 12, 34, 56));
    const response = await request(createApp({ now: () => fixed })).get('/api/server-time');

    const offset = -fixed.getTimezoneOffset();
    const shifted = new Date(fixed.getTime() + offset * 60_000);
    const hh = shifted.getUTCHours().toString().padStart(2, '0');
    const mm = shifted.getUTCMinutes().toString().padStart(2, '0');

    expect(response.status).toBe(200);
    expect(response.body.time).toMatch(new RegExp(`^${hh}:${mm}:56 GMT[+-]\\d{2}:\\d{2}$`));
  });
});
