import type { JwtClaims } from '../types';

/** Base64url-decode a JWT payload segment into its claims, or null if malformed. */
export function getClaims(token: string | null | undefined): JwtClaims | null {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    return JSON.parse(json) as JwtClaims;
  } catch {
    return null;
  }
}

/** True when the token is missing, malformed, or past its `exp` (with a small skew). */
export function isExpired(token: string | null | undefined, skewSeconds = 10): boolean {
  const claims = getClaims(token);
  if (!claims?.exp) return true;
  const nowSeconds = Math.floor(Date.now() / 1000);
  return claims.exp <= nowSeconds + skewSeconds;
}

/** Seconds remaining until expiry (0 if expired/invalid). */
export function secondsUntilExpiry(token: string | null | undefined): number {
  const claims = getClaims(token);
  if (!claims?.exp) return 0;
  return Math.max(0, claims.exp - Math.floor(Date.now() / 1000));
}
