#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
chmod +x mvnw
echo "Starting GroupBookingLA on http://localhost:8080"
echo "Sign in: Admin@rathin.com / R@t@2023"
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
