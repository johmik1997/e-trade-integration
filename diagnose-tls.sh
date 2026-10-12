#!/usr/bin/env bash
# Shows how e-Trade's TLS chain looks from this machine, and whether the JDK trusts it.
# Run this on a network that can reach the portal.
host="${1:-etrade.gov.et}"

echo "=== certificates the server actually sends ==="
# A healthy server sends leaf + intermediate(s). Just "0 s:" and nothing else means an
# incomplete chain: the JDK cannot complete it, though browsers paper over it via AIA.
openssl s_client -connect "$host:443" -servername "$host" -showcerts </dev/null 2>/dev/null \
  | grep -E '^ *[0-9]+ s:|^ *[0-9]+ i:'

echo
echo "=== openssl's own verdict ==="
openssl s_client -connect "$host:443" -servername "$host" </dev/null 2>/dev/null \
  | grep -E 'Verify return code|Verification'

echo
echo "=== the leaf's issuer and its AIA (where the missing intermediate lives) ==="
openssl s_client -connect "$host:443" -servername "$host" </dev/null 2>/dev/null \
  | openssl x509 -noout -issuer -subject -ext authorityInfoAccess 2>/dev/null

echo
echo "=== what the JDK makes of it ==="
keytool -printcert -sslserver "$host:443" -rfc 2>&1 | grep -cE 'BEGIN CERTIFICATE' \
  | xargs -I{} echo "certificates visible to keytool: {}"
