import express, { type Express } from 'express';

import { createInfoRouter, type InfoRouterDeps } from './routes/info';
import { fetchPublicIp } from './services/publicIp';

export type AppDeps = Partial<InfoRouterDeps>;

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

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
