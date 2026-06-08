#!/bin/sh
set -eu

BASE_URL="${BASE_URL:-http://app:8080}"
REQUESTS="${REQUESTS:-200}"
AUTH="${AUTH:-verifier:verifier-pass}"
SLEEP_MS="${SLEEP_MS:-0}"

set -- CJ 28 OY 43 0A XQ Ramirez Allen Reese ZZZNOMATCH
query_count=$#

http_2xx=0
http_4xx=0
http_5xx=0
http_other=0

echo "Sending ${REQUESTS} requests to ${BASE_URL}/backend-service (auth ${AUTH%%:*})"

i=0
while [ "$i" -lt "$REQUESTS" ]; do
    idx=$(( i % query_count + 1 ))
    eval "query=\${$idx}"
    id="$(cat /proc/sys/kernel/random/uuid)"

    code="$(curl -s -o /dev/null -w '%{http_code}' -u "$AUTH" \
        "${BASE_URL}/backend-service?verificationId=${id}&query=${query}")"

    case "$code" in
        2*) http_2xx=$(( http_2xx + 1 )) ;;
        4*) http_4xx=$(( http_4xx + 1 )) ;;
        5*) http_5xx=$(( http_5xx + 1 )) ;;
        *)  http_other=$(( http_other + 1 )) ;;
    esac

    i=$(( i + 1 ))
    if [ $(( i % 50 )) -eq 0 ]; then
        echo "  ${i}/${REQUESTS} sent (2xx=${http_2xx} 4xx=${http_4xx} 5xx=${http_5xx})"
    fi
    if [ "$SLEEP_MS" -gt 0 ]; then
        sleep "$(awk "BEGIN { print ${SLEEP_MS} / 1000 }")"
    fi
done

echo ""
echo "Done. Total=${REQUESTS} | 2xx=${http_2xx} 4xx=${http_4xx} 5xx=${http_5xx} other=${http_other}"
echo ""
echo "The FREE/PREMIUM 503 simulation is absorbed by the fallback, so it is not visible in the"
echo "response body. Observe the real failure rates in Prometheus (http://localhost:9090):"
echo "  FREE    ~0.40 : sum(thirdparty_calls_total{source=\"free\",outcome=\"unavailable\"}) / sum(thirdparty_calls_total{source=\"free\"})"
echo "  PREMIUM ~0.10 : sum(thirdparty_calls_total{source=\"premium\",outcome=\"unavailable\"}) / sum(thirdparty_calls_total{source=\"premium\"})"
