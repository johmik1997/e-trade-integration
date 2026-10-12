#!/usr/bin/env bash
# Builds a trust store that completes e-Trade's certificate chain.
#
# The portal serves an incomplete chain, so the JDK cannot validate it ("unable to find
# valid certification path"). Browsers hide this by fetching the missing intermediate from
# the certificate's AIA extension; the JDK does not do that unless told to.
#
# This script starts from the JDK's own cacerts — so every other host keeps validating
# normally — and adds the certificates the portal fails to send.
#
#   ./setup-truststore.sh [host] [output]
set -euo pipefail

host="${1:-etrade.gov.et}"
out="${2:-src/main/resources/etrade-truststore.p12}"
password="${TRUSTSTORE_PASSWORD:-changeit}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

java_home="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}"
cacerts="$java_home/lib/security/cacerts"
[ -f "$cacerts" ] || { echo "Could not find cacerts at $cacerts; set JAVA_HOME" >&2; exit 1; }

echo "Fetching the chain $host actually serves..."
openssl s_client -connect "$host:443" -servername "$host" -showcerts </dev/null 2>/dev/null \
  | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/' > "$work/served.pem"
[ -s "$work/served.pem" ] || { echo "No certificates returned by $host:443" >&2; exit 1; }

# Split the served chain into one file per certificate.
csplit -s -z -f "$work/served-" -b '%02d.pem' "$work/served.pem" '/BEGIN CERTIFICATE/' '{*}'
served_count=$(ls "$work"/served-*.pem | wc -l)
echo "  $host sent $served_count certificate(s)."

# Follow the leaf's AIA caIssuers link to recover the intermediate it omitted.
aia=$(openssl x509 -in "$work/served-00.pem" -noout -ext authorityInfoAccess 2>/dev/null \
  | sed -n 's/.*CA Issuers - URI://p' | head -1 | tr -d '[:space:]')
if [ -n "$aia" ]; then
  echo "Downloading the issuer certificate from $aia"
  if curl -fsS -o "$work/issuer.bin" "$aia"; then
    # AIA serves DER more often than PEM; try DER first, then fall back.
    openssl x509 -inform DER -in "$work/issuer.bin" -out "$work/issuer.pem" 2>/dev/null \
      || openssl x509 -in "$work/issuer.bin" -out "$work/issuer.pem" 2>/dev/null \
      || { echo "  Could not parse the downloaded issuer certificate; skipping." >&2; rm -f "$work/issuer.pem"; }
  else
    echo "  Download failed; skipping." >&2
  fi
else
  echo "The leaf certificate advertises no AIA caIssuers URL."
fi

echo "Copying the JDK trust store so other hosts keep validating..."
rm -f "$out"
mkdir -p "$(dirname "$out")"
keytool -importkeystore -noprompt \
  -srckeystore "$cacerts" -srcstorepass changeit -srcstoretype JKS \
  -destkeystore "$out" -deststorepass "$password" -deststoretype PKCS12 >/dev/null 2>&1

added=0
seen=""
# Import every CA certificate we have: the served intermediates (skipping the leaf, which
# is not a CA) and whatever AIA gave us. The AIA copy is usually also in the served chain,
# so de-duplicate by fingerprint.
for cert in "$work"/served-*.pem "$work/issuer.pem"; do
  [ -f "$cert" ] || continue
  [ "$cert" = "$work/served-00.pem" ] && continue
  fingerprint=$(openssl x509 -in "$cert" -noout -fingerprint -sha256 | cut -d= -f2)
  case "$seen" in *"$fingerprint"*) continue ;; esac
  seen="$seen $fingerprint"
  subject=$(openssl x509 -in "$cert" -noout -subject | sed 's/^subject=//')
  alias="etrade-ca-$added"
  if keytool -importcert -noprompt -alias "$alias" -file "$cert" \
      -keystore "$out" -storepass "$password" -storetype PKCS12 >/dev/null 2>&1; then
    echo "  added: $subject"
    added=$((added + 1))
  fi
done

if [ "$added" -eq 0 ]; then
  echo
  echo "Nothing was added — the chain looks complete, so the trust failure has another cause." >&2
  echo "Run ../diagnose-tls.sh and check whether the portal is signed by a CA the JDK" >&2
  echo "does not ship at all, in which case you need that root certificate from its issuer." >&2
  exit 1
fi

echo
echo "Wrote $out ($added certificate(s) added)."
cat <<YAML

Now point the service at it, in application.yml:

spring:
  ssl:
    bundle:
      jks:
        etrade:
          truststore:
            location: classpath:$(basename "$out")
            password: $password
            type: PKCS12

etrade:
  ssl-bundle: etrade
YAML
