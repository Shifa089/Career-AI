#!/bin/bash
# LocalStack init hook: provision the resumes S3 bucket with CORS for the dev frontend.
set -euo pipefail

BUCKET="${AWS_S3_BUCKET:-careerai-resumes}"

echo "[localstack-init] Creating S3 bucket: ${BUCKET}"
awslocal s3api create-bucket --bucket "${BUCKET}" || true

echo "[localstack-init] Applying CORS configuration for http://localhost:3000"
awslocal s3api put-bucket-cors --bucket "${BUCKET}" --cors-configuration '{
  "CORSRules": [
    {
      "AllowedOrigins": ["http://localhost:3000"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3000
    }
  ]
}'

echo "[localstack-init] Done."
