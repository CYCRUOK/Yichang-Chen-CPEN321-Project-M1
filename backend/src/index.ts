import { createServer } from 'node:http';

import { createApp } from './app';
import { env } from './config/env';
import { PixelRelay } from './ws/pixelRelay';

const relay = new PixelRelay({
  upstreamUrl: env.upstreamWsUrl,
  rejectUnauthorized: env.upstreamWsRejectUnauthorized,
});

const app = createApp({ relayStatus: () => relay.status() });
const server = createServer(app);
relay.attach(server, '/ws');
relay.start();

server.listen(env.port, () => {
  console.log(`Server listening on port ${env.port} (ws endpoint: /ws -> ${env.upstreamWsUrl})`);
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    void relay.stop().then(() => {
      server.close(() => {
        process.exit(0);
      });
    });
  });
}
