import express, { type Express } from 'express';

import { createInfoRouter, type InfoRouterDeps } from './routes/info';
import { fetchPublicIp } from './services/publicIp';
import { type PixelRelayStatus } from './ws/pixelRelay';

export type AppDeps = Partial<InfoRouterDeps> & {
  /** Reports the pixel relay state on GET /api/ws-status (omitted when no relay is running). */
  relayStatus?: () => PixelRelayStatus;
};

export function createApp(deps: AppDeps = {}): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.use(
    '/api',
    createInfoRouter({
      publicIpProvider: deps.publicIpProvider ?? fetchPublicIp,
      now: deps.now ?? (() => new Date()),
    }),
  );

  const relayStatus = deps.relayStatus;
  if (relayStatus) {
    app.get('/api/ws-status', (_req, res) => {
      res.json(relayStatus());
    });
  }

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
