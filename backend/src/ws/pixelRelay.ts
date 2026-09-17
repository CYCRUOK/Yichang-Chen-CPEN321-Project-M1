import type { IncomingMessage, Server as HttpServer } from 'node:http';
import type { Duplex } from 'node:stream';

import WebSocket, { WebSocketServer, type RawData } from 'ws';

export type PixelRelayOptions = {
  /** Course-provided pixel stream, e.g. wss://8.229.22.124 */
  upstreamUrl: string;
  /** Delay before re-dialling the upstream after it drops. */
  reconnectDelayMs?: number;
  /** Set to false only for self-signed upstreams; the course server has a valid cert. */
  rejectUnauthorized?: boolean;
  log?: (message: string) => void;
};

export type PixelRelayStatus = {
  upstreamUrl: string;
  upstreamConnected: boolean;
  clients: number;
  relayed: number;
};

/**
 * Keeps one connection to the course pixel server and fans every message out,
 * byte-for-byte and immediately, to all Android clients connected to our own
 * WebSocket endpoint. No batching, no parsing, no reformatting.
 */
export class PixelRelay {
  private readonly upstreamUrl: string;
  private readonly reconnectDelayMs: number;
  private readonly rejectUnauthorized: boolean;
  private readonly log: (message: string) => void;

  private upstream: WebSocket | null = null;
  private upstreamOpen = false;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private stopped = true;
  private relayed = 0;
  private wss: WebSocketServer | null = null;

  constructor(options: PixelRelayOptions) {
    this.upstreamUrl = options.upstreamUrl;
    this.reconnectDelayMs = options.reconnectDelayMs ?? 2_000;
    this.rejectUnauthorized = options.rejectUnauthorized ?? true;
    this.log = options.log ?? ((message) => console.log(`[pixel-relay] ${message}`));
  }

  /** Serves our own WebSocket endpoint on `path` of an existing HTTP(S) server. */
  attach(server: HttpServer, path = '/ws'): WebSocketServer {
    const wss = new WebSocketServer({ noServer: true });
    server.on('upgrade', (request: IncomingMessage, socket: Duplex, head: Buffer) => {
      const url = new URL(request.url ?? '/', 'http://localhost');
      if (url.pathname !== path) {
        socket.destroy();
        return;
      }
      wss.handleUpgrade(request, socket, head, (client) => {
        wss.emit('connection', client, request);
      });
    });
    wss.on('connection', (client, request) => {
      this.log(`client connected from ${request.socket.remoteAddress ?? '?'} (${wss.clients.size} total)`);
      client.on('close', () => this.log(`client disconnected (${wss.clients.size} total)`));
      client.on('error', (err) => this.log(`client error: ${err.message}`));
    });
    this.wss = wss;
    return wss;
  }

  /** Dials the upstream and keeps re-dialling until [stop] is called. */
  start(): void {
    this.stopped = false;
    this.connectUpstream();
  }

  async stop(): Promise<void> {
    this.stopped = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.upstream?.removeAllListeners();
    this.upstream?.terminate();
    this.upstream = null;
    this.upstreamOpen = false;
    const wss = this.wss;
    if (wss) {
      for (const client of wss.clients) client.terminate();
      await new Promise<void>((resolve) => wss.close(() => resolve()));
      this.wss = null;
    }
  }

  status(): PixelRelayStatus {
    return {
      upstreamUrl: this.upstreamUrl,
      upstreamConnected: this.upstreamOpen,
      clients: this.wss?.clients.size ?? 0,
      relayed: this.relayed,
    };
  }

  private connectUpstream(): void {
    if (this.stopped) return;
    this.log(`connecting to ${this.upstreamUrl}`);
    const ws = new WebSocket(this.upstreamUrl, {
      rejectUnauthorized: this.rejectUnauthorized,
      handshakeTimeout: 10_000,
    });
    this.upstream = ws;

    ws.on('open', () => {
      this.upstreamOpen = true;
      this.log('upstream connected');
    });
    ws.on('message', (data: RawData, isBinary: boolean) => {
      this.broadcast(data, isBinary);
    });
    ws.on('error', (err) => {
      this.log(`upstream error: ${err.message}`);
    });
    ws.on('close', (code, reason) => {
      this.upstreamOpen = false;
      this.log(`upstream closed (${code} ${reason.toString()})`);
      this.scheduleReconnect();
    });
  }

  private scheduleReconnect(): void {
    if (this.stopped || this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connectUpstream();
    }, this.reconnectDelayMs);
  }

  private broadcast(data: RawData, isBinary: boolean): void {
    const wss = this.wss;
    if (!wss) return;
    for (const client of wss.clients) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data, { binary: isBinary });
      }
    }
    this.relayed += 1;
  }
}
