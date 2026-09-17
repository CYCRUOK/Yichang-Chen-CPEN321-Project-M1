import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';

import request from 'supertest';
import WebSocket, { WebSocketServer } from 'ws';

import { createApp } from '../../src/app';
import { PixelRelay } from '../../src/ws/pixelRelay';

/** A stand-in for the course server: emits whatever we push into it. */
class FakeUpstream {
  private wss: WebSocketServer | null = null;
  port = 0;

  async start(port = 0): Promise<void> {
    this.wss = new WebSocketServer({ port });
    await new Promise<void>((resolve) => this.wss?.once('listening', resolve));
    this.port = (this.wss.address() as AddressInfo).port;
  }

  get url(): string {
    return `ws://127.0.0.1:${this.port}`;
  }

  get connections(): number {
    return this.wss?.clients.size ?? 0;
  }

  send(text: string): void {
    for (const c of this.wss?.clients ?? []) c.send(text);
  }

  async stop(): Promise<void> {
    const wss = this.wss;
    if (!wss) return;
    for (const c of wss.clients) c.terminate();
    await new Promise<void>((resolve) => wss.close(() => resolve()));
    this.wss = null;
  }
}

function waitFor(predicate: () => boolean, timeoutMs = 3_000): Promise<void> {
  const started = Date.now();
  return new Promise((resolve, reject) => {
    const tick = (): void => {
      if (predicate()) resolve();
      else if (Date.now() - started > timeoutMs) reject(new Error('timed out waiting'));
      else setTimeout(tick, 10);
    };
    tick();
  });
}

function connectClient(server: Server, path = '/ws'): Promise<WebSocket> {
  const { port } = server.address() as AddressInfo;
  const ws = new WebSocket(`ws://127.0.0.1:${port}${path}`);
  return new Promise((resolve, reject) => {
    ws.once('open', () => resolve(ws));
    ws.once('error', reject);
  });
}

describe('Unmocked: PixelRelay', () => {
  const upstream = new FakeUpstream();
  let relay: PixelRelay;
  let server: Server;

  beforeEach(async () => {
    await upstream.start();
    relay = new PixelRelay({ upstreamUrl: upstream.url, reconnectDelayMs: 100, log: () => undefined });
    server = createServer(createApp({ relayStatus: () => relay.status() }));
    relay.attach(server, '/ws');
    await new Promise<void>((resolve) => server.listen(0, resolve));
    relay.start();
    await waitFor(() => relay.status().upstreamConnected);
  });

  afterEach(async () => {
    await relay.stop();
    await new Promise<void>((resolve) => server.close(() => resolve()));
    await upstream.stop();
  });

  // Input: upstream emits three pixel updates while one client is connected
  // Expected behavior: each message is forwarded verbatim, in order, without delay
  test('Relays each pixel update byte-for-byte and in order', async () => {
    const client = await connectClient(server);
    const received: { text: string; at: number }[] = [];
    client.on('message', (d) => received.push({ text: d.toString(), at: Date.now() }));

    const pixels = [
      '{"x":2,"y":15,"color":"#FFFFFF"}',
      '{"x":6,"y":12,"color":"#ffd23f"}',
      '{"x":0,"y":7,"color":"#3a2a1a"}',
    ];
    const sentAt = Date.now();
    for (const p of pixels) upstream.send(p);
    await waitFor(() => received.length === 3);

    expect(received.map((r) => r.text)).toEqual(pixels);
    for (const r of received) expect(r.at - sentAt).toBeLessThan(200);
    expect(relay.status().relayed).toBe(3);
    client.close();
  });

  // Input: two clients connected
  // Expected behavior: both receive every update; the relay keeps ONE upstream connection
  test('Fans out to every connected client over a single upstream connection', async () => {
    const a = await connectClient(server);
    const b = await connectClient(server);
    const gotA: string[] = [];
    const gotB: string[] = [];
    a.on('message', (d) => gotA.push(d.toString()));
    b.on('message', (d) => gotB.push(d.toString()));
    await waitFor(() => relay.status().clients === 2);

    upstream.send('{"x":1,"y":1,"color":"#000000"}');
    upstream.send('{"x":2,"y":2,"color":"#111111"}');
    await waitFor(() => gotA.length === 2 && gotB.length === 2);

    expect(gotA).toEqual(gotB);
    expect(upstream.connections).toBe(1);
    a.close();
    b.close();
  });

  // Input: the upstream connection drops and comes back
  // Expected behavior: the relay reconnects by itself and clients keep receiving
  test('Reconnects to the upstream after it drops', async () => {
    const client = await connectClient(server);
    const got: string[] = [];
    client.on('message', (d) => got.push(d.toString()));

    const port = upstream.port;
    await upstream.stop();
    await waitFor(() => !relay.status().upstreamConnected);

    await upstream.start(port);
    await waitFor(() => relay.status().upstreamConnected, 5_000);
    upstream.send('{"x":9,"y":9,"color":"#abcdef"}');
    await waitFor(() => got.length === 1);

    expect(got).toEqual(['{"x":9,"y":9,"color":"#abcdef"}']);
    client.close();
  });

  // Input: WebSocket upgrade on a path other than /ws
  // Expected behavior: the socket is refused
  test('Rejects upgrades on unknown paths', async () => {
    await expect(connectClient(server, '/nope')).rejects.toBeDefined();
  });

  // Interface GET /api/ws-status
  // Expected output: { upstreamUrl, upstreamConnected: true, clients: 0, relayed: 0 }
  test('GET /api/ws-status reports the relay state', async () => {
    const response = await request(server).get('/api/ws-status');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({
      upstreamUrl: upstream.url,
      upstreamConnected: true,
      clients: 0,
      relayed: 0,
    });
  });
});

// Interface GET /api/ws-status without a relay configured
describe('Unmocked: GET /api/ws-status without relay', () => {
  test('Is not routed', async () => {
    const response = await request(createApp()).get('/api/ws-status');
    expect(response.status).toBe(404);
  });
});
