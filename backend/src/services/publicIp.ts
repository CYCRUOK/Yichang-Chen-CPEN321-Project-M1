export type PublicIpProvider = () => Promise<string>;

const IPIFY_URL = 'https://api64.ipify.org?format=json';
const TIMEOUT_MS = 5_000;

/** Looks up this machine's public IP (IPv4 or IPv6) via ipify. */
export const fetchPublicIp: PublicIpProvider = async () => {
  const response = await fetch(IPIFY_URL, { signal: AbortSignal.timeout(TIMEOUT_MS) });
  if (!response.ok) {
    throw new Error(`ipify responded with HTTP ${response.status}`);
  }
  const body = (await response.json()) as { ip?: unknown };
  if (typeof body.ip !== 'string' || body.ip === '') {
    throw new Error('ipify response did not contain an ip');
  }
  return body.ip;
};
