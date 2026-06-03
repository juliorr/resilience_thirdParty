#!/usr/bin/env bash
# Runs the Maven test goals inside a container, with a docker:dind sidecar so
# Testcontainers can launch DynamoDB Local. This avoids depending on a host JDK
# and works on Docker Desktop for macOS, where bind-mounting the host socket
# into a container does not expose the Docker Engine API.
set -euo pipefail

GOALS="${*:-verify}"
NET=incode-test-net
DIND=incode-dind
MVN_IMAGE=maven:3.9-eclipse-temurin-21
DIND_IMAGE=docker:27-dind
CLI_IMAGE=docker:27-cli
M2_VOLUME=incode-m2

cleanup() {
  docker rm -f "$DIND" >/dev/null 2>&1 || true
  docker network rm "$NET" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker volume create "$M2_VOLUME" >/dev/null
docker network create "$NET" >/dev/null 2>&1 || true
docker rm -f "$DIND" >/dev/null 2>&1 || true

docker run -d --privileged --name "$DIND" \
  --network "$NET" --network-alias docker \
  -e DOCKER_TLS_CERTDIR= \
  "$DIND_IMAGE" --tls=false >/dev/null

echo "Waiting for docker-in-docker daemon..."
for _ in $(seq 1 30); do
  if docker run --rm --network "$NET" -e DOCKER_HOST=tcp://docker:2375 "$CLI_IMAGE" docker info >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

# shellcheck disable=SC2086
docker run --rm \
  --network "$NET" \
  -v "$PWD":/app -v "$M2_VOLUME":/root/.m2 \
  -e DOCKER_HOST=tcp://docker:2375 \
  -e TESTCONTAINERS_HOST_OVERRIDE=docker \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -w /app "$MVN_IMAGE" mvn -B $GOALS
