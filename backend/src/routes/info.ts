import { Router } from 'express';

import { env } from '../config/env';
import { type PublicIpProvider } from '../services/publicIp';
import { formatLocalTime, localOffsetMinutes } from '../services/time';

export type InfoRouterDeps = {
  publicIpProvider: PublicIpProvider;
  now: () => Date;
};

export function createInfoRouter(deps: InfoRouterDeps): Router {
  const router = Router();

  // GET /api/server-ip -> { ip: "142.250.217.110" }
  router.get('/server-ip', async (_req, res) => {
    if (env.serverPublicIp !== undefined) {
      res.json({ ip: env.serverPublicIp });
      return;
    }
    try {
      const ip = await deps.publicIpProvider();
      res.json({ ip });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'unknown error';
      res.status(502).json({ error: `Could not determine server public IP: ${message}` });
    }
  });

  // GET /api/server-time -> { time: "12:18:22 GMT+01:00" }  (24-hour, at time of call)
  router.get('/server-time', (_req, res) => {
    const now = deps.now();
    res.json({ time: formatLocalTime(now, localOffsetMinutes(now)) });
  });

  // GET /api/name -> { first: "...", last: "..." }
  router.get('/name', (_req, res) => {
    res.json({ first: env.ownerFirstName, last: env.ownerLastName });
  });

  return router;
}
