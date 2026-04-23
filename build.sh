#!/usr/bin/env bash
# ----------------------------------------------------------------
# Smart Campus API – build & run helper
# Requirements: Java 11+, Maven 3.6+
# ----------------------------------------------------------------
set -e

echo "[1/3] Compiling and packaging..."
mvn clean package -q

echo "[2/3] Build successful."
echo "[3/3] Starting server at http://localhost:8080/api/v1"
echo "       Press ENTER to stop."
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
