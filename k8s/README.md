# CareerAI — Kubernetes manifests

Self-hosted reference deployment for the CareerAI platform. For production, replace
the in-cluster Postgres/Redis with managed services (RDS/Aurora + ElastiCache + MSK)
and source secrets from External Secrets (AWS Secrets Manager / Vault) rather than the
`secrets.yaml` template.

## Prerequisites

- A cluster with the **nginx ingress controller** and **cert-manager** installed
  (the ingress references a `letsencrypt-prod` ClusterIssuer).
- **metrics-server** installed (required by the HPAs).
- An image pull secret named `ghcr-pull` in the `careerai` namespace:

  ```bash
  kubectl -n careerai create secret docker-registry ghcr-pull \
    --docker-server=ghcr.io \
    --docker-username=<github-user> \
    --docker-password=<github-token>
  ```

## Apply order

```bash
kubectl apply -f namespace.yaml
kubectl apply -f secrets.yaml          # after filling in real values
kubectl apply -f configmap.yaml
kubectl apply -f postgres/statefulset.yaml
kubectl apply -f redis/deployment.yaml
kubectl apply -f auth-service/ -f resume-service/ -f interview-service/ \
               -f job-match-service/ -f api-gateway/ -f frontend/
kubectl apply -f hpa.yaml
kubectl apply -f ingress.yaml
```

## Notes

- Each service reads config from the `careerai-config` ConfigMap and secrets from the
  `careerai-secrets` Secret via `envFrom`. `DB_HOST=postgres` / `DB_PORT=5432`; the
  per-service database name (`auth_db`, `resume_db`, …) is baked into each service's
  JDBC URL and created by `postgres-initdb`.
- `api-gateway` is exposed via a `LoadBalancer` Service and the ingress; the four
  domain services are `ClusterIP` only.
- Kafka and the Eureka/Config servers are expected to run in-cluster or as managed
  endpoints; point `KAFKA_BOOTSTRAP_SERVERS` / `EUREKA_URL` / `CONFIG_SERVER_URL` in
  the ConfigMap accordingly.
