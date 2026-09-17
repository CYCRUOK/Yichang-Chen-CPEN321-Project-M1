import request from 'supertest';

import { createApp } from '../../src/app';
import { env } from '../../src/config/env';

const TIME_FORMAT = /^\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}$/;

// Interface GET /api/server-time
describe('Unmocked: GET /api/server-time', () => {
  // Input: GET request
  // Expected status code: 200
  // Expected behavior: current server local time is formatted as hh:mm:ss GMT+hh:mm
  // Expected output: { time: string matching /^\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}$/ }
  test('Returns current time in required format', async () => {
    const response = await request(createApp()).get('/api/server-time');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ time: expect.stringMatching(TIME_FORMAT) });
  });

  // Input: GET request
  // Expected behavior: GMT offset matches the process time zone
  test('Offset matches the process time zone', async () => {
    const response = await request(createApp()).get('/api/server-time');
    const offsetPart = (response.body as { time: string }).time.split(' ')[1];

    const minutes = -new Date().getTimezoneOffset();
    const sign = minutes < 0 ? '-' : '+';
    const abs = Math.abs(minutes);
    const expected = `GMT${sign}${String(Math.floor(abs / 60)).padStart(2, '0')}:${String(abs % 60).padStart(2, '0')}`;

    expect(offsetPart).toBe(expected);
  });
});

// Interface GET /api/name
describe('Unmocked: GET /api/name', () => {
  // Input: GET request
  // Expected status code: 200
  // Expected behavior: developer name from OWNER_FIRST_NAME / OWNER_LAST_NAME is returned
  // Expected output: { first: string, last: string }, both non-empty
  test('Returns first and last name', async () => {
    const response = await request(createApp()).get('/api/name');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ first: env.ownerFirstName, last: env.ownerLastName });
    expect(response.body.first).not.toBe('');
    expect(response.body.last).not.toBe('');
  });
});

// Interface GET /api/server-ip (real network)
describe('Unmocked: GET /api/server-ip', () => {
  // Input: GET request; real ipify lookup (or SERVER_PUBLIC_IP override)
  // Expected status code: 200 (or 502 when offline)
  // Expected behavior: a plausible IPv4/IPv6 literal is returned
  test('Returns a public IP literal', async () => {
    const response = await request(createApp()).get('/api/server-ip');

    if (response.status === 502) {
      // No internet in the test environment; failure path is covered in tests/mock.
      expect(response.body).toEqual({ error: expect.any(String) });
      return;
    }
    expect(response.status).toBe(200);
    expect(response.body.ip).toMatch(/^(\d{1,3}(\.\d{1,3}){3}|[0-9a-fA-F:]+)$/);
  });
});
