import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

function optional(name: string): string | undefined {
  const value = process.env[name];
  return value === undefined || value.trim() === '' ? undefined : value.trim();
}

export const env = {
  port,
  /** If set, returned by GET /api/server-ip instead of querying ipify. */
  serverPublicIp: optional('SERVER_PUBLIC_IP'),
  /** Developer's name, returned by GET /api/name. */
  ownerFirstName: optional('OWNER_FIRST_NAME') ?? 'First',
  ownerLastName: optional('OWNER_LAST_NAME') ?? 'Last',
} as const;
