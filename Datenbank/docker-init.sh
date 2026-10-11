#!/bin/bash
set -euo pipefail
# Mongo führt dies nur für ein neues Datenvolume aus.
cd /seed
mongosh --quiet mongodb://127.0.0.1:27017/tischreservierung --file console.js
